package hr.fer.zemris.projekt;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;


public class Controller implements Initializable {
	private static final float DEFAULT_DISPLAY_WIDTH = (float) 820.0;

	private  static float frameWidth;
	private  static float frameHeight;
	private static final float UNITS_SCROLLED_MAGNIFY = 1.2f;
	private static final float UNITS_SCROLLED_DECREASE = 0.8f;
	/**
	 * Current window scene.
	 */
	private Scene scene;

	/**
	 * Reference to the main application.
	 */
	private EvaluationMain evaluationMainApp;

	/**
	 * Active frame number.
	 */
	@FXML
	private Label frameNumberField;

	@FXML
	private static final int BLOCK_INCREMENT = 500;

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
	 * Rectangle in focus (selected one)
	 */
	private EditRectangle selectedRectangle = null;

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
	 * loadRectanglesFromFile
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
	private EditRectangle lastRectangle;

	/**
	 * Text field for entering frame number.
	 */
	@FXML
	private TextField frameNumberTextField;


	private List<EditRectangle> currentFrameRectangles;

	private AffineTransform affineTransform;

	private double videoWidth;
	private double videoHeight;
	private boolean repaintInMotion;
	private static final String shortcutsFile= "precaci.txt";
	private static final String instructionsFile="./upute.txt";


