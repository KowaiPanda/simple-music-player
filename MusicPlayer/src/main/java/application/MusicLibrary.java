package application;

import java.sql.*;
import java.util.*;
import javafx.util.Duration;

class MusicLibrary {
    private Connection db;
    private List<Song> songs;

    // Constructor to initialize list 'song'
    public MusicLibrary(String dbPath) {
        songs = new ArrayList<>();
        initializeDatabase(dbPath);
    }

    // Setting up the database
    private void initializeDatabase(String dbPath) {
        try {
            db = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // Establishing connection to the database
    public Connection getConnection() throws SQLException {
        if (db == null || db.isClosed()) {
            throw new SQLException("Database connection is not initialized or has been closed");
        }
        return db;
    }

    // Creating the table 'songs'
    private void createTables() {
        String createSongsTable = 
            "CREATE TABLE IF NOT EXISTS songs (" +
            "id INTEGER PRIMARY KEY, " +
            "title TEXT, " +
            "artist TEXT, " +
            "album TEXT, " +
            "duration INTEGER, " +
            "path TEXT, " +
            "format TEXT" +
            ")";
        
        try (Statement stmt = db.createStatement()) {
            stmt.execute(createSongsTable);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    // Loading songs to the list 'song' if there is any records in the table 'songs'
    public List<Song> loadSongsFromDatabase() {

        String sql = "SELECT * FROM songs";

        try (Statement statement = db.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String artist = resultSet.getString("artist");
                String album = resultSet.getString("album");
                long duration = resultSet.getLong("duration");
                String path = resultSet.getString("path");
                String format = resultSet.getString("format");

                // Create a new Song object
                Song song = new Song(title, artist, album, Duration.seconds(duration) , path, format);
                songs.add(song); // Reading and adding them one by one
            }
        } catch (SQLException e) {
            System.err.println("Error loading songs from database: " + e.getMessage());
        }
        return songs;
    }

    // Function to add a song into the table 'songs'
    public void addSong(Song song) {
        String sql = "INSERT INTO songs (title, artist, album, duration, path, format) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = db.prepareStatement(sql)) {
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setLong(4, (long) song.getDuration().toMinutes());
            pstmt.setString(5, song.getFilePath());
            pstmt.setString(6, song.getFormat());
            
            pstmt.executeUpdate();
            songs.add(song);
        } catch (SQLException e) {
            System.err.println("Error adding song: " + e.getMessage());
        }
    }

    // Returns an ArrayList which contains all the records of the table 'songs'
    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

}