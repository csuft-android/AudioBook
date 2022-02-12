package com.audiobook.fragment;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.audiobook.R;
import com.audiobook.adapter.PlayerTrackPagerAdapter;
import com.audiobook.base.BaseFragment;
import com.audiobook.customView.MyPopWindow;
import com.audiobook.presenter.PlayerPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.utils.UILoader;
import com.audiobook.view.IPlayerCallback;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST_LOOP;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_RANDOM;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_SINGLE_LOOP;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2022/2/12 16:08
 */
public class PlayFragment extends BaseFragment implements IPlayerCallback, ViewPager.OnPageChangeListener {
    private static final String TAG = "PlayerActivity";
    private final SimpleDateFormat mMinFormat = new SimpleDateFormat("mm:ss");
    private final SimpleDateFormat mHourFormat = new SimpleDateFormat("HH:mm:ss");
    private ImageView mControlBtn;
    private PlayerPresenter mPlayerPresenter;
    private TextView mTotalDuration;
    private TextView mCurrentPosition;
    private SeekBar mDurationBar;
    private int mCurrentProgress = 0;
    private boolean mIsUserTouchProgressBar = false;
    private ImageView mPlayPreBtn;
    private ImageView mPlayNextBtn;
    private TextView mTrackTitleTv;
    private String mTrackTitleText;
    private ViewPager mTrackPageView;
    private PlayerTrackPagerAdapter mTrackPagerAdapter;
    private boolean mIsUserSlidePager = false;
    private ImageView mPlayerModeSwitchBtn;
    private XmPlayListControl.PlayMode mCurrentMode = PLAY_MODEL_LIST;
    private ImageView mPlayerListBtn;
    private MyPopWindow mMyPopWindow;
    private ValueAnimator mEnterBgAnimator;
    private ValueAnimator mOutBgAnimator;
    public final int BG_ANIMATION_DURATION = 800;
    //在静态代码块中去初始化
    private static final Map<XmPlayListControl.PlayMode, XmPlayListControl.PlayMode> sPlayModeRule = new HashMap<>();

