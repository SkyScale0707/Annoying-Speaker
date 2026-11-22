/**
 *  音乐结构类
 *  用于定义音乐文件的结构和属性
 */
package com.ggscaletest.MusicRecordMethod;

import java.util.ArrayList;
import java.util.List;

public class MusicStructure {

    // 构造器
    public MusicStructure() {
        String musicName; // 音乐名称
        String musicPath; // 音乐文件路径
        MusicType musicType; // 音乐类型
    }

    // 创建List<MusicStructure>对象，用于存储从run\config\annoying_speaker\music_folder下读取的所有音乐文件信息
    public static List<MusicStructure> musicList = new ArrayList<>();

    /**
     * 根据音乐类型获取对应的音乐文件夹路径
     * 
     * @note 该方法用于获取音乐文件夹路径，方便后续读取音乐
     * @param musicType 音乐类型
     * @return 音乐文件夹路径
     */
    public static String getMusicDir(MusicType musicType) {
        if (musicType == MusicType.DRIVER_MUSIC) {
            return "run\\config\\annoying_speaker\\music_folder\\driver_music";
        } else if (musicType == MusicType.CELESTIAL_MELODY) {
            return "run\\config\\annoying_speaker\\music_folder\\celestial_melody";
        } else if (musicType == MusicType.NORMAL) {
            return "run\\config\\annoying_speaker\\music_folder\\normal";
        } else {
            return null;
        }
    }

    // getMusicList()方法，用于获取音乐列表
    public static List<MusicStructure> getMusicList(String musicDir) {
        return musicList;
    }
}
