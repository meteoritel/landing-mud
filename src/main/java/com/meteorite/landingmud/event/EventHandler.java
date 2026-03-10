package com.meteorite.landingmud.event;

import com.meteorite.landingmud.LandingMud;
import com.meteorite.landingmud.config.MudConfig;
import com.meteorite.landingmud.util.ConversionType;
import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = LandingMud.MODID)
public class EventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    // ========== 战利品表路径 ========== //
    // 主世界
    private static final ResourceLocation LOOT_GRAVEL_OVERWORLD =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_gravel");
    private static final ResourceLocation LOOT_SAND_OVERWORLD =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_sand");
    // 下界
    private static final ResourceLocation LOOT_GRAVEL_NETHER =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_gravel_nether");
    private static final ResourceLocation LOOT_SAND_NETHER =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_sand_nether");
    // 末地
    private static final ResourceLocation LOOT_GRAVEL_END =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_gravel_end");
    private static final ResourceLocation LOOT_SAND_END =
            ResourceLocation.fromNamespaceAndPath(LandingMud.MODID, "archaeology/suspicious_sand_end");

    // ========== 摔落事件 ========== //
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 落点上表面y
        BlockPos onPos = player.getOnPos();
        BlockState onState = serverLevel.getBlockState(onPos);

        // 获取方块碰撞箱的最高点
        VoxelShape shape = onState.getCollisionShape(serverLevel, onPos);
        double surfaceOffset = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);
        double landSurfaceY = onPos.getY() + surfaceOffset;

        double rawStartY = landSurfaceY + event.getDistance();
        double startY = Math.round(rawStartY * 2.0) / 2.0;
        // 当前的实际掉落格子数
        int fallBlocks = (int) Math.round(startY - landSurfaceY);

        if (fallBlocks < 4) {
            return;
        }

        if (serverLevel.getBlockState(onPos).getBlock() != Blocks.MUD) {
            return;
        }

        // 伤害减免，在已有减伤（掉落保护等）的基础上进行减伤
        if (MudConfig.COMMON.fallDamageReductionEnable.get()) {
            float multiplier = (float) (1 - MudConfig.COMMON.fallDamageReduction.get());
            event.setDamageMultiplier(event.getDamageMultiplier() * multiplier);
        }

        // 向上弹起一点点，防止转化为完整方块卡住
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.2D, motion.z);

        // 掉落到泥巴方块上概率出砖
        if (MudConfig.COMMON.brickOnFallEnable.get()
                && RANDOM.nextDouble() < MudConfig.COMMON.probBrickOnFall.get()) {
            serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                    player.getX(), player.getY(), player.getZ(),
                    new ItemStack(Items.BRICK)));
            applyBrickDamage(player);
            // LOGGER.debug("出砖啦！");
        }
        // 泥巴方块的转化处理，可疑方块填充自定义looter
        if (MudConfig.COMMON.conversionEnable.get()) {
            handleMudConversion(serverLevel, onPos, fallBlocks);
        }

    }

    // ========== 主逻辑 ========== //
    // 泥巴转化
    private static void handleMudConversion(ServerLevel level, BlockPos pos, int fallBlocks) {
        // 获取配置阈值
        ConversionType target = ConversionType.fromHeight(fallBlocks);
        // LOGGER.debug("当前高度为{}，选择{}",fallBlocks, target.name());
        double conversionChance = MudConfig.COMMON.probSuspiciousBlock.get();

        if (target == ConversionType.NO_CHANGE) {
            return;
        }

        // 对于需要转化为可疑方块的区间，转化失败则变为泥土
        if (target == ConversionType.SUSPICIOUS_GRAVEL || target == ConversionType.SUSPICIOUS_SAND) {
            if (RANDOM.nextDouble() >= conversionChance) {
                // 转化失败：变成泥土
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                // LOGGER.debug("退化为泥土");
                return;
            }
        }

        // 根据维度选择战利品表
        ResourceLocation gravelLoot = getLootTableForDimension(level, LOOT_GRAVEL_OVERWORLD,
                LOOT_GRAVEL_NETHER, LOOT_GRAVEL_END);
        ResourceLocation sandLoot = getLootTableForDimension(level, LOOT_SAND_OVERWORLD,
                LOOT_SAND_NETHER, LOOT_SAND_END);

        // 执行转化
        switch (target) {
            case SUSPICIOUS_GRAVEL -> {
                level.setBlock(pos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 3);
                // 延迟1tick再设置战利品表
                level.getServer().execute(() -> {
                    setSuspiciousLootTable(level, pos, gravelLoot);
                    // LOGGER.debug("生成可疑砂砾 (维度: {})", level.dimension().location());
                });
            }
            case SUSPICIOUS_SAND -> {
                level.setBlock(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 3);
                level.getServer().execute(() -> {
                    setSuspiciousLootTable(level, pos, sandLoot);
                    // LOGGER.debug("生成可疑沙子 (维度: {})", level.dimension().location());
                });
            }
            case BREAK_MUD -> {
                // 直接破坏泥巴，掉落泥土
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                Block.popResource(level, pos, new ItemStack(Blocks.DIRT));
            }
        }
    }

    // 根据维度选择战利品表
    private static ResourceLocation getLootTableForDimension(
            ServerLevel level,
            ResourceLocation overworld,
            ResourceLocation nether,
            ResourceLocation end) {
        ResourceKey<Level> dim = level.dimension();
        if (dim.equals(Level.NETHER)) {
            return nether;
        } else if (dim.equals(Level.END)) {
            return end;
        } else {
            return overworld;
        }
    }

    // 为可疑方块设置战利品表
    private static void setSuspiciousLootTable(ServerLevel level, BlockPos pos, ResourceLocation lootTableId) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BrushableBlockEntity brushable) {
            ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);
            brushable.setLootTable(lootTableKey, level.random.nextLong());
            brushable.setChanged();
        }
    }

    // ========== 胡萝卜彩蛋 ========== //
    /* 破坏成熟胡萝卜有概率挖出红砖或者陶片
     * 但是作为代价胡萝卜数量会减少1
     * 因为胡萝卜被挖断了 o(╥﹏╥)o
    */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(MudConfig.COMMON.carrotEasterEggEnable.get())) return;
        if (event.getLevel().isClientSide) return;
        if (!(event.getBreaker() instanceof Player player)) return;

        BlockState state = event.getState();
        if (state.getBlock() != Blocks.CARROTS || state.getValue(CarrotBlock.AGE) < 7) return;

        double triggerProb = getCarrotEasterEggProb(event.getLevel());
        if (RANDOM.nextDouble() >= triggerProb) return;

        List<ItemEntity> drops = event.getDrops();

        ItemEntity carrotEntity = null;
        for (ItemEntity entity : drops) {
            ItemStack stack = entity.getItem();
            if (stack.is(Items.CARROT) && stack.getCount() >= 1) {
                carrotEntity = entity;
                break;
            }
        }

        if (carrotEntity == null) return;

        ItemStack carrotStack = carrotEntity.getItem();
        carrotStack.shrink(1);

        if (carrotStack.isEmpty()) {
            drops.remove(carrotEntity);
        } else {
            carrotEntity.setItem(carrotStack);
        }

        drops.add(new ItemEntity(event.getLevel(),
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                getBrickOrSherd()));
        // 挖出砖磕到手了
        applyBrickDamage(player);
    }

    // 计算胡萝卜彩蛋的概率，包括雨天加成
    private static double getCarrotEasterEggProb(Level level) {
        double prob = MudConfig.COMMON.probCarrotEasterEgg.get();
        if (level.isRaining()) {
            prob += MudConfig.COMMON.probCarrotEasterEggRainBonus.get();
            // LOGGER.debug("雨天胡萝卜彩蛋加成，当前概率: {}", prob);
        }
        if (level.isThundering()) {
            prob += MudConfig.COMMON.probCarrotEasterEggThunderBonus.get();
            // LOGGER.debug("雷雨天胡萝卜彩蛋加成，当前概率: {}", prob);
        }
        // 概率上限 1.0
        return Math.min(prob, 1.0);
    }

    // 挖出红砖受到伤害
    private static void applyBrickDamage(Player player) {
        if (!MudConfig.COMMON.brickDamageEnable.get()) return;
        float damage = (float) MudConfig.COMMON.brickDamageAmount.get().doubleValue();
        DamageSource voidSource = player.level().damageSources().fellOutOfWorld();
        player.hurt(voidSource, damage);
        // LOGGER.debug("红砖伤害：{}点，玩家：{}", damage, player.getName().getString());
    }

    // 获得一个红砖或者随机任何一个陶片
    private static ItemStack getBrickOrSherd() {
        ItemStack reward;
        if (RANDOM.nextBoolean()) {
            reward = new ItemStack(Items.BRICK);
        } else {
            List<Item> sherds = BuiltInRegistries.ITEM.stream()
                    .filter(item -> {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                        return id.getPath().endsWith("_pottery_sherd");
                    })
                    .toList();

            reward =  new ItemStack(sherds.get(RANDOM.nextInt(sherds.size())));
        }
        reward.setCount(1);
        return reward;
    }
}
