package com.soundcraft.music;

public class MusicState {
    private String title = "No Track Playing";
    private String artist = "Unknown Artist";
    private String artworkUrl = "";
    private boolean isPlaying = false;
    private int currentTime = 0;
    private int duration = 0;

    public MusicState() {}

    public MusicState(String title, String artist, String artworkUrl, boolean isPlaying, int currentTime, int duration) {
        this.title = title;
        this.artist = artist;
        this.artworkUrl = artworkUrl;
        this.isPlaying = isPlaying;
        this.currentTime = currentTime;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public int getDuration() {
        return duration;
    }

    public float getProgressFraction() {
        if (duration <= 0) return 0.0f;
        return (float) currentTime / duration;
    }

    @Override
    public String toString() {
        return "MusicState{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", artworkUrl='" + artworkUrl + '\'' +
                ", isPlaying=" + isPlaying +
                ", currentTime=" + currentTime +
                ", duration=" + duration +
                '}';
    }
}
