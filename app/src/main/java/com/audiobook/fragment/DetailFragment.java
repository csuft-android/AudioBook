package com.audiobook.fragment;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.R;
import com.audiobook.adapter.DetailListAdapter;
import com.audiobook.base.BaseApplication;
import com.audiobook.base.BaseFragment;
import com.audiobook.customView.MyMarqueeView;
import com.audiobook.customView.RoundRectImageView;
import com.audiobook.presenter.AlbumDetailPresenter;
import com.audiobook.presenter.PlayerPresenter;
import com.audiobook.presenter.SubscriptionPresenter;
import com.audiobook.utils.Constants;
import com.audiobook.utils.ImageBlur;
import com.audiobook.utils.LogUtil;
import com.audiobook.utils.UILoader;
import com.audiobook.view.IAlbumDetailViewCallback;
import com.audiobook.view.IPlayerCallback;
import com.audiobook.view.ISubscriptionCallback;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.header.bezierlayout.BezierLayout;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2022/2/12 15:06
 */
public class DetailFragment extends BaseFragment implements IAlbumDetailViewCallback, DetailListAdapter.OnItemClickListener, IPlayerCallback, ISubscriptionCallback {
    private static final String TAG = "DetailFragment";
    private ImageView mLargeCover;
    private RoundRectImageView mSmallCover;
    private TextView mAlbumTitle;
    private TextView mAlbumAuthor;
    private AlbumDetailPresenter mAlbumDetailPresenter;
    private int mCurrentPage = 1;
    private RecyclerView mAlbum_detail_list;
    private DetailListAdapter mDetailListAdapter;
    private FrameLayout mDetailListContainer;
    private UILoader mUiLoader;
    private long mCurrentId = -1;
    private ImageView mPlayControlBtn;
    private MyMarqueeView mPlayControlTips;
    private PlayerPresenter mPlayerPresenter;
    private List<Track> mCurrentTracks = null;
    public static final int DEFAULT_PLAY_INDEX = 0;
    private TwinklingRefreshLayout mRefreshLayout;
    private String mCurrentTrackTitle = null;
    private TextView mSubBtn;
    private SubscriptionPresenter mSubscriptionPresenter;
    private Album mCurrentAlbum;
    private boolean mIsLoaderMore = false;
    private FragmentManager mFm;

    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        ConstraintLayout rootView = (ConstraintLayout) layoutInflater.inflate(R.layout.activity_detail, container, false);
        mFm = getFragmentManager();
        initView(rootView);
        //创建UILoader
        //避免重复创建，null才创建
        if (mUiLoader == null) {
            mUiLoader = new UILoader(getContext()) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView(container);
                }
            };
        }
        //解绑
        if (mUiLoader.getParent() instanceof ViewGroup) {
            ((ViewGroup) mUiLoader.getParent()).removeView(mUiLoader);
        }
        mDetailListContainer.removeAllViews();
        mDetailListContainer.addView(mUiLoader);
        initPresenter();
        initEvent();
        return rootView;
    }

    private void initView(ConstraintLayout rootView) {
        mDetailListContainer = rootView.findViewById(R.id.detail_list_container);
        mLargeCover = rootView.findViewById(R.id.iv_large_cover);
        mSmallCover = rootView.findViewById(R.id.iv_small_cover);
        mAlbumTitle = rootView.findViewById(R.id.tv_album_title);
        mAlbumAuthor = rootView.findViewById(R.id.tv_album_author);
        //播放控制的图标
        mPlayControlBtn = rootView.findViewById(R.id.detail_play_control);
        mPlayControlTips = rootView.findViewById(R.id.play_control_tv);
        //订阅按钮
        mSubBtn = rootView.findViewById(R.id.detail_sub_btn);
    }

    private void initEvent() {
        //设置订阅按钮的状态
        updateSubState();
        updatePlayState(mPlayerPresenter.isPlaying());
        mPlayControlBtn.setOnClickListener(v -> {
            //判断播放器是否有播放列表
            if (mPlayerPresenter != null) {
                boolean hasPlayList = mPlayerPresenter.hasPlayList();
                if (hasPlayList) {
                    handlePlayControl();
                } else {
                    handleNoPlayList();
                }
            }
        });
        mSubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSubscriptionPresenter != null) {
                    boolean isSub = mSubscriptionPresenter.isSub(mCurrentAlbum);
                    //如果没有订阅，就去订阅；否则就取消
                    if (isSub) {
                        LogUtil.d(TAG, "订阅了----》 " + isSub);
                        mSubscriptionPresenter.deleteSubscription(mCurrentAlbum);
                    } else {
                        LogUtil.d(TAG, "没有订阅----》 " + isSub);
                        mSubscriptionPresenter.addSubscription(mCurrentAlbum);
                    }
                    updateSubState();
                }
            }
        });
    }

    private void initPresenter() {
        mAlbumDetailPresenter = AlbumDetailPresenter.getInstance();
        //注册UI
        mAlbumDetailPresenter.registerViewCallback(this);
        //播放器的Presenter。
        mPlayerPresenter = PlayerPresenter.getPlayerPresenter();
        mPlayerPresenter.registerViewCallback(this);
        //订阅相关的presenter
        mSubscriptionPresenter = SubscriptionPresenter.getInstance();
        mSubscriptionPresenter.registerViewCallback(this);
        mSubscriptionPresenter.loadSubscriptionList();
    }

    private View createSuccessView(ViewGroup container) {
        View detailListView = LayoutInflater.from(getContext()).inflate(R.layout.item_detail_list, container, false);
        mAlbum_detail_list = detailListView.findViewById(R.id.album_detail_list);
        mRefreshLayout = detailListView.findViewById(R.id.refresh_layout);
        //RecyclerView的使用步骤
        //第一步：设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mAlbum_detail_list.setLayoutManager(layoutManager);
        //第二步：设置适配器
        mDetailListAdapter = new DetailListAdapter();
        mAlbum_detail_list.setAdapter(mDetailListAdapter);
        //设置item的上下间距
        mAlbum_detail_list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }
                int itemPosition = parent.getChildAdapterPosition(view);
                final int itemCount = layoutManager.getItemCount();
                final int lastItemIndex = itemCount - 1;
                outRect.top = UIUtil.dip2px(view.getContext(), 2);
                outRect.bottom = UIUtil.dip2px(view.getContext(), itemPosition != lastItemIndex ? 0 : 2);
                outRect.left = UIUtil.dip2px(view.getContext(), 2);
                outRect.right = UIUtil.dip2px(view.getContext(), 2);
            }
        });
        mDetailListAdapter.setOnItemClickListener(this);
        BezierLayout headerView = new BezierLayout(getContext());
        mRefreshLayout.setHeaderView(headerView);
        mRefreshLayout.setMaxHeadHeight(140);
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(TwinklingRefreshLayout refreshLayout) {
                super.onRefresh(refreshLayout);
                BaseApplication.getHandler().postDelayed(() -> mRefreshLayout.finishRefreshing(), 2000);
            }

            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                super.onLoadMore(refreshLayout);
                //去加载更多的内容
                if (mAlbumDetailPresenter != null) {
                    mAlbumDetailPresenter.loadMore();
                    mIsLoaderMore = true;
                }
            }
        });
        return detailListView;
    }

    /**
     * 设置订阅按钮的状态
     */
    private void updateSubState() {
        if (mSubscriptionPresenter != null) {
            boolean isSub = mSubscriptionPresenter.isSub(mCurrentAlbum);
            mSubBtn.setText(isSub ? R.string.cancel_sub_tips_text : R.string.sub_tips_text);
        }
    }

    /**
     * 当播放器里面没有播放的内容，我们要进行处理
     */
    private void handleNoPlayList() {
        mPlayerPresenter.setPlayList(mCurrentTracks, DEFAULT_PLAY_INDEX);
    }

    /**
     * 当播放器里面有播放的内容的处理
     */
    private void handlePlayControl() {
        //控制播放器的状态
        if (mPlayerPresenter.isPlaying()) {
            //正在播放，就暂停
            mPlayerPresenter.pause();
        } else {
            mPlayerPresenter.play();
        }
    }

    /**
     * Recommend点击的专辑数据就回来了
     *
     * @param tracks
     */
    @Override
    public void onDetailListLoaded(List<Track> tracks) {
        if (mIsLoaderMore && mRefreshLayout != null) {
            mRefreshLayout.finishLoadmore();
            mIsLoaderMore = false;
        }

        mCurrentTracks = tracks;
        //判断数据结果，根据结果控制UI显示
        if (tracks == null || tracks.size() == 0) {
            if (mUiLoader != null) {
                mUiLoader.upDateStatus(UILoader.UIStatus.EMPTY);
            }
        }
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
        }
        //更新/设置UI数据
        mDetailListAdapter.setData(tracks);
    }

    @Override
    public void onNetworkError(int errorCode, String errorMsg) {
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.NETWORK_ERROR);
        }
    }

    /**
     * 去获取专辑的详情
     *
     * @param album
     */
    @Override
    public void onAlbumLoaded(@NotNull Album album) {
        this.mCurrentAlbum = album;
        long id = album.getId();
        mCurrentId = id;
        LogUtil.d(TAG, "album -- > " + id);
        //获取专辑的详情内容
        if (mAlbumDetailPresenter != null) {
            mAlbumDetailPresenter.getAlbumDetail((int) id, mCurrentPage);
        }
        //拿数据，显示Loading状态
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
        }
        if (mAlbumTitle != null) {
            mAlbumTitle.setText(album.getAlbumTitle());
        }
        if (mAlbumAuthor != null) {
            mAlbumAuthor.setText(album.getAnnouncer().getNickname());
        }

        //做毛玻璃效果
        if (mLargeCover != null) {
            Glide.with(this)
                    .load(album.getCoverUrlLarge())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            BaseApplication.getHandler().post(() -> ImageBlur.makeBlur(getContext(), resource, mLargeCover));
                            return true;
                        }
                    }).submit();
        }

        if (mSmallCover != null) {
            Glide.with(this).load(album.getCoverUrlLarge()).into(mSmallCover);
        }
    }


    @Override
    public void onLoaderMoreFinished(int size) {
        if (size > 0) {
            Toast.makeText(getContext(), "成功加载" + size + "条节目", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "没有更多节目了！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRefreshFinished(int size) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAlbumDetailPresenter != null) {
            mAlbumDetailPresenter.unRegisterViewCallback(this);
        }
        if (mSubscriptionPresenter != null) {
            mSubscriptionPresenter.unRegisterViewCallback(this);
        }
        if (mPlayerPresenter != null) {
            mPlayerPresenter.unRegisterViewCallback(this);
        }
    }


    @Override
    public void onItemClick(List<Track> detailData, int position) {
        LogUtil.d(TAG, "详细列表--》" + detailData.size());
        //设置播放器的数据
        PlayerPresenter playerPresenter = PlayerPresenter.getPlayerPresenter();
        playerPresenter.setPlayList(detailData, position);
        //跳转到播放器界面
        //startActivity(new Intent(getContext(), PlayActivity.class));
        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.main_contain, new PlayFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onPlayStart() {
        //修改图标为暂停的，文字修改为正在播放。
        updatePlayState(true);
    }

    @Override
    public void onPlayPause() {
        //设置成播放的图标，文字修改成已暂停
        updatePlayState(false);
    }

    /**
     * 根据播放状态修改图标和文字
     *
     * @param playing
     */
    private void updatePlayState(boolean playing) {
        if (mPlayControlBtn != null && mPlayControlTips != null) {
            mPlayControlBtn.setImageResource(playing ? R.drawable.selector_play_control_pause : R.drawable.selector_play_control_play);
            if (!playing) {
                mPlayControlTips.setText(R.string.click_play_tips_text);
            } else {
                if (mCurrentTrackTitle != null && !mCurrentTrackTitle.isEmpty()) {
                    mPlayControlTips.setText(mCurrentTrackTitle);
                }
            }
        }
    }

    @Override
    public void onPlayStop() {

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
            mCurrentTrackTitle = track.getTrackTitle();
            if (!TextUtils.isEmpty(mCurrentTrackTitle) && mPlayControlTips != null) {
                mPlayControlTips.setText(mCurrentTrackTitle);
            }
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {

    }

    @Override
    public void onAddResult(boolean isSuccess) {
        LogUtil.d(TAG, "onAddResult...." + isSuccess);
        if (isSuccess) {
            //如果成功了，那就修改UI成取消订阅
            mSubBtn.setText(R.string.cancel_sub_tips_text);
        }
        //给个toast
        String tipsText = isSuccess ? "订阅成功" : "订阅失败";
        Toast.makeText(getContext(), tipsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteResult(boolean isSuccess) {
        if (isSuccess) {
            //如果成功了，那就修改UI成取消订阅
            mSubBtn.setText(R.string.sub_tips_text);
        }
        //给个toast
        String tipsText = isSuccess ? "删除成功" : "删除失败";
        Toast.makeText(getContext(), tipsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubscriptionsLoaded(List<Album> albums) {
        for (Album album : albums) {
            LogUtil.d(TAG, "alnum ===> " + album.getAlbumTitle());
        }
    }

    @Override
    public void onSubFull() {
        //处理一个即可，toast
        Toast.makeText(getContext(), "订阅数量不得超过" + Constants.MAX_SUB_COUNT, Toast.LENGTH_SHORT).show();
    }
}

