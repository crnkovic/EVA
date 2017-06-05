package me.crnkovic.thesis;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

/**
 * Created by bmihaela.
 */
public class EditRectangle extends Rectangle {
	private static final Paint SELECTION_COLOR = Color.CORNFLOWERBLUE;
	private Paint originalColor;

	public EditRectangle(double x, double y, double width, double height) {
		super(x, y, width, height);
	}

	public EditRectangle() {

	}
	public void select(){
		this.setStroke(SELECTION_COLOR);
	}

	public void unSelect(){
		this.setStroke(originalColor);
	}

	public void setOriginalColor(Paint originalColor){
		this.originalColor = originalColor;
	}

	@Override
	public String toString() {
		return getX() + "," + getY() + "," + getWidth() + "," + getHeight();
	}

	public static EditRectangle parse(String linija) {
		String[] splitanaLinija = linija.split(",");
		return new EditRectangle(
				Double.valueOf(splitanaLinija[1]),
				Double.valueOf(splitanaLinija[2]),
				Double.valueOf(splitanaLinija[3]),
				Double.valueOf(splitanaLinija[4])
		);
	}


	public Paint getOriginalColor() {
		return originalColor;
	}
}
