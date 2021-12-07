package com.audiobook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.audiobook.Activity.PlayActivity;
import com.audiobook.Activity.SearchActivity;
import com.audiobook.adapter.IndicatorAdapter;
import com.audiobook.adapter.MainContentAdapter;
import com.audiobook.customView.RoundRectImageView;
import com.audiobook.presenter.PlayerPresenter;
import com.audiobook.presenter.RecommendPresenter;
import com.audiobook.utils.FragmentCreator;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.IPlayerCallback;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;

import java.util.List;

import leakcanary.AppWatcher;

/**
 * @author 优雅永不过时
 * @Package com.audiobook
 * @Date 2021/10/25 16:56
 */
public class MainActivity extends FragmentActivity implements IPlayerCallback {

    private static final String TAG = "MainActivity";
    private MagicIndicator mMagicIndicator;
    private ViewPager mContentPager;
    private IndicatorAdapter mIndicatorAdapter;
    private RoundRectImageView mRoundRectImageView;
    private TextView mHeaderTitle;
    private TextView mSubTitle;
    private ImageView mPlayControl;
    private PlayerPresenter mPlayerPresenter;
    private LinearLayout mPlayControlItem;
    private ImageView mSearchBtn;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        initPresenter();
    }

    private void initPresenter() {
        mPlayerPresenter = PlayerPresenter.getPlayerPresenter();
        mPlayerPresenter.registerViewCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
        }
        if (mContentPager != null) {
            mContentPager.setAdapter(null);
        }
    }

    private void initEvent() {
        mIndicatorAdapter.setOnIndicatorTapClickListener(new IndicatorAdapter.OnIndicatorTabClickListener() {
            @Override
            public void onTabClick(int index) {
                if (mContentPager != null) {
                    mContentPager.setCurrentItem(index, false);
                }
            }
        });
        mPlayControl.setOnClickListener(v -> {
            if (mPlayerPresenter != null) {
                boolean hasPlayList = mPlayerPresenter.hasPlayList();
                if (!hasPlayList) {
                    //没有设置过播放列表，我们就播放默认的第一个推荐专辑
                    //第一个推荐专辑，每天都会变的
                    playFirstRecommend();
                } else {
                    if (mPlayerPresenter.isPlaying()) {
                        mPlayerPresenter.pause();
                    } else {
                        mPlayerPresenter.play();
                    }
                }
            }
        });
        mPlayControlItem.setOnClickListener(v -> {
            boolean hasPlayList = mPlayerPresenter.hasPlayList();
            if (!hasPlayList) {
                playFirstRecommend();
            }
            //跳转到播放器界面
            startActivity(new Intent(this, PlayActivity.class));
        });
        mSearchBtn.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
    }

    /**
     * 播放第一个推荐的内容
     */
    private void playFirstRecommend() {
        List<Album> currentRecommend = RecommendPresenter.getInstance().getCurrentRecommend();
        if (currentRecommend != null) {
            Album album = currentRecommend.get(0);
            long albumId = album.getId();
            mPlayerPresenter.playByAlbumId(albumId);
        }
    }


    @SuppressLint("WrongViewCast")
    private void initView() {
        mMagicIndicator = this.findViewById(R.id.main_indicator);
        mMagicIndicator.setBackgroundColor(getResources().getColor(R.color.main_color));
        //创建indicator的适配器
        mIndicatorAdapter = new IndicatorAdapter(this);
        CommonNavigator commonNavigator = new CommonNavigator(this);
        commonNavigator.setAdjustMode(true);//设置调整模式(自我调节，平分位置)
        commonNavigator.setAdapter(mIndicatorAdapter);
        //viewPager
        mContentPager = findViewById(R.id.content_pager);
        mContentPager.setOffscreenPageLimit(FragmentCreator.PAGE_COUNT);
        //创建内容适配器
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        MainContentAdapter mainContentAdapter = new MainContentAdapter(supportFragmentManager);
        mContentPager.setAdapter(mainContentAdapter);

        //把ViewPager和indicator绑定到一起
        mMagicIndicator.setNavigator(commonNavigator);
        ViewPagerHelper.bind(mMagicIndicator, mContentPager);
        //播放控制相关的
        mRoundRectImageView = findViewById(R.id.main_track_cover);
        mHeaderTitle = findViewById(R.id.main_head_title);
        mSubTitle = findViewById(R.id.main_sub_title);
        mPlayControl = findViewById(R.id.main_play_control);
        mPlayControlItem = findViewById(R.id.main_play_control_item);
        //ViewKt.setRoundRectBg(mPlayControlItem, Color.parseColor("#CCCCCC"), ScreenUtils.dp2px(18));
        //搜索
        mSearchBtn = findViewById(R.id.main_search_btn);
    }

    @Override
    public void onPlayStart() {
        updatePlayControl(true);
    }

    private void updatePlayControl(boolean isPlaying) {
        if (mPlayControl != null) {
            mPlayControl.setImageResource(isPlaying ? R.drawable.selector_player_pause : R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayPause() {
        updatePlayControl(false);
    }

    @Override
    public void onPlayStop() {
        updatePlayControl(false);
    }

    @Override
    public void onPlayError(Track track) {

    }

    @Override
    public void onPrePlay(Track track) {

    }

    @Override
    public void onListLoad(List<Track> list) {

    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode playMode) {

    }

    @Override
    public void onProgressChange(int currentProgress, int total) {

    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        if (track != null) {
            String trackTitle = track.getTrackTitle();
            String nickname = track.getAnnouncer().getNickname();
            String coverUrlMiddle = track.getCoverUrlMiddle();
            LogUtil.d(TAG, "trackTitle --- > " + trackTitle);
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(trackTitle);
            }
            LogUtil.d(TAG, "nickname --- > " + nickname);
            if (mSubTitle != null) {
                mSubTitle.setText(nickname);
            }
            LogUtil.d(TAG, "coverUrlMiddle --- > " + coverUrlMiddle);
            Glide.with(this).load(coverUrlMiddle).into(mRoundRectImageView);
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {

    }
}
