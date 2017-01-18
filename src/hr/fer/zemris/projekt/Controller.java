package hr.fer.zemris.projekt;

import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;
import javax.xml.soap.Text;
import java.awt.*;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class Controller implements Initializable {
    /**
     * Current window scene.
     */
    private Scene scene;

    /**
     * Reference to the main application.
     */
    private EvaluationMain evaluationMainApp;

    /**
     * Offset to the next frame.
     */
    private static final int FRAME_HOP = 15;

    /**
     * Active frame number.
     */
    @FXML
    private Label frameNumberField;

    /**
     * Frame slider.
     */
    @FXML
    private Slider frameSlider;

    /**
     * Shows image of the football field (of the current frame).
     */
    @FXML
    private ImageView footballFieldImage;

    private Rectangle selectedId = null;

    /**
     * List of marked frames.
     */
    @FXML
    private ListView<String> markedFramesList;

    /**
     * Recall value.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    @FXML
    private Label recallValue;

    /**
     * Precision value.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    @FXML
    private Label precisionValue;

    /**
     * F-measure value.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    @FXML
    private Label f1Value;

    /**
     * <b>TextField</b> object containing computed Jaccard index.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    @FXML
    private TextField jaccardIndex;

    /**
     * Image pane which consists of the rendered image from the frame.
     */
    @FXML
    private Pane imagePane;

    /**
     * X coordinate of where the user started drawing the rectangle.
     */
    private double beginningX;

    /**
     * Y coordinate of where the user started drawing the rectangle.
     */
    private double beginningY;

    /**
     * User is drawing a rectangle.
     */
    private boolean drawingInitialized = false;

    /**
     * Acceptable video extensions.
     */
    private String[] videoExtensions = {"*.mp4", "*.avi", "*.mkv", "*.webm", "*.mov"};

    /**
     * Contains last drawn rectangle.
     */
    private javafx.scene.shape.Rectangle lastRectangle;

    @FXML
    private TextField frameNumberTextField;

    /**
     * List containing drawn rectangles.
     */
    private List<javafx.scene.shape.Rectangle> drawnRectangles;

    /**
     * List containing indices of removed frames.
     */
    static List<Integer> removedFrames = new ArrayList<>();

    /**
     * Called when the frame slider's position is changed.
     * Sets the new frame based on slider value.
     *
     * @param event Event
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    @FXML
    public void newFrameSelected(Event event) throws IOException, JCodecException {
        setSelectedFrame(setLabelForSliderValue());
        frameNumberTextField.clear();
    }

    /**
     * Called when user pressed the "choose video" button. Only accepts video extensions.
     * Collects number of frames in the video, sets up frame slider.
     *
     * @param actionEvent Event
     * @throws FrameGrabber.Exception Exception
     * @throws IOException            IOException
     * @throws JCodecException        JCodecException
     */
    @FXML
    public void setVideoAndSetUp(ActionEvent actionEvent) throws FrameGrabber.Exception, IOException, JCodecException {
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Video (" + String.join(", ", videoExtensions).replaceAll("\\*", "") + ")", videoExtensions);

        // Show file chooser to the user and let it choose a video
        // Only accepts video extensions defined in a class property
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Odaberite video za evaluaciju");
        File file = fileChooser.showOpenDialog(scene.getWindow());

        // Set path to the video in the main application and collect number of frames from the video
        evaluationMainApp.setVideoPath(file.getPath());
        int numberOfFrames = VideoUtil.getNumberOfFrames(file.getPath());

        // Set up slider
        frameSlider.setMax(numberOfFrames / FRAME_HOP);
        frameSlider.setMin(0);
        frameSlider.setBlockIncrement(50);

        // Reset ground truth file
        evaluationMainApp.setEvaluationFile(null);

        File tempDir = new File("./temp");
        tempDir.mkdir();

        if (!tempDir.canWrite()) {
            throw new IOException("Ne može se pisati u \"temp\" datoteku.");
        }

        // Set up the reference to the dumping directory in the main application
        evaluationMainApp.setDumpDir(tempDir);

        setSelectedFrame(1);
        setLabelForSliderValue();

        frameSlider.setDisable(false);
    }

    @FXML
    public void njihovo() throws IOException, JCodecException {
        if (evaluationMainApp.getEvaluationFile() == null) {
            Message.error("Greška!", "Niste učitali datoteku s detektiranim oznakama.");

            return;
        }

        List<javafx.scene.shape.Rectangle> rectangles = rectanglesForAFrame(getFrameNumber((long) frameSlider.getValue()));

        drawnRectangles.addAll(rectangles);

        System.out.println(frameSlider.getValue());
        System.out.println(getFrameNumber((long) frameSlider.getValue()));

        for (Rectangle rectangle : rectangles) {
            rectangle.setDisable(false);
            rectangle.setFill(null);
            rectangle.setStroke(javafx.scene.paint.Color.RED);
            rectangle.setStrokeWidth(1);

            imagePane.getChildren().add(rectangle);
        }
    }

    /**
     * Called when user pressed the "choose evaluation file" button. File should contain all the ground truth references.
     * Sets up a reference to that file in the main application.
     *
     * @param actionEvent Event
     */
    @FXML
    public void setEvaluationFile(ActionEvent actionEvent) throws IOException, JCodecException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odaberite datoteku s referentnim oznakama");

        evaluationMainApp.setEvaluationFile(fileChooser.showOpenDialog(scene.getWindow()));
    }

    /**
     * Called when the user presses the evaluate button. Does the magic.
     * Runs through each ground truth marked frame, and calculates the properties using the Jaccard index.
     * // TODO: more explanation
     *
     * @param actionEvent Event
     */
    @FXML
    public void evaluate(ActionEvent actionEvent) {
        // Make sure video is loaded
        if (!evaluationMainApp.isVideoDirSet()) {
            Message.error("Video nije učitan!", "Molimo učitajte video te pokušajte ponovno.");

            return;
        }
        if (evaluationMainApp.getEvaluationFile() == null) {
            Message.warning("Upozorenje!", "Niste unijeli datoteku sa oznakama.");
            return;
        }

        // There are no frames to compare to?
        if (evaluationMainApp.getMarkedFrames().size() == 0) {
            Message.warning("Upozorenje!", "Niste spremili oznake.");

            return;
        }
        if (jaccardIndex.getText().isEmpty()) {
            Message.warning("Upozorenje!", "Niste unijeli prag za Jaccardov index.");
            return;
        }
        try {
            Float.parseFloat(jaccardIndex.getText());
        } catch (Exception e) {
            Message.warning("Upozorenje!", "Niste unijeli pravilan izraz za Jaccardov index. Provjerite je li broj zadan s točkom.");
            return;
        }

        // Initialize default computed properties to 0
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

        // Loop through the marked frames and do the magic for each one
        for (int frameNumber : evaluationMainApp.getMarkedFrames().keySet()) {
            // Get ground truth rectangles and user-defined rectangles for this specific frame
            List<javafx.scene.shape.Rectangle> groundTruthRectangles = evaluationMainApp.getMarkedFrame(frameNumber);
            List<javafx.scene.shape.Rectangle> detectedRectangles = rectanglesForAFrame(frameNumber);

            // Set false negatives count to all detected rectangles size, so we can decrement it once we hit a rectangle
            falseNegatives += groundTruthRectangles.size();

            for (javafx.scene.shape.Rectangle groundTruthRectangle : groundTruthRectangles) {
                boolean hit = false;

                for (javafx.scene.shape.Rectangle generatorRectangle : detectedRectangles) {
                    if (computeJaccardIndex(groundTruthRectangle, generatorRectangle) > Float.parseFloat(jaccardIndex.getText())) {
                        // We have hit it, well done, increment the true positive and decrement the false negative!
                        hit = true;
                        truePositives++;
                        falseNegatives--;

                        break;
                    }
//                    System.out.println("ne sijeku se");
                }

                // We haven't hit it? False positive it seems.
                if (!hit) {
                    falsePositives++;
                }
            }
        }

        System.out.println(truePositives);
        System.out.println(falsePositives);
        System.out.println(falseNegatives);

        // Compute all necessary properties
        float recall = ComputationUtils.computeRecall(truePositives, falseNegatives);
        float precision = ComputationUtils.computePrecision(truePositives, falsePositives);
        float f1 = ComputationUtils.computeF1(recall, precision);
        System.out.println("recall:" + recall);
        System.out.println("precision:" + precision);
        System.out.println("f1:" + f1);

        recallValue.setText(String.valueOf(recall) + "%");
        precisionValue.setText(String.valueOf(precision) + "%");
        f1Value.setText(String.valueOf(f1) + "%");
    }

    @FXML
    public void saveCurrentFrame(ActionEvent actionEvent) {
        Label oznaka = new Label("Želite li spremiti označene okvire:");
        ToggleGroup oznOkviri = new ToggleGroup();
        RadioButton btnDa = new RadioButton("Da");
        btnDa.setToggleGroup(oznOkviri);
        RadioButton btnNe = new RadioButton("Ne");
        btnNe.setToggleGroup(oznOkviri);

        Label spremanje = new Label("Koliko okvira želite spremiti:");
        ToggleGroup sviOkviri = new ToggleGroup();
        RadioButton sve = new RadioButton("Sve");
        RadioButton neke = new RadioButton("Neke");
        sve.setToggleGroup(sviOkviri);
        neke.setToggleGroup(sviOkviri);

        TextField upisi = new TextField();
        upisi.setPromptText("Ovdje unesite brojeve okvira koje želite spremiti");
        upisi.setDisable(true);

        neke.setOnAction(event -> upisi.setDisable(false));
        sve.setOnAction(event -> upisi.setDisable(true));

        Button spremi = new Button("Spremi");

        spremi.setOnAction(event -> {
            int numberOfFrames = 0;
            try {
                numberOfFrames = VideoUtil.getNumberOfFrames(evaluationMainApp.getVideoPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (btnNe.isSelected()) {
                if (sve.isSelected()) {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Spremi neoznačene okvire");
                    File directory = directoryChooser.showDialog(scene.getWindow());

                    for (int i = 0; i < numberOfFrames; ++i) {
                        BufferedImage fieldImage = null;

                        try {
                            fieldImage = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), i);
                        } catch (IOException | JCodecException e) {
                            e.printStackTrace();
                        }

                        File frameFile = directory.toPath().resolve("okvir" + i + ".png").toFile();

                        try {
                            ImageIO.write(fieldImage, "png", frameFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (neke.isSelected()) {
                        if (upisi.getText() == null || upisi.getText().trim().isEmpty()) {
                            Message.warning("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti oblika: 1, 5, 74, 89, ...");
                        } else if (upisi.getText().matches(".*[a-zA-Z]+.*")) {
                            Message.warning("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: 1, 5, 74, 89, ...");
                        } else {
                            String[] brojeviOkvira = upisi.getText().split(",");
                            int[] okviri = null;
                            int i = 0;
                            int disableOpen = 0;

                            for (String okvir : brojeviOkvira) {
                                okvir = okvir.trim();
                                okviri[i] = Integer.parseInt(brojeviOkvira[i]);

                                if (numberOfFrames < okviri[i] || okviri[i] < 0) {
                                    Message.warning("Izvan raspona", "Broj ili neki od brojeva su veći od ukupnog broja okvira ili su manji od 0.");
                                    disableOpen = 1;
                                    break;
                                }
                                i++;
                            }
                            if (disableOpen == 0) {
                                DirectoryChooser directChooser = new DirectoryChooser();
                                directChooser.setTitle("Spremi neoznačene okvire");
                                File direct = directoryChooser.showDialog(scene.getWindow());
                                for (int mjesto : okviri) {
                                    BufferedImage fieldImage = null;
                                    try {
                                        fieldImage = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), mjesto);
                                    } catch (IOException | JCodecException e) {
                                        e.printStackTrace();
                                    }
                                    File frameFile = direct.toPath().resolve("okvir" + i + ".png").toFile();
                                    try {
                                        ImageIO.write(fieldImage, "png", frameFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Spremi označene okvire");
                    File file = directoryChooser.showDialog(scene.getWindow());
                    if (sve.isSelected()) {
                        for (int frameNumber : evaluationMainApp.getMarkedFrames().keySet()) {
                            BufferedImage image = null;
                            try {
                                image = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), frameNumber);
                            } catch (IOException | JCodecException e) {
                                e.printStackTrace();
                            }
                            Graphics graph = image.createGraphics();
                            file = file.toPath().resolve("O" + frameNumber + ".jpg").toFile();
                            for (javafx.scene.shape.Rectangle rec : evaluationMainApp.getMarkedFrame(frameNumber)) {
                                graph.setColor(Color.RED);
//								graph.drawRect(rec.getxCoordinate(), rec.getyCoordinate(), rec.getHeight(), rec
//										.getWidth());
                            }
                            try {
                                ImageIO.write(image, "png", file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (upisi.getText() == null || upisi.getText().trim().isEmpty()) {
                            Message.warning("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti" +
                                    " " +
                                    "oblika: 1, 5, 74, 89, ...");
                        } else if (upisi.getText().matches(".*[a-zA-Z]+.*")) {
                            Message.warning("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: " +
                                    "1," +
                                    " " +
                                    "5, 74, 89, ...");
                        }
                        String[] okviri = upisi.getText().split(",");
                        int[] brojeviOkvira = null;
                        int i = 0;
                        int postoji = 0;
                        List<Integer> lista = new ArrayList<>();
                        for (String okvir : okviri) {
                            okvir = okvir.trim();
                            brojeviOkvira[i] = Integer.parseInt(okvir);
                            if (evaluationMainApp.getMarkedFrames().keySet().contains(brojeviOkvira[i])) {
                                lista.add(brojeviOkvira[i]);
                                postoji = 1;
                            }
                            i++;
                        }
                        if (postoji == 1) {
                            String s = null;
                            for (int j = 0; i < lista.size(); j++) {
                                s = s + lista.get(j) + "\t";
                            }
                            Message.warning("Krivi upis", "Okviri koji su navedeni, a još nisu označeni (ili su " +
                                    "manji" +
                                    " od 0) su: " + s);
                        }
                        for (int broj : brojeviOkvira) {
                            if (postoji == 1) {
                                break;
                            }
                            BufferedImage image = null;
                            try {
                                image = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), broj);
                            } catch (IOException | JCodecException e) {
                                e.printStackTrace();
                            }
                            Graphics graph = image.createGraphics();
                            file = file.toPath().resolve("O" + broj + ".jpg").toFile();
                            for (javafx.scene.shape.Rectangle rec : evaluationMainApp.getMarkedFrame(broj)) {
                                graph.setColor(Color.RED);
//								graph.drawRect(rec.getxCoordinate(), rec.getyCoordinate(), rec.getHeight(), rec
//										.getWidth());
                            }
                            try {
                                ImageIO.write(image, "png", file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20, 20, 20, 20));

        FlowPane pane2 = new FlowPane();
        pane2.setVgap(100);
        pane2.setHgap(30);
        pane2.getChildren().addAll(oznaka, btnDa, btnNe);

        pane.setTop(pane2);

        BorderPane pane3 = new BorderPane();
        pane3.setPadding(new Insets(50, 0, 50, 0));
        pane.setCenter(pane3);

        FlowPane pane4 = new FlowPane();
        pane4.setVgap(100);
        pane4.setHgap(50);
        pane4.getChildren().addAll(spremanje, sve, neke);

        pane3.setTop(pane4);
        pane3.setBottom(upisi);

        BorderPane pane5 = new BorderPane();
        pane5.setRight(spremi);
        pane.setBottom(pane5);

        Scene secondScene = new Scene(pane, 700, 250);

        Stage secondStage = new Stage();
        secondStage.initModality(Modality.APPLICATION_MODAL);
        secondStage.setTitle("Spremanje okvira");
        secondStage.setScene(secondScene);

        secondStage.show();
    }

    /**
     * Called when the "save file containing marks" button is pressed.
     * Entire process to the saving:
     * - Gets user chosen directory where to save a file
     * - Creates new .txt file
     * - Loops through the frame numbers, gets list of marked rectangles for specific frame
     * - Writes to the file
     *
     * @param actionEvent Event
     * @throws IOException Exception thrown if the file cannot be created or something funny happened to the stream
     */
    @FXML
    public void saveFileWithMarks(ActionEvent actionEvent) throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Spremi datoteku s oznakama");

        // Get user chosen directory
        File file = directoryChooser.showDialog(scene.getWindow());
        Path directory = file.toPath();

        // Create new textual file whose path is dynamically generated from the user chosen directory
        // Also create UTF8 charset class that writes to the file
        File txtFile = new File(directory + File.separator + "oznakeOkvira.txt");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(txtFile), "UTF-8"));

        List<Integer> frameNumbersList = new ArrayList<>();
        frameNumbersList.addAll(evaluationMainApp.getMarkedFrames().keySet());
        frameNumbersList.sort(null);

        for (int frameNumber : frameNumbersList) {
            // Loop through the rectangles for this specific frame and write it to the file
            for (javafx.scene.shape.Rectangle label : evaluationMainApp.getMarkedFrame(frameNumber)) {
                writer.write(label + System.lineSeparator());
                writer.flush();
            }
        }

        writer.close();
    }

    /**
     * Called when the slider value (slider position) is changed.
     * Sets the label next to the slider depending on its position.
     *
     * @param event Event
     */
    @FXML
    public void setFrameNumberInLabel(Event event) {
        setLabelForSliderValue();
        frameNumberTextField.clear();
    }

    /**
     * Called when the save marks button is pressed.
     * Calculates the frame number based on the slider value, gets all drawn rectangles
     * and updates the marked frame map with gathered data.
     *
     * @param actionEvent Event
     */
    @FXML
    public void saveMarks(ActionEvent actionEvent) {
        // Get map of all marked frames from the application
        Map<Integer, List<javafx.scene.shape.Rectangle>> markedFrames = evaluationMainApp.getMarkedFrames();

        // Calculate frame number
        int frameNumber = getFrameNumber(Math.round(frameSlider.getValue()));

        // Get all drawn rectangles in this frame and save them to the markedFrames map
        List<javafx.scene.shape.Rectangle> rectangles = new ArrayList<>();
        rectangles.addAll(drawnRectangles);
        markedFrames.put(frameNumber, rectangles);

        // Add this frame number to the marked frames list if it's not already there
        if (!markedFramesList.getItems().contains(String.valueOf(frameNumber))) {
            markedFramesList.getItems().add(String.valueOf(frameNumber));
        }
    }

    @FXML
    public void deleteMarks(ActionEvent actionEvent) throws NumberFormatException, IOException {
        ObservableList<String> selectedIndices = markedFramesList.getSelectionModel().getSelectedItems();

        selectedIndices.forEach(item -> {
            int frame = Integer.parseInt(item);

            evaluationMainApp.removeMarkedFrame(frame);
            markedFramesList.getItems().remove(String.valueOf(frame));

            try {
                setSelectedFrame(frame);
            } catch (IOException | JCodecException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * This method is called by the main application to give a reference back to itself and accepts scene and the application itself.
     * Also resets the list containing drawn rectangles.
     *
     * @param scene          Scene
     * @param evaluationMain Application
     */
    public void setUp(Scene scene, EvaluationMain evaluationMain) {
        this.scene = scene;
        this.evaluationMainApp = evaluationMain;

        drawnRectangles = new ArrayList<>();
    }

    /**
     * Draws a red-bordered rectangle based its coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return A rectangle object (Instance of the <b>javafx.scene.shape.Rectangle</b> class)
     */
    private javafx.scene.shape.Rectangle drawRectangle(double x, double y) {
        double width = Math.abs(beginningX - x);
        double height = Math.abs(beginningY - y);

        double startX = beginningX < x ? beginningX : x;
        double startY = beginningY < y ? beginningY : y;

        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle(startX, startY, width, height);
        rectangle.setDisable(false);
        rectangle.setFill(null);
        rectangle.setStroke(javafx.scene.paint.Color.RED);
        rectangle.setStrokeWidth(1);

        return rectangle;
    }

    /**
     * "Sets" the selected frame by displaying it and draws any rectangles that were previously drawn.
     *
     * @param number Frame number
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    private void setSelectedFrame(int number) throws IOException, JCodecException {
        selectedId = null;

        for (Rectangle drawnRectangle : drawnRectangles) {
            drawnRectangle.setStroke(javafx.scene.paint.Color.RED);
        }

        BufferedImage fieldImage = getImageForFrame(number);
        Image image = SwingFXUtils.toFXImage(fieldImage, null);

        // Display (draw) the frame to the user
        footballFieldImage.setImage(image);

        List<javafx.scene.shape.Rectangle> rectanglesToDraw = evaluationMainApp.getMarkedFrame(number);

        imagePane.getChildren().removeAll(drawnRectangles);
        drawnRectangles.clear();

        // There are any rectangles to be drawn?
        if (rectanglesToDraw != null) {
            // Loop through the rectangles to be drawn and draw them
            for (javafx.scene.shape.Rectangle rectangle : rectanglesToDraw) {
                drawnRectangles.add(rectangle);

                imagePane.getChildren().add(rectangle);
            }
        }
    }

    /**
     * Converts video frame to the image (.png format).
     *
     * @param number Frame number
     * @return Instance of the <b>BufferedImage</b> class containing the picture.
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    private BufferedImage getImageForFrame(int number) throws IOException, JCodecException {
        int frameNumber = number - 1;
        String format = "png";

        File frameFile = evaluationMainApp.getDumpDir()
                .toPath()
                .resolve(frameNumber + "." + format)
                .toFile();

        // If picture already exists, there is no need to create a new one, just return the one
        if (frameFile.exists()) {
            return ImageIO.read(frameFile);
        }

        // Create the image from the frame number and write it to the user
        BufferedImage fieldImage = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), frameNumber);
        ImageIO.write(fieldImage, format, frameFile);

        return fieldImage;
    }

    /**
     * Computes the Jaccard index based on the rectangles' properties.
     * Delegates <i>computeJaccardIndex</i> method from the <b>ComputationUtils</b> helper class.
     *
     * @param markedRectangle Marked rectangle object
     * @param generatorRect   Ground truth rectangle object
     * @return Computed index
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    private double computeJaccardIndex(javafx.scene.shape.Rectangle markedRectangle, javafx.scene.shape.Rectangle generatorRect) {

        //System.out.println("multipier"+evaluationMainApp.getWidthMultiplier());
        return ComputationUtils.computeJaccardIndex(
                // Marked rectangle properties
                markedRectangle.getX(),
                markedRectangle.getY(),
                markedRectangle.getWidth(),
                markedRectangle.getHeight(),

                // "Ground truth" rectangle properties
                generatorRect.getX(),
                generatorRect.getY(),
                generatorRect.getWidth(),
                generatorRect.getHeight()
        );
    }

    /**
     * Reads all rectangles for specific frame from the file, parses it then returns them in a list.
     * <p>
     * Property variable structure (index - meaning):
     * 0 - Frame number
     * 1 - Team ID
     * 2 - Player ID
     * 3 - Player's X coordinate in the field
     * 4 - Player's Y coordinate in the field
     * 5 - Rectangle's left-bottom X coordinate
     * 6 - Rectangle's left-bottom Y coordinate
     * 7 - Width of the rectangle
     * 8 - Height of the rectangle
     * 9 - Useless
     * </p>
     *
     * @param frameNumber Frame number
     * @return Rectangle
     */
    private LinkedList<javafx.scene.shape.Rectangle> rectanglesForAFrame(int frameNumber) {
        LinkedList<javafx.scene.shape.Rectangle> rectangles = new LinkedList<>();

        try {
            Files.lines(evaluationMainApp.getEvaluationFile().toPath())
                    .filter(line -> line.startsWith(Integer.toString(frameNumber) + ","))
                    .forEach(line -> {
                                // Structure of this variable is defined in this method's JavaDoc.
                                String[] property = line.split(",");
                                rectangles.add(new javafx.scene.shape.Rectangle(
                                        Double.parseDouble(property[5]) * evaluationMainApp.getWidthMultiplier(),
                                        Double.parseDouble(property[6]) * evaluationMainApp.getWidthMultiplier() - Double.parseDouble(property[8]) * evaluationMainApp.getWidthMultiplier(),
                                        Double.parseDouble(property[7]) * evaluationMainApp.getWidthMultiplier(),
                                        Double.parseDouble(property[8]) * evaluationMainApp.getWidthMultiplier())
                                );
                            }
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rectangles;
    }

    /**
     * Helper method that sets the label next to the slider depending on its position.
     *
     * @return Frame number depending on the slider position
     */
    private int setLabelForSliderValue() {
        int frameNumber = getFrameNumber(Math.round(frameSlider.getValue()));
        frameNumberField.setText(String.valueOf(frameNumber));

        return frameNumber;
    }

    /**
     * Get "real" calculated frame number based on the slider value.
     * Multiplies slider value by the FRAME_HOP constant.
     *
     * @param sliderValue Slider value
     * @return Calculate frame number
     */
    private int getFrameNumber(long sliderValue) {
        // Return first frame number (1) if slider is at its zeroth position.
        if (sliderValue == 0) {
            return 1;
        }

        return (int) (FRAME_HOP * sliderValue);
    }

    @FXML
    public void handleEnterPressed(KeyEvent e) throws IOException, JCodecException {
        if (e.getCode() == KeyCode.ENTER) {
            setSelectedFrame(Integer.parseInt(frameNumberTextField.getText()));

            frameSlider.setValue(Integer.parseInt(frameNumberTextField.getText()) / FRAME_HOP);
            setLabelForSliderValue();
            frameNumberTextField.clear();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        footballFieldImage.requestFocus();

        // User is starting to draw a rectangle!
        footballFieldImage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
            // Works only if there is a video to draw on
            if (evaluationMainApp.isDumpingDirSet() && evaluationMainApp.isVideoDirSet()) {
                beginningX = mouseEvent.getX();
                beginningY = mouseEvent.getY();

                selectedId = null;

                System.out.println(evaluationMainApp.getMarkedFrames());

                drawingInitialized = true;

                for (Rectangle rectangle : drawnRectangles) {
                    if (rectangle.getX() < beginningX && rectangle.getY() < beginningY && rectangle.getX() + rectangle.getWidth() > beginningX && rectangle.getY() + rectangle.getHeight() > beginningY) {
                        drawingInitialized = false;
                        rectangle.setStroke(javafx.scene.paint.Color.CORNFLOWERBLUE);

                        selectedId = rectangle;
                        footballFieldImage.requestFocus();
                    } else {
                        rectangle.setStroke(javafx.scene.paint.Color.RED);
                    }
                }
            }
        });

        footballFieldImage.setFocusTraversable(true);

        footballFieldImage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DELETE && selectedId != null) {
                int frame = getFrameNumber((long) frameSlider.getValue());

                drawnRectangles.remove(selectedId);
                imagePane.getChildren().remove(selectedId);

                List<Rectangle> lista = evaluationMainApp.getMarkedFrame(frame);
                lista.remove(selectedId);

                evaluationMainApp.updateMarkedFrame(frame, lista);
                System.out.println(evaluationMainApp.getMarkedFrame(frame));
            }
        });

        // User is drawing the rectangle right now!
        footballFieldImage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {
            if (drawingInitialized) {
                if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage.getFitHeight() && mouseEvent.getX() > 0 && mouseEvent.getY() > 0) {
                    javafx.scene.shape.Rectangle rectangle = drawRectangle(mouseEvent.getX(), mouseEvent.getY());

                    imagePane.getChildren().remove(lastRectangle);
                    lastRectangle = rectangle;
                    imagePane.getChildren().add(rectangle);
                } else {
                    imagePane.getChildren().remove(lastRectangle);
                }
            }
        });

        // Draw the rectangle once user finishes dragging.
        footballFieldImage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
            if (drawingInitialized) {
                //TODO edit boudns, now only works for the left one and not for the on on the bottom need a better solution
                if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage.getFitHeight()) {
                    javafx.scene.shape.Rectangle rectangle = drawRectangle(mouseEvent.getX(), mouseEvent.getY());

                    imagePane.getChildren().add(rectangle);
                    imagePane.getChildren().remove(lastRectangle);
                    drawnRectangles.add(rectangle);
                }
            }

            drawingInitialized = false;
        });

        markedFramesList.setCellFactory(TextFieldListCell.forListView());
        markedFramesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int frameNumber = Integer.parseInt(newValue);

            try {
                setSelectedFrame(frameNumber);
            } catch (IOException | JCodecException e) {
                Message.error("Greška!", "Dogodila se greška u sustavu.");

                e.printStackTrace();
            }

            frameNumberField.setText(String.valueOf(frameNumber));
            frameSlider.setValue(frameNumber / FRAME_HOP);
        });
    }
}