package hr.fer.zemris.projekt;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by bmihaela.
 */
public class VideoUtil {

	public static int getNumberOfFrames(String pathToVideo) throws FrameGrabber.Exception {
		//TODO : makni izlaz na system err
		FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(pathToVideo);
		frameGrabber.start();
		int lenght = frameGrabber.getLengthInFrames();
		frameGrabber.stop();
		return lenght;
	}

	public static BufferedImage getFrame(String path,int frameNumber) throws IOException, JCodecException {
		Picture picture = FrameGrab.getNativeFrame(new File(path), frameNumber);
		return AWTUtil.toBufferedImage(picture);
	}


}
