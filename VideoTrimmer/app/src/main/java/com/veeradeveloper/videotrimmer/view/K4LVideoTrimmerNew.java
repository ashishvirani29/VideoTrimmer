
package com.veeradeveloper.videotrimmer.view;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.veeradeveloper.videotrimmer.R;
import com.veeradeveloper.videotrimmer.Thumb;
import com.veeradeveloper.videotrimmer.interfaces.OnK4LVideoListener;
import com.veeradeveloper.videotrimmer.interfaces.OnProgressVideoListener;
import com.veeradeveloper.videotrimmer.interfaces.OnRangeSeekBarListener;
import com.veeradeveloper.videotrimmer.interfaces.OnTrimVideoListener;
import com.veeradeveloper.videotrimmer.utils.BackgroundExecutor;
import com.veeradeveloper.videotrimmer.utils.TrimVideoUtils;
import com.veeradeveloper.videotrimmer.utils.UiThreadExecutor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.veeradeveloper.videotrimmer.utils.TrimVideoUtils.stringForTime;

public class K4LVideoTrimmerNew extends FrameLayout {


    private static final String TAG = K4LVideoTrimmerNew.class.getSimpleName();
    private static final int MIN_TIME_FRAME = 1000;
    private static final int SHOW_PROGRESS = 2;

    int tooltipFlag;
    private SeekBar mHolderTopView;
    private RangeSeekBarView mRangeSeekBarView;
    private int iconFlag = 0;
    private RelativeLayout mLinearVideo;
    private View mTimeInfoContainer;
    private VideoView mVideoView;
    private ImageView mPlayView;
    MediaPlayer mediaPlayer;
    private TextView mTextSize;
    private TextView txtStartTime;
    private TextView txtEndTime;
    private TextView mTextTime;
    private TimeLineView mTimeLineView;
    public static final String TIP_TEXT = "Please swipe handle to set trim position.";
    private ProgressBarView mVideoProgressIndicator;
    private ProgressBarView mVideoProgressIndicator1;
    private Uri mSrc;
    private String path;
    private String mFinalPath;
    private RelativeLayout bottomlayout;
    private int mMaxDuration;
    private int totalDuration;
    private List<OnProgressVideoListener> mListeners;

    private OnTrimVideoListener mOnTrimVideoListener;
    private OnK4LVideoListener mOnK4LVideoListener;
    int vidHeight;
    int vidWidth;
    private int mDuration = 0;
    private int mTimeVideo = 0;
    public static int mStartPosition = 0;
    public static int mEndPosition = 0;

    private long mOriginSizeFile;
    private boolean mResetSeekBar = true;
    private final MessageHandler mMessageHandler = new MessageHandler(this);

    private float default_speed = 1.0f;
    private RangeSeekbar ranges;
    int currentPosition;
    private String command = "null";
    int seekflag;
    private boolean running = true;
    int speedFlage;
    int repeatPlay;
    private Context mContext;

    public K4LVideoTrimmerNew(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public K4LVideoTrimmerNew(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true);

        mContext = context;

        bottomlayout = ((RelativeLayout) findViewById(R.id.bottomlayout));
        mHolderTopView = ((SeekBar) findViewById(R.id.handlerTop));
        mVideoProgressIndicator = ((ProgressBarView) findViewById(R.id.timeVideoView));
        mVideoProgressIndicator1 = ((ProgressBarView) findViewById(R.id.timeVideoView1));
        mRangeSeekBarView = ((RangeSeekBarView) findViewById(R.id.timeLineBar));
        mLinearVideo = ((RelativeLayout) findViewById(R.id.layout_surface_view));
        mVideoView = ((VideoView) findViewById(R.id.video_loader2));
        mVideoView.setVisibility(VISIBLE);
        mPlayView = ((ImageView) findViewById(R.id.icon_video_play_trim));
        // mTimeInfoContainer = findViewById(R.id.timeText);
        //mTextSize = ((TextView) findViewById(R.id.textSize));
        txtStartTime = ((TextView) findViewById(R.id.txtstartTime));

        txtEndTime = ((TextView) findViewById(R.id.txtendTime));

        mTextTime = ((TextView) findViewById(R.id.txtrunningTime));

        mTimeLineView = ((TimeLineView) findViewById(R.id.timeLineView));

        Button save = ((Button) findViewById(R.id.btn_save_trim));

        mediaPlayer = new MediaPlayer();

        setUpListeners();
        setUpMargins();

    }

