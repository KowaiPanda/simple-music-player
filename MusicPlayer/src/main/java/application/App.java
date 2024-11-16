package application;

//importing necessary modules
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.sql.*;

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