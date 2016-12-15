package hr.fer.zemris.projekt;

/**
 * Created by bmihaela.
 */
public class MarkedRectangle {
	private int frameNumber;
	private int xCoordinate;
	private int yCoordinate;
	private int width;
	private int height;

	public MarkedRectangle(int frameNumber, int xCoordinate, int yCoordinate, int width, int height) {
		this.frameNumber = frameNumber;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.width = width;
		this.height = height;
	}

	public int getFrameNumber() {
		return frameNumber;
	}

	public int getxCoordinate() {
		return xCoordinate;
	}

	public void setxCoordinate(int xCoordinate) {
		this.xCoordinate = xCoordinate;
	}

	public int getyCoordinate() {
		return yCoordinate;
	}

	public void setyCoordinate(int yCoordinate) {
		this.yCoordinate = yCoordinate;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(frameNumber).append(",").append(xCoordinate).append(",").append(yCoordinate).append(",").append(width).append(",").append(height);
		return builder.toString();
	}


	/**
	 *  Returns the Jaccard's index value for two rectangles.
	 *  Jaccard value is calculated as intersection area divided by union area of the two rectangles
	 */
	public float jaccardsIndex(MarkedRectangle rect){
		int newX = Math.max(this.xCoordinate, rect.xCoordinate);
		int newY = Math.min(this.yCoordinate, rect.yCoordinate);
		int newWidth = Math.min(this.xCoordinate + this.width, rect.xCoordinate + rect.width) - newX;
		int newHeight = Math.max(this.yCoordinate + this.height, rect.yCoordinate + rect.height) - newY;

		int intersectionArea = newWidth*newHeight;

		int unionArea = this.height*this.width + rect.height*rect.width - intersectionArea;

		return (float)intersectionArea/unionArea;
	}

}
