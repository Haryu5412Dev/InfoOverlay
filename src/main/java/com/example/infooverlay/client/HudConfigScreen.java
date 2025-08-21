package com.example.infooverlay.client;

import com.example.infooverlay.config.InfoOverlayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HudConfigScreen extends Screen {
    private final Screen parent;

    // 탭
    private enum Tab { DISPLAY, ORDER }
    private Tab activeTab = Tab.DISPLAY;

    // 허용 키
    private final List<String> allowed = Arrays.asList("coords", "biome", "day", "worldplay");

    // ========= 로컬 상태(화면 내 임시값) =========
    private boolean lShowCoords;
    private boolean lShowDay;
    private boolean lShowBiome;
    private boolean lShowWorldPlay;

    private double lHudScale;   // 0.0 ~ 1.0
    private double lBgAlpha;    // 0.0 ~ 1.0
    private int    lDecimals;   // 0 ~ 3

    private List<String> lOrder; // 순서(가변 복사본)

    // 위젯 참조
    private DraggableOrderList orderList;
    private FloatField alphaField;    // 0.00 ~ 1.00
    private IntField   decimalsField; // 0 ~ 3

    public HudConfigScreen(Screen parent) {
        super(Component.translatable("infooverlay.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // ===== Config → 로컬 복사 =====
        lShowCoords    = InfoOverlayConfig.showCoords.get();
        lShowDay       = InfoOverlayConfig.showDay.get();
        lShowBiome     = InfoOverlayConfig.showBiome.get();
        lShowWorldPlay = InfoOverlayConfig.showWorldPlay.get();
        lHudScale      = clamp01(InfoOverlayConfig.hudScale.get());
        lBgAlpha       = clamp01(InfoOverlayConfig.bgAlpha.get());
        lDecimals      = clampInt(InfoOverlayConfig.coordDecimals.get(), 0, 3);

        lOrder = new ArrayList<>(InfoOverlayConfig.hudOrder.get().stream().map(String::valueOf).toList());
        normalizeOrder();

        buildUI();
    }

    private void buildUI() {
        clearWidgets();

        final int cx = width / 2;
        int tabW = 120, tabH = 20, tabGap = 6, tabY = 24;

        // 탭 버튼
        addRenderableWidget(Button.builder(tabTitle(Tab.DISPLAY), b -> { activeTab = Tab.DISPLAY; buildUI(); })
                .bounds(cx - tabW - tabGap/2, tabY, tabW, tabH).build());
        addRenderableWidget(Button.builder(tabTitle(Tab.ORDER), b -> { activeTab = Tab.ORDER; buildUI(); })
                .bounds(cx + tabGap/2, tabY, tabW, tabH).build());

        if (activeTab == Tab.DISPLAY) buildDisplayTab();
        else buildOrderTab();

        // Done (여기서만 Config에 커밋)
        int bottomY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            // 입력칸 최종 커밋(문자열 → 수치 반영)
            if (alphaField != null) alphaField.commitToLocal();
            if (decimalsField != null) decimalsField.commitToLocal();

            // ===== 로컬 → Config 한 번에 저장 =====
            InfoOverlayConfig.showCoords.set(lShowCoords);
            InfoOverlayConfig.showDay.set(lShowDay);
            InfoOverlayConfig.showBiome.set(lShowBiome);
            InfoOverlayConfig.showWorldPlay.set(lShowWorldPlay);

            InfoOverlayConfig.hudScale.set(clamp01(lHudScale));
            InfoOverlayConfig.bgAlpha.set(clamp01(lBgAlpha));
            InfoOverlayConfig.coordDecimals.set(clampInt(lDecimals, 0, 3));
            normalizeOrder();
            InfoOverlayConfig.hudOrder.set(new ArrayList<>(lOrder));

            Minecraft.getInstance().setScreen(parent);
        }).bounds(cx - 100, bottomY, 200, 20).build());
    }

    private Component tabTitle(Tab t) {
        boolean selected = (t == activeTab);
        String mark = selected ? "[*] " : "[ ] ";
        String key = (t == Tab.DISPLAY) ? "infooverlay.config.tab.display" : "infooverlay.config.tab.order";
        return Component.literal(mark).append(Component.translatable(key));
    }

    private void buildDisplayTab() {
        int cx = width / 2;
        int y = 56;
        int w = 220, h = 20, gap = 22;

        // 토글들 (로컬 상태만 변경)
        addRenderableWidget(makeToggleButton(cx, y, w, h, "infooverlay.config.coords",
                lShowCoords, v -> lShowCoords = v));
        y += gap;

        addRenderableWidget(makeToggleButton(cx, y, w, h, "infooverlay.config.day",
                lShowDay, v -> lShowDay = v));
        y += gap;

        addRenderableWidget(makeToggleButton(cx, y, w, h, "infooverlay.config.biome",
                lShowBiome, v -> lShowBiome = v));
        y += gap;

        addRenderableWidget(makeToggleButton(cx, y, w, h, "infooverlay.config.worldplay",
                lShowWorldPlay, v -> lShowWorldPlay = v));
        y += gap + 4;

        // HUD 크기 슬라이더 (0.0 ~ 1.0) — 로컬만 갱신
        addRenderableWidget(new HudScaleSlider(cx - w/2, y, w, h, 0.0, 1.0, lHudScale, v -> lHudScale = v));
        y += gap + 6;

        // ===== 입력 칸 =====
        int labelW = 130, fieldW = 70, space = 8;

        // 배경 알파 (0.00 ~ 1.00) — 로컬만 갱신
        addRenderableWidget(new Label(cx - (labelW + space + fieldW)/2, y + 6,
                Component.translatable("infooverlay.config.bg_alpha")));
        alphaField = new FloatField(font,
                cx - (labelW + space + fieldW)/2 + labelW + space, y, fieldW, h,
                0.00, 1.00, lBgAlpha,
                v -> lBgAlpha = v);
        addRenderableWidget(alphaField);
        y += gap;

        // 좌표 소수 자릿수 (0 ~ 3) — 로컬만 갱신
        addRenderableWidget(new Label(cx - (labelW + space + fieldW)/2, y + 6,
                Component.translatable("infooverlay.config.coord_decimals")));
        decimalsField = new IntField(font,
                cx - (labelW + space + fieldW)/2 + labelW + space, y, fieldW, h,
                0, 3, lDecimals,
                v -> lDecimals = v);
        addRenderableWidget(decimalsField);
    }

    private void buildOrderTab() {
        int cx = width / 2;
        int listW = 240;
        int listH = Math.min(height - 120, 120);
        int y = 64;

        addRenderableWidget(new CenterLabel(cx, y - 14,
                Component.translatable("infooverlay.config.order_hint")));
        orderList = new DraggableOrderList(cx - listW/2, y, listW, listH, 22);
        addRenderableWidget(orderList);
    }

    private AbstractWidget makeToggleButton(int cx, int y, int w, int h,
                                            String labelKey, boolean initial,
                                            java.util.function.Consumer<Boolean> onToggle) {
        Component msg = composeToggleMessage(labelKey, initial);
        return Button.builder(msg, b -> {
            boolean next = !initialValueFromMessage(b.getMessage());
            onToggle.accept(next); // 로컬만 변경
            b.setMessage(composeToggleMessage(labelKey, next));
        }).bounds(cx - w/2, y, w, h).build();
    }

    private Component composeToggleMessage(String key, boolean value) {
        return Component.translatable(key)
                .append(Component.literal(": "))
                .append(Component.translatable(value ? "infooverlay.config.on" : "infooverlay.config.off"));
    }

    private boolean initialValueFromMessage(Component message) {
        String s = message.getString();
        return s.endsWith(Component.translatable("infooverlay.config.on").getString());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        g.drawCenteredString(font, getTitle(), width / 2, 10, 0xFFFFFF);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        if (alphaField != null) alphaField.tick();       // IME 안정화
        if (decimalsField != null) decimalsField.tick(); // IME 안정화
    }

    private void normalizeOrder() {
        List<String> cleaned = new ArrayList<>();
        for (String k : lOrder) if (allowed.contains(k) && !cleaned.contains(k)) cleaned.add(k);
        for (String k : allowed) if (!cleaned.contains(k)) cleaned.add(k);
        lOrder = cleaned;
    }

    // ---------- 간단 라벨 ----------
    private static class Label extends AbstractWidget {
        Label(int x, int y, Component text) { super(x, y, 120, 12, text); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.drawString(Minecraft.getInstance().font, getMessage(), getX(), getY(), 0xFFFFFF, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
    private static class CenterLabel extends AbstractWidget {
        CenterLabel(int cx, int y, Component text) { super(cx, y, 240, 12, text); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX(), getY(), 0xFFFFFF);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput out) {}
    }

    // ---------- 유틸 ----------
    private static double clamp01(double v){ return v<0?0:(v>1?1:v); }
    private static int clampInt(int v,int min,int max){ return v<min?min:(v>max?max:v); }

    // ---------- 숫자 입력(로컬만 갱신) ----------
    /** 부동소수 (0.00 ~ 1.00) — IME 안전, Enter/ESC/Done 시 로컬 커밋 */
    private static class FloatField extends EditBox {
        private final double min, max;
        private final java.util.function.DoubleConsumer onLocalCommit;
        private boolean committing = false;

        FloatField(Font font, int x, int y, int w, int h,
                   double min, double max, double initial,
                   java.util.function.DoubleConsumer onLocalCommit) {
            super(font, x, y, w, h, Component.literal("float"));
            this.min = min; this.max = max; this.onLocalCommit = onLocalCommit;
            setMaxLength(8);
            setValue(format(initial));
        }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            boolean handled = super.keyPressed(key, scan, mods);
            if (isFocused() && (key == 257 || key == 335 || key == 256)) commitToLocal();
            return handled;
        }
        void commitToLocal() {
            if (committing) return;
            committing = true;
            try {
                double val;
                try { val = Double.parseDouble(getValue().trim()); }
                catch (Exception e) { return; }
                if (Double.isNaN(val) || Double.isInfinite(val)) return;
                if (val < min) val = min;
                if (val > max) val = max;
                super.setValue(format(val));
                if (onLocalCommit != null) onLocalCommit.accept(val);
            } finally {
                committing = false;
            }
        }
        private static String format(double v){
            return String.format(java.util.Locale.ROOT, "%.2f", v);
        }
    }

    /** 정수 (0 ~ 3) — IME 안전, Enter/ESC/Done 시 로컬 커밋 */
    private static class IntField extends EditBox {
        private final int min, max;
        private final java.util.function.IntConsumer onLocalCommit;
        private boolean committing = false;

        IntField(Font font, int x, int y, int w, int h,
                 int min, int max, int initial,
                 java.util.function.IntConsumer onLocalCommit) {
            super(font, x, y, w, h, Component.literal("int"));
            this.min=min; this.max=max; this.onLocalCommit=onLocalCommit;
            setMaxLength(3);
            setValue(Integer.toString(clampInt(initial, min, max)));
        }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            boolean handled = super.keyPressed(key, scan, mods);
            if (isFocused() && (key == 257 || key == 335 || key == 256)) commitToLocal();
            return handled;
        }
        @Override public void insertText(String text) {
            String filtered = text.replaceAll("[^0-9]", "");
            super.insertText(filtered);
        }
        void commitToLocal() {
            if (committing) return;
            committing = true;
            try {
                int v;
                try { v = Integer.parseInt(getValue().trim()); }
                catch (Exception e) { return; }
                if (v < min) v = min;
                if (v > max) v = max;
                super.setValue(Integer.toString(v));
                if (onLocalCommit != null) onLocalCommit.accept(v);
            } finally {
                committing = false;
            }
        }
    }

    // ---------- HUD 크기 슬라이더 (0.0 ~ 1.0, 로컬만 갱신, ε 보정은 커밋시 적용) ----------
    class HudScaleSlider extends AbstractSliderButton {
        private final double min, max;
        private final java.util.function.DoubleConsumer onLocalChange;

        HudScaleSlider(int x, int y, int w, int h, double min, double max, double current,
                       java.util.function.DoubleConsumer onLocalChange) {
            super(x, y, w, h, Component.empty(), 0.0D);
            this.min = min; this.max = max; this.onLocalChange = onLocalChange;
            double denom = Math.max(1e-9, (max - min));
            this.value = (current - min) / denom;
            if (this.value < 0) this.value = 0;
            if (this.value > 1) this.value = 1;
            updateMessage();
        }
        @Override protected void updateMessage() {
            double val = min + value * (max - min);
            val = Math.max(min, Math.min(max, val));
            setMessage(Component.translatable("infooverlay.config.scale",
                    String.format(java.util.Locale.ROOT, "%.2f", val)));
        }
        @Override protected void applyValue() {
            double v = min + value * (max - min);
            v = Math.max(min, Math.min(max, v));
            if (onLocalChange != null) onLocalChange.accept(v); // 로컬만 갱신
        }
        @Override public void playDownSound(SoundManager sm) { super.playDownSound(sm); }
    }

    // ---------- 순서 드래그 리스트 (로컬만 변경) ----------
    class DraggableOrderList extends AbstractWidget {
        private final int itemHeight;
        private int draggingIndex = -1;
        private int dragMouseOffsetY = 0;
        private int scrollOffset = 0;

        DraggableOrderList(int x, int y, int w, int h, int itemHeight) {
            super(x, y, w, h, Component.empty());
            this.itemHeight = itemHeight;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66000000);

            int visible = Math.max(1, getHeight() / itemHeight);
            int start = Math.max(0, Math.min(scrollOffset, Math.max(0, lOrder.size() - visible)));
            int end = Math.min(lOrder.size(), start + visible);

            for (int idx = start; idx < end; idx++) {
                int i = idx - start;
                int y = getY() + i * itemHeight;
                int x = getX();
                int w = getWidth();

                int bg = 0x22000000;
                if (idx == draggingIndex) {
                    g.fill(x + 2, y + 2, x + w - 2, y + itemHeight - 2, 0x33000000);
                } else if (isMouseOver(mouseX, mouseY) && mouseY >= y && mouseY < y + itemHeight) {
                    bg = 0x33FFFFFF;
                    g.fill(x + 2, y + 2, x + w - 2, y + itemHeight - 2, bg);
                } else {
                    g.fill(x + 2, y + 2, x + w - 2, y + itemHeight - 2, bg);
                }

                String name = prettyName(lOrder.get(idx));
                g.drawString(Minecraft.getInstance().font, name,
                        x + 8, y + (itemHeight - Minecraft.getInstance().font.lineHeight)/2, 0xFFFFFF, false);
            }

            if (draggingIndex >= 0 && draggingIndex < lOrder.size()) {
                int mouseYClamped = Math.max(getY() + 2, Math.min(mouseY, getY() + getHeight() - 2));
                int drawY = mouseYClamped - dragMouseOffsetY;
                int x = getX(), w = getWidth();

                g.fill(x + 2, drawY, x + w - 2, drawY + itemHeight - 2, 0xAAFFFFFF);
                g.drawString(Minecraft.getInstance().font, prettyName(lOrder.get(draggingIndex)),
                        x + 8, drawY + (itemHeight - Minecraft.getInstance().font.lineHeight)/2, 0x000000, false);

                int target = indexFromY(mouseY) + Math.max(0, Math.min(scrollOffset, Math.max(0, lOrder.size() - Math.max(1, getHeight()/itemHeight))));
                target = Math.max(0, Math.min(target, lOrder.size()));
                int rel = target - Math.max(0, Math.min(scrollOffset, Math.max(0, lOrder.size() - Math.max(1, getHeight()/itemHeight))));
                int guideY = getY() + rel * itemHeight;
                g.fill(x + 2, guideY - 1, x + w - 2, guideY + 1, 0xFF66CCFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || button != 0) return false;
            int start = Math.max(0, Math.min(scrollOffset, Math.max(0, lOrder.size() - Math.max(1, getHeight()/itemHeight))));
            int idx = indexFromY((int)mouseY) + start;
            if (idx >= 0 && idx < lOrder.size()) {
                draggingIndex = idx;
                int itemTop = getY() + (idx - start) * itemHeight;
                dragMouseOffsetY = (int)mouseY - itemTop;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (draggingIndex == -1) return false;
            int margin = 10;
            if (mouseY < getY() + margin && scrollOffset > 0) {
                scrollOffset--;
            } else if (mouseY > getY() + getHeight() - margin) {
                int maxOff = Math.max(0, lOrder.size() - Math.max(1, getHeight()/itemHeight));
                if (scrollOffset < maxOff) scrollOffset++;
            }
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (draggingIndex == -1) return false;
            int start = Math.max(0, Math.min(scrollOffset, Math.max(0, lOrder.size() - Math.max(1, getHeight()/itemHeight))));
            int to = indexFromY((int)mouseY) + start;
            to = Math.max(0, Math.min(to, lOrder.size() - 1));
            if (to != draggingIndex) {
                String item = lOrder.remove(draggingIndex);
                lOrder.add(to, item);
            }
            draggingIndex = -1;
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY)) return false;
            int visible = Math.max(1, getHeight() / itemHeight);
            int maxOff = Math.max(0, lOrder.size() - visible);
            if (delta < 0 && scrollOffset < maxOff) scrollOffset++;
            else if (delta > 0 && scrollOffset > 0) scrollOffset--;
            return true;
        }

        private int indexFromY(int mouseY) {
            int rel = mouseY - getY();
            if (rel < 0) return 0;
            int idx = rel / itemHeight;
            int maxIdx = Math.max(0, getHeight() / itemHeight);
            if (idx > maxIdx) idx = maxIdx;
            return idx;
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput out) {}
        @Override public boolean isMouseOver(double mx, double my) {
            return mx >= getX() && mx < getX() + getWidth() && my >= getY() && my < getY() + getHeight();
        }
    }

    private String prettyName(String key) {
        return switch (key) {
            case "coords"    -> Component.translatable("infooverlay.item.coords").getString();
            case "biome"     -> Component.translatable("infooverlay.item.biome").getString();
            case "day"       -> Component.translatable("infooverlay.item.day").getString();
            case "worldplay" -> Component.translatable("infooverlay.item.worldplay").getString();
            default -> key;
        };
    }
}
