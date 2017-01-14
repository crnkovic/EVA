package hr.fer.zemris.projekt;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;
import java.awt.*;
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

    /**
     * List of marked frames.
     */
    @FXML
    private ListView markedFramesList;
    @FXML
    private Label recallValue;
    @FXML
    private Label precisionValue;
    @FXML
    private Label f1Value;

    /**
     * <b>TextField</b> object containing computed Jaccard index.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    @FXML
    private TextField jaccardIndex;
    @FXML
    private Pane imagePane;

    private double beginningX;
    private double beginningY;
    private boolean set = false;

    /**
     * Contains last drawn rectangle.
     */
    private javafx.scene.shape.Rectangle lastRectangle;

    /**
     * List containing drawn rectangles.
     */
    private List<javafx.scene.shape.Rectangle> drawnRectangles;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        footballFieldImage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
            if (evaluationMainApp.isDumpingDirSet() && evaluationMainApp.isVideoDirSet()) {
                beginningX = mouseEvent.getX();
                beginningY = mouseEvent.getY();
                set = true;
                System.out.println("mouse click detected! " + mouseEvent.getX());
                System.out.println(mouseEvent.getY());
            }
        });


        footballFieldImage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
            if (set) {
                //TODO edit boudns, now only works for the left one and not for the on on the bottom need a better solution
                if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage
                        .getFitHeight()) {

                    javafx.scene.shape.Rectangle rectangle = drawRectangle(mouseEvent.getX(), mouseEvent.getY());
                    imagePane.getChildren().add(rectangle);
                    imagePane.getChildren().remove(lastRectangle);
                    drawnRectangles.add(rectangle);
                }
            }
            set = false;
        });
        footballFieldImage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {
            if (set) {
                if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage.getFitHeight()) {

                    javafx.scene.shape.Rectangle rectangle = drawRectangle(mouseEvent.getX(), mouseEvent.getY());
                    imagePane.getChildren().remove(lastRectangle);
                    lastRectangle = rectangle;
                    imagePane.getChildren().add(rectangle);
                } else {
                    imagePane.getChildren().remove(lastRectangle);
                }
            }
        });
        markedFramesList.setCellFactory(TextFieldListCell.forListView());
        markedFramesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int frameNumber = Integer.parseInt(newValue.toString());
            try {
                setSelectedFrame(frameNumber);
            } catch (IOException e) {
                //TODO error message
            } catch (JCodecException e) {
                //TODO error message
            }
            frameNumberField.setText(String.valueOf(frameNumber));
            frameSlider.setValue(frameNumber / FRAME_HOP);
        });
    }

    private javafx.scene.shape.Rectangle drawRectangle(double x, double y) {
        double width = Math.abs(beginningX - x);
        double height = Math.abs(beginningY - y);
        double startX;
        double startY;

        if (beginningX < x) {
            startX = beginningX;
        } else {
            startX = x;
        }

        if (beginningY < y) {
            startY = beginningY;
        } else {
            startY = y;
        }
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle(startX, startY, width, height);
        rectangle.setDisable(false);
        rectangle.setFill(null);
        rectangle.setStroke(javafx.scene.paint.Color.RED);
        rectangle.setStrokeWidth(1);
        return rectangle;
    }


    //TODO add marking on the frames

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
    }

    /**
     * "Sets" the selected frame by displaying it and draws any rectangles that were previously drawn.
     *
     * @param number Frame number
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    private void setSelectedFrame(int number) throws IOException, JCodecException {
        BufferedImage fieldImage = getImageForFrame(number);
        Image image = SwingFXUtils.toFXImage(fieldImage, null);

        // Display (draw) the frame to the user
        footballFieldImage.setImage(image);

        // Remove rectangles that were drawn in the previous frame and collects all rectangles that were to be marked
        imagePane.getChildren().removeAll(drawnRectangles);
        drawnRectangles.clear();
        List<javafx.scene.shape.Rectangle> rectanglesToDraw = evaluationMainApp.getMarkedFrame(number);

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

    @FXML
    public void setVideoAndSetUp(ActionEvent actionEvent) throws FrameGrabber.Exception, IOException, JCodecException {
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Odaberi video");
        File file = fileChooser.showOpenDialog(scene.getWindow());

        evaluationMainApp.setVideoPath(file.getPath());
        int numberOfFrames = VideoUtil.getNumberOfFrames(file.getPath());

        frameSlider.setMax(numberOfFrames / FRAME_HOP);
        frameSlider.setMin(0);
        frameSlider.setBlockIncrement(50);

        evaluationMainApp.setEvaluationFile(null);

        if (evaluationMainApp.isDumpingDirSet()) {
            primarySetup();
        }
    }

    @FXML
    public void setImageDumpDir(ActionEvent actionEvent) throws IOException, JCodecException {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Odaberi direktorij");
        File file = fileChooser.showDialog(scene.getWindow());
        file = file.toPath().resolve("images").toFile();
        file.mkdir();
        evaluationMainApp.setDumpDir(file);
        if (evaluationMainApp.isVideoDirSet()) {
            primarySetup();
        }
    }

    private void primarySetup() throws IOException, JCodecException {
        setSelectedFrame(1);
        setLabelForSliderValue();

        frameSlider.setDisable(false);
    }

    @FXML
    public void setEvaluationFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odaberi datoteku za evaluaciju");
        File file = fileChooser.showOpenDialog(scene.getWindow());

        evaluationMainApp.setEvaluationFile(file);
    }

    @FXML
    public void evaluate(ActionEvent actionEvent) {
        //TODO check first if you can evaluate
        if (evaluationMainApp.getMarkedFrames().size() == 0) {
            //TODO throw warning that there should be marked frames
            return;
        }

        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

        for (int frameNumber : evaluationMainApp.getMarkedFrames().keySet()) {

            List<javafx.scene.shape.Rectangle> groundTruthRectangles = evaluationMainApp.getMarkedFrames().get(frameNumber);
            List<javafx.scene.shape.Rectangle> detectedRectangles = rectanglesForAFrame(frameNumber);
            falseNegatives += detectedRectangles.size();

            for (javafx.scene.shape.Rectangle groundTruthRectangle : groundTruthRectangles) {
                boolean hit = false;
                javafx.scene.shape.Rectangle usedRectangle = null;

                for (javafx.scene.shape.Rectangle generatorRectangle : detectedRectangles) {
                    if (computerJaccardIndex(generatorRectangle, groundTruthRectangle) > Float.parseFloat(jaccardIndex
                            .getText())) {
                        //hit
                        usedRectangle = generatorRectangle;
                        hit = true;
                        truePositives++;
                        falseNegatives--;
                        break;
                    }

                }

                if (!hit) {
                    falsePositives++;
                }
            }


        }
        System.out.println(truePositives);
        System.out.println(falsePositives);
        System.out.println(falseNegatives);

        float recall = (float) truePositives / (truePositives + falseNegatives);
        recallValue.setText(String.valueOf(recall) + "%");

        float precision = (float) truePositives / (truePositives + falsePositives);
        precisionValue.setText(String.valueOf(precision) + "%");

        float f1 = 2 * (recall * precision) / (recall + precision);
        f1Value.setText(String.valueOf(f1) + "%");
    }

    /**
     * Computes the Jaccard index based on the rectangles' properties.
     * Delegates <i>computeJaccardIndex</i> method from the <b>ComputationUtils</b> helper class.
     *
     * @param markedRectangle      Marked rectangle object
     * @param groundTruthRectangle Ground truth rectangle object
     * @return Computed index
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    public static double computerJaccardIndex(javafx.scene.shape.Rectangle markedRectangle, javafx.scene.shape.Rectangle groundTruthRectangle) {
        return ComputationUtils.computeJaccardIndex(
                // Marked rectangle properties
                markedRectangle.getX(),
                markedRectangle.getY(),
                markedRectangle.getWidth(),
                markedRectangle.getHeight(),

                // "Ground truth" rectangle properties
                groundTruthRectangle.getX(),
                groundTruthRectangle.getY(),
                groundTruthRectangle.getWidth(),
                groundTruthRectangle.getHeight()
        );

//        double newX = Math.max(firstRectangle.getX(), groundTruthRectangle.getX());
//        double newY = Math.min(firstRectangle.getY(), groundTruthRectangle.getY());
//        double newWidth = Math.min(firstRectangle.getX() + firstRectangle.getWidth(), groundTruthRectangle.getX() + groundTruthRectangle.getWidth()) - newX;
//        double newHeight = Math.max(firstRectangle.getY() + firstRectangle.getY(), groundTruthRectangle.getY() + groundTruthRectangle.getHeight()) - newY;
//        System.out.println(newHeight);
//        System.out.println(newWidth);
//
//        double intersectionArea = newWidth * newHeight;
//        System.out.println(intersectionArea);
//        double unionArea = firstRectangle.getHeight() * firstRectangle.getWidth() + groundTruthRectangle.getHeight() * groundTruthRectangle.getWidth() - intersectionArea;
//        System.out.println(unionArea);
//        return intersectionArea / unionArea;
    }

    private LinkedList<javafx.scene.shape.Rectangle> rectanglesForAFrame(int frameNumber) {
        LinkedList<javafx.scene.shape.Rectangle> rectangles = new LinkedList<>();

        try {
            Files.lines(evaluationMainApp.getEvaluationFile().toPath())
                    .filter(line -> line.startsWith(Integer.toString(frameNumber) + ","))
                    .forEach(line -> {
                                String[] polje = line.split(",");
                        /*
                        polje{
							brojFramea,
							ID tima,
							ID igraca,
							x koordinata u terenu,
							y koordinata u terenu,
							x koordinata LD vrha pravokutnika,
							y koordinata LD vrha,
							sirina pravokutnika,
							visina pravokutnika,
							zanemariva oznaka
						}
						*/
                                rectangles.add(
                                        new javafx.scene.shape.Rectangle(
                                                Double.parseDouble(polje[5]),
                                                Double.parseDouble(polje[6]),
                                                Double.parseDouble(polje[7]),
                                                Double.parseDouble(polje[8])
                                        ));
                            }
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rectangles;
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
                            izbaciUpozorenje("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti" +
                                    " " +
                                    "oblika: 1, 5, 74, 89, ...");
                        } else if (upisi.getText().matches(".*[a-zA-Z]+.*")) {
                            izbaciUpozorenje("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: " +
                                    "1," +
                                    " " +
                                    "5, 74, 89, ...");
                        } else {
                            String[] brojeviOkvira = upisi.getText().split(",");
                            int[] okviri = null;
                            int i = 0;
                            int disableOpen = 0;
                            for (String okvir : brojeviOkvira) {
                                okvir = okvir.trim();
                                okviri[i] = Integer.parseInt(brojeviOkvira[i]);
                                if (numberOfFrames < okviri[i] || okviri[i] < 0) {
                                    izbaciUpozorenje("Izvan raspona", "Broj ili neki od brojeva su veći od ukupnog " +
                                            "broja okvira ili su manji od 0.");
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
                            izbaciUpozorenje("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti" +
                                    " " +
                                    "oblika: 1, 5, 74, 89, ...");
                        } else if (upisi.getText().matches(".*[a-zA-Z]+.*")) {
                            izbaciUpozorenje("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: " +
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
                            izbaciUpozorenje("Krivi upis", "Okviri koji su navedeni, a još nisu označeni (ili su " +
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

        // TODO sortirat rectangleove po broju frame-a i dodat metodu koja pretvara u pravilan oblik
        List<Integer> frameNumbersList = new ArrayList<>();
        frameNumbersList.addAll(evaluationMainApp.getMarkedFrames().keySet());
        frameNumbersList.sort(null);

        for (int frameNumber : frameNumbersList) {
            // Loop through the rectangles for this specific frame and write it to the file
            for (javafx.scene.shape.Rectangle label : evaluationMainApp.getMarkedFrame(frameNumber)) {
                writer.write(label.toString() + System.lineSeparator());
                writer.flush();
            }
        }

        writer.close();
    }


    public void izbaciUpozorenje(String naslov, String tekst) {
        Alert fail = new Alert(AlertType.INFORMATION);
        fail.setHeaderText(naslov);
        fail.setContentText(tekst);
        fail.showAndWait();
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
}