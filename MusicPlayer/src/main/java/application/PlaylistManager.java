package application;

import java.sql.*;
import java.util.*;
import javafx.util.Duration;

class PlaylistManager {
    private Connection db;
    private Map<String, List<Song>> playlists;
    private String currentPlaylist; 
    private int i;
    public PlaylistManager(Connection db) {
        this.db = db;
        this.playlists = new HashMap<>();
        this.currentPlaylist = null;  // Initially, no current playlist is set
        createPlaylistTable();
        loadPlaylists();
    }

    // Creating the table 'playlists' and 'playlist_songs'
    private void createPlaylistTable() {
        String createPlaylistsTable = 
            "CREATE TABLE IF NOT EXISTS playlists (" +
            "id INTEGER PRIMARY KEY, " +
            "name TEXT UNIQUE" +
            ")";
        
        String createPlaylistSongsTable = 
            "CREATE TABLE IF NOT EXISTS playlist_songs (" +
            "playlist_id INTEGER, " +
            "song_id INTEGER, " +
            "position INTEGER, " +
            "FOREIGN KEY (playlist_id) REFERENCES playlists(id), " +
            "FOREIGN KEY (song_id) REFERENCES songs(id)" +
            ")";

        try (Statement stmt = db.createStatement()) {
            stmt.execute(createPlaylistsTable);
            stmt.execute(createPlaylistSongsTable);
        } catch (SQLException e) {
            System.err.println("Error creating playlist tables: " + e.getMessage());
        }
    }

    // Creating a playlist
    public void createPlaylist(String name) {
        String sql = "INSERT INTO playlists (name) VALUES (?)";
        
        try (PreparedStatement pstmt = db.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            playlists.put(name, new ArrayList<>());
        } catch (SQLException e) {
            System.err.println("Error creating playlist: " + e.getMessage());
        }
    }

    // Functions to add and remove songs from the current playlist
    public void addToPlaylist(String playlistName, Song song) {
        List<Song> playlist = playlists.get(playlistName);
        if (playlist != null) {
            playlist.add(song);
            savePlaylistToDB(playlistName, song, 2);
        }
    }
    public void removeFromPlaylist(String playlistName, Song song) {
        List<Song> playlist = playlists.get(playlistName);
        if (playlist != null) {
            playlist.remove(song);
            savePlaylistToDB(playlistName, song, 1);
        }
    }

    // Adds or removes entries from the 'playlist_songs' table
    private void savePlaylistToDB(String playlistName, Song song, int op) {
        try {
            // First, get playlist ID
            String sql = "SELECT id FROM playlists WHERE name = ?";
            PreparedStatement pstmt = db.prepareStatement(sql);
            pstmt.setString(1, playlistName);
            ResultSet rs = pstmt.executeQuery();

            sql = "SELECT * FROM playlist_songs ORDER BY position DESC LIMIT 1";
            pstmt = db.prepareStatement(sql);
            ResultSet r1 = pstmt.executeQuery();
            i = r1.getInt("position");

            sql = "SELECT id FROM songs WHERE title = ?";
            pstmt = db.prepareStatement(sql);
            pstmt.setString(1, song.getTitle());
            ResultSet r = pstmt.executeQuery();
            
            if (rs.next()) {
                int playlistId = rs.getInt("id");
                int songId = r.getInt("id");

                if(op==1){
                    // Clear a song record
                    sql = "DELETE FROM playlist_songs WHERE song_id = ?";
                    pstmt = db.prepareStatement(sql);
                    pstmt.setInt(1, songId);
                    pstmt.executeUpdate();
                }
                if(op==2){
                    // Add current songs
                    i++;
                    sql = "INSERT INTO playlist_songs (playlist_id, song_id, position) VALUES (?, ?, ?)";
                    pstmt = db.prepareStatement(sql);
                    pstmt.setInt(1, playlistId);
                    pstmt.setInt(2, songId);  
                    pstmt.setInt(3, i);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving playlist: " + e.getMessage());
        }
    }

    // Maps the playlist with its songs
    private void loadPlaylists() {
        try {
            Statement stmt = db.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM playlists");
            
            while (rs.next()) {
                String name = rs.getString("name");
                playlists.put(name, new ArrayList<>());
                
                // Load songs for this playlist
                PreparedStatement pstmt = db.prepareStatement(
                    "SELECT s.* FROM songs s " +
                    "JOIN playlist_songs ps ON s.id = ps.song_id " +
                    "WHERE ps.playlist_id = ? " +
                    "ORDER BY ps.position"
                );
                pstmt.setInt(1, rs.getInt("id"));
                ResultSet songRs = pstmt.executeQuery();
                
                while (songRs.next()) {
                    Song song = new Song(
                        songRs.getString("title"),
                        songRs.getString("artist"),
                        songRs.getString("album"),
                        Duration.seconds(songRs.getLong("duration")),
                        songRs.getString("path"),
                        songRs.getString("format")
                    );
                    playlists.get(name).add(song);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading playlists: " + e.getMessage());
        }
    }

    // Functions to get a playlist name and an ArrayList of the playlist
    public List<String> getPlaylistNames() {
        return new ArrayList<>(playlists.keySet());
    }
    public List<Song> getPlaylist(String name) {
        return new ArrayList<>(playlists.getOrDefault(name, new ArrayList<>()));
    }

    // Set the current playlist by name
    public void setCurrentPlaylist(String name) {
        if (playlists.containsKey(name)) {
            this.currentPlaylist = name;
        } else {
            System.err.println("Playlist does not exist: " + name);
        }
    }

    // Get the current playlist
    public List<Song> getCurrentPlaylist() {
        if (currentPlaylist == null) {
            System.err.println("No current playlist is selected.");
            return new ArrayList<>();
        }
        return playlists.getOrDefault(currentPlaylist, new ArrayList<>());
    }

    // Get the current playlist name
    public String getCurrentPlaylistName() {
        return currentPlaylist;
    }
}