	public Controller() throws IOException, JCodecException {
	}

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
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Video (" + String.join(", ",
				videoExtensions).replaceAll("\\*", "") + ")", videoExtensions);

		// Show file chooser to the user and let it choose a video
		// Only accepts video extensions defined in a class property
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setTitle("Odaberite video za evaluaciju");
		File file = fileChooser.showOpenDialog(scene.getWindow());

		if (file == null) {
			return;
		}
		// Set path to the video in the main application and collect number of frames from the video
		evaluationMainApp.setVideoPath(file.getPath());
		int numberOfFrames = VideoUtil.getNumberOfFrames(file.getPath());

		BufferedImage bufferedImage = VideoUtil.getFrame(file.getPath(), 0);
		frameWidth=bufferedImage.getWidth();
		frameHeight=bufferedImage.getHeight();
		videoHeight = bufferedImage.getHeight() * (videoHeight / bufferedImage.getWidth());
		affineTransform = new AffineTransform();
		affineTransform.scale(videoWidth / bufferedImage.getWidth(), videoWidth / bufferedImage.getWidth());

		// Set up slider
		frameSlider.setMax(numberOfFrames);
		frameSlider.setMin(1);
		frameSlider.setBlockIncrement(BLOCK_INCREMENT);

		// Reset ground truth file
		evaluationMainApp.setEvaluationFile(null);


		// Set up the reference to the dumping

		setLabelForSliderValue();
		frameSlider.setDisable(false);
		setSelectedFrame(1);
	}


	private EditRectangle scaleRectangle(EditRectangle rectangle) {
		EditRectangle rectangle1 = new EditRectangle();
		rectangle1.setX((rectangle.getX() * affineTransform.getScaleX()) + affineTransform.getTranslateX());
		rectangle1.setY((rectangle.getY() * affineTransform.getScaleY()) + affineTransform.getTranslateY());
		rectangle1.setHeight((rectangle.getHeight() * affineTransform.getScaleY()));
		rectangle1.setWidth(rectangle.getWidth() * affineTransform.getScaleX());
		DrawingUtil.translateProperties(rectangle, rectangle1);
		return rectangle1;
	}

	private EditRectangle unscaleRectangle(EditRectangle rectangle) {
		EditRectangle rectangle1 = new EditRectangle();
		rectangle1.setX((rectangle.getX() - affineTransform.getTranslateX()) / affineTransform.getScaleX());
		rectangle1.setY((rectangle.getY() - affineTransform.getTranslateY()) / affineTransform.getScaleY());
		rectangle1.setHeight((rectangle.getHeight() / affineTransform.getScaleY()));
		rectangle1.setWidth(rectangle.getWidth() / affineTransform.getScaleX());
		DrawingUtil.translateProperties(rectangle, rectangle1);
		return rectangle1;
	}


	public void drawTemporaryRectangleOnImageView(EditRectangle temporary) {
		imagePane.getChildren().remove(lastRectangle);
		lastRectangle = temporary;
		imagePane.getChildren().add(temporary);
	}

	public void drawRectangleOnImageView(EditRectangle rectangle) {
		imagePane.getChildren().add(rectangle);
		imagePane.getChildren().remove(lastRectangle);
		EditRectangle unscaleRectangle = unscaleRectangle(rectangle);
		currentFrameRectangles.add(unscaleRectangle);
		lastRectangle = null;

	}


	private void repaintElements() throws IOException, JCodecException {
		BufferedImage bufferedImage = getImageForFrame(Integer.parseInt(frameNumberField.getText()));
		videoHeight = bufferedImage.getHeight() * (videoWidth / bufferedImage.getWidth());

		Iterator iterator = imagePane.getChildren().iterator();
		while (iterator.hasNext()) {
			Node node = (Node) iterator.next();
			if (node instanceof EditRectangle) {
				iterator.remove();
			}
		}

		for (EditRectangle rectangle : currentFrameRectangles) {
			EditRectangle scaledRectangle = scaleRectangle(rectangle);
			if (canShow(scaledRectangle)) {
				imagePane.getChildren().add(scaledRectangle);
			}
		}

		AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
		bufferedImage = affineTransformOp.filter(bufferedImage, null);
		BufferedImage imageWhite = new BufferedImage((int) videoWidth, (int) videoHeight, BufferedImage.TYPE_INT_ARGB);
		imageWhite.getGraphics().setColor(new Color(235, 235, 235));
		imageWhite.getGraphics().fillRect(0, 0, (int) videoWidth, (int) videoHeight);
		imageWhite.setData(bufferedImage.getData());

		Image image = SwingFXUtils.toFXImage(imageWhite, null);

//		footballFieldImage.setScaleX(affineTransform.getScaleX());
//		footballFieldImage.setScaleY(affineTransform.getScaleY());
//		footballFieldImage.setTranslateX(affineTransform.getTranslateX());
//		footballFieldImage.setTranslateY(affineTransform.getTranslateY());
		footballFieldImage.setImage(image);
		repaintInMotion = true;
		evaluationMainApp.pack();
	}

	private boolean canShow(EditRectangle scaledRectangle) {
		if (scaledRectangle.getWidth() + scaledRectangle.getX() < videoWidth && scaledRectangle.getHeight() +
				scaledRectangle.getY() < videoHeight) {
			return true;
		}
		return false;
	}


	@FXML
	public void loadRectanglesFromFile() throws IOException, JCodecException {
		if (evaluationMainApp.getEvaluationFile() == null) {
			Message.error("Greška!", "Niste učitali datoteku s detektiranim oznakama.");

			return;
		}

		Set<EditRectangle> rectangles = rectanglesForAFrame((int) Math.floor(frameSlider.getValue()));
		rectangles.forEach(z -> {
			DrawingUtil.setDefaultProperties(z);
			z.setOriginalColor(javafx.scene.paint.Color.ORANGE);
			z.setStroke(z.getOriginalColor());
		});

		currentFrameRectangles.addAll(rectangles);
		repaintElements();

//		drawnRectangles.addAll(rectangles);
//
//		for (EditRectangle rectangle : rectangles) {
//			DrawingUtil.setDefaultProperties(rectangle);
//
//			imagePane.getChildren().add(rectangle);
//		}
	}

	/**
	 * Called when user pressed the "choose evaluation file" button. File should contain all the ground truth
	 * references.
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
	 * returns the index the first element in set that equals value, return -1 if no element equals value
	 *
	 * @param set
	 * @param value
	 * @return
	 */
	public static int getIndex(Set<? extends Object> set, Object value) {
		int result = 0;
		for (Object entry : set) {
			if (entry.equals(value)) {
				return result;
			}
			result++;
		}
		return -1;
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
			Message.warning("Upozorenje!", "Niste unijeli pravilan izraz za Jaccardov index. Provjerite je li broj " +
					"zadan s točkom.");
			return;
		}

		// Initialize default computed properties to 0
		int truePositives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;

		// Loop through the marked frames and do the magic for each one
		for (int frameNumber : evaluationMainApp.getMarkedFrames().keySet()) {
			// Get ground truth rectangles and user-defined rectangles for this specific frame
			Set<EditRectangle> groundTruthRectangles = evaluationMainApp.getMarkedFrame(frameNumber);
			Set<EditRectangle> detectedRectangles = rectanglesForAFrame(frameNumber);
			Set<EditRectangle> generatorRectangles = rectanglesForAFrame(frameNumber);

			// Set false negatives count to all detected rectangles size, so we can decrement it once we hit a
			// rectangle
			falseNegatives += groundTruthRectangles.size();

			List<Integer> takenRectangles = new LinkedList<>();
			for (javafx.scene.shape.Rectangle generatorRectangle : generatorRectangles) {
				boolean hit = false;

				Map<Integer, Double> groundTruthIndexJaccardMap = new HashMap<>();
				for (EditRectangle groundTruthRectangle : groundTruthRectangles) {
					if (takenRectangles.contains(getIndex(groundTruthRectangles, groundTruthRectangle))) {
						continue;
					}
					double jaccIndex = computeJaccardIndex(groundTruthRectangle, generatorRectangle);
					if (jaccIndex > Float.parseFloat(jaccardIndex.getText())) {
						//We have found a rectangle that fits, remember that one and look for a one that fits better
						int GTRectIndex = getIndex(groundTruthRectangles, groundTruthRectangle);
						groundTruthIndexJaccardMap.put(GTRectIndex, jaccIndex);
						hit = true;
					}
				}

				// We haven't hit it? False positive it seems.
				if (!hit) {
					falsePositives++;
				}else{
					// We hit it, well done, increment true positive and decrement false negative!
					// Find out which one fits the most and add it to the takenRectangles list
					int bestIndex = 0;
					double maxValue = 0;
					boolean firstElement = true;
					for (int index:groundTruthIndexJaccardMap.keySet()) {
						if(firstElement){
							bestIndex=index;
							maxValue=groundTruthIndexJaccardMap.get(index);
							firstElement=false;
						}
						double jaccIndex = groundTruthIndexJaccardMap.get(index);
						if(jaccIndex > maxValue){
							bestIndex=index;
							maxValue=jaccIndex;
						}
					}
					takenRectangles.add(bestIndex);

					truePositives++;
					falseNegatives--;

//					break;
				}
			}
		}


		System.out.println("TP:"+truePositives);
		System.out.println("FP:"+falsePositives);
		System.out.println("FN:"+falseNegatives);
		// Compute all necessary properties
		float recall = ComputationUtils.computeRecall(truePositives, falseNegatives);
		float precision = ComputationUtils.computePrecision(truePositives, falsePositives);
		float f1 = ComputationUtils.computeF1(recall, precision);
		System.out.println("recall:" + recall);
		System.out.println("precision:" + precision);
		System.out.println("f1:" + f1);

		if(!Float.isNaN(recall)){
			recallValue.setText(String.valueOf(recall) + "%");
		}else{
			recallValue.setText(String.valueOf(recall));
		}

		if(!Float.isNaN(precision)){
			precisionValue.setText(String.valueOf(precision) + "%");
		}else{
			precisionValue.setText(String.valueOf(precision));
		}

		if(!Float.isNaN(f1)){
			f1Value.setText(String.valueOf(100*f1) + "%");
		}else{
			f1Value.setText(String.valueOf(f1));
		}
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
		FileChooser directoryChooser = new FileChooser();
		directoryChooser.setTitle("Spremi datoteku s oznakama");

		// Get user chosen directory
		File file = directoryChooser.showSaveDialog(scene.getWindow());
