package application;

import javafx.util.Duration;

class Song {
    private int id;
    private String title;
    private String artist;
    private String album;
    private Duration duration;
    private String filePath;
    private String format;

    public Song(String title, String artist, String album, Duration duration, String filePath, String format) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
        this.format = format;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public Duration getDuration() { return duration; }
    public String getFilePath() { return filePath; }
    public String getFormat() { return format; }

    @Override
    public String toString() {
        return String.format("%s - %s", artist, title);
    }
}
