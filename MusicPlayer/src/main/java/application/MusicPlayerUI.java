package application;

import javafx.util.Duration;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import java.io.File;
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