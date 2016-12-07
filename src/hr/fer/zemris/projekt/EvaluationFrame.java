package hr.fer.zemris.projekt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmihaela.
 */
public class EvaluationFrame {

	private int frameNumber;
	private List<EvaluationContainer> containers;

	public EvaluationFrame(int frameNumber) {
		this.frameNumber = frameNumber;
		containers = new ArrayList<>();
	}

	public void addContainer(EvaluationContainer evaluationContainer){
		containers.add(evaluationContainer);
	}
}
