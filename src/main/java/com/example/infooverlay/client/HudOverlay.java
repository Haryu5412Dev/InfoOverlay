package com.example.infooverlay.client;

import com.example.infooverlay.config.InfoOverlayConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.infooverlay.client.ClientKeyBindings;

public class HudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientKeyBindings.hudVisible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return; // F3 켜지면 숨김

        // ===== 라인 조립 (사용자 순서 반영 + 토글 체크) =====
        List<String> lines = new ArrayList<>();
        List<? extends String> orderCfg = InfoOverlayConfig.hudOrder.get();

        // order 안전 정규화 (허용 키만, 중복 제거, 누락은 뒤에)
        List<String> allowed = List.of("coords", "biome", "day", "worldplay");
        Set<String> seen = new HashSet<>();
        List<String> order = new ArrayList<>();
        for (String k : orderCfg) {
            if (allowed.contains(k) && seen.add(k)) order.add(k);
        }
        for (String k : allowed) if (seen.add(k)) order.add(k);

        for (String key : order) {
            switch (key) {
                case "coords" -> {
                    if (InfoOverlayConfig.showCoords.get()) {
                        // 좌표: 소수 자릿수 적용
                        int decimals = Math.max(0, Math.min(3, InfoOverlayConfig.coordDecimals.get()));
                        String fmt = "%." + decimals + "f";
                        String x = String.format(java.util.Locale.ROOT, fmt, mc.player.getX());
                        String y = String.format(java.util.Locale.ROOT, fmt, mc.player.getY());
                        String z = String.format(java.util.Locale.ROOT, fmt, mc.player.getZ());
                        lines.add(I18n.get("infooverlay.coords_line", x, y, z));
                    }
                }
                case "biome" -> {
                    if (InfoOverlayConfig.showBiome.get()) {
                        String biomeLine = "";
                        try {
                            var holder = mc.level.getBiome(mc.player.blockPosition());
                            var keyOpt = holder.unwrapKey();
                            if (keyOpt.isPresent()) {
                                ResourceLocation id = keyOpt.get().location();
                                String trKey = "biome." + id.getNamespace() + "." + id.getPath();
                                biomeLine = I18n.get("infooverlay.biome_line", I18n.get(trKey));
                            } else {
                                String idStr = mc.level.registryAccess().registryOrThrow(Registries.BIOME)
                                        .getKey(holder.value()).toString();
                                biomeLine = I18n.get("infooverlay.biome_line", idStr);
                            }
                        } catch (Throwable t) {
                            biomeLine = I18n.get("infooverlay.biome_line", "unknown");
                        }
                        lines.add(biomeLine);
                    }
                }
                case "day" -> {
                    if (InfoOverlayConfig.showDay.get()) {
                        long day = mc.level.getDayTime() / 24000L;
                        lines.add(I18n.get("infooverlay.day_line", day));
                    }
                }
                case "worldplay" -> {
                    if (InfoOverlayConfig.showWorldPlay.get()) {
                        long ticks = mc.level.getGameTime();   // 월드 전체 경과 틱
                        long totalSeconds = ticks / 20L;
                        long hours = totalSeconds / 3600L;
                        long minutes = (totalSeconds % 3600L) / 60L;
                        long seconds = totalSeconds % 60L;
                        String playStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        lines.add(I18n.get("infooverlay.worldplay_line", playStr));
                    }
                }
            }
        }

        if (lines.isEmpty()) return;

        // ===== 렌더링 (스케일/알파 안전 클램프) =====
        float scale = (float)(double) InfoOverlayConfig.hudScale.get();
        if (!Float.isFinite(scale)) scale = 1.0f;
        if (scale < 0.01f) scale = 0.01f; // ε
        if (scale > 1.0f)  scale = 1.0f;

        float alpha = (float)(double) InfoOverlayConfig.bgAlpha.get();
        if (!Float.isFinite(alpha)) alpha = 0.5f;
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;
        int bgColor = ((int)(alpha * 255) & 0xFF) << 24; // ARGB: 검은 배경 + 알파

        g.pose().pushPose();
        try {
            g.pose().scale(scale, scale, 1f);

            int x = 8, y = 8;
            int lineH = mc.font.lineHeight + 2;

            int maxW = 0;
            for (String s : lines) maxW = Math.max(maxW, mc.font.width(s));
            int height = lines.size() * lineH;

            RenderSystem.enableBlend();
            g.fill(x - 6, y - 6, x + maxW + 8, y + height + 6, bgColor);

            int yy = y;
            for (String s : lines) {
                g.drawString(mc.font, s, x, yy, 0xFFFFFF, true);
                yy += lineH;
            }
        } finally {
            g.pose().popPose();
        }
    }
}
