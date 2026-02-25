package com.meteorite.landingmud.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class MudConfig {

    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static final class Common {

        // 开关
        public final ModConfigSpec.BooleanValue fallDamageReductionEnable;
        public final ModConfigSpec.BooleanValue convensionEnable;
        public final ModConfigSpec.BooleanValue brickOnFallEnable;
        public final ModConfigSpec.BooleanValue carrotEasterEggEnable;
        // 落地伤害减免比例
        public final ModConfigSpec.DoubleValue fallDamageReduction;

        // 掉落高度阈值（格）
        public final ModConfigSpec.DoubleValue heightSuspiciousGravel;
        public final ModConfigSpec.DoubleValue heightSuspiciousSand;
        public final ModConfigSpec.DoubleValue heightBreakMud;

        // 转化概率
        public final ModConfigSpec.DoubleValue probSuspiciousBlock;

        // 彩蛋概率
        public final ModConfigSpec.DoubleValue probBrickOnFall;
        public final ModConfigSpec.DoubleValue probCarrotEasterEgg;

        Common(ModConfigSpec.Builder builder) {

            builder.comment("========== 通用配置 ==========")
                    .push("common");

            fallDamageReductionEnable = builder
                    .comment("打开落在泥巴方块上时的伤害减免")
                    .comment("Enable damage reduction when landing on mud block")
                    .define("fallDamageReductionEnable", true);

            convensionEnable = builder
                    .comment("打开泥巴方块转化为可疑方块")
                    .comment("Enable mud block convert to suspicicious block")
                    .define("convensionEnable", true);

            brickOnFallEnable = builder
                    .comment("打开摔落到泥巴方块上概率出砖")
                    .comment("Enable falling onto mud block spawn brick")
                    .define("brickOnFallEnable", true);

            carrotEasterEggEnable = builder
                    .comment("打开胡萝卜彩蛋")
                    .comment("Enable carrot easter egg")
                    .define("carrotEasterEggEnable", true);

            fallDamageReduction = builder
                    .comment("落在泥巴方块上时的伤害减免 (0.0 = 无减免, 0.8 = 减免80%)")
                    .comment("Damage reduction when landing on mud block (0.0 = no reduction, 0.8 = 80% reduction)")
                    .defineInRange("fallDamageReduction", 0.8, 0.0, 1.0);

            heightSuspiciousGravel = builder
                    .comment("达到此高度(格)：泥巴尝试转化为可疑的砂砾，低于则不会有变化")
                    .comment("Reaching this height: Mud attempts to transform into suspicious gravel; below no change")
                    .defineInRange("heightSuspiciousGravel", 8.0, 0.0, 256.0);

            heightSuspiciousSand = builder
                    .comment("达到此高度(格)：泥巴尝试转化为可疑的沙子")
                    .comment("Reaching this height: Mud attempts to transform into suspicious sand")
                    .defineInRange("heightSuspiciousSand", 15.0, 0.0, 256.0);

            heightBreakMud = builder
                    .comment("达到此高度(格)：泥巴直接破坏并掉落泥土")
                    .comment("Reaching this height: Mud will be broken and drop dirt item")
                    .defineInRange("heightBreakMud", 24.0, 0.0, 256.0);

            probSuspiciousBlock = builder
                    .comment("泥巴转化为可疑方块的成功概率 (失败则变为泥土方块)")
                    .comment("The success probability of converting mud block into suspicious block (failure will result in the transformation into dirt block)")
                    .defineInRange("probSuspiciousBlock", 0.8, 0.0, 1.0);

            probBrickOnFall = builder
                    .comment("摔落到泥巴方块上时在玩家脚边额外生成1个红砖的概率")
                    .comment("The probability of an additional brick item spawn at the player's feet when falling onto a mud block.")
                    .defineInRange("probBrickOnFall", 0.1, 0.0, 1.0);

            probCarrotEasterEgg = builder
                    .comment("破坏胡萝卜时触发彩蛋的概率 (扣减1个胡萝卜并随机给予红砖或陶片)")
                    .comment("The probability of triggering Easter egg when destroying carrot (deducting 1 carrot and randomly giving a brick or a pottery sherd)")
                    .defineInRange("probCarrotEasterEgg", 0.015, 0.0, 1.0);

            builder.pop();
        }
    }
}