    //处理播放模式的切换
    //1、默认的是：PLAY_MODEL_LIST
    //2、列表循环：PLAY_MODEL_LIST_LOOP
    //3、随机播放：PLAY_MODEL_RANDOM
    //4、单曲循环：PLAY_MODEL_SINGLE_LOOP
    static {
        sPlayModeRule.put(PLAY_MODEL_LIST, PLAY_MODEL_LIST_LOOP);
        sPlayModeRule.put(PLAY_MODEL_LIST_LOOP, PLAY_MODEL_RANDOM);
        sPlayModeRule.put(PLAY_MODEL_RANDOM, PLAY_MODEL_SINGLE_LOOP);
        sPlayModeRule.put(PLAY_MODEL_SINGLE_LOOP, PLAY_MODEL_LIST);
    }

    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        initBgAnimation();
        LinearLayout rootView = (LinearLayout) layoutInflater.inflate(R.layout.activity_play, container, false);
        initView(rootView);
        initPresenter();
        initEvent();
        return rootView;
    }

    /**
     * 初始化View控件
     */
    private void initView(LinearLayout rootView) {
        mControlBtn = rootView.findViewById(R.id.play_or_pause_btn);
        mTotalDuration = rootView.findViewById(R.id.track_duration);
        mCurrentPosition = rootView.findViewById(R.id.current_position);
        mDurationBar = rootView.findViewById(R.id.track_seek_bar);
        mPlayPreBtn = rootView.findViewById(R.id.play_pre);
        mPlayNextBtn = rootView.findViewById(R.id.player_next);
        mTrackTitleTv = rootView.findViewById(R.id.track_title);
        if (!TextUtils.isEmpty(mTrackTitleText)) {
            mTrackTitleTv.setText(mTrackTitleText);
        }
        //mImageCover = findViewById(R.id.album_cover_view);
        //中间图片的适配器
        mTrackPageView = rootView.findViewById(R.id.track_pager_view);
        //创建适配器
        mTrackPagerAdapter = new PlayerTrackPagerAdapter();
        //设置适配器
        mTrackPageView.setAdapter(mTrackPagerAdapter);
        //切换播放模式的按钮
        mPlayerModeSwitchBtn = rootView.findViewById(R.id.player_mode_switch_btn);
        //播放器列表
        mPlayerListBtn = rootView.findViewById(R.id.player_list);
        mMyPopWindow = new MyPopWindow();
    }


    private void initPresenter() {
        mPlayerPresenter = PlayerPresenter.getPlayerPresenter();
        mPlayerPresenter.registerViewCallback(this);
        //在界面初始化才去获取数据
        mPlayerPresenter.getPlayList();
    }

    /**
     * 根据当前的状态，更新播放模式图标
     * PLAY_MODEL_LIST
     * PLAY_MODEL_LIST_LOOP
     * PLAY_MODEL_RANDOM
     * PLAY_MODEL_SINGLE_LOOP
     */
    private void updatePlayModeBtnImg() {
        int resId = R.drawable.selector_player_mode_list_order;
        switch (mCurrentMode) {
            case PLAY_MODEL_LIST:
                resId = R.drawable.selector_player_mode_list_order;
                break;
            case PLAY_MODEL_RANDOM:
                resId = R.drawable.selector_player_mode_random;
                break;
            case PLAY_MODEL_LIST_LOOP:
                resId = R.drawable.selector_player_mode_list_order_looper;
                break;
            case PLAY_MODEL_SINGLE_LOOP:
                resId = R.drawable.selector_player_mode_single_loop;
                break;
        }
        mPlayerModeSwitchBtn.setImageResource(resId);
    }


    /**
     * 初始化事件
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果现在的状态是正在播放的，那么就暂停
                if (mPlayerPresenter.isPlaying()) {
                    mPlayerPresenter.pause();
                } else {
                    //如果现在的状态是非播放的，那么我们就让播放器播放节目
                    mPlayerPresenter.play();
                }
            }
        });
        mDurationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean isFromUser) {
                if (isFromUser) {
                    mCurrentProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = false;
                //手离开拖动进度条的时候更新进度
                mPlayerPresenter.seekTo(mCurrentProgress);
            }
        });
        mPlayPreBtn.setOnClickListener(v -> {
            //播放上一个节目
            if (mPlayerPresenter != null) {
                mPlayerPresenter.playPre();
            }
        });
        mPlayNextBtn.setOnClickListener(v -> {
            //播放下一个节目
            if (mPlayerPresenter != null) {
                mPlayerPresenter.playNext();
            }
        });
        mTrackPageView.addOnPageChangeListener(this);
        mTrackPageView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mIsUserSlidePager = true;
                    break;
            }
            return false;
        });
        mPlayerModeSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d(TAG, "切换模式......");
                //处理播放模式的切换
                switchPlayMode();
                updatePlayModeBtnImg();
            }
        });
        mPlayerListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //展示播放列表
                mMyPopWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);
                //修改背景的透明度，有一个渐变的过程
                mEnterBgAnimator.start();
            }
        });
        mMyPopWindow.setOnDismissListener(() -> {
            //pop窗体消失以后，恢复透明度
            mOutBgAnimator.start();
        });
        mMyPopWindow.setPlayListItemClickListener(position -> {
            //说明播放列表里的Item被点击了
            if (mPlayerPresenter != null) {
                mPlayerPresenter.playByIndex(position);
            }
        });
        mMyPopWindow.setPlayListActionListener(new MyPopWindow.PlayListActionClickListener() {
            @Override
            public void onPlayModeClick() {
                //切换播放模式
                switchPlayMode();
            }

            @Override
            public void onOrderClick() {
                //点击了切换顺序和逆序
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.reversePlayList();
                }
            }
        });
    }

    private void initBgAnimation() {
        mEnterBgAnimator = ValueAnimator.ofFloat(1.0f, 0.5f);
        mEnterBgAnimator.setDuration(BG_ANIMATION_DURATION);
        mEnterBgAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            //处理一下背景，有点透明度
            updateBgAlpha(value);
        });
        //退出的
        mOutBgAnimator = ValueAnimator.ofFloat(0.5f, 1.0f);
        mOutBgAnimator.setDuration(BG_ANIMATION_DURATION);
        mOutBgAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            updateBgAlpha(value);
        });
    }

    public void updateBgAlpha(float alpha) {
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.alpha = alpha;
        window.setAttributes(attributes);
    }

    private void switchPlayMode() {
        //根据当前的mode获取到下一个mode
        XmPlayListControl.PlayMode playMode = sPlayModeRule.get(mCurrentMode);
        //修改播放模式
        if (mPlayerPresenter != null) {
            mPlayerPresenter.switchPlayMode(playMode);
        }
    }

    @Override
    public void onPlayStart() {
        //开始播放，修改UI层暂停的按钮
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_pause);
        }
    }

    @Override
    public void onPlayPause() {
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayStop() {
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_pause);
        }
    }

    @Override
    public void onPlayError(Track track) {

    }

    @Override
    public void onPrePlay(Track track) {

    }

    @Override
    public void onListLoad(List<Track> list) {
        LogUtil.d(TAG, "list --- > " + list);
        //把数据设置到适配器里
        if (mTrackPagerAdapter != null) {
            mTrackPagerAdapter.setData(list);
        }
        //数据回来以后，也要给节目列表一份
        if (mMyPopWindow != null) {
            mMyPopWindow.setListData(list);
        }
    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode playMode) {
        //更新播放模式，并且修改UI。
        mCurrentMode = playMode;
        //更新pop里的播放模式
        mMyPopWindow.updatePlayMode(mCurrentMode);
        updatePlayModeBtnImg();
    }

    @Override
    public void onProgressChange(int currentDuration, int total) {
        mDurationBar.setMax(total);
        //更新播放进度，更新进度条
        String totalDuration;
        String currentPosition;
        if (total > 1000 * 60 * 60) {
            totalDuration = mHourFormat.format(total);
            currentPosition = mHourFormat.format(currentDuration);
        } else {
            totalDuration = mMinFormat.format(total);
            currentPosition = mMinFormat.format(currentDuration);
        }
        if (mTotalDuration != null) {
            mTotalDuration.setText(totalDuration);
        }
        //更新当前时间
        if (mCurrentPosition != null) {
            mCurrentPosition.setText(currentPosition);
        }
        //更新进度
        //计算当前的进度
        if (!mIsUserTouchProgressBar) {
            mDurationBar.setProgress(currentDuration);
        }
    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        LogUtil.d(TAG, "onTrackUpdate=============");

        if (track == null) {
            LogUtil.d(TAG, "onTrackUpdate --- > track null.");
            return;
        }
        mTrackTitleText = track.getTrackTitle();
        if (mTrackTitleTv != null) {
            //设置当前节目的标题
            mTrackTitleTv.setText(mTrackTitleText);
        }
        //当节目改变的时候，我们就获取到当前播放中播放位置
        //当前的节目改变以后，要修改页面的图片
        if (mTrackPageView != null) {
            mTrackPageView.setCurrentItem(playIndex, true);
        }
        //修改播放列表里的播放位置
        if (mMyPopWindow != null) {
            mMyPopWindow.setCurrentPlayPosition(playIndex);
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {
        mMyPopWindow.updateOrderIcon(!isReverse);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
            mPlayerPresenter = null;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        //当页面选中的时候，就去切换播放器的内容
        if (mPlayerPresenter != null && mIsUserSlidePager) {
            mPlayerPresenter.playByIndex(position);
        }
        mIsUserSlidePager = false;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
