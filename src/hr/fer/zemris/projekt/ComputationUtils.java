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
     */
    public static double computeJaccardIndex(double x, double y, double width, double height, double groundTruthX, double groundTruthY, double groundTruthWidth, double groundTruthHeight) {
        double newWidth = Math.min(x + width, groundTruthX + groundTruthWidth) - Math.max(x, groundTruthX);
        double newHeight = Math.max(y + y, groundTruthY + height) - Math.min(y, groundTruthY);

        double intersectionArea = newWidth * newHeight;
        double unionArea = (height * width) + (groundTruthHeight * groundTruthWidth) - intersectionArea;

        return intersectionArea / unionArea;
    }
}
