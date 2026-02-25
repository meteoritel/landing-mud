package com.meteorite.landingmud;

import com.meteorite.landingmud.config.MudConfig;

import com.meteorite.landingmud.debug.DebugEventHandler;
import com.meteorite.landingmud.event.EventHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(LandingMud.MODID)
public class LandingMud {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "landingmud";

    public LandingMud(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, MudConfig.SPEC);

        // 注册 NeoForge 事件总线事件
        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(DebugEventHandler.class);
    }

    // 调试用，用来检查自定义战利品表有没有正常注册
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // 获取所有已注册的战利品表 key，过滤出考古相关的
        List<ResourceLocation> archaeologyTables = event.getServer()
                .reloadableRegistries()
                .get()
                .registryOrThrow(Registries.LOOT_TABLE)
                .keySet()
                .stream()
                .filter(rl -> rl.getPath().contains("archaeology"))
                .sorted()
                .toList();

        if (archaeologyTables.isEmpty()) {
            LOGGER.warn("[DEBUG] 未找到任何考古相关战利品表，请检查文件路径是否正确");
        } else {
            LOGGER.debug("[DEBUG] 已加载的考古战利品表共 {} 个：", archaeologyTables.size());
            archaeologyTables.forEach(rl -> LOGGER.info("[DEBUG]   - {}", rl));
        }
    }
}
