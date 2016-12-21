package hr.fer.zemris.projekt;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.JCodecException;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
		Label oznaka = new Label("Želite li spremiti oznaèene okvire:");
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
			
		neke.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				upisi.setDisable(false);
			}	
		});
			
		sve.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				upisi.setDisable(true);
			}	
		});
			
		Button spremi = new Button("Spremi");
			
		spremi.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				int numberOfFrames = 0;
				try {
					numberOfFrames = VideoUtil.getNumberOfFrames(evaluationMainApp.getVideoPath());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(btnNe.isSelected()){
					if(sve.isSelected()){
						DirectoryChooser directoryChooser = new DirectoryChooser();
						directoryChooser.setTitle("Spremi neoznaèene okvire");
						File directory = directoryChooser.showDialog(scene.getWindow());
						for(int i=0; i<numberOfFrames; ++i){
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
					if(neke.isSelected()) {
						if(upisi.getText() == null || upisi.getText().trim().isEmpty()) {
							izbaciUpozorenje("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti oblika: 1, 5, 74, 89, ...");
						} else if(upisi.getText().matches(".*[a-zA-Z]+.*")){
							izbaciUpozorenje("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: 1, 5, 74, 89, ...");
						} else {
							String[] brojeviOkvira = upisi.getText().split(",");
							int[] okviri = null;
							int i = 0;
							int disableOpen=0;
							for(String okvir : brojeviOkvira) {
								okvir = okvir.trim();
								okviri[i]=Integer.parseInt(brojeviOkvira[i]);
								if(numberOfFrames < okviri[i] || okviri[i] < 0) {
									izbaciUpozorenje("Izvan raspona", "Broj ili neki od brojeva su veæi od ukupnog broja okvira ili su manji od 0.");
									disableOpen=1;
									break;
								}
								i++;
							}
						if(disableOpen==0) {
							DirectoryChooser directChooser = new DirectoryChooser();
							directChooser.setTitle("Spremi neoznaèene okvire");
							File direct = directoryChooser.showDialog(scene.getWindow());
								for(int mjesto : okviri) {
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
					directoryChooser.setTitle("Spremi oznaèene okvire");
					File file = directoryChooser.showDialog(scene.getWindow());
					if(sve.isSelected()){
						for(int frameNumber : evaluationMainApp.getMarkedFrames().keySet()) {
							BufferedImage image = null;
							try {
								image = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), frameNumber);
							} catch (IOException | JCodecException e) {
								e.printStackTrace();
							}
							Graphics graph = image.createGraphics();
							file = file.toPath().resolve("O" + frameNumber + ".jpg").toFile();   
							for(MarkedRectangle rec : evaluationMainApp.getMarkedFrame(frameNumber)) {
								graph.setColor(Color.RED);
								graph.drawRect(rec.getxCoordinate(),rec.getyCoordinate(),rec.getHeight(),rec.getWidth());
							}	
							try {
								ImageIO.write(image, "png", file);
							} catch (IOException e) {
								e.printStackTrace();
							}	
						}
					} else {
						if(upisi.getText() == null || upisi.getText().trim().isEmpty()) {
							izbaciUpozorenje("Neispravan unos", "Nije unesen niti jedan broj okvira. Unos treba biti oblika: 1, 5, 74, 89, ...");
						} else if(upisi.getText().matches(".*[a-zA-Z]+.*")){
							izbaciUpozorenje("Neispravan unos", "Unos slova nije dopušten. Unos treba biti oblika: 1, 5, 74, 89, ...");
						}
						String[] okviri = upisi.getText().split(",");
						int[] brojeviOkvira = null;
						int i=0;
						int postoji = 0;
						List<Integer> lista = new ArrayList<>();
						for(String okvir:okviri) {
							okvir=okvir.trim();
							brojeviOkvira[i]=Integer.parseInt(okvir);
							if(evaluationMainApp.getMarkedFrames().keySet().contains(brojeviOkvira[i])) {
								lista.add(brojeviOkvira[i]);
								postoji = 1;
							}
							i++;
						}
						if(postoji == 1) {
							String s = null;
							for(int j=0; i<lista.size(); j++) {
								s = s + lista.get(j) + "\t";	
							}
							izbaciUpozorenje("Krivi upis", "Okviri koji su navedeni, a još nisu oznaèeni (ili su manji od 0) su: " + s);
						}
						for(int broj : brojeviOkvira) {
							if(postoji == 1) break;
							BufferedImage image = null;
							try {
								image = VideoUtil.getFrame(evaluationMainApp.getVideoPath(), broj);
							} catch (IOException | JCodecException e) {
								e.printStackTrace();
							}
							Graphics graph = image.createGraphics();
							file = file.toPath().resolve("O" + broj + ".jpg").toFile();
							for(MarkedRectangle rec : evaluationMainApp.getMarkedFrame(broj)) {
								graph.setColor(Color.RED);
								graph.drawRect(rec.getxCoordinate(),rec.getyCoordinate(),rec.getHeight(),rec.getWidth());
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


	@FXML
	public void saveFileWithMarks(ActionEvent actionEvent) throws IOException {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Spremi datoteku s oznakama");
		File file = directoryChooser.showDialog(scene.getWindow());
		Path directory = file.toPath();
		String path = directory + File.separator + "oznakeOkvira.txt";
		File txtFile = new File(path);
		
		
		List<Integer> redoslijed = new ArrayList<>();
		redoslijed.addAll(evaluationMainApp.getMarkedFrames().keySet());
		redoslijed.sort(null);
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(txtFile), "UTF-8"));
		
		for(int brojOkvira : redoslijed) {
			List<MarkedRectangle> rectangles = evaluationMainApp.getMarkedFrame(brojOkvira);
				for(MarkedRectangle oznaka : rectangles) {
					 writer.write(oznaka.toString() + System.lineSeparator());
					 writer.flush();
			}
		}
		writer.close();
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
	
	public void izbaciUpozorenje(String naslov, String tekst) {
		Alert fail= new Alert(AlertType.INFORMATION);
        fail.setHeaderText(naslov);
        fail.setContentText(tekst);
        fail.showAndWait();
	}
}
