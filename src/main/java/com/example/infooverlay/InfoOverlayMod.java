package com.example.infooverlay;

import com.example.infooverlay.client.HudConfigScreen;
import com.example.infooverlay.client.HudOverlay;
import com.example.infooverlay.config.InfoOverlayConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(InfoOverlayMod.MODID)
public class InfoOverlayMod {
    public static final String MODID = "infooverlay";

    public InfoOverlayMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onRegisterOverlays);

        MinecraftForge.EVENT_BUS.register(this);

        // Config
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                InfoOverlayConfig.SPEC
        );
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
}
