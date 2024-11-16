package application;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;

class AudioPlayer {
    public MediaPlayer mediaPlayer;
    private boolean isPlaying;
    private double volume;
    private Song currentSong;

    // Constructor to initialize values
    public AudioPlayer() {
        this.volume = 1.0;
        this.isPlaying = false;
    }

    public void loadSong(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
        
        try {
            Media media = new Media(new File(song.getFilePath()).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volume);
            currentSong = song;
            
            mediaPlayer.setOnEndOfMedia(() -> {
                stop();
            });
        } catch (Exception e) {
            System.err.println("Error loading song: " + e.getMessage());
        }
    }

    //Various functions to implement basic player functions play, pause, stop, seek, adjusting volume
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            isPlaying = true;
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
        }
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(duration);
        }
    }

    public void setVolume(double volume) {
        this.volume = volume;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Duration getCurrentTime() {
        return mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO;
    }

    public Duration getTotalDuration() {
        return mediaPlayer != null ? mediaPlayer.getTotalDuration() : Duration.ZERO;
    }
}