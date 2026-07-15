package com.soundcraft.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.soundcraft.SoundCraft;
import com.soundcraft.music.MusicManager;
import com.soundcraft.music.MusicState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class MusicHUD {
    private final MinecraftClient client;
    
    // HUD size and spacing (Pinterest micro-widget layout)
    private static final int HUD_WIDTH = 135;
    private static final int HUD_HEIGHT = 34;
    private static final int PADDING = 5;
    private static final int COVER_SIZE = 24;

    private float hudAlpha = 1.0f;
    private long lastCombatTime = 0;

    public MusicHUD(MinecraftClient client) {
        this.client = client;
    }

    public void render(DrawContext context, float tickDelta) {
        if (client.options.hudHidden || client.player == null) {
            return;
        }

        // Combat Detection: Check if player is holding a weapon (Sword, Mace, Trident, Axe) using Registry IDs to avoid ClassNotFoundException
        boolean holdingWeapon = false;
        if (client.player != null) {
            net.minecraft.item.ItemStack mainHand = client.player.getMainHandStack();
            if (mainHand != null && !mainHand.isEmpty()) {
                net.minecraft.item.Item item = mainHand.getItem();
                net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ITEM.getId(item);
                if (id != null) {
                    String path = id.getPath().toLowerCase();
                    holdingWeapon = path.endsWith("_sword") 
                            || path.equals("mace") 
                            || path.endsWith("_axe") 
                            || path.equals("trident");
                }
            }
        }

        // Only trigger combat hiding if holding a weapon AND performing attack action (or took damage)
        boolean isAttacking = client.player.hurtTime > 0 
                || (holdingWeapon && (
                    client.player.handSwinging 
                    || client.player.getAttacking() != null 
                    || client.options.attackKey.isPressed() 
                    || client.player.getAttackCooldownProgress(0.5f) < 0.99f
                ));

        if (isAttacking) {
            lastCombatTime = System.currentTimeMillis();
        }

        // Fading Logic: fade out to 0% alpha for 2 seconds after last combat action, else fade in to 100%
        long timeSinceCombat = System.currentTimeMillis() - lastCombatTime;
        float targetAlpha = (timeSinceCombat < 2000) ? 0.0f : 1.0f;
        
        // Smoothly interpolate transparency using lerp
        hudAlpha = MathHelper.lerp(tickDelta * 0.1f, hudAlpha, targetAlpha);

        // Completely skip rendering when invisible to save draw calls
        if (hudAlpha < 0.01f) {
            return;
        }

        MusicManager musicManager = SoundCraft.getMusicManager();
        MusicState state = musicManager.getCurrentState();
        
        // Top-Right placement coordinates
        int screenWidth = client.getWindow().getScaledWidth();
        int x = screenWidth - HUD_WIDTH - 8;
        int y = 8;

        // Render Widget Shadows (Gives depth to glass panel)
        renderShadow(context, x, y, HUD_WIDTH, HUD_HEIGHT);

        // Render Glassmorphism Base (Dark semi-transparent glass body + white specular outline)
        renderGlassPanel(context, x, y, HUD_WIDTH, HUD_HEIGHT);

        // 1. Render Cover Art (Left side, size 24x24)
        Identifier coverTexture = musicManager.getCoverTexture();
        safeDrawTexture(context, coverTexture, x + PADDING, y + PADDING, COVER_SIZE, COVER_SIZE);

        // 2. Render Text Metadata (Right side, marquee supported)
        TextRenderer font = client.textRenderer;
        int textX = x + PADDING + COVER_SIZE + PADDING;
        int maxTextWidth = HUD_WIDTH - (PADDING * 3) - COVER_SIZE;

        // Render Song Title (Bold White)
        String title = state.getTitle();
        int titleWidth = font.getWidth(title);
        int titleY = y + 5;
        int titleColor = ((int) (255 * hudAlpha) << 24) | 0xFFFFFF;

        if (titleWidth > maxTextWidth) {
            // Infinite marquee scrolling math
            double elapsedSeconds = System.currentTimeMillis() / 1000.0;
            int speed = 25; // Pixels per second
            int scrollRange = titleWidth + 50; // Text width + space padding (larger for no-overlap)
            int offset = (int) ((elapsedSeconds * speed) % scrollRange);

            context.enableScissor(textX, y + PADDING, textX + maxTextWidth, y + HUD_HEIGHT - PADDING);
            safeDrawText(context, font, title, textX - offset, titleY, titleColor, false);
            safeDrawText(context, font, title, textX - offset + scrollRange, titleY, titleColor, false);
            context.disableScissor();
        } else {
            safeDrawText(context, font, title, textX, titleY, titleColor, false);
        }

        // Render Artist (Smaller, dimmed grey-white)
        String artist = state.getArtist();
        int artistWidth = font.getWidth(artist);
        int artistY = y + 16;
        int artistColor = ((int) (180 * hudAlpha) << 24) | 0xCCCCCC;

        if (artistWidth > maxTextWidth) {
            double elapsedSeconds = System.currentTimeMillis() / 1000.0;
            int speed = 20;
            int scrollRange = artistWidth + 50; // Spacious padding
            int offset = (int) ((elapsedSeconds * speed) % scrollRange);

            context.enableScissor(textX, y + PADDING, textX + maxTextWidth, y + HUD_HEIGHT - PADDING);
            safeDrawText(context, font, artist, textX - offset, artistY, artistColor, false);
            safeDrawText(context, font, artist, textX - offset + scrollRange, artistY, artistColor, false);
            context.disableScissor();
        } else {
            safeDrawText(context, font, artist, textX, artistY, artistColor, false);
        }

        // 3. Render Dynamic Accent Progress Bar (2px height, hugging the bottom border)
        float progress = state.getProgressFraction();
        int progressBarWidth = (int) (HUD_WIDTH * progress);
        int progressBarY = y + HUD_HEIGHT - 2;

        int dominantColor = musicManager.getDominantColor();
        // Extracted dominant color with HUD alpha mapping
        int accentColor = ((int) (255 * hudAlpha) << 24) | (dominantColor & 0xFFFFFF);
        int trackColor = ((int) (60 * hudAlpha) << 24) | (dominantColor & 0xFFFFFF);

        // Draw progress background track
        context.fill(x, progressBarY, x + HUD_WIDTH, progressBarY + 2, trackColor);
        // Draw progress filled bar
        context.fill(x, progressBarY, x + progressBarWidth, progressBarY + 2, accentColor);

    }

    private void renderShadow(DrawContext context, int x, int y, int w, int h) {
        int shadowColor = ((int) (70 * hudAlpha) << 24) | 0x000000;
        // Subtle shifted shadow
        context.fill(x + 1, y + 1, x + w + 1, y + h + 1, shadowColor);
    }

    /**
     * Renders a notched rounded rectangle simulating a glass card layout.
     * Includes a subtle translucent border to simulate glass thickness/specularity.
     */
    private void renderGlassPanel(DrawContext context, int x, int y, int w, int h) {
        // Base dark translucent background (glass layer)
        int glassBg = ((int) (130 * hudAlpha) << 24) | 0x121212;

        // Notched rounded rectangle fill
        context.fill(x + 2, y, x + w - 2, y + h, glassBg);
        context.fill(x, y + 2, x + 2, y + h - 2, glassBg);
        context.fill(x + w - 2, y + 2, x + w, y + h - 2, glassBg);

        // Render Glass Border Specularity (light overlay highlight)
        int glassBorder = ((int) (40 * hudAlpha) << 24) | 0xFFFFFF;

        // Horizontal lines
        context.fill(x + 2, y, x + w - 2, y + 1, glassBorder);
        context.fill(x + 2, y + h - 1, x + w - 2, y + h, glassBorder);
        
        // Vertical lines
        context.fill(x, y + 2, x + 1, y + h - 2, glassBorder);
        context.fill(x + w - 1, y + 2, x + w, y + h - 2, glassBorder);

        // Diagonal corners
        context.fill(x + 1, y + 1, x + 2, y + 2, glassBorder); // Top-Left
        context.fill(x + w - 2, y + 1, x + w - 1, y + 2, glassBorder); // Top-Right
        context.fill(x + 1, y + h - 2, x + 2, y + h - 1, glassBorder); // Bottom-Left
        context.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, glassBorder); // Bottom-Right
    }

    private void safeDrawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        try {
            // 1. Try modern 1.21.2+ drawTexture with float dimensions (no RenderPipeline)
            for (java.lang.reflect.Method method : DrawContext.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 9 
                        && params[0] == Identifier.class 
                        && params[1] == int.class 
                        && params[2] == int.class 
                        && params[3] == int.class 
                        && params[4] == int.class 
                        && params[5] == float.class 
                        && params[6] == float.class 
                        && params[7] == float.class 
                        && params[8] == float.class) {
                    try {
                        // method_70845 is drawTexturedQuad(Identifier texture, int x1, int y1, int x2, int y2, float u1, float u2, float v1, float v2)
                        method.invoke(context, texture, x, y, x + width, y + height, 0.0f, 1.0f, 0.0f, 1.0f);
                        return;
                    } catch (Exception ex) {
                        SoundCraft.LOGGER.error("[SoundCraft] method_70845 invocation failed for texture " + texture, ex);
                    }
                }
            }

            // 2. Try modern 1.21.2+ texture drawing with RenderLayer Function provider
            for (java.lang.reflect.Method method : DrawContext.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                // Check if first param is Function and second is Identifier
                if (params.length >= 10 && params[0] == java.util.function.Function.class && params[1] == Identifier.class) {
                    // Find getGui(Identifier) dynamically in RenderLayer class
                    java.lang.reflect.Method getGuiMethod = null;
                    try {
                        getGuiMethod = RenderLayer.class.getMethod("getGui", Identifier.class);
                    } catch (NoSuchMethodException e) {
                        // Scan for a static method that takes Identifier and returns RenderLayer
                        for (java.lang.reflect.Method m : RenderLayer.class.getDeclaredMethods()) {
                            if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) 
                                    && m.getParameterCount() == 1 
                                    && m.getParameterTypes()[0] == Identifier.class 
                                    && m.getReturnType() == RenderLayer.class) {
                                getGuiMethod = m;
                                break;
                            }
                        }
                    }

                    if (getGuiMethod != null) {
                        final java.lang.reflect.Method finalGetGui = getGuiMethod;
                        java.util.function.Function<Identifier, RenderLayer> renderLayerProvider = id -> {
                            try {
                                return (RenderLayer) finalGetGui.invoke(null, id);
                            } catch (Exception ex) {
                                return null;
                            }
                        };

                        if (params.length == 10) {
                            method.invoke(context, renderLayerProvider, texture, x, y, 0.0f, 0.0f, width, height, width, height);
                        } else if (params.length == 11) {
                            method.invoke(context, renderLayerProvider, texture, x, y, 0.0f, 0.0f, width, height, width, height, 0xFFFFFF);
                        }
                        return;
                    }
                }
            }
            
            // 3. Fallback for 1.21.1- style (or development env):
            for (java.lang.reflect.Method method : DrawContext.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                // drawTexture(Identifier, int, int, float, float, int, int, int, int)
                if (params.length == 9 && params[0] == Identifier.class && (params[3] == float.class || params[3] == int.class)) {
                    try {
                        Object uVal = params[3] == float.class ? 0.0f : 0;
                        Object vVal = params[4] == float.class ? 0.0f : 0;
                        method.invoke(context, texture, x, y, uVal, vVal, width, height, width, height);
                        return;
                    } catch (Exception ex) {
                        SoundCraft.LOGGER.error("[SoundCraft] Fallback 9-param drawTexture failed", ex);
                    }
                }
                // drawTexture(Identifier, int, int, int, int, float, float, int, int, int, int)
                if (params.length == 11 && params[0] == Identifier.class && (params[5] == float.class || params[5] == int.class)) {
                    try {
                        Object uVal = params[5] == float.class ? 0.0f : 0;
                        Object vVal = params[6] == float.class ? 0.0f : 0;
                        method.invoke(context, texture, x, y, width, height, uVal, vVal, width, height, width, height);
                        return;
                    } catch (Exception ex) {
                        SoundCraft.LOGGER.error("[SoundCraft] Fallback 11-param drawTexture failed", ex);
                    }
                }
                // drawTexture(Identifier, int, int, int, int, int, int)
                if (params.length == 7 && params[0] == Identifier.class) {
                    try {
                        method.invoke(context, texture, x, y, 0, 0, width, height);
                        return;
                    } catch (Exception ex) {
                        SoundCraft.LOGGER.error("[SoundCraft] Fallback 7-param drawTexture failed", ex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void safeDrawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        try {
            // Find the drawText method matching (TextRenderer, String, int, int, int, boolean) signature dynamically
            for (java.lang.reflect.Method method : DrawContext.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 6 
                        && params[0] == TextRenderer.class 
                        && params[1] == String.class 
                        && params[2] == int.class 
                        && params[3] == int.class 
                        && params[4] == int.class 
                        && params[5] == boolean.class) {
                    method.invoke(context, textRenderer, text, x, y, color, shadow);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
