package hr.fer.zemris.projekt;

public class ComputationUtils {
    /**
     * Computes Jaccard index based on the rectangle and "ground truth" rectangle properties.
     *
     * @param x                 Coordinate of the marked rectangle
     * @param y                 Coordinate of the marked rectangle
     * @param width             Width of the marked rectangle
     * @param height            Height of the marked rectangle
     * @param groundTruthX      Coordinate of the ground truth rectangle
     * @param groundTruthY      Coordinate of the ground truth rectangle
     * @param groundTruthWidth  Width of the ground truth rectangle
     * @param groundTruthHeight Height of the ground truth rectangle
     * @return Computed Jaccard index
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    public static double computeJaccardIndex(double x, double y, double width, double height, double groundTruthX, double groundTruthY, double groundTruthWidth, double groundTruthHeight) {
        double newWidth = Math.min(x + width, groundTruthX + groundTruthWidth) - Math.max(x, groundTruthX);
        double newHeight = Math.max(y + y, groundTruthY + height) - Math.min(y, groundTruthY);

        double intersectionArea = newWidth * newHeight;
        double unionArea = (height * width) + (groundTruthHeight * groundTruthWidth) - intersectionArea;

        return intersectionArea / unionArea;
    }

    /**
     * Computes the recall value.
     *
     * @param truePositives  True positive count
     * @param falseNegatives False negative count
     * @return Recall value
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    public static float computeRecall(int truePositives, int falseNegatives) {
        return (float) truePositives / (truePositives + falseNegatives);
    }

    /**
     * Computes the precision value.
     *
     * @param truePositives  True positive count
     * @param falsePositives False positive count
     * @return Recall value
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    public static float computePrecision(int truePositives, int falsePositives) {
        return (float) truePositives / (truePositives + falsePositives);
    }

    /**
     * Computes the F1 value.
     *
     * @param recall    Recall value
     * @param precision Precision value
     * @return Recall value
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall value</a>
     */
    public static float computeF1(float recall, float precision) {
        return 2 * (recall * precision) / (recall + precision);
    }
}
