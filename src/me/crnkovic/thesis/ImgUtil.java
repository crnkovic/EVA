package me.crnkovic.thesis;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImgUtil {
    private static BufferedImage matToBufferedImage(Mat original) {
        byte[] data = new byte[original.rows() * original.cols() * (int) (original.elemSize())];
        original.get(0, 0, data);

        BufferedImage image = null;

        if (original.channels() > 1) {
            image = new BufferedImage(original.cols(), original.rows(), BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(original.cols(), original.rows(), BufferedImage.TYPE_BYTE_GRAY);
        }

        image.getRaster().setDataElements(0, 0, original.cols(), original.rows(), data);

        return image;
    }

    public static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);

        return mat;
    }

    public static Mat newMat(BufferedImage img) {
        return new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
    }

    public static void saveImage(Mat img, String name) throws IOException {
        File file = new File("hsv/" + name + ".png");
        ImageIO.write(matToBufferedImage(img), "png", file);
    }

    public static void saveImage(BufferedImage img, String name) throws IOException {
        saveImage(bufferedImageToMat(img), name);
    }
}
