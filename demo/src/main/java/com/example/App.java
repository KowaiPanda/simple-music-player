package com.example;

//importing necessary modules
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;

import java.io.File;
import java.sql.*;
import java.util.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.collections.FXCollections;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ClipboardContent;
import java.util.stream.Collectors;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

//Main class where the application is sets up
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        
        try{
            // Initialize components
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.jpeg")));
            AudioPlayer audioPlayer = new AudioPlayer();
            MusicLibrary library = new MusicLibrary("musicdb.sqlite");
            PlaylistManager playlistManager = new PlaylistManager(library.getConnection());
            
            // Create UI
            MusicPlayerUI ui = new MusicPlayerUI(audioPlayer, library, playlistManager);
            
            // Set up stage
            Scene scene = new Scene(ui, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/darkTheme.css").toExternalForm());
            primaryStage.setTitle("Music Player");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(SQLException e) {
            System.out.println("Error with sql shi");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

//Defining the Song datatype
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

// 1. Audio Player Class
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

// 2. Music Library Class
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

// 3. Playlist Manager Class
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

// 4. QueueManager Class
class QueueManager {
    private List<Song> queue;
    private int currentIndex;

    public QueueManager() {
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
    }

    public void setQueue(List<Song> songs) {
        this.queue = new ArrayList<>(songs);
        this.currentIndex = -1;
    }

    public void addToQueue(Song song) {
        queue.add(song);
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            if (index < currentIndex) currentIndex--;
        }
    }

    public Song getNextSong() {
        if (queue.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % queue.size();
        return queue.get(currentIndex);
    }

    public Song getPreviousSong() {
        if (queue.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        return queue.get(currentIndex);
    }

    public List<Song> getQueue() {
        return new ArrayList<>(queue);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}

// 5. Music Player UI Class and implementation of Queue
class MusicPlayerUI extends BorderPane {
    private AudioPlayer audioPlayer;
    private MusicLibrary library;
    private PlaylistManager playlistManager;
    private ListView<Song> songListView;
    private ListView<String> playlistListView;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button nextButton;
    private Button previousButton;
    private Label nowPlayingLabel;
    private QueueManager queueManager;
    private ListView<Song> playlistContentView;
    private ListView<Song> queueListView;
    private TabPane centerTabPane;
    MenuItem playNowItem = new MenuItem("Play Now");
    MenuItem addToQueueItem = new MenuItem("Add to Queue");
    Menu addToPlaylistMenu = new Menu("Add to Playlist");
    
    public MusicPlayerUI(AudioPlayer audioPlayer, MusicLibrary library, PlaylistManager playlistManager) {
        this.audioPlayer = audioPlayer;
        this.library = library;
        this.playlistManager = playlistManager;
        this.queueManager = new QueueManager();

        initializeUI();
        setupQueueHandling();
    }

    private void initializeUI() {
        VBox leftPanel = createLeftPanel();
        TabPane centerPanel = createCenterPanel();
        VBox bottomPanel = createBottomPanel();
        
        Button addSongsButton = new Button("Add Songs");
        addSongsButton.setOnAction(e -> addSongsFromDirectory());
        leftPanel.getChildren().add(addSongsButton);
        
        setLeft(leftPanel);
        setCenter(centerPanel);
        setBottom(bottomPanel);
    
        setupContextMenus();
        addSearchFeature();
        setupDragAndDrop();
    }
    
    private void addSongsFromDirectory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music Files");
    
        FileChooser.ExtensionFilter musicFilter = new FileChooser.ExtensionFilter(
            "Music Files", "*.mp3", "*.wav", "*.m4a", "*.aac");
        fileChooser.getExtensionFilters().add(musicFilter);
    
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getScene().getWindow());
    
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                try {
                    AudioFile f = AudioFileIO.read(file);
                    Tag tag = f.getTag();

                    String fileName = file.getName();
                    String title = fileName.replaceFirst("[.][^.]+$", "");
                    String[] parts = fileName.split("[.][^.]+$");
                    String extension = parts.length > 1 ? parts[1] : "";
                    String artist = tag.getFirst(FieldKey.ARTIST);
                    String album = tag.getFirst(FieldKey.ALBUM);
                    int length = f.getAudioHeader().getTrackLength();

                    if(artist.length()==0) artist = "Unknown Artist";
                    if(album.length()==0) album = "Unknown Album";
    
                    Song song = new Song(
                        title,
                        artist,  
                        album, 
                        Duration.minutes(length),
                        file.getAbsolutePath(),
                        extension
                    );

                    if (playlistManager.getCurrentPlaylist() == null) {
                        System.out.println("No current playlist is selected.");
                        return;
                    }

                    library.addSong(song);
                    playlistManager.addToPlaylist(playlistManager.getCurrentPlaylistName(), song);
                } catch (Exception e) {
                    System.err.println("Error adding file: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
        //Refreshing the listview
        songListView.setItems(FXCollections.observableArrayList(library.getAllSongs()));
    }    

    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new javafx.geometry.Insets(10));
        
        Label playlistLabel = new Label("Playlists");
        playlistListView = new ListView<>();
        playlistListView.getItems().addAll(playlistManager.getPlaylistNames());
        
        Button createPlaylistButton = new Button("New Playlist");
        createPlaylistButton.setOnAction(e ->{
            showCreatePlaylistDialog();
            refreshAddToPlaylistMenu();
        });
        
        panel.getChildren().addAll(playlistLabel, playlistListView, createPlaylistButton);
        return panel;
    }

    private TabPane createCenterPanel() {
        centerTabPane = new TabPane();
    
        Tab libraryTab = new Tab("Library");
        libraryTab.setClosable(false);
        VBox libraryBox = new VBox(10);
        libraryBox.setPadding(new javafx.geometry.Insets(10));
        
        songListView = new ListView<>();
        songListView.getItems().addAll(library.loadSongsFromDatabase());
        songListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selectedSong = songListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    playSong(selectedSong);
                }
            }
        });
        
        libraryBox.getChildren().addAll(new Label("Library"), songListView);
        libraryTab.setContent(libraryBox);
        
        Tab playlistTab = new Tab("Current Playlist");
        playlistTab.setClosable(false);
        VBox playlistBox = new VBox(10);
        playlistBox.setPadding(new javafx.geometry.Insets(10));
        
        playlistContentView = new ListView<>();
        playlistBox.getChildren().addAll(new Label("Playlist Contents"), playlistContentView);
        playlistTab.setContent(playlistBox);
        
        Tab queueTab = new Tab("Queue");
        queueTab.setClosable(false);
        VBox queueBox = new VBox(10);
        queueBox.setPadding(new javafx.geometry.Insets(10));
        
        queueListView = new ListView<>();
        Button clearQueueButton = new Button("Clear Queue");
        clearQueueButton.setOnAction(e -> clearQueue());
        
        queueBox.getChildren().addAll(new Label("Current Queue"), queueListView, clearQueueButton);
        queueTab.setContent(queueBox);
        
        centerTabPane.getTabs().addAll(libraryTab, playlistTab, queueTab);
        return centerTabPane;
    }

    private VBox createBottomPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new javafx.geometry.Insets(10));
        
        HBox controlsBox = new HBox(10);
        playButton = new Button("▶");
        pauseButton = new Button("⏸");
        stopButton = new Button("⏹");
        previousButton = new Button("⏮");
        nextButton = new Button("⏭");
        
        playButton.setOnAction(e -> audioPlayer.play());
        pauseButton.setOnAction(e -> audioPlayer.pause());
        stopButton.setOnAction(e -> audioPlayer.stop());
        
        controlsBox.getChildren().addAll(
            previousButton, playButton, pauseButton, stopButton, nextButton
        );
        
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        
        volumeSlider = new Slider(0, 1, 1);
        volumeSlider.setOrientation(Orientation.HORIZONTAL);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            audioPlayer.setVolume(newVal.doubleValue()));
        
        nowPlayingLabel = new Label("No song playing");
        
        panel.getChildren().addAll(
            nowPlayingLabel,
            progressSlider,
            controlsBox,
            new HBox(10, new Label("Volume:"), volumeSlider)
        );
        return panel;
    }

    private void playSong(Song song) {
        audioPlayer.loadSong(song);
        audioPlayer.play();
        updateNowPlaying(song);
        startProgressUpdater();

        audioPlayer.mediaPlayer.setOnEndOfMedia(() -> {
            playNextSong();
        });
    }

    private void updateNowPlaying(Song song) {
        nowPlayingLabel.setText(String.format("Now Playing: %s - %s", song.getTitle(), song.getArtist()));
    }

    private void startProgressUpdater() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e -> {
                if (audioPlayer.isPlaying()) {
                    double currentTime = audioPlayer.getCurrentTime().toSeconds();
                    double totalTime = audioPlayer.getTotalDuration().toSeconds();
                    if (totalTime > 0) {
                        progressSlider.setValue((currentTime / totalTime) * 100);
                    }
                }
            })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void showCreatePlaylistDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter playlist name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.isEmpty()) {
                playlistManager.createPlaylist(name);
                playlistListView.getItems().add(name);
            }
        });
    }

    private void setupQueueHandling() {
        nextButton.setOnAction(e -> playNextSong());
        previousButton.setOnAction(e -> playPreviousSong());
        
        playlistListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    playlistManager.setCurrentPlaylist(newValue);
                    updatePlaylistContent();
                }
            }
        );
    }

    private void updatePlaylistContent() {
        String currentPlaylist = playlistManager.getCurrentPlaylistName();
        if (currentPlaylist != null) {
            List<Song> playlistSongs = playlistManager.getPlaylist(currentPlaylist);
            playlistContentView.setItems(FXCollections.observableArrayList(playlistSongs));
        }
    }

    private void playNextSong() {
        Song nextSong = queueManager.getNextSong();
        if (nextSong != null) {
            playSong(nextSong);
        }
    }

    private void playPreviousSong() {
        Song previousSong = queueManager.getPreviousSong();
        if (previousSong != null) {
            playSong(previousSong);
        }
    }

    private void clearQueue() {
        queueManager.setQueue(new ArrayList<>());
        updateQueueView();
    }

    private void updateQueueView() {
        queueListView.setItems(FXCollections.observableArrayList(queueManager.getQueue()));
    }

    private void setupContextMenus() {
        ContextMenu songContextMenu = createSongContextMenu();
        songListView.setContextMenu(songContextMenu);
        
        ContextMenu playlistContextMenu = createPlaylistContextMenu();
        playlistContentView.setContextMenu(playlistContextMenu);
        
        ContextMenu queueContextMenu = createQueueContextMenu();
        queueListView.setContextMenu(queueContextMenu);
    }

    private ContextMenu createSongContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        playNowItem.setId("playNowItem");
        playNowItem.setOnAction(e -> {
            Song selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playSong(selectedSong);
            }
        });
        
        addToQueueItem.setId("addToQueueItem");
        addToQueueItem.setOnAction(e -> {
            Song selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                queueManager.addToQueue(selectedSong);
                updateQueueView();
            }
        });
        
        playlistManager.getPlaylistNames().forEach(playlistName -> {
            MenuItem playlistItem = new MenuItem(playlistName);
            playlistItem.setOnAction(e -> {
                Song selectedSong = songListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    playlistManager.addToPlaylist(playlistName, selectedSong);
                    updatePlaylistContent();
                }
            });
            addToPlaylistMenu.getItems().add(playlistItem);
        });
        
        menu.getItems().addAll(playNowItem, addToQueueItem, addToPlaylistMenu);
        return menu;
    }

    private ContextMenu createPlaylistContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem playNowItem = new MenuItem("Play Now");
        playNowItem.setId("playNowItem");
        playNowItem.setOnAction(e -> {
            Song selectedSong = playlistContentView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playSong(selectedSong);
            }
        });
        
        MenuItem addToQueueItem = new MenuItem("Add to Queue");
        addToQueueItem.setId("addToQueueItem");
        addToQueueItem.setOnAction(e -> {
            Song selectedSong = playlistContentView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                queueManager.addToQueue(selectedSong);
                updateQueueView();
            }
        });
    
        MenuItem removeFromPlaylistItem = new MenuItem("Remove from Playlist");
        removeFromPlaylistItem.setId("removeFromPlaylistItem");
        removeFromPlaylistItem.setOnAction(e -> {
            Song selectedSong = playlistContentView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playlistManager.removeFromPlaylist(
                    playlistManager.getCurrentPlaylistName(), 
                    selectedSong
                );
                updatePlaylistContent();
            }
        });
        
        MenuItem playEntirePlaylist = new MenuItem("Play Entire Playlist");
        playEntirePlaylist.setId("playEntirePlaylist");
        playEntirePlaylist.setOnAction(e -> {
            List<Song> playlistSongs = playlistManager.getCurrentPlaylist();
            if (!playlistSongs.isEmpty()) {
                queueManager.setQueue(playlistSongs);
                updateQueueView();
                playSong(playlistSongs.get(0));
            }
        });
        
        menu.getItems().addAll(playNowItem, addToQueueItem, removeFromPlaylistItem, playEntirePlaylist);
        return menu;
    }

    private ContextMenu createQueueContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem removeFromQueueItem = new MenuItem("Remove from Queue");
        removeFromQueueItem.setId("removeFromQueueItem");
        removeFromQueueItem.setOnAction(e -> {
            int selectedIndex = queueListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                queueManager.removeFromQueue(selectedIndex);
                updateQueueView();
            }
        });
        
        menu.getItems().add(removeFromQueueItem);
        return menu;
    }

    private void addSearchFeature() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search songs...");
        songListView.setItems(FXCollections.observableArrayList(library.getAllSongs()));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                songListView.setItems(FXCollections.observableArrayList(library.getAllSongs()));
            } else {
                List<Song> filteredSongs = library.getAllSongs().stream()
                    .filter(song -> 
                        song.getTitle().toLowerCase().contains(newValue.toLowerCase()) ||
                        song.getArtist().toLowerCase().contains(newValue.toLowerCase()) ||
                        song.getAlbum().toLowerCase().contains(newValue.toLowerCase())
                    )
                    .collect(Collectors.toList());
                songListView.setItems(FXCollections.observableArrayList(filteredSongs));
            }
        });

        TabPane tabPane = (TabPane) getCenter();
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        VBox tabContent = (VBox) selectedTab.getContent();
        tabContent.getChildren().add(0, searchField);
    }

    private void refreshAddToPlaylistMenu() {
        addToPlaylistMenu.getItems().clear();
        
        playlistManager.getPlaylistNames().forEach(playlistName -> {
            MenuItem playlistItem = new MenuItem(playlistName);
            playlistItem.setOnAction(e -> {
                Song selectedSong = songListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    playlistManager.addToPlaylist(playlistName, selectedSong);
                    updatePlaylistContent();
                }
            });
            addToPlaylistMenu.getItems().add(playlistItem);
        });
    }

    private void setupDragAndDrop() {
        songListView.setOnDragDetected(e -> {
            Song selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                Dragboard db = songListView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedSong.getTitle());
                db.setContent(content);
                e.consume();
            }
        });

        playlistListView.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        playlistListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
                if (selectedPlaylist != null) {
                    playlistManager.setCurrentPlaylist(selectedPlaylist);
                }
            }
        });

        playlistListView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String playlistName = playlistListView.getSelectionModel().getSelectedItem();
                Song draggedSong = songListView.getSelectionModel().getSelectedItem();
                if (playlistName != null && draggedSong != null) {
                    playlistManager.addToPlaylist(playlistName, draggedSong);
                    updatePlaylistContent();
                    success = true;
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }
}