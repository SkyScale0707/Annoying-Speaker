/**
 *  枚举类：音乐类型
 *  @note 三种类型：司凤音乐、天籁之音、正常旋律
 *  @note 目前该枚举量暂时不做进一步的处理，仅为占位符
 */

package com.ggscaletest.MusicRecordMethod;

public enum MusicType {
    // MOTHER_DIED, // 司凤音乐, 但是这个表述肯定不文雅
    DRIVER_MUSIC, // 司凤音乐: 避讳
    CELESTIAL_MELODY, // 天籁之音
    NORMAL, // 正常旋律(default)

    // 占位: 构造器
    MusicType() {
    }
}
