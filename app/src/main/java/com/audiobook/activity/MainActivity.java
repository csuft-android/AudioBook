package com.audiobook.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.audiobook.R;
import com.audiobook.adapter.IndicatorAdapter;
import com.audiobook.adapter.MainContentAdapter;
import com.audiobook.customView.RoundRectImageView;
import com.audiobook.fragment.PlayFragment;
import com.audiobook.fragment.SearchFragment;
import com.audiobook.presenter.PlayerPresenter;
import com.audiobook.presenter.RecommendPresenter;
import com.audiobook.utils.FragmentCreator;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.IPlayerCallback;
import com.bumptech.glide.Glide;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;

import java.util.List;

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
    private TextView mSearchBtn;
    private FragmentManager mSupportFragmentManager;

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
            FragmentTransaction ft = mSupportFragmentManager.beginTransaction();
            ft.replace(R.id.main_contain, new PlayFragment());
            ft.addToBackStack(null);
            ft.commit();
        });
        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到搜索页面
           /*     Intent intent = new Intent();
                intent.setClass(MainActivity.this, SearchActivity.class);
                startActivity(intent);*/
                FragmentTransaction ft = mSupportFragmentManager.beginTransaction();
                ft.replace(R.id.main_contain, new SearchFragment());
                ft.addToBackStack(null);
                ft.commit();

            }
        });
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

    private FragmentCreator mFragmentCreator;

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
        mSupportFragmentManager = getSupportFragmentManager();
        MainContentAdapter mainContentAdapter = new MainContentAdapter(mSupportFragmentManager);
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
        mSearchBtn = findViewById(R.id.search_input);
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

    // 用来计算返回键的点击间隔时间
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            // 获取当前回退栈中的Fragment个数
            int backStackEntryCount = mSupportFragmentManager.getBackStackEntryCount();
            if(backStackEntryCount > 0){
                mSupportFragmentManager.popBackStack();
            }else{
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    //弹出提示
                    Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    finish();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
