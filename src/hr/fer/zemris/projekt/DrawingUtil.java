package hr.fer.zemris.projekt;

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
	 public static EditRectangle drawRectangle(double x, double y, double beginningX, double beginningY) {
		double width = Math.abs(beginningX - x);
		double height = Math.abs(beginningY - y);

		double startX = beginningX < x ? beginningX : x;
		double startY = beginningY < y ? beginningY : y;

		EditRectangle rectangle = new EditRectangle(startX, startY, width, height);
		setProperties(rectangle);

		return rectangle;
	 }

	public static void setProperties(EditRectangle rectangle){
		rectangle.setDisable(false);
		rectangle.setFill(null);
		rectangle.setStroke(javafx.scene.paint.Color.RED);
		rectangle.setStrokeWidth(1);
	}
}
