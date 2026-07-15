package com.soundcraft.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.soundcraft.SoundCraft;
import com.soundcraft.music.MusicState;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;

public class SoundCraftWebSocketServer extends WebSocketServer {
    private static final Gson GSON = new Gson();
    private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SoundCraftWebSocketServer(int port) {
        // Bind strictly to localhost (127.0.0.1) for security & bypassing firewall warnings
        super(new InetSocketAddress("127.0.0.1", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        SoundCraft.LOGGER.info("[SoundCraft] SoundCloud Extension client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        SoundCraft.LOGGER.info("[SoundCraft] SoundCloud Extension client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SoundCraft.LOGGER.info("[SoundCraft] Received WebSocket message: " + message);
        try {
            MusicState state = GSON.fromJson(message, MusicState.class);
            if (state != null) {
                // Update active music state in manager
                SoundCraft.getMusicManager().updateState(state);
            }
        } catch (Exception e) {
            SoundCraft.LOGGER.error("[SoundCraft] Failed to parse WebSocket message: " + message, e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        SoundCraft.LOGGER.error("[SoundCraft] WebSocket Server encountered an error", ex);
    }

    @Override
    public void onStart() {
        SoundCraft.LOGGER.info("[SoundCraft] Local WebSocket Server successfully started on ws://127.0.0.1:" + getPort());
    }

    /**
     * Sends a command (e.g. CMD_PLAY_PAUSE, CMD_NEXT, CMD_PREV) back to the Chrome extension.
     */
    public void sendCommand(String commandName) {
        JsonObject json = new JsonObject();
        json.addProperty("command", commandName);
        String payload = GSON.toJson(json);

        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send(payload);
            }
        }
    }
}
