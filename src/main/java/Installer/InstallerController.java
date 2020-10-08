package Installer;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

public class InstallerController implements Initializable {

    @FXML
    private TextField pathTxtFld;

    @FXML
    private Button choosePathBtn;

    @FXML
    private TextArea logsTxtA;

    @FXML
    private Button executeBtn;

    @FXML
    private Button cancelBtn;

    @FXML
    private Circle connectionStatusC;

    @FXML
    private Label connectionStatusLbl;

    private ProcessStatus downloadingJarStatus, pullingImageStatus, initializingImageStatus;

    private Thread connectionChecker;

    public InstallerController() {

        connectionChecker = new Thread() {
            @Override
            public void run() {

                Timeline connectionCheckerTimeLine = new Timeline(
                        new KeyFrame(
                                Duration.seconds(10),
                                e -> {
                                    if (isThereAnInternetConnection()) {
                                        connectionStatusC.setFill(Color.FORESTGREEN);
                                        connectionStatusLbl.setText("Online");
                                    } else {
                                        connectionStatusC.setFill(Color.RED);
                                        connectionStatusLbl.setText("Offline");
                                    }
                                }
                        )
                );

                connectionCheckerTimeLine.setCycleCount(Timeline.INDEFINITE);
                connectionCheckerTimeLine.play();
            }

        };

    }

    private boolean isThereAnInternetConnection() {

        try {
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();

            return true;

        } catch (Exception e) {
            return false;
        }

    }

    private void downloadTodoListJar() {

        try {
            ProcessExecutor processExecutor = new ProcessExecutor();
            processExecutor.directory(new File(pathTxtFld.getText()));

            processExecutor.command("curl", "-L",
                    "https://github.com/MostafaTwfiq/To-Do-List/releases/download/v1.0.0/ToDoList-1.0.0.jar",
                    "-o", "ToDoList-1.0.0.jar")
                    .redirectOutput(new LogOutputStream() {

                        @Override
                        protected void processLine(String line) {
                            logsTxtA.appendText(line + "\n");
                        }
                    }).execute();

            downloadingJarStatus = ProcessStatus.DONE;
            logsTxtA.appendText("\n<<<<<<<<Done downloading todo list jar>>>>>>>>\n\n");
        } catch (Exception e) {
            downloadingJarStatus = ProcessStatus.FAIL;
        }

    }

    private void pullDatabaseDockerImage() {

        try {
            ProcessExecutor processExecutor = new ProcessExecutor();
            processExecutor.directory(new File(pathTxtFld.getText()));

            processExecutor.command("docker", "pull", "am429/todolist_app_database_container")
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            logsTxtA.appendText(line + "\n");
                        }
                    }).execute();

            pullingImageStatus = ProcessStatus.DONE;
            logsTxtA.appendText("\n<<<<<<<<Done downloading the data base docker image>>>>>>>>\n\n");

        } catch (Exception e) {
            pullingImageStatus = ProcessStatus.FAIL;
        }

    }

    private void initializeDockerImage() {

        try {
            ProcessExecutor processExecutor = new ProcessExecutor();
            processExecutor.directory(new File(pathTxtFld.getText()));

            processExecutor.command("docker", "run", "-d", "-p", "1212:3306", "--name", "TodoListApp", "am429/todolist_app_database_container")
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            logsTxtA.appendText(line + "\n");
                        }
                    }).execute();

            initializingImageStatus = ProcessStatus.DONE;
            logsTxtA.appendText("\n<<<<<<<<Done initializing the docker image>>>>>>>>\n\n");

        } catch (Exception e) {
            initializingImageStatus = ProcessStatus.FAIL;
        }

    }

    private void popDoneMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText("The application installed successfully.");
        alert.showAndWait();
    }

    private void setupExecuteBtn() {
        executeBtn.setOnAction(e -> {

            if (!isThereAnInternetConnection()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Message");
                alert.setContentText("The installing process needs an internet connection.");
                alert.showAndWait();
                unlockScreen();
                return;
            } else if (pathTxtFld.getText().isEmpty() || pathTxtFld.getText().isBlank()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Message");
                alert.setContentText("Please enter the folder path to download the jar.");
                alert.showAndWait();
                unlockScreen();
                return;
            }

            downloadingJarStatus = ProcessStatus.NONE;
            pullingImageStatus = ProcessStatus.NONE;
            initializingImageStatus = ProcessStatus.NONE;
            logsTxtA.clear();

            Thread thread = new Thread() {

                @Override
                public void run() {

                    lockUpScreen();

                    downloadTodoListJar();
                    if (downloadingJarStatus == ProcessStatus.FAIL) {
                        raiseAnError();
                        return;
                    }

                    pullDatabaseDockerImage();
                    if (pullingImageStatus == ProcessStatus.FAIL) {
                        raiseAnError();
                        return;
                    }

                    initializeDockerImage();
                    if (initializingImageStatus == ProcessStatus.FAIL) {
                        raiseAnError();
                        return;
                    }

                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            popDoneMessage();
                        }
                    });

                    unlockScreen();

                }

            };

            thread.start();

        });
    }

    private boolean checkForErrors(String line) {
        String lineLowerCase = line.toLowerCase();
        return lineLowerCase.contains("error") || lineLowerCase.contains("fail") || lineLowerCase.contains("failed");
    }

    private void writeIntoLog(Process process) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                logsTxtA.appendText(line + "\n");

                if (checkForErrors(line))
                    raiseAnError();

            }
        } catch (Exception e) {
            raiseAnError();
        }

    }

    private void raiseAnError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Something went wrong, please try again.");
        alert.showAndWait();
        unlockScreen();
        logsTxtA.clear();
    }

    private void setupChoosePathBtn() {
        choosePathBtn.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose application directory");
            File selectedDirectory = directoryChooser.showDialog(new Stage());
            if (selectedDirectory != null)
                pathTxtFld.setText(selectedDirectory.getAbsolutePath());

        });
    }

    private void setupCancelBtn() {
        cancelBtn.setOnAction(e -> {
            System.exit(0);
        });
    }

    private void lockUpScreen() {
        choosePathBtn.setDisable(true);
        executeBtn.setDisable(true);
        cancelBtn.setDisable(true);
    }

    private void unlockScreen() {
        choosePathBtn.setDisable(false);
        executeBtn.setDisable(false);
        cancelBtn.setDisable(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        setupCancelBtn();
        setupChoosePathBtn();
        setupExecuteBtn();

        pathTxtFld.setDisable(true);
        logsTxtA.setEditable(false);

        if (isThereAnInternetConnection()) {
            connectionStatusC.setFill(Color.FORESTGREEN);
            connectionStatusLbl.setText("Online");
        } else {
            connectionStatusC.setFill(Color.RED);
            connectionStatusLbl.setText("Offline");
        }

        connectionChecker.start();

    }


}
