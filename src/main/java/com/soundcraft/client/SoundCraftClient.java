package com.soundcraft.client;

import com.soundcraft.SoundCraft;
import com.soundcraft.client.hud.MusicHUD;
import com.soundcraft.client.gui.SoundCraftControlScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class SoundCraftClient implements ClientModInitializer {
    private static MusicHUD musicHud;
    
    // De-bounce states for GLFW key presses to ensure single-trigger on click
    private static boolean wasPlayPausePressedLastTick = false;
    private static boolean wasNextPressedLastTick = false;
    private static boolean wasPrevPressedLastTick = false;
    private static boolean wasGuiKeyPressedLastTick = false;

    @Override
    public void onInitializeClient() {
        // Instantiate the drawing widget
        musicHud = new MusicHUD(MinecraftClient.getInstance());

        // Log DrawContext method signatures to latest.log to aid compatibility checking
        try {
            SoundCraft.LOGGER.info("[SoundCraft] Scanning DrawContext methods:");
            for (java.lang.reflect.Method method : DrawContext.class.getDeclaredMethods()) {
                StringBuilder sb = new StringBuilder();
                sb.append(method.getReturnType().getSimpleName())
                  .append(" ")
                  .append(method.getName())
                  .append("(");
                for (Class<?> p : method.getParameterTypes()) {
                    sb.append(p.getSimpleName()).append(", ");
                }
                if (method.getParameterTypes().length > 0) {
                    sb.setLength(sb.length() - 2); // Trim trailing comma
                }
                sb.append(")");
                SoundCraft.LOGGER.info("[SoundCraft] Method signature: " + sb.toString());
            }
        } catch (Exception e) {
            SoundCraft.LOGGER.error("[SoundCraft] Error scanning DrawContext methods", e);
        }

        // Register client tick event to check key presses directly via GLFW
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getWindow() == null) return;
            
            // If the player has a screen open, only check if it is our custom control screen (to handle close/reset state), otherwise ignore
            if (client.currentScreen != null) {
                wasPlayPausePressedLastTick = false;
                wasNextPressedLastTick = false;
                wasPrevPressedLastTick = false;
                wasGuiKeyPressedLastTick = true; // prevent immediate re-open if they close via J
                return;
            }

            long handle = client.getWindow().getHandle();
            
            // Directly query GLFW key states (Numpad 5, 6, 4 and J key)
            boolean isPlayPausePressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_5) == GLFW.GLFW_PRESS;
            boolean isNextPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_6) == GLFW.GLFW_PRESS;
            boolean isPrevPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_KP_4) == GLFW.GLFW_PRESS;
            boolean isGuiKeyPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_J) == GLFW.GLFW_PRESS;

            // Trigger commands on key-down event (rising edge)
            if (isPlayPausePressed && !wasPlayPausePressedLastTick) {
                SoundCraft.getMusicManager().sendPlayPauseCommand();
            }
            if (isNextPressed && !wasNextPressedLastTick) {
                SoundCraft.getMusicManager().sendNextCommand();
            }
            if (isPrevPressed && !wasPrevPressedLastTick) {
                SoundCraft.getMusicManager().sendPrevCommand();
            }
            if (isGuiKeyPressed && !wasGuiKeyPressedLastTick) {
                // Open the controller screen on the main render thread
                client.setScreen(new SoundCraftControlScreen());
            }

            // Save state for the next tick de-bounce check
            wasPlayPausePressedLastTick = isPlayPausePressed;
            wasNextPressedLastTick = isNextPressed;
            wasPrevPressedLastTick = isPrevPressed;
            wasGuiKeyPressedLastTick = isGuiKeyPressed;
        });
    }

    public static MusicHUD getMusicHud() {
        return musicHud;
    }
}
