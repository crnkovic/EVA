package hr.fer.zemris.projekt;

import javafx.scene.shape.Rectangle;

/**
 * Created by bmihaela.
 */
public class EditRectangle extends Rectangle {

	public EditRectangle(double x, double y, double width, double height) {
		super(x, y, width, height);
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
}
