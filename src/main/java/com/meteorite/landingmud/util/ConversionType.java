package com.meteorite.landingmud.util;

import com.meteorite.landingmud.config.MudConfig;

/*
 * 根据掉落高度判断泥巴方块触发的阶段
 */
public enum ConversionType {
    // 高度不足，泥巴无变化
    NO_CHANGE,
    // 转化为可疑的砂砾
    SUSPICIOUS_GRAVEL,
    // 转化为可疑的沙子
    SUSPICIOUS_SAND,
    // 泥巴直接破坏掉落泥土
    BREAK_MUD;

    public static ConversionType fromHeight(int fallBlocks) {

        ConversionType target;
        // 获取配置阈值
        double gravelThreshold = MudConfig.COMMON.heightSuspiciousGravel.get();
        double sandThreshold = MudConfig.COMMON.heightSuspiciousSand.get();
        double breakThreshold = MudConfig.COMMON.heightBreakMud.get();

        if (fallBlocks < gravelThreshold) {
            target = ConversionType.NO_CHANGE;
        } else if (fallBlocks < sandThreshold) {
            target = ConversionType.SUSPICIOUS_GRAVEL;
        } else if (fallBlocks < breakThreshold) {
            target = ConversionType.SUSPICIOUS_SAND;
        } else {
            target = ConversionType.BREAK_MUD;
        }
        return target;
    }
}
