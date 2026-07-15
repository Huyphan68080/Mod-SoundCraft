package com.soundcraft;

import com.soundcraft.music.MusicManager;
import com.soundcraft.websocket.SoundCraftWebSocketServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundCraft implements ModInitializer {
    public static final String MOD_ID = "soundcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MusicManager musicManager;
    private static SoundCraftWebSocketServer webSocketServer;

    @Override
    public void onInitialize() {
        LOGGER.info("[SoundCraft] Initializing SoundCraft Mod...");

        // Initialize state manager
        musicManager = new MusicManager();

        // Start WebSocket Server on port 8887
        try {
            webSocketServer = new SoundCraftWebSocketServer(8887);
            webSocketServer.start();
        } catch (Exception e) {
            LOGGER.error("[SoundCraft] CRITICAL: Failed to launch local WebSocket Server on port 8887!", e);
        }

        // JVM shutdown hook to ensure the server socket is released when closing Minecraft
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[SoundCraft] Stopping WebSocket Server...");
            try {
                if (webSocketServer != null) {
                    webSocketServer.stop();
                }
            } catch (Exception e) {
                LOGGER.error("[SoundCraft] Error stopping WebSocket Server during shutdown", e);
            }
        }));
    }

    public static MusicManager getMusicManager() {
        return musicManager;
    }

    public static SoundCraftWebSocketServer getWebSocketServer() {
        return webSocketServer;
    }
}
