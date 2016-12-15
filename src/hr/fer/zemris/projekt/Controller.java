package hr.fer.zemris.projekt;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
	private Scene scene;
	private EvaluationMain evaluationMainApp;
	private static final int FRAME_HOP = 15;
	@FXML
	private Label frameNumberField;
	@FXML
	private Slider frameSlider;
	@FXML
	private ImageView footballFieldImage;
	@FXML
	private ListView markedFramesList;
	@FXML
	private Label recallValue;
	@FXML
	private Label precisionValue;
	@FXML
	private Label f1Value;
	@FXML
	private TextField jaccardovIndex;

	public void setUp(Scene scene, EvaluationMain evaluationMain) {
		this.scene = scene;
		this.evaluationMainApp = evaluationMain;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}


	//TODO add marking on the frames

	@FXML
	public void newFrameSelected(Event event) throws IOException, JCodecException {
		int frameNumber = setLabelForSliderValue();
		setSelectedFrame(frameNumber);
	}

	private int getRealFrameNumberForSliderValue(long frame) {
		return (int) (FRAME_HOP * frame) + 1;
	}

	public void setSelectedFrame(int number) throws IOException, JCodecException {
		BufferedImage fieldImage = getImageForFrame(number);
		Image image = SwingFXUtils.toFXImage(fieldImage, null);
		footballFieldImage.setImage(image);

		//TODO: add marked rectangles
	}

	public BufferedImage getImageForFrame(int number) throws IOException, JCodecException {
		int frameNumber = number-1;
		File frameFile = evaluationMainApp.getDumpDir().toPath().resolve(frameNumber + ".png").toFile();
		BufferedImage fieldImage;
		if (frameFile.exists()) {
			fieldImage = ImageIO.read(frameFile);
		} else {
			fieldImage = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), frameNumber);
			ImageIO.write(fieldImage, "png", frameFile);
		}
		return fieldImage;
	}

	@FXML
	public void setVideoAndSetUp(ActionEvent actionEvent) throws FrameGrabber.Exception, IOException, JCodecException {
		//TODO : dodajte jos neke ekstenzije ako mislite da je potrebno
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

		if(evaluationMainApp.isDumpFolderSet()){
			setSelectedFrame(0);
			frameSlider.setDisable(false);
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
		if(evaluationMainApp.isVideoDirSet()){
			setSelectedFrame(0);
			frameSlider.setDisable(false);
		}
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
		if(evaluationMainApp.getMarkedFrames().size() == 0){
			//TODO throw warning that there should be marked frames
			return;
		}

		int truePositives=0;
		int falsePositives=0;
		int falseNegatives=0;

		for(int frameNumber : evaluationMainApp.getMarkedFrames().keySet()){

			List<MarkedRectangle> groundTruthFrame = evaluationMainApp.getMarkedFrames().get(frameNumber);
			List<MarkedRectangle> notusedGeneratorRectangles = rectanglesForAFrame(frameNumber);
			for (MarkedRectangle groundTruthRectangle: groundTruthFrame) {

				for (MarkedRectangle generatorRectangle : notusedGeneratorRectangles) {
					MarkedRectangle usedRectangle=null;
					boolean hit = false;
					if(generatorRectangle.jaccardsIndex(groundTruthRectangle) > Float.parseFloat(jaccardovIndex.getText())){
						//hit
						usedRectangle=generatorRectangle;
						hit = true;
						break;
					}
					if(hit){
						notusedGeneratorRectangles.remove(usedRectangle);
						hit=false;
						truePositives++;
					}else{
						falseNegatives++;
					}
				}

			}
			falsePositives=notusedGeneratorRectangles.size();
		}
		float recall = (float) truePositives/(truePositives+falseNegatives);
		recallValue.setText(String.valueOf(recall)+"%");

		float precision = (float) truePositives/(truePositives+falsePositives);
		precisionValue.setText(String.valueOf(precision)+"%");

		float f1 = 2 * (recall*precision)/(recall+precision);
		f1Value.setText(String.valueOf(f1)+"%");
	}

	public LinkedList<MarkedRectangle> rectanglesForAFrame(int numberOfFrame){
		LinkedList<MarkedRectangle> rectangles = new LinkedList<>();
		try {
			Files.lines(evaluationMainApp.getEvaluationFile().toPath())
					.filter(line -> line.startsWith(Integer.toString(numberOfFrame) + ","))
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
								new MarkedRectangle(
										numberOfFrame,
										Integer.parseInt(polje[5]),
										Integer.parseInt(polje[6]),
										Integer.parseInt(polje[7]),
										Integer.parseInt(polje[8])
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
	}

	@FXML
	public void saveFileWithMarks(ActionEvent actionEvent) {
	}

	@FXML
	public void saveMarks(ActionEvent actionEvent) {
	}

	@FXML
	public void setFrameNumberInLabel(Event event) {
		setLabelForSliderValue();
	}

	/**
	 * Sets the label next to the slider depending on the slider position.
	 * @return selected frame with the slider position
	 */
	public int setLabelForSliderValue(){
		long frame = Math.round(frameSlider.getValue());
		int frameNumber = getRealFrameNumberForSliderValue(frame);
		frameNumberField.setText(String.valueOf(frameNumber));
		return frameNumber;
	}

	@FXML
	public void setMarkedFramesFile(ActionEvent actionEvent) {
	}

	@FXML
	public void markFromGivenFile(ActionEvent actionEvent) {
	}

	@FXML
	public void markFromEvaluation(ActionEvent actionEvent) {
	}
}
