package Main;

import Installer.InstallerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Main extends Application{

        @Override
        public void start(Stage stage) throws Exception {

            //Check for docker if installed or not:
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("docker", "--version");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            boolean dockerIsInstalled = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Docker version")) {
                    dockerIsInstalled = true;
                    break;
                }

            }

            if (!dockerIsInstalled) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Message");
                alert.setContentText("Docker needs to be installed on your machine.");
                alert.showAndWait();
                System.exit(0);
            }


            stage.setTitle("Todo list application installer");
            stage.setResizable(false);
            stage.getIcons().add(new Image("/logo.png"));

            InstallerController installerController = new InstallerController();
            Parent parent = null;

            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/InstallerDesign.fxml"));
                loader.setController(installerController);
                parent = loader.load();

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Something went wrong.");
                alert.showAndWait();
                System.exit(0);
            }

            stage.setScene(new Scene(parent, 700, 500));

            stage.show();

        }

        public static void main(String[] args) {
            launch(args);
        }

}