    public void setUpListeners() {
        mListeners = new ArrayList<>();
        mListeners.add(new OnProgressVideoListener() {
            @Override
            public void updateProgress(int time, int max, float scale) {
                updateVideoProgress(time);
            }
        });
        mListeners.add(mVideoProgressIndicator);
        mListeners.add(mVideoProgressIndicator1);

        findViewById(R.id.btCancel)
                .setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    onCancelClicked();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );

        findViewById(R.id.btn_save_trim)
                .setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onSaveClicked();
                            }
                        }
                );


        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onError("Something went wrong reason : " + what);
                return false;
            }
        });

        final GestureDetector gestureDetector = new
                GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        onClickVideoPlayPause();
                        return true;
                    }
                }
        );

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onError("Something went wrong reason : " + what);
                return false;
            }
        });

        mVideoView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });


        mRangeSeekBarView.addOnRangeSeekBarListener(new OnRangeSeekBarListener() {
            @Override
            public void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value) {
                // Do nothing
            }

            @Override
            public void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onSeekThumbs(index, value);
            }

            @Override
            public void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value) {
                // Do nothing
            }

            @Override
            public void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onStopSeekThumbs();
            }
        });
        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator);
        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator1);

        mHolderTopView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onPlayerIndicatorSeekChanged(progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onVideoPrepared(mp, default_speed);
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onStopSeekThumbs();
            }
        });
    }

    private void setUpMargins() {
        int marge = mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
        int widthSeek = mHolderTopView.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHolderTopView.getLayoutParams();
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mHolderTopView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mTimeLineView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mVideoProgressIndicator.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mVideoProgressIndicator1.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator1.setLayoutParams(lp);

    }

    private void onSaveClicked() {
        int du = mEndPosition - mStartPosition;
        if (du > 3000) {
            if (mStartPosition > 0 || mEndPosition < totalDuration) {
                mVideoView.pause();
                Log.e(TAG, "Trim");
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(getContext(), mSrc);
                long METADATA_KEY_DURATION = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

                final File file = new File(mSrc.getPath());
                gettingVideoDimension(file.getPath().toString());
                if (mTimeVideo < MIN_TIME_FRAME) {

                    if ((METADATA_KEY_DURATION - mEndPosition) > (MIN_TIME_FRAME - mTimeVideo)) {
                        mEndPosition += (MIN_TIME_FRAME - mTimeVideo);
                    } else if (mStartPosition > (MIN_TIME_FRAME - mTimeVideo)) {
                        mStartPosition -= (MIN_TIME_FRAME - mTimeVideo);
                    }
                }

                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onTrimStarted();

                BackgroundExecutor.execute(
                        new BackgroundExecutor.Task("", 0L, "") {
                            @Override
                            public void execute() {
                                try {
                                    TrimVideoUtils.startTrim(file, getDestinationPath(), mStartPosition, mEndPosition, mOnTrimVideoListener, command, speedFlage);
                                } catch (final Throwable e) {
                                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                                }
                            }
                        }
                );
                Log.e(TAG, "Cuting Position:" + "Start:" + mStartPosition + " End:" + mEndPosition);
            } else {
                mOnTrimVideoListener.getResult(mSrc);
            }
        } else {
            Log.e(TAG, "Not Trim");
            Toast.makeText(getContext(), "Please select more Duration.", Toast.LENGTH_SHORT).show();
            mPlayView.setVisibility(View.GONE);
            onStopSeekThumbs();
        }
    }

    private void gettingVideoDimension(String path) {
        try {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(path);
            this.vidHeight = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            this.vidWidth = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            Log.e(TAG, "Video Width:" + vidWidth + " Video Height:" + vidHeight);

        } catch (IllegalArgumentException exx) {
            exx.printStackTrace();
        }
    }

    private void onCancelClicked() {
        mVideoView.stopPlayback();
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener.cancelAction();
        }
    }

    private String getDestinationPath() {
        if (mFinalPath == null) {
            File folder = new File(Environment.getExternalStorageDirectory() + "/VideoTrimmer/");
            if (!folder.exists()) {
                folder.mkdir();
            }
            //File folder = mContext.getExternalCacheDir();
            mFinalPath = folder.getPath() + File.separator;
            Log.d(TAG, "Using default path " + mFinalPath);
        }
        return mFinalPath;
    }

    private void onPlayerIndicatorSeekChanged(int progress, boolean fromUser) {

        int duration = (int) ((mDuration * progress) / 1000L);

        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition);
                duration = mStartPosition;
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition);
                duration = mEndPosition;
            }
            setTimeVideo(duration);
        }
    }


    private void onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);
        notifyProgressUpdate(false);
    }

    private void onPlayerIndicatorSeekStop(@NonNull SeekBar seekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);

        int duration = (int) ((mDuration * seekBar.getProgress()) / 1000L);
        mVideoView.seekTo(duration);
        setTimeVideo(duration);
        notifyProgressUpdate(false);
    }

    private void onVideoPrepared(@NonNull MediaPlayer mp, float default_speed) {
        mediaPlayer = mp;
        playVideo(mp, default_speed);

    }


    private void playVideo(MediaPlayer mp, float default_speed) {
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        mVideoView.setLayoutParams(lp);

        mDuration = mVideoView.getDuration();
        if (seekflag == 0) {
            seekflag = 1;
            setSeekBarPosition();
        }

        setTimeFrames();
        setTimeVideo(0);

        if (mOnK4LVideoListener != null) {
            mOnK4LVideoListener.onVideoPrepared();
        }

        mVideoView.start();
        mPlayView.setVisibility(GONE);

    }

    private void setSeekBarPosition() {

        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2;
            mEndPosition = mDuration / 2 + mMaxDuration / 2;

            mRangeSeekBarView.setThumbValue(0, (mStartPosition * 100) / mDuration);
            mRangeSeekBarView.setThumbValue(1, (mEndPosition * 100) / mDuration);

        } else {
            mStartPosition = 0;
            mEndPosition = mDuration;
        }

        setProgressBarPosition(mStartPosition);
        mVideoView.seekTo(mStartPosition);

        mTimeVideo = mDuration;
        mRangeSeekBarView.initMaxWidth();
    }

    private void setTimeFrames() {
        txtStartTime.setText(String.format("%s %s", stringForTime(mStartPosition), ""));
        txtEndTime.setText(String.format("%s %s", stringForTime(mEndPosition), ""));
    }

    private void setTimeVideo(int position) {
        String time = txtEndTime.getText().toString();
        String currentTime = String.format("%s %s", stringForTime(position), "");
        Log.e(TAG, "EndTimeVideo: " + time + "  " + currentTime);
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
        if (time.equals(currentTime)) {
            mPlayView.setVisibility(VISIBLE);
        }
    }


    private void onClickVideoPlayPause() {
        if (mVideoView.isPlaying()) {
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
            mPlayView.setVisibility(VISIBLE);
        } else {
            if (mResetSeekBar) {
                mResetSeekBar = false;
                mVideoView.seekTo(mStartPosition);
            }
            mPlayView.setVisibility(GONE);
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
            mVideoView.start();
        }
    }

    private void onSeekThumbs(int index, float value) {
        switch (index) {
            case Thumb.LEFT: {
                mStartPosition = (int) ((mDuration * value) / 100L);
                mVideoView.seekTo(mStartPosition);
                break;
            }
            case Thumb.RIGHT: {
                mEndPosition = (int) ((mDuration * value) / 100L);
                break;
            }
        }
        setProgressBarPosition(mStartPosition);

        setTimeFrames();
        mTimeVideo = mEndPosition - mStartPosition;
        mPlayView.setVisibility(GONE);
    }

    private void onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        if (mResetSeekBar) {
            mResetSeekBar = false;
            mVideoView.seekTo(mStartPosition);
        }

        mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
        mVideoView.start();
    }

    private void onVideoCompleted(MediaPlayer mp) {
        mp.seekTo(mStartPosition);
        mp.start();
    }

    private void notifyProgressUpdate(boolean all) {
        if (mDuration == 0) return;

        int position = mVideoView.getCurrentPosition();
        if (all) {
            for (OnProgressVideoListener item : mListeners) {
                item.updateProgress(position, mDuration, ((position * 100) / mDuration));
            }
        } else {
            mListeners.get(1).updateProgress(position, mDuration, ((position * 100) / mDuration));
        }
    }

    private void updateVideoProgress(int time) {
        if (mVideoView == null) {
            return;
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
            mResetSeekBar = true;
            return;
        }

        if (mHolderTopView != null) {
            setProgressBarPosition(time);
        }
        setTimeVideo(time);
    }

    private void setProgressBarPosition(int position) {
        if (mDuration > 0) {
            long pos = 1000L * position / mDuration;
            mHolderTopView.setProgress((int) pos);
        }
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    public void setVideoInformationVisibility(boolean visible) {
        //mTimeInfoContainer.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    @SuppressWarnings("unused")
    public void setOnTrimVideoListener(OnTrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    /**
     * Listener for some {@link VideoView} events
     *
     * @param onK4LVideoListener interface for events
     */
    @SuppressWarnings("unused")
    public void setOnK4LVideoListener(OnK4LVideoListener onK4LVideoListener) {
        mOnK4LVideoListener = onK4LVideoListener;
    }

    /**
     * Sets the path where the trimmed video will be saved
     * Ex: /storage/emulated/0/MyAppFolder/
     *
     * @param finalPath the full path
     */
    @SuppressWarnings("unused")
    public void setDestinationPath(final String finalPath) {
        mFinalPath = finalPath;
        Log.d(TAG, "Setting custom path " + mFinalPath);
    }

    /**
     * Cancel all current operations
     */
    public void destroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The video_audio_trimmer interface wont allow the user to set duration longer than maxDuration
     *
     * @param maxDuration the maximum duration of the trimmed video in seconds
     */
    @SuppressWarnings("unused")
    public void setMaxDuration(int maxDuration) {
        mMaxDuration = maxDuration * 1000;
    }

    /**
     * Sets the uri of the video to be video_audio_trimmer
     *
     * @param videoURI Uri of the video
     */
    @SuppressWarnings("unused")
    public void setVideoURI(final Uri videoURI) {
        mSrc = videoURI;
        path = String.valueOf(videoURI);

        if (mOriginSizeFile == 0) {
            File file = new File(mSrc.getPath());

            mOriginSizeFile = file.length();
            long fileSizeInKB = mOriginSizeFile / 1024;

            if (fileSizeInKB > 1000) {
                long fileSizeInMB = fileSizeInKB / 1024;
                //mTextSize.setText(String.format("%s %s", fileSizeInMB, getContext().getString(R.string.megabyte)));
            } else {
                //mTextSize.setText(String.format("%s %s", fileSizeInKB, getContext().getString(R.string.kilobyte)));
            }
        }

        mVideoView.setVideoURI(mSrc);
        mVideoView.requestFocus();

        mTimeLineView.setVideo(mSrc);
    }

    public void setTotalDuration(int duration) {
        totalDuration = duration;
    }

    private static class MessageHandler extends Handler {

        @NonNull
        private final WeakReference<K4LVideoTrimmerNew> mView;

        MessageHandler(K4LVideoTrimmerNew view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            K4LVideoTrimmerNew view = mView.get();
            if (view == null || view.mVideoView == null) {
                return;
            }

            view.notifyProgressUpdate(true);
            if (view.mVideoView.isPlaying()) {
                sendEmptyMessageDelayed(0, 10);
            }
        }
    }
}
