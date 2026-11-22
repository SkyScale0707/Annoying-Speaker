<#
.SYNOPSIS
  MineCraft 26.3 源码检索器（clientOnly / common 双源码树）。

.DESCRIPTION
  把 .gradle\loom-cache 里的 sources jar 解包到 .agent\.cache\src 下，并提供
  查找类、正则检索、按包统计、定位锚点类等只读操作。
  本脚本只读取 Minecraft 源码，绝不修改 .gradle 内的任何文件。

  调用方式（Windows PowerShell 5.1，注意必须显式放开执行策略）：
    powershell -NoProfile -ExecutionPolicy Bypass -File "<本文件>" <Action> [参数]

  Action:
    ensure            解包/校验缓存（jar 变了会自动重解）
    find    <正则>    按文件名/类名找文件（可 -Side）
    grep    <正则>    全文正则检索，输出 相对路径:行号: 内容
    show    <路径>    带行号打印文件，-From/-To 截取
    tree    [包路径]  统计各子包 java 文件数
    stats             双树总览 + 顶层包分布
    anchors           关键入口类的行数与路径（回答"从哪开始读"）

.EXAMPLE
  ... -File mc-src.ps1 ensure
.EXAMPLE
  ... -File mc-src.ps1 grep "class ClientLevel" -Side client
.EXAMPLE
  ... -File mc-src.ps1 find PlaySound
.EXAMPLE
  ... -File mc-src.ps1 show net/minecraft/world/item/Item.java -From 1 -To 40
.EXAMPLE
  ... -File mc-src.ps1 tree net/minecraft/client/gui -Depth 1
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Action = 'help',

    [Parameter(Position = 1)]
    [string]$Query,

    [ValidateSet('common', 'client', 'both')]
    [string]$Side = 'both',

    [int]$Max = 40,
    [int]$Depth = 1,
    [int]$From = 1,
    [int]$To = 0,
    [switch]$CaseSensitive
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

# ---- 路径推导：<repo>\.agent\.skill\scripts\mc-src.ps1 -> <repo> ----
$skillDir = Split-Path -Parent $PSScriptRoot          # .agent\.skill
$agentDir = Split-Path -Parent $skillDir              # .agent
$repoRoot = Split-Path -Parent $agentDir              # 仓库根（模组工程）
$cacheRoot = Join-Path $agentDir '.cache\src'
$jarRoot = Join-Path $repoRoot '.gradle\loom-cache\minecraftMaven\net\minecraft'

function Get-SideRoot([string]$s) { Join-Path $cacheRoot $s }

function Get-SourceJars {
    if (-not (Test-Path $jarRoot)) { throw "找不到源码 jar 目录: $jarRoot" }
    $map = @{}
    Get-ChildItem -Path $jarRoot -Recurse -File -Filter '*-sources.jar' | ForEach-Object {
        if ($_.Name -like '*clientOnly*') { $map['client'] = $_.FullName }
        elseif ($_.Name -like '*common*') { $map['common'] = $_.FullName }
    }
    if ($map.Count -eq 0) { throw "在 $jarRoot 下没有找到 *-sources.jar" }
    return $map
}

function Ensure-Cache {
    $jars = Get-SourceJars
    $report = @()
    foreach ($side in @('common', 'client')) {
        if (-not $jars.ContainsKey($side)) { continue }
        $jar = $jars[$side]
        $hash = (Get-FileHash -Path $jar -Algorithm SHA256).Hash
        $dest = Get-SideRoot $side
        $stamp = Join-Path $dest '.source-stamp.txt'
        $ok = (Test-Path $dest) -and (Test-Path $stamp) -and ((Get-Content $stamp -Raw).Trim() -eq $hash)
        if (-not $ok) {
            if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
            New-Item -ItemType Directory -Path $dest -Force | Out-Null
            [System.IO.Compression.ZipFile]::ExtractToDirectory($jar, $dest)
            Set-Content -Path $stamp -Value $hash -Encoding ASCII
            $state = 'extracted'
        }
        else { $state = 'cached' }
        $count = (Get-ChildItem -Path $dest -Recurse -File -Filter *.java).Count
        $report += [pscustomobject]@{ Side = $side; State = $state; JavaFiles = $count; Jar = $jar; Sha256 = $hash.Substring(0, 16) }
    }
    return $report
}

function Resolve-Sides {
    switch ($Side) {
        'common' { return @('common') }
        'client' { return @('client') }
        default { return @('common', 'client') }
    }
}

