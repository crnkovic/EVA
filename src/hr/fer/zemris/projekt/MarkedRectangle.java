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
}
