package hr.fer.zemris.projekt;

/**
 * Created by bmihaela.
 */
public class EvaluationContainer {
	private int frameNumber;
	private int teamMark;
	private int playerID;
	private int xCoordinate;
	private int yCoordinate;
	private int width;
	private int height;

	public EvaluationContainer(int frameNumber, int teamMark, int playerID, int xCoordinate, int yCoordinate, int
			width, int height) {
		this.frameNumber = frameNumber;
		this.teamMark = teamMark;
		this.playerID = playerID;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.width = width;
		this.height = height;
	}


	public EvaluationContainer parse(String evaluationAsString){
		String [] elements = evaluationAsString.split(",");
		frameNumber = Integer.parseInt(elements[0]);
		teamMark = Integer.parseInt(elements[1]);
		playerID = Integer.parseInt(elements[2]);
		xCoordinate = Integer.parseInt(elements[3]);
		yCoordinate = Integer.parseInt(elements[4]);
		width = Integer.parseInt(elements[5]);
		height = Integer.parseInt(elements[6]);
		return new EvaluationContainer(frameNumber,teamMark,playerID,xCoordinate,yCoordinate,width,height);
	}

	public int getFrameNumber() {
		return frameNumber;
	}

	public int getTeamMark() {
		return teamMark;
	}

	public int getPlayerID() {
		return playerID;
	}

	public int getxCoordinate() {
		return xCoordinate;
	}

	public int getyCoordinate() {
		return yCoordinate;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
