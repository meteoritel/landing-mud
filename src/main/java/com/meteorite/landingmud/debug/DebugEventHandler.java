package com.meteorite.landingmud.debug;

import com.meteorite.landingmud.LandingMud;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

// 日志调试类，空手右键可疑方块输出战利品表类型
@EventBusSubscriber(modid = LandingMud.MODID)
public class DebugEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    // 日志开关
    private static final boolean isEnable = false;

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 仅服务端处理
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        if (!isEnable) return;

        Player player = event.getEntity();

        // 必须空手 + shift
        if (!player.isShiftKeyDown()) return;
        if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return;
        if (!player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) return;

        BlockPos pos = event.getPos();
        BlockEntity be = serverLevel.getBlockEntity(pos);

        if (!(be instanceof BrushableBlockEntity brushable)) return;

        // 尝试通过反射读取私有字段 lootTable
        try {
            Field lootTableField = null;
            Class<?> clazz = BrushableBlockEntity.class;
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    // 字段类型为 ResourceKey<LootTable>
                    if (f.getType().equals(ResourceKey.class)) {
                        lootTableField = f;
                        break;
                    }
                }
                if (lootTableField != null) break;
                clazz = clazz.getSuperclass();
            }

            if (lootTableField == null) {
                LOGGER.warn("[DEBUG] 未找到 BrushableBlockEntity 中的战利品表字段，可能需要更新字段名");
                return;
            }

            lootTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ResourceKey<LootTable> lootTableKey = (ResourceKey<LootTable>) lootTableField.get(brushable);

            if (lootTableKey == null) {
                LOGGER.info("[DEBUG] 位置 {} 的可疑方块：战利品表字段为 null（未设置或已被清空）", pos);
            } else {
                LOGGER.info("[DEBUG] 位置 {} 的可疑方块：战利品表 = {}", pos, lootTableKey.location());

                // 进一步验证该战利品表是否能在服务端被解析到
                LootTable resolvedTable = serverLevel.getServer()
                        .reloadableRegistries()
                        .getLootTable(lootTableKey);

                if (resolvedTable == LootTable.EMPTY) {
                    LOGGER.warn("[DEBUG] 警告：战利品表 {} 解析结果为 EMPTY，说明该战利品表文件未被正确加载！", lootTableKey.location());
                } else {
                    LOGGER.info("[DEBUG] 战利品表 {} 解析成功，可正常使用", lootTableKey.location());
                }
            }

        } catch (Exception e) {
            LOGGER.error("[DEBUG] 读取战利品表时发生异常", e);
        }
    }
}