//		Path directory = file.toPath();

		// Create new textual file whose path is dynamically generated from the user chosen directory
		// Also create UTF8 charset class that writes to the file
//		File txtFile = new File(directory + File.separator + "oznakeOkvira.txt");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

		List<Integer> frameNumbersList = new ArrayList<>();
		frameNumbersList.addAll(evaluationMainApp.getMarkedFrames().keySet());
		frameNumbersList.sort(null);

		for (int frameNumber : frameNumbersList) {
			// Loop through the rectangles for this specific frame and write it to the file
			for (EditRectangle label : evaluationMainApp.getMarkedFrame(frameNumber)) {
				System.out.println("frame number:" + frameNumber + " label:" + label);
				writer.write(frameNumber + "," + label.toString() + System.lineSeparator());
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
		if (!evaluationMainApp.isVideoDirSet()) {
			Message.warning("Upozorenje!", "Video nije učitan.");
		}

		int frameNumber = Integer.parseInt(frameNumberField.getText());
		// Get all drawn rectangles in this frame and save them to the markedFrames map
		Set<EditRectangle> rectanglesToBeSaved = new HashSet<>(currentFrameRectangles);
		evaluationMainApp.updateMarkedFrame(frameNumber, rectanglesToBeSaved);

		// Add this frame number to the marked frames list if it's not already there
		if (!markedFramesList.getItems().contains(String.valueOf(frameNumber))) {
			markedFramesList.getItems().add(String.valueOf(frameNumber));
		}
	}


	/**
	 * This method is called by the main application to give a reference back to itself and accepts scene and the
	 * application itself.
	 * Also resets the list containing drawn rectangles.
	 *
	 * @param scene          Scene
	 * @param evaluationMain Application
	 */
	public void setUp(Scene scene, EvaluationMain evaluationMain) {
		this.scene = scene;
		this.evaluationMainApp = evaluationMain;
		affineTransform = new AffineTransform();
		videoWidth = DEFAULT_DISPLAY_WIDTH;
		currentFrameRectangles = new ArrayList<>();
		scene.widthProperty().addListener((observable, oldValue, newValue) -> {
			if (!evaluationMain.isVideoDirSet()) {
				return;
			}
			if (repaintInMotion) {
				repaintInMotion = false;
				return;
			}
			int changedIntWidth = newValue.intValue() - oldValue.intValue();

			double newVideoWidth = videoWidth + changedIntWidth;
			if (newVideoWidth > DEFAULT_DISPLAY_WIDTH) {

				videoWidth = newVideoWidth;
				try {
					repaintElements();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JCodecException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * "Sets" the selected frame by displaying it and draws any rectangles that were previously drawn.
	 *
	 * @param number Frame number
	 * @throws IOException     IOException
	 * @throws JCodecException JCodecException
	 */
	private void setSelectedFrame(int number) throws IOException, JCodecException {
		selectedRectangle = null;

		Set<EditRectangle> rectanglesToDraw = evaluationMainApp.getMarkedFrame(number);
		currentFrameRectangles.clear();
		if (rectanglesToDraw != null) {
			currentFrameRectangles.addAll(rectanglesToDraw);
		}
		repaintElements();
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
		int frameNumber = number;
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
	private double computeJaccardIndex(javafx.scene.shape.Rectangle markedRectangle, javafx.scene.shape.Rectangle
			generatorRect) {

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
	private Set<EditRectangle> rectanglesForAFrame(int frameNumber) {
		Set<EditRectangle> rectangles = new HashSet<>();

		try {
			Files.lines(evaluationMainApp.getEvaluationFile().toPath())
					.filter(line -> line.startsWith(Integer.toString(frameNumber) + ","))
					.forEach(line -> {
								// Structure of this variable is defined in this method's JavaDoc.
								String[] property = line.split(",");
								rectangles.add(new EditRectangle(
										Double.parseDouble(property[5]),
										Double.parseDouble(property[6]) -
												Double.parseDouble(property[8]),
										Double.parseDouble(property[7]),
										Double.parseDouble(property[8]))
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
		int frameNumber = (int) Math.floor(frameSlider.getValue()); //getFrameNumber((long) Math.floor(frameSlider
		// .getValue()));
		frameNumberField.setText(String.valueOf(frameNumber));

		return frameNumber;
	}


	@FXML
	public void handleEnterPressed(KeyEvent e) throws IOException, JCodecException, FrameGrabber.Exception {
		if (e.getCode() == KeyCode.ENTER) {
			if (!evaluationMainApp.isVideoDirSet()) {
				Message.warning("Upozorenje!", "Niste učitali video.");
				frameNumberTextField.clear();
				return;
			}
			int broj;

			try {
				broj = (Integer.valueOf(frameNumberTextField.getText()));
			} catch (NumberFormatException ex) {
				Message.warning("Upozorenje!", "Niste unijeli cijeli broj.");
				return;
			}
			int totalNumberOfFrames = VideoUtil.getNumberOfFrames(evaluationMainApp.getVideoPath());
			if (broj > totalNumberOfFrames) {
				broj = totalNumberOfFrames;
			}

			frameSlider.setValue(broj);
			frameNumberField.setText(String.valueOf(broj));
			frameNumberTextField.clear();
			setSelectedFrame(broj);
		}
	}

	@FXML
	private MenuItem video;

	@FXML
	private MenuItem file;

	@FXML
	private MenuItem gFile;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		footballFieldImage.requestFocus();


		video.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
		file.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
		gFile.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));


		// User is starting to draw a rectangle!
		footballFieldImage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
			// Works only if there is a video to draw on
			if (evaluationMainApp.isVideoDirSet()) {
				footballFieldImage.requestFocus();
				beginningX = mouseEvent.getX();
				beginningY = mouseEvent.getY();

				selectedRectangle = null;
				drawingInitialized = true;


				boolean rectangleSelected = false;

				for (EditRectangle rectangle : currentFrameRectangles) {
					EditRectangle scaledRectangle = scaleRectangle(rectangle);
					if (scaledRectangle.getX() < beginningX && scaledRectangle.getY() < beginningY && scaledRectangle
							.getX() +
							scaledRectangle.getWidth() > beginningX
							&& scaledRectangle.getY() + scaledRectangle.getHeight() > beginningY &&
							!rectangleSelected) {
						selectedRectangle = rectangle;
						rectangle.select();
						drawingInitialized = false;
						footballFieldImage.requestFocus();
						rectangleSelected = true;
					} else {
						rectangle.unSelect();
					}
				}

				if (rectangleSelected) {
					try {
						repaintElements();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JCodecException e) {
						e.printStackTrace();
					}
				}

			}
		});

		footballFieldImage.setFocusTraversable(true);


		footballFieldImage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			boolean repaint = false;
			if (!evaluationMainApp.isVideoDirSet()) {
				return;
			}
			if (e.isShiftDown()) {
				if (e.getCode().equals(KeyCode.W)) {
					if (-affineTransform.getTranslateY()+videoHeight > frameHeight*affineTransform.getScaleX()) {
						return;
					}

					repaint = true;
					AffineTransform tempTransform = new AffineTransform();
					tempTransform.concatenate(affineTransform);
					tempTransform.translate(0, -50);
					affineTransform = tempTransform;
				} else if (e.getCode().equals(KeyCode.S)) {
					if (affineTransform.getTranslateY() > 0 ) {
						return;
					}
					repaint = true;
					AffineTransform tempTransform = new AffineTransform();
					tempTransform.concatenate(affineTransform);
					tempTransform.translate(0, 50);
					affineTransform = tempTransform;
				} else if (e.getCode().equals(KeyCode.A)) {
					System.out.println(videoHeight);
					if ((-affineTransform.getTranslateX() + DEFAULT_DISPLAY_WIDTH) > frameWidth*affineTransform.getScaleX()) {
						return;
					}
					repaint = true;
					AffineTransform tempTransform = new AffineTransform();
					tempTransform.concatenate(affineTransform);
					tempTransform.translate(-50, 0);
					affineTransform = tempTransform;
				} else if (e.getCode().equals(KeyCode.D)) {
					if (affineTransform.getTranslateX() > 0) {
						return;
					}
					repaint = true;
					AffineTransform tempTransform = new AffineTransform();
					tempTransform.concatenate(affineTransform);
					tempTransform.translate(50, 0);
					affineTransform = tempTransform;
				}
			} else if (e.getCode() == KeyCode.F) {
				if (repaintInMotion) {
					repaintInMotion = false;
					return;
				}
				int changedIntWidth = 50;
				double newVideoWidth = videoWidth + changedIntWidth;
				videoWidth = newVideoWidth;
				repaint = true;
			} else if (e.getCode() == KeyCode.H) {
				if (repaintInMotion) {
					repaintInMotion = false;
					return;
				}
				int changedIntWidth = -50;
				double newVideoWidth = videoWidth + changedIntWidth;
				if (newVideoWidth > DEFAULT_DISPLAY_WIDTH) {
					videoWidth = newVideoWidth;
					repaint = true;
				} else {
					videoWidth = DEFAULT_DISPLAY_WIDTH;
					repaint = true;
				}
			} else if (e.getCode() == KeyCode.T) {
				try {
					resetScalingAndTransformation(null);
					return;
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (JCodecException e1) {
					e1.printStackTrace();
				}
			}

			if (repaint) {
				try {
					repaintElements();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (JCodecException e1) {
					e1.printStackTrace();
				}
				return;
			}

			if (selectedRectangle == null) {
				return;
			}
			if (e.getCode() == KeyCode.DELETE) {

				int index = currentFrameRectangles.indexOf(selectedRectangle);
				currentFrameRectangles.remove(selectedRectangle);
				if (currentFrameRectangles.size() > 0) {
					if (index >= currentFrameRectangles.size()) {
						selectedRectangle = currentFrameRectangles.get(0);
					} else {
						selectedRectangle = currentFrameRectangles.get(index);
					}
					selectedRectangle.select();
				}

			} else if (e.getCode() == KeyCode.P) {

				int index = currentFrameRectangles.indexOf(selectedRectangle);
				selectedRectangle.unSelect();
				int rectanglesPased = 0;
				while (true) {
					index--;
					if (index < 0) {
						selectedRectangle = currentFrameRectangles.get(currentFrameRectangles.size() - 1);
						index = currentFrameRectangles.size() - 1;
					} else {
						selectedRectangle = currentFrameRectangles.get(index);
					}
					if (canShow(scaleRectangle(selectedRectangle))) {
						break;
					}
					rectanglesPased++;
					if (rectanglesPased == currentFrameRectangles.size()) {
						System.out.println("tu");
						return;
					}
				}
				selectedRectangle.select();

			} else if (e.getCode() == KeyCode.N) {
				int index = currentFrameRectangles.indexOf(selectedRectangle);
				selectedRectangle.unSelect();
				int rectanglesPassed = 0;
				while (true) {
					index++;
					if (index >= currentFrameRectangles.size()) {
						selectedRectangle = currentFrameRectangles.get(0);
						index = 0;
					} else {
						selectedRectangle = currentFrameRectangles.get(index);
					}
					if (canShow(scaleRectangle(selectedRectangle))) {
						break;
					}
					rectanglesPassed++;
					if (rectanglesPassed == currentFrameRectangles.size()) {
						return;
					}
				}
				selectedRectangle.select();

			} else if (e.isShiftDown()) {

				if (e.getCode().equals(KeyCode.I)) {
					if (selectedRectangle.getHeight() == 1) {
						return;
					}

					selectedRectangle.setHeight(selectedRectangle.getHeight() - 1);
					selectedRectangle.setY(selectedRectangle.getY() + 1);

				} else if (e.getCode().equals(KeyCode.K)) {
					if (selectedRectangle.getHeight() == 1) {
						return;
					}
					selectedRectangle.setHeight(selectedRectangle.getHeight() - 1);

				} else if (e.getCode().equals(KeyCode.J)) {
					if (selectedRectangle.getWidth() == 1) {
						return;
					}
					selectedRectangle.setWidth(selectedRectangle.getWidth() - 1);
					selectedRectangle.setX(selectedRectangle.getX() + 1);

				} else if (e.getCode().equals(KeyCode.L)) {
					if (selectedRectangle.getWidth() == 1) {
						return;
					}
					selectedRectangle.setWidth(selectedRectangle.getWidth() - 1);

				}
			} else if (e.getCode().equals(KeyCode.W)) {
				if (affineTransform.getScaleY() * (selectedRectangle.getY() - 1) + affineTransform.getTranslateY() <=
						0) {
					return;
				}
				selectedRectangle.setY(selectedRectangle.getY() - 1);
			} else if (e.getCode().equals(KeyCode.S)) {
				if (affineTransform.getScaleY() * (selectedRectangle.getY() + 1 + selectedRectangle.getHeight()) +
						affineTransform.getTranslateY() >= videoHeight) {
					return;
				}
				selectedRectangle.setY(selectedRectangle.getY() + 1);
			} else if (e.getCode().equals(KeyCode.A)) {
				if (affineTransform.getScaleX() * (selectedRectangle.getX() - 1) + affineTransform.getTranslateX() <=
						0) {
					return;
				}
				selectedRectangle.setX(selectedRectangle.getX() - 1);
			} else if (e.getCode().equals(KeyCode.D)) {
				if (affineTransform.getScaleX() * (selectedRectangle.getX() + 1 + selectedRectangle.getWidth()) +
						affineTransform.getTranslateX() >= videoWidth) {
					return;
				}
				selectedRectangle.setX(selectedRectangle.getX() + 1);
			} else if (e.getCode().equals(KeyCode.I)) {
				if (affineTransform.getScaleY() * (selectedRectangle.getY() - 1) + affineTransform.getTranslateY() <=
						0) {
					return;
				}
				selectedRectangle.setHeight(selectedRectangle.getHeight() + 1);
				selectedRectangle.setY(selectedRectangle.getY() - 1);
			} else if (e.getCode().equals(KeyCode.K)) {
				if (affineTransform.getScaleY() * (selectedRectangle.getHeight() + 1 + selectedRectangle.getY()) +
						affineTransform.getTranslateY() >= videoHeight) {
					return;
				}
				selectedRectangle.setHeight(selectedRectangle.getHeight() + 1);
			} else if (e.getCode().equals(KeyCode.J)) {
				if (affineTransform.getScaleX() * (selectedRectangle.getX() - 1) + affineTransform.getTranslateX() <=
						0) {
					return;
				}
				selectedRectangle.setWidth(selectedRectangle.getWidth() + 1);
				selectedRectangle.setX(selectedRectangle.getX() - 1);
			} else if (e.getCode().equals(KeyCode.L)) {
				if (affineTransform.getScaleX() * (selectedRectangle.getWidth() + 1 + selectedRectangle.getX()) +
						affineTransform.getTranslateX() >= videoWidth) {
					return;
				}
				selectedRectangle.setWidth(selectedRectangle.getWidth() + 1);
			}

			try {
				repaintElements();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (JCodecException e1) {
				e1.printStackTrace();
			}

		});

		footballFieldImage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {
			if (drawingInitialized) {
				if (mouseEvent.getX() < videoWidth && mouseEvent.getY() < videoHeight && mouseEvent.getX() > 0 &&
						mouseEvent.getY() > 0) {
					EditRectangle rectangle = DrawingUtil.createRectangle(mouseEvent.getX(), mouseEvent.getY(),
							beginningX, beginningY);
					drawTemporaryRectangleOnImageView(rectangle);
				} else {
					try {
						repaintElements();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JCodecException e) {
						e.printStackTrace();
					}
				}
			}
		});


		footballFieldImage.setOnScroll(event -> {
			AffineTransform tempTransform = new AffineTransform();
			tempTransform.concatenate(affineTransform);
			if (event.getDeltaY() < 0) {
				if (affineTransform.getTranslateX() < -(videoWidth * affineTransform.getScaleX() *
						UNITS_SCROLLED_DECREASE) / 2 || affineTransform.getTranslateY() < -(videoHeight *
						affineTransform.getScaleY() * UNITS_SCROLLED_DECREASE) / 2) {
					return;
				}
				tempTransform.scale(UNITS_SCROLLED_DECREASE, UNITS_SCROLLED_DECREASE);
				//umanjenje
			} else if (event.getDeltaY() > 0) {
				//uvecanje
				tempTransform.scale(UNITS_SCROLLED_MAGNIFY, UNITS_SCROLLED_MAGNIFY);
			}
			affineTransform = tempTransform;
			try {
				repaintElements();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JCodecException e) {
				e.printStackTrace();
			}
		});

		// Draw the rectangle once user finishes dragging.
		footballFieldImage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
			if (drawingInitialized) {
				if (mouseEvent.getX() < videoWidth && mouseEvent.getY() < videoHeight && mouseEvent.getX() > 0 &&
						mouseEvent.getY() > 0) {
					EditRectangle rectangle = DrawingUtil.createRectangle(mouseEvent.getX(), mouseEvent.getY(),
							beginningX, beginningY);
					drawRectangleOnImageView(rectangle);
				}
			}

			drawingInitialized = false;
		});

		markedFramesList.setCellFactory(TextFieldListCell.forListView());
		markedFramesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				return;
			}
			int frameNumber = Integer.parseInt(newValue);

			frameNumberField.setText(String.valueOf(frameNumber));
			frameSlider.setValue(frameNumber);
			try {
				setSelectedFrame(frameNumber);
			} catch (IOException | JCodecException e) {
				Message.error("Greška!", "Dogodila se greška u sustavu.");

				e.printStackTrace();
			}

		});
	}


	@FXML
	public void edit(Event event) throws IOException, JCodecException {
		KeyEvent keyEvent = (KeyEvent) event;
		if (keyEvent.getCode().equals(KeyCode.DELETE)) {
			String framenumber = markedFramesList.getSelectionModel().getSelectedItems().get(0);
			if (framenumber == null) {
				return;
			}
			if (markedFramesList.getItems().contains(framenumber)) {
				markedFramesList.getItems().remove(framenumber);
			}
			if (Integer.parseInt(framenumber) == setLabelForSliderValue()) {// getFrameNumber(setLabelForSliderValue()
				// )) {
				setSelectedFrame(Integer.parseInt(framenumber));
			}
			evaluationMainApp.removeMarkedFrame(Integer.parseInt(framenumber));
		}
	}

	@FXML
	public void setGroundTruthFile(ActionEvent actionEvent) throws IOException, JCodecException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Odaberite datoteku s ground truth datotekom");

		File datoteka = fileChooser.showOpenDialog(scene.getWindow());
		if (datoteka == null) {
			return;
		}
		evaluationMainApp.clearMarkedFrames();
		BufferedReader reader = new BufferedReader(new FileReader(datoteka));
		String line = reader.readLine();
		while (line != null) {
			//vratise
			EditRectangle rect = EditRectangle.parse(line);
			DrawingUtil.setDefaultProperties(rect);
			int frame = Integer.parseInt(line.split(",")[0]);
			Set<EditRectangle> rectangles = evaluationMainApp.getMarkedFrame(frame);
			if (rectangles == null) {
				rectangles = new HashSet<>();
			}
			rectangles.add(rect);
			evaluationMainApp.updateMarkedFrame(frame, rectangles);
			line = reader.readLine();
		}
		markedFramesList.getItems().clear();
		for (Map.Entry<Integer, Set<EditRectangle>> entry : evaluationMainApp.getMarkedFrames().entrySet()) {
			markedFramesList.getItems().add(String.valueOf(entry.getKey()));
		}
		setSelectedFrame(setLabelForSliderValue());//getFrameNumber(setLabelForSliderValue()));

	}

	@FXML
	public void removeAllMarksForCurrentFrame(ActionEvent actionEvent) throws IOException, JCodecException {
		currentFrameRectangles.clear();
		if (!evaluationMainApp.isVideoDirSet()) {
			Message.warning("Upozorenje!", "Niste učitali video.");
			return;
		}
		repaintElements();
	}

	@FXML
	public void resetScalingAndTransformation(ActionEvent actionEvent) throws IOException, JCodecException {
		affineTransform = new AffineTransform();
		videoWidth = DEFAULT_DISPLAY_WIDTH;
		if (!evaluationMainApp.isVideoDirSet()) {
			return;
		}
		repaintElements();
		footballFieldImage.requestFocus();
	}

	public void displayKeyboardShortcuts(ActionEvent actionEvent) throws IOException {
		String shortcutsData = new String(Files.readAllBytes(Paths.get(shortcutsFile)));
		Stage newStage = new Stage();
		newStage.initModality(Modality.APPLICATION_MODAL);
		evaluationMainApp.addChild(newStage);
		VBox vBox = new VBox(20);
		vBox.setPrefWidth(300);
		vBox.setPrefHeight(500);
		vBox.getChildren().add(new Text(shortcutsData));
		Scene scene = new Scene(vBox);
		newStage.setScene(scene);
		newStage.show();
	}

	public void displayInstructions(ActionEvent actionEvent) throws IOException {
		String instructionsData = new String(Files.readAllBytes(Paths.get(instructionsFile)));
		Stage newStage = new Stage();
		newStage.initModality(Modality.APPLICATION_MODAL);
		evaluationMainApp.addChild(newStage);
		VBox vBox = new VBox(20);
		vBox.setPrefWidth(300);
		vBox.setPrefHeight(500);
		vBox.getChildren().add(new Text(instructionsData));
		Scene scene = new Scene(vBox);
		newStage.setScene(scene);
		newStage.show();

	}
}