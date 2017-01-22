package hr.fer.zemris.projekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jcodec.api.JCodecException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private Map<Integer, Set<EditRectangle>> markedFrames;

    /**
     * Launch the application by running the launch() method from the parent <b>Application</b> class.
     * Accepts command line arguments which then passes on to the launch() method.
     *
     * @param args Input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public void setVideoPath(String videoPath) throws IOException, JCodecException {
        this.videoPath = videoPath;

        if (isDumpingDirSet()) {
            removeDumpDir();
        }


		File tempDir = new File("./temp");
		tempDir.mkdir();

		if (!tempDir.canWrite()) {
			throw new IOException("Ne može se pisati u \"temp\" datoteku.");
		}
		dumpDir = tempDir;

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
    public void cleanDumpFolder() {
        if (!isDumpingDirSet()) return;

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
    public Map<Integer, Set<EditRectangle>> getMarkedFrames() {
        return markedFrames;
    }

    public void removeMarkedFrame(int id) {
        markedFrames.remove(id);
    }

    public void updateMarkedFrame(int id, Set<EditRectangle> list) {
        markedFrames.put(id, list);
    }

    /**
     * Get specific marked frame from the <b>MarkedFrames</b> map.
     *
     * @param frameNumber Marked frame index
     * @return Marked frame
     */
    public Set<EditRectangle> getMarkedFrame(int frameNumber) {
        return markedFrames.get(frameNumber);
    }

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage= primaryStage;
        primaryStage.setTitle("Aplikacija za evaluaciju");

        // Empty out the marked frames map
        markedFrames = new HashMap<>();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        // Set default ("root") scene to the container.
        Scene scene = new Scene(root);
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("main.css").toExternalForm());

        primaryStage.setScene(scene);

        // Let controller have access to the application.
        Controller controller = loader.getController();
        controller.setUp(scene, this);

        // Set an event which handles removing dumping directory as the application closes.
        primaryStage.setOnCloseRequest(event -> {
            if (isDumpingDirSet()) {
                removeDumpDir();
            }
        });

        primaryStage.show();
    }
    public void pack(){
        primaryStage.sizeToScene();
    }

	public void clearMarkedFrames() {
	 this.markedFrames.clear();
	}

}