# 00 · 系列索引（Minecraft 26.3 源码 skill）

本目录是一套**可增长的**源码知识库，用于回答"这个功能在 Minecraft 26.3 的哪儿、怎么写"。
它由两部分组成：**静态结构地图**（01–04，很少变）+ **累积问答记录**（05，每轮提问后追加）。

源码本体不在这里，在 `.gradle\loom-cache\minecraftMaven\net\minecraft` 的两个 sources jar 里；
本目录的 `.agent\.cache\src\{common,client}` 是它们的解包缓存，由脚本按 jar 哈希自动维护。

---

## 文件清单（建议按此顺序读）

| 文件 | 内容 | 何时读 |
|---|---|---|
| `agent_read.md` | 用户下发的原始指令（**不要修改**） | 每次会话开始 |
| `00-index.md` | 本索引：用法、维护规则 | 第一次进入 |
| `01-sources.md` | 源码包坐标、两侧划分、缓存机制、版本信息 | 需要确认"该看哪个 jar / 缓存新鲜吗" |
| `02-server-structure.md` | **通用 + 逻辑服务端**（`minecraft-common`）源码结构 | 写服务端逻辑、注册、世界、网络、数据 |
| `03-client-structure.md` | **纯客户端**（`minecraft-clientOnly`）源码结构 | 写渲染、GUI、HUD、按键、声音、客户端缓存 |
| `04-retrieval.md` | 检索手册：五步法、命名约定、双端对照表、任务→路径 | 每次真正开始找代码前 |
| `05-findings.md` | 问答结论库（按 F-xxx 编号追加） | 找之前先扫一遍，避免重复踩坑 |
| `90-update-log.md` | 修订日志 + 维护清单 | 每次改动本系列后 |
| `scripts/mc-src.ps1` | 检索工具：解包 / 找类 / 正则检索 / 看文件 / 包统计 / 锚点表 | 全程 |

---

## 30 秒上手

```powershell
$s = "$PWD\.agent\.skill\scripts\mc-src.ps1"   # 在模组工程根目录下执行
powershell -NoProfile -ExecutionPolicy Bypass -File $s ensure      # 解包/校验缓存
powershell -NoProfile -ExecutionPolicy Bypass -File $s anchors     # 关键入口类一览
powershell -NoProfile -ExecutionPolicy Bypass -File $s find Sound   # 找类
powershell -NoProfile -ExecutionPolicy Bypass -File $s grep "class ClientLevel" -Side client
powershell -NoProfile -ExecutionPolicy Bypass -File $s tree net/minecraft/client/gui -Side client
powershell -NoProfile -ExecutionPolicy Bypass -File $s show net/minecraft/world/item/Item.java -From 1 -To 60
```

> `-ExecutionPolicy Bypass` 必须带，否则本机策略会拒绝加载 `.ps1`。

也可以用 grep / 文本工具直接扫 `.agent\.cache\src\common\**\*.java` 与 `.agent\.cache\src\client\**\*.java`，
两者与 jar 内路径一一对应（缓存里的相对路径 == jar 内路径）。

---

## 回答一个问题时的固定流程

1. **分清侧别**：这段逻辑跑在 common（双端/逻辑服务端）还是 clientOnly（纯客户端）？见 `01-sources.md`。
2. **先查记录**：`05-findings.md` 里有没有已经答过的同题（含改名、陷阱）。
3. **定位包**：用 `02`/`03` 的包地图 + `mc-src.ps1 tree` 收敛到 1–2 个目录。
4. **找类**：`find`（类名）→ `grep "class X"`（确认定义）→ `show` 读关键方法。
5. **追调用**：`grep "new X("` / `grep "\.methodName\("` / `grep "import .*X;"` 判断谁在用、被谁调。
6. **落地写码**：给出 Fabric 26.3 可编译的写法；若依赖 Fabric API（不在本源码里），明确说明来源。
7. **回写 skill**：按 `05-findings.md` 顶部的模板追加一条，并在 `90-update-log.md` 记一行。

---

## 维护规则（每次问答都必须执行）

- **追加，不覆盖**：`05-findings.md` 只在末尾追加新条目；旧条目意见改变时在条目内追加 `> 修订:` 行。
- **可验证**：每条结论必须给出至少一个 `路径:行号` 或一条能复现的检索命令。
- **纠错就地改**：`01`–`04` 里的结构性事实若发现过时/错误，直接改原文，并在 `90-update-log.md` 记为「更正」。
- **新检索手法进 04**：任何"这次这样搜很快就找到了"的技巧，抽象成一行追加到 `04-retrieval.md` §9。
- **不改 `agent_read.md`**：它是用户下发的原始指令，永久保持原样。
- **不手改缓存**：`.agent\.cache\src` 由 `ensure` 生成，不要在其中做任何编辑。
