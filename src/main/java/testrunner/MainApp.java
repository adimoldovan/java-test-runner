package testrunner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import testrunner.controllers.MainViewController;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    public static Logger log = LogManager.getLogger(MainApp.class.getName());
    private Stage primaryStage;
    private BorderPane rootLayout;

    public static void main(String[] args) {
        launch(args);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public BorderPane getRootLayout() {
        return rootLayout;
    }

    public void setRootLayout(BorderPane rootLayout) {
        this.rootLayout = rootLayout;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("TestRunnerApp");
        initRootLayout();
        showMainView();
    }

    /**
     * Initializes the root layout.
     */
    private void initRootLayout() {
        try {
            // Load root layout from fxml file.
            URL fxmlFileUrl = getClass().getClassLoader().getResource("view/RootLayout.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlFileUrl);
            rootLayout = (BorderPane) loader.load(fxmlFileUrl.openStream());

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            String css = getClass().getClassLoader().getResource("css/runner.css").toExternalForm();
            scene.getStylesheets().add(css);
            primaryStage.setScene(scene);
            primaryStage.getIcons().add(new Image("runner.png"));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * Shows the main screen inside the root layout.
     */
    private void showMainView() {
        try {
            // Load person overview.
            URL fxmlFileUrl = getClass().getClassLoader().getResource("view/MainView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlFileUrl);
            AnchorPane mainView = (AnchorPane) loader.load(fxmlFileUrl.openStream());

            // Set main screen into the center of root layout.
            rootLayout.setCenter(mainView);

            MainViewController mainViewController = loader.getController();
            mainViewController.setMainApp(this);

        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }
}
