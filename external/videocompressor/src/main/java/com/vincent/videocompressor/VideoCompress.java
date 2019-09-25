package com.vincent.videocompressor;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Vincent Woo
 * Date: 2017/8/16
 * Time: 15:15
 */

public class VideoCompress {
    private static final String TAG = VideoCompress.class.getSimpleName();

    public static DataSource getChannel(String videoPath) throws IOException {
        DataSource channel = (DataSource) new FileDataSourceImpl(videoPath);
        return channel;
    }

    public static IsoFile getIsoFile(DataSource channel) {
        IsoFile isoFile = null;
        try {
            isoFile = new IsoFile(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isoFile;

    }

    public static List<TrackBox> getTrackBoxes(IsoFile isoFile) {
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        return trackBoxes;
    }

    public static TimeToSampleBox.Entry getFirstEntry (TrackBox trackBox) {
        TimeToSampleBox.Entry firstEntry = trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox().getTimeToSampleBox().getEntries().get(0);
        return firstEntry;
    }

    private static final String VIDEO_DIRECTORY_NAME = "odiVideo";
    public static File getOutputMediaFile() {

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + VIDEO_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        return mediaFile;
    }

    public static Movie getNewMovie() {
        Movie movie = new Movie();
        return movie;
    }

    public static Mp4TrackImpl getMp4TrackImpl (TrackBox trackBox, DataSource channel) {
        Mp4TrackImpl newObject = new Mp4TrackImpl(channel.toString() + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]", trackBox);
        return newObject;
    }

    public static Container getContainer(Movie movie) {
        Container newContainer = new DefaultMp4Builder().build(movie);
        return newContainer;
    }


    public static String parseVideo(String mFilePath) throws IOException {
        DataSource channel = new FileDataSourceImpl(mFilePath);
        IsoFile isoFile = new IsoFile(channel);
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        boolean isError = false;
        for (TrackBox trackBox : trackBoxes) {
            TimeToSampleBox.Entry firstEntry = trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox().getTimeToSampleBox().getEntries().get(0);
            // Detect if first sample is a problem and fix it in isoFile
            // This is a hack. The audio deltas are 1024 for my files, and video deltas about 3000
            // 10000 seems sufficient since for 30 fps the normal delta is about 3000
            if (firstEntry.getDelta() > 10000) {
                isError = true;
                firstEntry.setDelta(3000);
            }
        }
        File file = getOutputMediaFile();
        String filePath = file.getAbsolutePath();
        if (isError) {
            Movie movie = new Movie();
            for (TrackBox trackBox : trackBoxes) {
                movie.addTrack(new Mp4TrackImpl(channel.toString() + "[" + trackBox.getTrackHeaderBox().getTrackId() + "]", trackBox));
            }
            movie.setMatrix(isoFile.getMovieBox().getMovieHeaderBox().getMatrix());
            Container out = new DefaultMp4Builder().build(movie);

            //delete file first!
            FileChannel fc = new RandomAccessFile(filePath, "rw").getChannel();
            out.writeContainer(fc);
            fc.close();
            Log.d(TAG, "Finished correcting raw video");
            return filePath;
        }
        return mFilePath;
    }


    public static VideoCompressTask compressVideoHigh(String srcPath, String destPath, CompressListener listener) {
        VideoCompressTask task = new VideoCompressTask(listener, VideoController.COMPRESS_QUALITY_HIGH);
        task.execute(srcPath, destPath);
        return task;
    }

    public static VideoCompressTask compressVideoMedium(String srcPath, String destPath, CompressListener listener) {
        VideoCompressTask task = new VideoCompressTask(listener, VideoController.COMPRESS_QUALITY_MEDIUM);
        task.execute(srcPath, destPath);
        return task;
    }

    public static VideoCompressTask compressVideoLow(String srcPath, String destPath, CompressListener listener) {
        VideoCompressTask task =  new VideoCompressTask(listener, VideoController.COMPRESS_QUALITY_LOW);
        task.execute(srcPath, destPath);
        return task;
    }

    private static class VideoCompressTask extends AsyncTask<String, Float, Boolean> {
        private CompressListener mListener;
        private int mQuality;

        public VideoCompressTask(CompressListener listener, int quality) {
            mListener = listener;
            mQuality = quality;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mListener != null) {
                mListener.onStart();
            }
        }

        @Override
        protected Boolean doInBackground(String... paths) {
            return VideoController.getInstance().convertVideo(paths[0], paths[1], mQuality, new VideoController.CompressProgressListener() {
                @Override
                public void onProgress(float percent) {
                    publishProgress(percent);
                }
            });
        }

        @Override
        protected void onProgressUpdate(Float... percent) {
            super.onProgressUpdate(percent);
            if (mListener != null) {
                mListener.onProgress(percent[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mListener != null) {
                if (result) {
                    mListener.onSuccess();
                } else {
                    mListener.onFail();
                }
            }
        }
    }

    public interface CompressListener {
        void onStart();
        void onSuccess();
        void onFail();
        void onProgress(float percent);
    }
}
