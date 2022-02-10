package com.audiobook.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.activity.PlayActivity;
import com.audiobook.R;
import com.audiobook.adapter.TrackListAdapter;
import com.audiobook.base.BaseFragment;
import com.audiobook.customView.ConfirmCheckBoxDialog;
import com.audiobook.presenter.HistoryPresenter;
import com.audiobook.presenter.PlayerPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.utils.UILoader;
import com.audiobook.view.IHistoryCallBack;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2021/10/25 21:23
 */
public class HistoryFragment extends BaseFragment implements IHistoryCallBack, TrackListAdapter.ItemClickListener, TrackListAdapter.ItemLongClickListener, ConfirmCheckBoxDialog.OnDialogActionClickListener {

    private static final String TAG = "HistoryFragment";
    private UILoader mUiLoader;
    private HistoryPresenter mHistoryPresenter;
    private TrackListAdapter mTrackListAdapter;
    private Track mCurrentClickHistoryItem;

    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        FrameLayout rootView = (FrameLayout) layoutInflater.inflate(R.layout.fragment_history, container, false);
        if (mUiLoader == null) {
            mUiLoader = new UILoader(getContext()) {
               // mUiLoader = new UILoader(BaseApplication.getAppContext()) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView(container);
                }

                @Override
                protected View getEmptyView() {
                    View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_empty_view, this, false);
                    TextView tips = emptyView.findViewById(R.id.empty_view_tips_tv);
                    tips.setText("没有历史记录哦，亲");
                    return emptyView;
                }
            };
            if (mUiLoader.getParent() instanceof ViewGroup) {
                ((ViewGroup) mUiLoader.getParent()).removeView(mUiLoader);
            }
            rootView.addView(mUiLoader);
        }
        //HistoryPresenter
        mHistoryPresenter = HistoryPresenter.getHistoryPresenter();
        mHistoryPresenter.registerViewCallback(this);
        mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
        mHistoryPresenter.listHistory();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHistoryPresenter != null) {
            mHistoryPresenter.unRegisterViewCallback(this);
        }
        if (mTrackListAdapter != null) {
            mTrackListAdapter.setItemClickListener(null);
            mTrackListAdapter.setItemLongClickListener(null);
        }
    }

  /*  @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHistoryPresenter != null) {
            mHistoryPresenter.unRegisterViewCallback(this);
        }
        if (mTrackListAdapter != null) {
            mTrackListAdapter.setItemClickListener(null);
            mTrackListAdapter.setItemLongClickListener(null);
        }
    }*/

    private View createSuccessView(ViewGroup container) {
        View successView = LayoutInflater.from(container.getContext()).inflate(R.layout.item_history, container, false);
        TwinklingRefreshLayout refreshLayout = successView.findViewById(R.id.over_scroll_view);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setEnableOverScroll(true);
        //RecyclerView
        RecyclerView historyList = successView.findViewById(R.id.history_list);
        historyList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }
                int itemPosition = parent.getChildAdapterPosition(view);
                final int itemCount = layoutManager.getItemCount();
                final int lastItemIndex = itemCount - 1;
                outRect.top = UIUtil.dip2px(view.getContext(), 6);
                outRect.bottom = UIUtil.dip2px(view.getContext(), itemPosition != lastItemIndex ? 0 : 6);
                outRect.left = UIUtil.dip2px(view.getContext(), 6);
                outRect.right = UIUtil.dip2px(view.getContext(), 6);
            }
        });
        historyList.setLayoutManager(new LinearLayoutManager(container.getContext()));
        //设置适配器
        mTrackListAdapter = new TrackListAdapter();
        mTrackListAdapter.setItemClickListener(this);
        mTrackListAdapter.setItemLongClickListener(this);
        historyList.setAdapter(mTrackListAdapter);
        return successView;
    }

    @Override
    public void onHistoryLoaded(List<Track> tracks) {
        LogUtil.d(TAG, "tracks size==>" + tracks.size());
        if (tracks.size() == 0) {
            mUiLoader.upDateStatus(UILoader.UIStatus.EMPTY);
        } else {
            mTrackListAdapter.setData(tracks);
            mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
        }
    }

    @Override
    public void onItemClick(List<Track> detailData, int position) {
        //设置播放器的数据
        PlayerPresenter playerPresenter = PlayerPresenter.getPlayerPresenter();
        playerPresenter.setPlayList(detailData, position);
        //跳转到播放器界面
        startActivity(new Intent(getActivity(), PlayActivity.class));
    }

    @Override
    public void onItemLongClick(Track track) {
        this.mCurrentClickHistoryItem = track;
        //去删除历史
        //Toast.makeText(getActivity(),"历史记录长按..." + track.getTrackTitle(),Toast.LENGTH_SHORT).show();
        ConfirmCheckBoxDialog dialog = new ConfirmCheckBoxDialog(getActivity());
        dialog.setOnDialogActionClickListener(this);
        dialog.show();
    }

    @Override
    public void onCancelClick() {

    }

    @Override
    public void onConfirmClick(boolean isCheck) {
        //删除历史
        if (mHistoryPresenter != null && mCurrentClickHistoryItem != null) {
            if (isCheck) {
                mHistoryPresenter.cleanHistory();
            } else {
                mHistoryPresenter.delHistory(mCurrentClickHistoryItem);
            }
        }
    }
}
