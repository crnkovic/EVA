package hr.fer.zemris.projekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmihaela.
 */
public class MarkedFrame {

	private List<MarkedRectangle> containers;

	public MarkedFrame(int frameNumber) {
		containers = new ArrayList<>();
	}

	public void addMarkedRectangle(MarkedRectangle markedRectangle){
		containers.add(markedRectangle);
	}
}
