package hr.fer.zemris.projekt;

public class ComputationUtils {
    /**
     * Computes Jaccard index based on the rectangle and "ground truth" rectangle properties.
     *
     * @param x                 Coordinate of the marked rectangle
     * @param y                 Coordinate of the marked rectangle
     * @param width             Width of the marked rectangle
     * @param height            Height of the marked rectangle
     * @param genX      Coordinate of the ground truth rectangle
     * @param genY      Coordinate of the ground truth rectangle
     * @param genWidth  Width of the ground truth rectangle
     * @param genHeight Height of the ground truth rectangle
     * @return Computed Jaccard index
     * @see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Calculating the Jaccard index</a>
     */
    public static double computeJaccardIndex(double x, double y, double width, double height, double genX, double genY, double genWidth, double genHeight) {
        java.awt.geom.Rectangle2D markedRect = new java.awt.geom.Rectangle2D.Double(x,y,width,height);
        java.awt.geom.Rectangle2D genRect= new java.awt.geom.Rectangle2D.Double(genX,genY,genWidth,genHeight);
        if(
                !(markedRect.intersects(genRect))
        ) {
            return 0;
        }
        java.awt.geom.Rectangle2D intersectionAreaRect = (markedRect.createIntersection(genRect));
        double intersectionArea = intersectionAreaRect.getWidth() * intersectionAreaRect.getHeight();
        java.awt.geom.Rectangle2D UnionAreaRectangle = (markedRect.createUnion(genRect));
        double unionArea = UnionAreaRectangle.getWidth() * UnionAreaRectangle.getHeight();
        System.out.println("presjek:"+intersectionArea+" unija:"+unionArea);
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
        return (float) ((double)truePositives / (truePositives + falseNegatives)) * 100;
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
        return (float) ((float)truePositives / (truePositives + falsePositives)) * 100;
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
        return (float)(0.02 * ((float)recall * precision) / (recall + precision));
    }
}
