package com.veeradeveloper.videotrimmer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import com.veeradeveloper.videotrimmer.interfaces.OnTrimVideoListener;
import com.veeradeveloper.videotrimmer.view.K4LVideoTrimmerNew;

import java.util.Formatter;


/**
 * Created by Aspiration-3 on 10/7/2016.
 */
public class VideoTrimActivity extends Activity implements OnTrimVideoListener {
    private static final String TAG = VideoTrimActivity.class.getSimpleName();
    private K4LVideoTrimmerNew mVideoTrimmer;
    private String videoInputPath;
    private ProgressDialog progressDialog;
    public static String mStartPosition;
    public static String mEndPosition;
    private String videoName;
    int duration;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.video_trim_activity_new);
        videoInputPath= Environment.getExternalStorageDirectory()+"/Video/intr.mp4";

        this.progressDialog = new ProgressDialog(VideoTrimActivity.this);
        this.progressDialog.setMessage("please wait...");
        this.progressDialog.setCancelable(false);


        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(videoInputPath);
            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Integer.parseInt(time);

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (videoInputPath == null) {
            Toast.makeText(getApplicationContext(), "invalid video file", Toast.LENGTH_SHORT).show();
        } else {
            mVideoTrimmer = ((K4LVideoTrimmerNew) findViewById(R.id.videotrimmer));
            if (mVideoTrimmer != null) {
                mVideoTrimmer.setMaxDuration(10000);
                mVideoTrimmer.setTotalDuration(duration);
                mVideoTrimmer.setOnTrimVideoListener(this);
                mVideoTrimmer.setVideoURI(Uri.parse(videoInputPath));
                mVideoTrimmer.setVideoInformationVisibility(true);
            }
        }

    }

    @Override
    public void onTrimStarted() {
        progressDialog.show();
    }

    @Override
    public void getResult(final Uri uri) {
        Log.e(TAG, "uri videoInputPath" + uri.toString());
        progressDialog.dismiss();
        Intent intent = new Intent(VideoTrimActivity.this, VideoViewActivity.class);
        intent.putExtra(ConstantFlag.VIDEO, uri.toString());
        startActivity(intent);
        finish();
    }

    @Override
    public void getPosition(int startms, int endms) {
        mStartPosition = stringForTime(startms);
        mEndPosition = stringForTime(endms);
    }

    @Override
    public void cancelAction() {
        if (!(progressDialog == null)) {
        } else {
            progressDialog.dismiss();
        }
        mVideoTrimmer.destroy();
        finish();
    }

    @Override
    public void onError(final String message) {
        progressDialog.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoTrimActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        Formatter mFormatter = new Formatter();

        return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
