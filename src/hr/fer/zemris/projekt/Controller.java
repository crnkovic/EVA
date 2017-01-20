package hr.fer.zemris.projekt;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class Controller implements Initializable {
	private static final float DISPLAY_WIDTH = (float) 820.0;
	private static final float DISPLAY_HEIGHT = (float) 470.0;
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

	/**
	 * List containing drawn rectangles.
	 */
	private List<EditRectangle> drawnRectangles;

	/**
	 * List containing indices of removed frames.
	 */
	static List<Integer> removedFrames = new ArrayList<>();
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

		// Set up slider
		frameSlider.setMax(numberOfFrames);
		frameSlider.setMin(1);
		frameSlider.setBlockIncrement(BLOCK_INCREMENT);

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
	public void loadRectanglesFromFile() throws IOException, JCodecException {
		if (evaluationMainApp.getEvaluationFile() == null) {
			Message.error("Greška!", "Niste učitali datoteku s detektiranim oznakama.");

			return;
		}

		Set<EditRectangle> rectangles = rectanglesForAFrame((int) Math.floor(frameSlider.getValue()));

		drawnRectangles.addAll(rectangles);

		for (EditRectangle rectangle : rectangles) {
			rectangle.setDisable(false);
			rectangle.setFill(null);
			rectangle.setStroke(javafx.scene.paint.Color.RED);
			rectangle.setStrokeWidth(1);

			imagePane.getChildren().add(rectangle);
		}
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

			// Set false negatives count to all detected rectangles size, so we can decrement it once we hit a
			// rectangle
			falseNegatives += groundTruthRectangles.size();

			for (javafx.scene.shape.Rectangle generatorRectangle : detectedRectangles) {
				boolean hit = false;

				System.out.println("asasddsa");
				for (javafx.scene.shape.Rectangle groundTruthRectangle : groundTruthRectangles) {
					if (computeJaccardIndex(groundTruthRectangle, generatorRectangle) > Float.parseFloat(jaccardIndex
							.getText())) {
						// We have hit it, well done, increment the true positive and decrement the false negative!
						hit = true;
						truePositives++;
						falseNegatives--;

						break;
					}
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
		if(!evaluationMainApp.isVideoDirSet()){
			Message.warning("Upozorenje!","Video nije učitan.");
		}
		// Get map of all marked frames from the application
		Map<Integer, Set<EditRectangle>> markedFrames = evaluationMainApp.getMarkedFrames();

		// Calculate frame number
		int frameNumber = (int)Math.floor(frameSlider.getValue());//getFrameNumber((long)Math.floor(frameSlider.getValue()));
		frameNumber = Integer.parseInt(frameNumberField.getText());
		// Get all drawn rectangles in this frame and save them to the markedFrames map
		Set<EditRectangle> rectangles = new HashSet<>();
		rectangles.addAll(drawnRectangles);
		markedFrames.put(frameNumber, rectangles);

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

		drawnRectangles = new ArrayList<>();
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

		for (EditRectangle drawnRectangle : drawnRectangles) {
			drawnRectangle.setStroke(javafx.scene.paint.Color.RED);
		}

		BufferedImage fieldImage = getImageForFrame(number);
		Image image = SwingFXUtils.toFXImage(fieldImage, null);

		// Display (draw) the frame to the user
		footballFieldImage.setImage(image);

		Set<EditRectangle> rectanglesToDraw = evaluationMainApp.getMarkedFrame(number);

		imagePane.getChildren().removeAll(drawnRectangles);
		drawnRectangles.clear();

		// There are any rectangles to be drawn?
		if (rectanglesToDraw != null) {
			// Loop through the rectangles to be drawn and draw them
			for (EditRectangle rectangle : rectanglesToDraw) {
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
										Double.parseDouble(property[5]) * evaluationMainApp.getWidthMultiplier(),
										Double.parseDouble(property[6]) * evaluationMainApp.getWidthMultiplier() -
												Double.parseDouble(property[8]) * evaluationMainApp.getWidthMultiplier(),
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
		int frameNumber =(int) Math.floor(frameSlider.getValue()); //getFrameNumber((long) Math.floor(frameSlider.getValue()));
		frameNumberField.setText(String.valueOf(frameNumber));

		return frameNumber;
	}

	/**
	 * Get "real" calculated frame number based on the slider value.
	 * Multiplies slider value by the FRAME_HOP constant.
	 *
//	 * @param sliderValue Slider value
	 * @return Calculate frame number
	 */
//	private int getFrameNumber(long sliderValue) {
//		// Return frame 1+0
//		return (int) (sliderValue + 1);
//	}

	@FXML
	public void handleEnterPressed(KeyEvent e) throws IOException, JCodecException, FrameGrabber.Exception {
		if (e.getCode() == KeyCode.ENTER) {
			if(!evaluationMainApp.isVideoDirSet()){
				Message.warning("Upozorenje!","Niste učitali video.");
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
			int totalNumberOfFrames =VideoUtil.getNumberOfFrames(evaluationMainApp.getVideoPath());
			if(broj > totalNumberOfFrames){
				broj = totalNumberOfFrames;
			}

			setSelectedFrame(broj);
			frameSlider.setValue(broj-1);
			frameNumberField.setText(String.valueOf(broj));
			frameNumberTextField.clear();
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
			if (evaluationMainApp.isDumpingDirSet() && evaluationMainApp.isVideoDirSet()) {
				beginningX = mouseEvent.getX();
				beginningY = mouseEvent.getY();

				selectedRectangle = null;
				drawingInitialized = true;

				for (EditRectangle rectangle : drawnRectangles) {
					if (rectangle.getX() < beginningX && rectangle.getY() < beginningY && rectangle.getX() + rectangle
							.getWidth() > beginningX && rectangle.getY() + rectangle.getHeight() > beginningY) {
						drawingInitialized = false;
						rectangle.setStroke(javafx.scene.paint.Color.CORNFLOWERBLUE);

						selectedRectangle = rectangle;
						footballFieldImage.requestFocus();
					} else {
						rectangle.setStroke(javafx.scene.paint.Color.RED);
					}
				}
			}
		});

		footballFieldImage.setFocusTraversable(true);


		footballFieldImage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if(selectedRectangle == null){
				return;
			}
			if (e.getCode() == KeyCode.DELETE) {
				int frame = (int)Math.floor(frameSlider.getValue());//getFrameNumber((long) Math.floor(frameSlider.getValue()));

				int indexRectaZaObrisati = 0;
				for (EditRectangle rectangle : drawnRectangles) {
					if (rectangle.equals(selectedRectangle)) {
						break;
					}

					indexRectaZaObrisati++;
				}



				drawnRectangles.remove(selectedRectangle);
				imagePane.getChildren().remove(selectedRectangle);

				Set<EditRectangle> remainingRectangles = evaluationMainApp.getMarkedFrame(frame);
				if (remainingRectangles != null) {

					System.out.println("index:"+indexRectaZaObrisati);
					remainingRectangles.remove(selectedRectangle);

				}
				if(indexRectaZaObrisati>=1){
					//nadji sljedeceg i postavi ga kao odabranog ako nismo obrisali zadnjeg
					if(indexRectaZaObrisati==drawnRectangles.size()){
						indexRectaZaObrisati--;
					}
					drawnRectangles.get(indexRectaZaObrisati).setStroke(javafx.scene.paint.Color.CORNFLOWERBLUE);
					selectedRectangle = drawnRectangles.get(indexRectaZaObrisati);
				}
				evaluationMainApp.updateMarkedFrame(frame, remainingRectangles);
			} else if (e.getCode() == KeyCode.P) {
				int i = 0;

				for (EditRectangle rectangle : drawnRectangles) {
					if (rectangle.equals(selectedRectangle)) {
						break;
					}

					i++;
				}

				drawnRectangles.get(i).setStroke(javafx.scene.paint.Color.RED);

				if (i == 0) {
					i = drawnRectangles.size();
				}

				drawnRectangles.get(i - 1).setStroke(javafx.scene.paint.Color.CORNFLOWERBLUE);
				selectedRectangle = drawnRectangles.get(i - 1);
			} else if (e.getCode() == KeyCode.N) {
				int i = 0;

				for (EditRectangle rectangle : drawnRectangles) {
					if (rectangle.equals(selectedRectangle)) {
						break;
					}

					i++;
				}

				drawnRectangles.get(i).setStroke(javafx.scene.paint.Color.RED);

				if (i == drawnRectangles.size() - 1) {
					i = -1;
				}

				drawnRectangles.get(i + 1).setStroke(javafx.scene.paint.Color.CORNFLOWERBLUE);
				selectedRectangle = drawnRectangles.get(i + 1);
			}else if(e.isShiftDown()){

				if(e.getCode().equals(KeyCode.I)){
					if(selectedRectangle.getHeight() == 1){
						return;
					}
					selectedRectangle.setHeight(selectedRectangle.getHeight()-1);
					selectedRectangle.setY(selectedRectangle.getY()+1);

				}else if(e.getCode().equals(KeyCode.K)){
					if(selectedRectangle.getHeight() == 1){
						return;
					}
					selectedRectangle.setHeight(selectedRectangle.getHeight()-1);

				}else if(e.getCode().equals(KeyCode.J)){
					if(selectedRectangle.getWidth() == 1){
						return;
					}
					selectedRectangle.setWidth(selectedRectangle.getWidth()-1);
					selectedRectangle.setX(selectedRectangle.getX()+1);

				}else if(e.getCode().equals(KeyCode.L)){
					if(selectedRectangle.getWidth() == 1){
						return;
					}
					selectedRectangle.setWidth(selectedRectangle.getWidth()-1);

				}

			}else if(e.getCode().equals(KeyCode.W)){
				if(selectedRectangle.getY() == 1){
					return;
				}
				selectedRectangle.setY(selectedRectangle.getY()-1);

			}else if(e.getCode().equals(KeyCode.S)){
					if(selectedRectangle.getY()+selectedRectangle.getHeight()
                            >=
                            evaluationMainApp.getVideoHeight()*evaluationMainApp.getWidthMultiplier()-2
					) {

						return;
					}
				selectedRectangle.setY(selectedRectangle.getY()+1);

			}else if(e.getCode().equals(KeyCode.A)){
				if(selectedRectangle.getX()==1){
					return;
				}
				selectedRectangle.setX(selectedRectangle.getX()-1);

			}else if(e.getCode().equals(KeyCode.D)){
				if(selectedRectangle.getX()+selectedRectangle.getWidth() >= DISPLAY_WIDTH-1){
					return;
				}
				selectedRectangle.setX(selectedRectangle.getX()+1);

			}else if(e.getCode().equals(KeyCode.I)){
				if(selectedRectangle.getY() == 1){
					return;
				}
				selectedRectangle.setHeight(selectedRectangle.getHeight()+1);
				selectedRectangle.setY(selectedRectangle.getY()-1);

			}else if(e.getCode().equals(KeyCode.K)){
				if(selectedRectangle.getY()+selectedRectangle.getHeight()
						>=
						evaluationMainApp.getVideoHeight()*evaluationMainApp.getWidthMultiplier()-2
						) {

					return;
				}
				selectedRectangle.setHeight(selectedRectangle.getHeight()+1);

			}else if(e.getCode().equals(KeyCode.J)){
				if(selectedRectangle.getX() == 1 ){
					return;
				}
				selectedRectangle.setWidth(selectedRectangle.getWidth()+1);
				selectedRectangle.setX(selectedRectangle.getX()-1);

			}else if(e.getCode().equals(KeyCode.L)){
				if(selectedRectangle.getX()+selectedRectangle.getWidth() >= DISPLAY_WIDTH-1){
					return;
				}
				selectedRectangle.setWidth(selectedRectangle.getWidth()+1);

			}
		});

		// User is drawing the rectangle right now!
		footballFieldImage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {
			if (drawingInitialized) {
				if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage
						.getFitHeight() && mouseEvent.getX() > 0 && mouseEvent.getY() > 0) {
					EditRectangle rectangle = DrawingUtil.drawRectangle(mouseEvent.getX(), mouseEvent.getY(),
							beginningX, beginningY);

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
				if (mouseEvent.getX() < footballFieldImage.getFitWidth() && mouseEvent.getY() < footballFieldImage
						.getFitHeight() && mouseEvent.getX() > 0 && mouseEvent.getY() > 0) {
					EditRectangle rectangle = DrawingUtil.drawRectangle(mouseEvent.getX(), mouseEvent.getY(),
							beginningX, beginningY);

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
			frameSlider.setValue(frameNumber);
		});
	}


	@FXML
	public void edit(Event event) throws IOException, JCodecException {
		KeyEvent keyEvent = (KeyEvent) event;
		if (keyEvent.getCode().equals(KeyCode.DELETE)) {
			String framenumber = markedFramesList.getSelectionModel().getSelectedItems().get(0);
			if (markedFramesList.getItems().contains(framenumber)) {
				markedFramesList.getItems().remove(framenumber);
			}
			if (Integer.parseInt(framenumber) == setLabelForSliderValue()){// getFrameNumber(setLabelForSliderValue())) {
				setSelectedFrame(Integer.parseInt(framenumber));
			}
			HashSet<EditRectangle> emptrySet = new HashSet<>();
			evaluationMainApp.updateMarkedFrame(Integer.parseInt(framenumber), emptrySet);
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
			DrawingUtil.setProperties(rect);
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
}