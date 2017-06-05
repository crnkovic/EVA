package me.crnkovic.thesis;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class VideoUtil {
    /**
     * Get number of frames for specific video whose path is passed on through the <b>pathToVideo</b> variable.
     *
     * @param pathToVideo Path to the video
     * @return Number of frames in a video
     * @throws FrameGrabber.Exception Exception
     */
    public static int getNumberOfFrames(String pathToVideo) throws FrameGrabber.Exception {
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(pathToVideo);

        frameGrabber.start();
        int length = frameGrabber.getLengthInFrames();
        frameGrabber.stop();

        return length;
    }

    /**
     * Get specific video frame.
     *
     * @param path        Path to the file
     * @param frameNumber Frame index
     * @return BufferedImage instance of the frame
     * @throws IOException     IOException
     * @throws JCodecException JCodecException
     */
    public static BufferedImage getFrame(String path, int frameNumber) throws IOException, JCodecException {
        return AWTUtil.toBufferedImage(
                FrameGrab.getNativeFrame(new File(path), frameNumber-1)
        );
    }
}
