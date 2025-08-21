package com.example.infooverlay.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class InfoOverlayConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue showCoords;
    public static final ForgeConfigSpec.BooleanValue showDay;
    public static final ForgeConfigSpec.BooleanValue showBiome;
    public static final ForgeConfigSpec.BooleanValue showWorldPlay;

    public static final ForgeConfigSpec.DoubleValue hudScale; // 0.0 ~ 1.0
    public static final ForgeConfigSpec.DoubleValue bgAlpha;  // 0.0 ~ 1.0

    public static final ForgeConfigSpec.IntValue coordDecimals; // 0 ~ 3
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> hudOrder;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("HUD Settings");

        showCoords    = builder.comment("Show player coordinates")
                .define("showCoords", true);

        showDay       = builder.comment("Show in-game day")
                .define("showDay", true);

        showBiome     = builder.comment("Show current biome")
                .define("showBiome", true);

        showWorldPlay = builder.comment("Show world playtime")
                .define("showWorldPlay", true);

        // 크래시 방지: 0.0~1.0로 고정
        hudScale = builder.comment("HUD scale (0.0 ~ 1.0)")
                .defineInRange("hudScale", 1.0, 0.0, 1.0);

        bgAlpha = builder.comment("HUD background alpha (0.0 ~ 1.0)")
                .defineInRange("bgAlpha", 0.5, 0.0, 1.0);

        coordDecimals = builder.comment("Coordinate decimal places (0..3)")
                .defineInRange("coordDecimals", 1, 0, 3);

        hudOrder = builder.comment("HUD line order (allowed: coords, biome, day, worldplay)")
                .defineList("hudOrder",
                        Arrays.asList("coords", "biome", "day", "worldplay"),
                        v -> v instanceof String);

        builder.pop();

        SPEC = builder.build();
    }
}
