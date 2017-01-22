package hr.fer.zemris.projekt;

import javafx.scene.paint.Color;

/**
 * Created by bmihaela.
 */
public class DrawingUtil {




	/**
	 * Draws a red-bordered rectangle based its coordinates.
	 *
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @return A rectangle object (Instance of the <b>javafx.scene.shape.Rectangle</b> class)
	 */
	 public static EditRectangle createRectangle(double x, double y, double beginningX, double beginningY) {
		double width = Math.abs(beginningX - x);
		double height = Math.abs(beginningY - y);

		double startX = beginningX < x ? beginningX : x;
		double startY = beginningY < y ? beginningY : y;

		EditRectangle rectangle = new EditRectangle(startX, startY, width, height);
		setDefaultProperties(rectangle);

		return rectangle;
	 }

	public static void setDefaultProperties(EditRectangle rectangle){
		rectangle.setDisable(false);
		rectangle.setFill(null);
		rectangle.setOriginalColor(Color.RED);
		rectangle.setStroke(rectangle.getOriginalColor());
		rectangle.setStrokeWidth(1);
	}

	public static void translateProperties(EditRectangle fromRectangle, EditRectangle toRectnagle){
		toRectnagle.setDisable(fromRectangle.isDisable());
		toRectnagle.setFill(fromRectangle.getFill());
		toRectnagle.setOriginalColor(fromRectangle.getOriginalColor());
		toRectnagle.setStroke(fromRectangle.getStroke());
		toRectnagle.setStrokeWidth(fromRectangle.getStrokeWidth());
	}
}
