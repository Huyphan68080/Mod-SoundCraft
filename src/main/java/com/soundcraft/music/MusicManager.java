package com.soundcraft.music;

import com.soundcraft.SoundCraft;
import com.soundcraft.texture.UrlTextureManager;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class MusicManager {
    private MusicState currentState = new MusicState();
    private Identifier currentCoverIdentifier = null;
    private int dominantColor = 0xFFFFFFFF; // Default to white
    private boolean isDownloading = false;
    private String lastDownloadedUrl = "";

    // Default placeholder texture for when no music is playing or art is loading
    private static final Identifier PLACEHOLDER_COVER = Identifier.of("soundcraft", "textures/gui/placeholder.png");

    public synchronized void updateState(MusicState newState) {
        if (newState == null) return;
        
        // Track changes in songs
        boolean trackChanged = !Objects.equals(this.currentState.getTitle(), newState.getTitle()) ||
                               !Objects.equals(this.currentState.getArtist(), newState.getArtist());
        
        this.currentState = newState;

        if (trackChanged || !Objects.equals(this.lastDownloadedUrl, newState.getArtworkUrl())) {
            String url = newState.getArtworkUrl();
            if (url != null && !url.isEmpty()) {
                triggerCoverDownload(url);
            } else {
                this.currentCoverIdentifier = null;
                this.dominantColor = 0xFFCCCCCC; // Default grayish color
            }
        }
    }

    private void triggerCoverDownload(String url) {
        this.lastDownloadedUrl = url;
        this.isDownloading = true;

        UrlTextureManager.downloadAndRegisterTextureAsync(url, (identifier, color) -> {
            synchronized (this) {
                // Ensure we don't apply an outdated cover if the song changed in the meantime
                if (Objects.equals(this.lastDownloadedUrl, url)) {
                    this.currentCoverIdentifier = identifier;
                    this.dominantColor = color;
                    this.isDownloading = false;
                }
            }
        });
    }

    public synchronized MusicState getCurrentState() {
        return currentState;
    }

    public synchronized Identifier getCoverTexture() {
        if (currentCoverIdentifier != null) {
            return currentCoverIdentifier;
        }
        return PLACEHOLDER_COVER;
    }

    public synchronized int getDominantColor() {
        return dominantColor;
    }

    public synchronized boolean isDownloading() {
        return isDownloading;
    }

    public void sendPlayPauseCommand() {
        if (SoundCraft.getWebSocketServer() != null) {
            SoundCraft.getWebSocketServer().sendCommand("CMD_PLAY_PAUSE");
        }
    }

    public void sendNextCommand() {
        if (SoundCraft.getWebSocketServer() != null) {
            SoundCraft.getWebSocketServer().sendCommand("CMD_NEXT");
        }
    }

    public void sendPrevCommand() {
        if (SoundCraft.getWebSocketServer() != null) {
            SoundCraft.getWebSocketServer().sendCommand("CMD_PREV");
        }
    }
}