function To-Rel([string]$root, [string]$full) {
    return ($full.Substring($root.Length + 1) -replace '\\', '/')
}

switch ($Action.ToLower()) {
    'ensure' {
        Ensure-Cache | Format-Table -AutoSize
    }

    'stats' {
        Ensure-Cache | Format-Table -AutoSize
        foreach ($s in Resolve-Sides) {
            $root = Get-SideRoot $s
            $files = Get-ChildItem -Path $root -Recurse -File -Filter *.java
            "`n=== $s : $($files.Count) java files ==="
            $files | ForEach-Object { To-Rel $root $_.FullName } |
                ForEach-Object { $p = $_ -split '/'; if ($p.Count -ge 4) { $p[0..2] -join '/' } elseif ($p.Count -ge 2) { $p[0..($p.Count - 2)] -join '/' } else { '(root)' } } |
                Group-Object | Sort-Object Count -Descending | Select-Object -First 25 |
                ForEach-Object { "  {0,-46} {1}" -f $_.Name, $_.Count }
        }
    }

    'tree' {
        Ensure-Cache | Out-Null
        foreach ($s in Resolve-Sides) {
            $root = Get-SideRoot $s
            $target = if ($Query) { Join-Path $root ($Query -replace '/', '\') } else { $root }
            if (-not (Test-Path $target)) { "  [$s] 没有这个路径: $Query"; continue }
            $direct = (Get-ChildItem -Path $target -File -Filter *.java -ErrorAction SilentlyContinue).Count
            "== [$s] $Query  （本级 java: $direct）"
            Get-ChildItem -Path $target -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $n = (Get-ChildItem -Path $_.FullName -Recurse -File -Filter *.java).Count
                [pscustomobject]@{ Dir = $_.Name; Files = $n }
            } | Sort-Object Files -Descending | ForEach-Object { "   {0,-34} {1}" -f $_.Dir, $_.Files }
        }
    }

    'find' {
        if (-not $Query) { throw 'find 需要一个正则参数，例如: find ClientLevel' }
        Ensure-Cache | Out-Null
        $re = if ($CaseSensitive) { [regex]::new($Query) } else { [regex]::new($Query, 'IgnoreCase') }
        $hits = 0
        foreach ($s in Resolve-Sides) {
            $root = Get-SideRoot $s
            Get-ChildItem -Path $root -Recurse -File -Filter *.java |
                Where-Object { $re.IsMatch($_.Name) } |
                Sort-Object Name |
                ForEach-Object {
                    $hits++
                    if ($hits -le $Max) { "[{0}] {1}" -f $s, (To-Rel $root $_.FullName) }
                }
        }
        "`n共 $hits 个文件（显示前 $([math]::Min($hits,$Max)) 个）"
    }

    'grep' {
        if (-not $Query) { throw 'grep 需要一个正则参数' }
        Ensure-Cache | Out-Null
        $total = 0
        foreach ($s in Resolve-Sides) {
            $root = Get-SideRoot $s
            $res = Get-ChildItem -Path $root -Recurse -File -Filter *.java |
                Select-String -Pattern $Query -CaseSensitive:$CaseSensitive
            if (-not $res) { continue }
            $total += @($res).Count
            "=== [$s] 命中 $(@($res).Count) 行（显示前 $([math]::Min(@($res).Count,$Max))）==="
            @($res) | Select-Object -First $Max | ForEach-Object {
                $rel = To-Rel $root $_.Path
                "{0}:{1}: {2}" -f $rel, $_.LineNumber, $_.Line.Trim()
            }
        }
        if ($total -eq 0) { "没有命中: $Query" } else { "`n共 $total 行命中" }
    }

    'show' {
        if (-not $Query) { throw 'show 需要一个相对路径，例如: show net/minecraft/server/MinecraftServer.java' }
        Ensure-Cache | Out-Null
        $rel = ($Query -replace '/', '\')
        $found = $null
        foreach ($s in Resolve-Sides) {
            $p = Join-Path (Get-SideRoot $s) $rel
            if (Test-Path $p) { $found = $p; $sideName = $s; break }
        }
        if (-not $found) { throw "两个源码树里都没有: $Query" }
        $lines = Get-Content -LiteralPath $found
        $end = if ($To -gt 0) { $To } else { $lines.Count }
        "--- [$sideName] $($rel -replace '\\','/')  （共 $($lines.Count) 行，显示 $From-$end）---"
        for ($i = $From; $i -le [math]::Min($end, $lines.Count); $i++) {
            "{0,5}: {1}" -f $i, $lines[$i - 1]
        }
    }

    'anchors' {
        Ensure-Cache | Out-Null
        $anchors = @(
            @('common', 'net/minecraft/server/Main.java', '专用服务端启动 main'),
            @('common', 'net/minecraft/server/MinecraftServer.java', '服务端主循环/总调度'),
            @('common', 'net/minecraft/server/dedicated/DedicatedServer.java', '专用服务端实现'),
            @('common', 'net/minecraft/server/players/PlayerList.java', '玩家连接/上下线/广播'),
            @('common', 'net/minecraft/server/level/ServerLevel.java', '服务端世界'),
            @('common', 'net/minecraft/server/level/ServerChunkCache.java', '服务端区块调度'),
            @('common', 'net/minecraft/server/level/ServerPlayer.java', '服务端玩家实体'),
            @('common', 'net/minecraft/server/network/ServerGamePacketListenerImpl.java', '服务端收包入口'),
            @('common', 'net/minecraft/world/level/Level.java', '世界抽象基类'),
            @('common', 'net/minecraft/world/entity/Entity.java', '实体基类'),
            @('common', 'net/minecraft/world/entity/LivingEntity.java', '生物基类'),
            @('common', 'net/minecraft/world/item/Item.java', '物品基类'),
            @('common', 'net/minecraft/world/item/ItemStack.java', '物品堆'),
            @('common', 'net/minecraft/world/level/block/Block.java', '方块基类'),
            @('common', 'net/minecraft/world/level/block/state/BlockBehaviour.java', '方块行为/属性'),
            @('common', 'net/minecraft/core/registries/BuiltInRegistries.java', '内置注册表入口'),
            @('common', 'net/minecraft/core/registries/Registries.java', '注册表键集合'),
            @('common', 'net/minecraft/resources/Identifier.java', '资源标识符（原 ResourceLocation）'),
            @('common', 'net/minecraft/world/item/Items.java', '全部物品注册（索引表）'),
            @('common', 'net/minecraft/world/level/block/Blocks.java', '全部方块注册（索引表）'),
            @('common', 'net/minecraft/sounds/SoundEvents.java', '全部声音事件（索引表）'),
            @('common', 'net/minecraft/world/entity/EntityType.java', '全部实体类型（索引表）'),
            @('client', 'net/minecraft/client/main/Main.java', '客户端启动 main'),
            @('client', 'net/minecraft/client/Minecraft.java', '客户端主对象/主循环'),
            @('client', 'net/minecraft/client/renderer/GameRenderer.java', '世界渲染总入口'),
            @('client', 'net/minecraft/client/renderer/LevelRenderer.java', '世界几何渲染'),
            @('client', 'net/minecraft/client/gui/Gui.java', 'HUD 之外的常驻界面调度'),
            @('client', 'net/minecraft/client/gui/Hud.java', 'HUD 绘制'),
            @('client', 'net/minecraft/client/gui/GuiGraphicsExtractor.java', '新 GUI 绘制 API（原 GuiGraphics）'),
            @('client', 'net/minecraft/client/gui/screens/Screen.java', '界面基类'),
            @('client', 'net/minecraft/client/gui/components/AbstractWidget.java', '控件基类'),
            @('client', 'net/minecraft/client/multiplayer/ClientPacketListener.java', '客户端收包入口'),
            @('client', 'net/minecraft/client/multiplayer/ClientLevel.java', '客户端世界'),
            @('client', 'net/minecraft/client/player/LocalPlayer.java', '本地玩家'),
            @('client', 'net/minecraft/client/sounds/SoundManager.java', '声音播放管理'),
            @('client', 'net/minecraft/client/renderer/texture/TextureManager.java', '贴图管理'),
            @('client', 'net/minecraft/client/Options.java', '客户端设置项')
        )
        $rows = foreach ($a in $anchors) {
            $p = Join-Path (Get-SideRoot $a[0]) ($a[1] -replace '/', '\')
            if (Test-Path $p) {
                $n = (Get-Content -LiteralPath $p).Count
                [pscustomobject]@{ Side = $a[0]; Lines = $n; Path = $a[1]; Role = $a[2] }
            }
            else {
                [pscustomobject]@{ Side = $a[0]; Lines = -1; Path = $a[1] + '  <-- 已移动/改名'; Role = $a[2] }
            }
        }
        $rows | Format-Table -AutoSize -Wrap
    }

    default {
        Get-Help $PSCommandPath -Detailed
    }
}
