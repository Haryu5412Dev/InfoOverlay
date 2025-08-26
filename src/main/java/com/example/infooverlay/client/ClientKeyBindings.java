package com.example.infooverlay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 키 등록 + 처리
 */
public class ClientKeyBindings {
    /** HUD 표시 여부(런타임 토글). true = 보임 */
    public static boolean hudVisible = true;

    /** HUD 토글 키 */
    public static KeyMapping TOGGLE_HUD;

    /** 키 매핑 등록 (키 설정 메뉴에 나타남) */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        TOGGLE_HUD = new KeyMapping(
                "infooverlay.key.toggle_hud",     // 번역 키
                GLFW.GLFW_KEY_H,                  // 기본 키: H
                "infooverlay.key.category"        // 카테고리 번역 키
        );
        event.register(TOGGLE_HUD);
    }

    /** 클라이언트 틱에서 키 입력 처리 */
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // 게임이 로딩되기 전 등 Null 상황 회피
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // 한 번 누를 때마다 true를 반환 — while로 중복 입력도 처리
        while (TOGGLE_HUD != null && TOGGLE_HUD.consumeClick()) {
            hudVisible = !hudVisible;
            // (선택) 채팅/타이틀로 상태 안내하고 싶다면 아래 주석 해제
            // if (mc.player != null) {
            //     mc.player.displayClientMessage(
            //         Component.translatable(hudVisible ? "infooverlay.hud.on" : "infooverlay.hud.off"),
            //         true
            //     );
            // }
        }
    }
}
