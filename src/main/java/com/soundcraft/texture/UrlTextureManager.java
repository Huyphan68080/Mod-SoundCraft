package com.soundcraft.texture;

import com.soundcraft.SoundCraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UrlTextureManager {
    private static final ExecutorService DOWNLOAD_POOL = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "SoundCraft-Texture-Downloader");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<String, TextureCacheEntry> CACHE = new ConcurrentHashMap<>();

    public interface DownloadCallback {
        void onCompleted(Identifier identifier, int dominantColor);
    }

    public static class TextureCacheEntry {
        public final Identifier identifier;
        public final int dominantColor;

        public TextureCacheEntry(Identifier identifier, int dominantColor) {
            this.identifier = identifier;
            this.dominantColor = dominantColor;
        }
    }

    public static void downloadAndRegisterTextureAsync(String urlString, DownloadCallback callback) {
        if (CACHE.containsKey(urlString)) {
            TextureCacheEntry entry = CACHE.get(urlString);
            callback.onCompleted(entry.identifier, entry.dominantColor);
            return;
        }

        DOWNLOAD_POOL.submit(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    SoundCraft.LOGGER.error("[SoundCraft] Failed to download cover art. HTTP status: " + responseCode);
                    return;
                }

                try (InputStream inputStream = connection.getInputStream()) {
                    // Copy stream to byte array to allow reading twice (once for BufferedImage, once for NativeImage)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    byte[] imageData = baos.toByteArray();

                    // 1. Process dominant color extraction in background thread
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
                    int dominantColor = extractDominantColor(bufferedImage);

                    // Convert BufferedImage to PNG bytes since NativeImage.read expects PNG signature
                    ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
                    byte[] pngDataBytes;
                    if (bufferedImage != null) {
                        ImageIO.write(bufferedImage, "png", pngBaos);
                        pngDataBytes = pngBaos.toByteArray();
                    } else {
                        pngDataBytes = imageData; // Fallback to raw bytes
                    }

                    // 2. Schedule GL texture creation on the Minecraft Client render thread
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngDataBytes));
                            NativeImageBackedTexture dynamicTexture = null;
                            for (java.lang.reflect.Constructor<?> constructor : NativeImageBackedTexture.class.getConstructors()) {
                                Class<?>[] paramTypes = constructor.getParameterTypes();
                                if (paramTypes.length == 1 && paramTypes[0] == NativeImage.class) {
                                    dynamicTexture = (NativeImageBackedTexture) constructor.newInstance(nativeImage);
                                    break;
                                } else if (paramTypes.length == 2 && paramTypes[0] == java.util.function.Supplier.class && paramTypes[1] == NativeImage.class) {
                                    java.util.function.Supplier<String> nameSupplier = () -> "soundcraft_cover";
                                    dynamicTexture = (NativeImageBackedTexture) constructor.newInstance(nameSupplier, nativeImage);
                                    break;
                                }
                            }
                            
                            if (dynamicTexture == null) {
                                throw new IllegalStateException("[SoundCraft] Failed to find a suitable constructor for NativeImageBackedTexture");
                            }
                            
                            // Generate unique identifier using hash code of URL
                            String textureName = "cover_" + Math.abs(urlString.hashCode());
                            Identifier identifier = Identifier.of("soundcraft", textureName);
                            
                            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, dynamicTexture);
                            
                            TextureCacheEntry entry = new TextureCacheEntry(identifier, dominantColor);
                            CACHE.put(urlString, entry);
                            
                            callback.onCompleted(identifier, dominantColor);
                        } catch (Exception e) {
                            SoundCraft.LOGGER.error("[SoundCraft] Error registering GL Texture on client thread", e);
                        }
                    });
                }
            } catch (Exception e) {
                SoundCraft.LOGGER.error("[SoundCraft] Failed to download or process cover image from: " + urlString, e);
            }
        });
    }

    /**
     * Extracts the average vibrant dominant color from the image.
     * Skips near-grayscale, absolute white, and absolute black pixels to get actual colorful accents.
     */
    private static int extractDominantColor(BufferedImage image) {
        if (image == null) return 0xFF00E6FF; // Default cyan glow fallback

        int width = image.getWidth();
        int height = image.getHeight();
        
        long sumHue = 0;
        long sumSat = 0;
        long sumBright = 0;
        int count = 0;

        // Sample in a 16x16 grid for high performance and accurate sampling
        int stepX = Math.max(1, width / 16);
        int stepY = Math.max(1, height / 16);

        for (int x = 0; x < width; x += stepX) {
            for (int y = 0; y < height; y += stepY) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);

                // Exclude desaturated (saturation < 0.15) and very dark or very bright colors
                if (hsb[1] > 0.15f && hsb[2] > 0.15f && hsb[2] < 0.95f) {
                    sumHue += (long) (hsb[0] * 36000);
                    sumSat += (long) (hsb[1] * 1000);
                    sumBright += (long) (hsb[2] * 1000);
                    count++;
                }
            }
        }

        // If no colorful pixel was found, retry without filters
        if (count == 0) {
            for (int x = 0; x < width; x += stepX) {
                for (int y = 0; y < height; y += stepY) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
                    sumHue += (long) (hsb[0] * 36000);
                    sumSat += (long) (hsb[1] * 1000);
                    sumBright += (long) (hsb[2] * 1000);
                    count++;
                }
            }
        }

        if (count == 0) return 0xFF00E6FF;

        float avgHue = (sumHue / (float) count) / 36000.0f;
        float avgSat = (sumSat / (float) count) / 1000.0f;
        float avgBright = (sumBright / (float) count) / 1000.0f;

        // Boost saturation slightly to make the neon progress bar look good
        avgSat = Math.min(1.0f, avgSat * 1.25f);
        // Ensure brightness is high enough to show up well against dark glassmorphism
        avgBright = Math.max(0.7f, avgBright);

        return java.awt.Color.HSBtoRGB(avgHue, avgSat, avgBright);
    }
}
