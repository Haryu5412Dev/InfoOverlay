package com.example.infooverlay;

import com.example.infooverlay.client.HudConfigScreen;
import com.example.infooverlay.client.HudOverlay;
import com.example.infooverlay.client.ClientKeyBindings;
import com.example.infooverlay.config.InfoOverlayConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod(InfoOverlayMod.MODID)
public class InfoOverlayMod {
    public static final String MODID = "infooverlay";

    // ★ 중복 등록 가드
    private static final AtomicBoolean CONFIG_REGISTERED = new AtomicBoolean(false);

    public InfoOverlayMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onRegisterOverlays);
        bus.addListener(this::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(ClientKeyBindings::onClientTick);

        // ★ Config 등록 (가드 적용)
        if (CONFIG_REGISTERED.compareAndSet(false, true)) {
            ModLoadingContext.get().registerConfig(
                    net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                    InfoOverlayConfig.SPEC,
                    MODID + "-client.toml" // 명시적 파일명(충돌 예방)
            );
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new HudConfigScreen(parent)
                )
        );

        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> "ANY",
                        (remote, isServer) -> true
                )
        );
    }

    private void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.DEBUG_TEXT.id(),
                "infooverlay",
                new HudOverlay()
        );
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientKeyBindings.registerKeys(event);
    }
}
