package com.soundcraft.client.gui;

import com.soundcraft.SoundCraft;
import com.soundcraft.music.MusicManager;
import com.soundcraft.music.MusicState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SoundCraftControlScreen extends Screen {
    private static final int SCREEN_WIDTH = 220;
    private static final int SCREEN_HEIGHT = 130;
    
    public SoundCraftControlScreen() {
        super(Text.literal("SoundCraft Controller"));
    }

    @Override
    protected void init() {
        int x = (this.width - SCREEN_WIDTH) / 2;
        int y = (this.height - SCREEN_HEIGHT) / 2;

        // In Minecraft 1.21.1 Yarn, ButtonWidget.builder takes (Text message, PressAction onPress)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏮ Prev"), button -> {
            SoundCraft.getMusicManager().sendPrevCommand();
        }).dimensions(x + 15, y + SCREEN_HEIGHT - 35, 55, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏯ Play/Pause"), button -> {
            SoundCraft.getMusicManager().sendPlayPauseCommand();
        }).dimensions(x + 75, y + SCREEN_HEIGHT - 35, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏭ Next"), button -> {
            SoundCraft.getMusicManager().sendNextCommand();
        }).dimensions(x + 150, y + SCREEN_HEIGHT - 35, 55, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw the semi-transparent black background overlay directly (prevents "Can only blur once per frame" crashes on 1.21.2+)
        context.fill(0, 0, this.width, this.height, 0x60000000);

        int x = (this.width - SCREEN_WIDTH) / 2;
        int y = (this.height - SCREEN_HEIGHT) / 2;

        // Render beautiful Glassmorphism GUI Panel Background
        int glassBg = 0xD5121212; // Dark elegant high-opacity glass
        context.fill(x + 4, y, x + SCREEN_WIDTH - 4, y + SCREEN_HEIGHT, glassBg);
        context.fill(x, y + 4, x + 4, y + SCREEN_HEIGHT - 4, glassBg);
        context.fill(x + SCREEN_WIDTH - 4, y + 4, x + SCREEN_WIDTH, y + SCREEN_HEIGHT - 4, glassBg);

        // Specular glass border
        int borderCol = 0x40FFFFFF;
        context.fill(x + 4, y, x + SCREEN_WIDTH - 4, y + 1, borderCol);
        context.fill(x + 4, y + SCREEN_HEIGHT - 1, x + SCREEN_WIDTH - 4, y + SCREEN_HEIGHT, borderCol);
        context.fill(x, y + 4, x + 1, y + SCREEN_HEIGHT - 4, borderCol);
        context.fill(x + SCREEN_WIDTH - 1, y + 4, x + SCREEN_WIDTH, y + SCREEN_HEIGHT - 4, borderCol);

        MusicManager musicManager = SoundCraft.getMusicManager();
        MusicState state = musicManager.getCurrentState();

        // 1. Draw Cover Art (Size 48x48)
        Identifier coverTexture = musicManager.getCoverTexture();
        safeDrawTexture(context, coverTexture, x + 15, y + 15, 48, 48);

        // 2. Draw Metadata Text
        String title = state.getTitle();
        if (title.length() > 22) title = title.substring(0, 19) + "...";
        safeDrawText(context, this.textRenderer, "§l" + title, x + 75, y + 18, 0xFFFFFFFF, true);

        String artist = state.getArtist();
        if (artist.length() > 25) artist = artist.substring(0, 22) + "...";
        safeDrawText(context, this.textRenderer, artist, x + 75, y + 32, 0xFFCCCCCC, true);

        String status = state.isPlaying() ? "§a• Playing" : "§c• Paused";
        safeDrawText(context, this.textRenderer, status, x + 75, y + 46, 0xFFFFFFFF, true);

        // 3. Draw Accent Progress Bar
        float progress = state.getProgressFraction();
        int barW = SCREEN_WIDTH - 30;
        int barX = x + 15;
        int barY = y + 72;
        context.fill(barX, barY, barX + barW, barY + 3, 0x25FFFFFF); // Track background
        
        int filledW = (int) (barW * progress);
        int dominantColor = musicManager.getDominantColor();
        int accentColor = 0xFF000000 | (dominantColor & 0xFFFFFF);
        context.fill(barX, barY, barX + filledW, barY + 3, accentColor); // Accent filled progress

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't freeze the game in singleplayer
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
                if (params.length >= 10 && params[0] == java.util.function.Function.class && params[1] == Identifier.class) {
                    java.lang.reflect.Method getGuiMethod = null;
                    try {
                        getGuiMethod = RenderLayer.class.getMethod("getGui", Identifier.class);
                    } catch (NoSuchMethodException e) {
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
                if (params.length == 9 && params[0] == Identifier.class && (params[3] == float.class || params[3] == int.class)) {
                    try {
                        Object uVal = params[3] == float.class ? 0.0f : 0;
                        Object vVal = params[4] == float.class ? 0.0f : 0;
                        method.invoke(context, texture, x, y, uVal, vVal, width, height, width, height);
                        return;
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                if (params.length == 11 && params[0] == Identifier.class && (params[5] == float.class || params[5] == int.class)) {
                    try {
                        Object uVal = params[5] == float.class ? 0.0f : 0;
                        Object vVal = params[6] == float.class ? 0.0f : 0;
                        method.invoke(context, texture, x, y, width, height, uVal, vVal, width, height, width, height);
                        return;
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                if (params.length == 7 && params[0] == Identifier.class) {
                    try {
                        method.invoke(context, texture, x, y, 0, 0, width, height);
                        return;
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void safeDrawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        try {
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
