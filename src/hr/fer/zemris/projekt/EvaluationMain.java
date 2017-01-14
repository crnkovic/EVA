package hr.fer.zemris.projekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jcodec.api.JCodecException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationMain extends Application {
    /**
     * Path to the video file.
     */
    private String videoPath = null;

    /**
     * Dumping directory.
     */
    private File dumpDir = null;

    /**
     * Evaluation file.
     */
    private File evaluationFile = null;

    /**
     * Rectangle area multiplier.
     */
    private int rectangleSizeMultiplier = 1;

    /**
     * Instance of the <b>Controller</b> class.
     */
    public Controller controller;

    /**
     * Instance of the Java <b>Stage</b> class which acts as a main UI container.
     */
    public Stage primaryStage;

    /**
     * Map of marked frames.
     * Each key-value pair consists of the index of the frame as well as list of <b>Rectangle</b> objects.
     */
    private Map<Integer, List<javafx.scene.shape.Rectangle>> markedFrames;

    /**
     * Launch the application by running the launch() method from the parent <b>Application</b> class.
     * Accepts command line arguments which then passes on to the launch() method.
     *
     * @param args Input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * @return int
     */
    public int getRectangleSizeMultiplier() {
        return rectangleSizeMultiplier;
    }

    /**
     * Set video file path and clean up the dumping directory so we have a fresh directory.
     *
     * @param videoPath Path to the video
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    public void setVideoPath(String videoPath) throws IOException, JCodecException {
        this.videoPath = videoPath;

        // Get first frame then calculate the width of the rectangle
        BufferedImage bufferedImage = VideoUtil.getFrame(videoPath, 0);
        rectangleSizeMultiplier = 900 / bufferedImage.getWidth();

        // Clean the dumping directory when we set video path.
        if (isDumpingDirSet()) {
            cleanDumpFolder();
        }
    }

    /**
     * Removes dumping directory, but first clean it up.
     *
     * @return True if dumping directory is removed, false otherwise.
     */
    private boolean removeDumpDir() {
        cleanDumpFolder();

        return dumpDir.delete();
    }

    /**
     * Cleans up dump folder.
     * Deletes all files inside the directory.
     */
    private void cleanDumpFolder() {
        File[] files = dumpDir.listFiles();

        // If empty, may produce null, so we gotta check.
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Get path to the video file.
     *
     * @return Video file path
     */
    public String getVideoPath() {
        return videoPath;
    }

    /**
     * Get dumping directory as an instance of <b>File</b> class.
     *
     * @return Dumping directory
     */
    public File getDumpDir() {
        return dumpDir;
    }

    /**
     * Set dumping directory. Accepts <b>File</b> object and if one exists (the dumping directory), remove it.
     *
     * @param dumpDir Dumping directory
     */
    public void setDumpDir(File dumpDir) {
        if (isDumpingDirSet()) {
            removeDumpDir();
        }

        this.dumpDir = dumpDir;
    }

    /**
     * Check if dumping directory is initialized.
     *
     * @return True if dumping directory is initialized, false otherwise.
     */
    public boolean isDumpingDirSet() {
        return dumpDir != null;
    }

    /**
     * Check if video path is initialized.
     *
     * @return True if video path is initialized, false otherwise.
     */
    public boolean isVideoDirSet() {
        return videoPath != null;
    }

    /**
     * Get the evaluation file.
     *
     * @return Evaluation file
     */
    public File getEvaluationFile() {
        return evaluationFile;
    }

    /**
     * Set the file that needs to be evaluated.
     *
     * @param evaluationFile Evaluation file
     */
    public void setEvaluationFile(File evaluationFile) {
        this.evaluationFile = evaluationFile;
    }

    /**
     * Get all marked frames.
     *
     * @return Marked frames
     */
    public Map<Integer, List<javafx.scene.shape.Rectangle>> getMarkedFrames() {
        return markedFrames;
    }

    /**
     * Get specific marked frame from the <b>MarkedFrames</b> map.
     *
     * @param frameNumber Marked frame index
     * @return Marked frame
     */
    public List<javafx.scene.shape.Rectangle> getMarkedFrame(int frameNumber) {
        return markedFrames.get(frameNumber);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Aplikacija za evaluaciju");

        markedFrames = new HashMap<>();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        // Set default ("root") scene to the container.
        Scene scene = new Scene(root);
        this.primaryStage.setScene(scene);

        // Let controller have access to the application.
        controller = loader.getController();
        controller.setUp(scene, this);

        // Set an event which handles removing dumping directory when the application closes.
        this.primaryStage.setOnCloseRequest(event -> {
            if (isDumpingDirSet()) {
                removeDumpDir();
            }
        });

        this.primaryStage.show();
    }
}