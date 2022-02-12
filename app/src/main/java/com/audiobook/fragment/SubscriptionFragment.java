package com.audiobook.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.R;
import com.audiobook.adapter.AlbumListAdapter;
import com.audiobook.base.BaseApplication;
import com.audiobook.base.BaseFragment;
import com.audiobook.customView.ConfirmDialog;
import com.audiobook.presenter.AlbumDetailPresenter;
import com.audiobook.presenter.SubscriptionPresenter;
import com.audiobook.utils.Constants;
import com.audiobook.utils.UILoader;
import com.audiobook.view.ISubscriptionCallback;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.ximalaya.ting.android.opensdk.model.album.Album;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2021/10/25 21:23
 */
public class SubscriptionFragment extends BaseFragment implements ISubscriptionCallback, AlbumListAdapter.onAlbumItemClickListener, AlbumListAdapter.onAlbumItemLongClickListener, ConfirmDialog.OnDialogActionClickListener {

    private static final String TAG = "SubscriptionFragment";
    private SubscriptionPresenter mSubscriptionPresenter;
    private RecyclerView mSubListView;
    private AlbumListAdapter mAlbumListAdapter;
    private Album mCurrentClickAlbum = null;
    private UILoader mUiLoader;
    private FragmentManager mFm;


    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        FrameLayout rootView = (FrameLayout) layoutInflater.inflate(R.layout.fragment_subscription, container, false);
        mFm = getFragmentManager();
        if (mUiLoader == null) {
            // mUiLoader = new UILoader(container.getContext()) {
            mUiLoader = new UILoader(getContext()) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView();
                }

                @Override
                protected View getEmptyView() {
                    //创建一个新的
                    View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_empty_view, this, false);
                    TextView tipsView = emptyView.findViewById(R.id.empty_view_tips_tv);
                    tipsView.setText(R.string.no_sub_content_tips_text);
                    return emptyView;
                }
            };
            if (mUiLoader.getParent() instanceof ViewGroup) {
                ((ViewGroup) mUiLoader.getParent()).removeView(mUiLoader);
            }
            rootView.addView(mUiLoader);
        }
        return rootView;
    }

    private View createSuccessView() {
        View itemView = LayoutInflater.from(BaseApplication.getAppContext()).inflate(R.layout.item_subscription, null);
        TwinklingRefreshLayout refreshLayout = itemView.findViewById(R.id.over_scroll_view);
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setEnableRefresh(false);
        mSubListView = itemView.findViewById(R.id.subscription_list);
        mSubListView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        mAlbumListAdapter = new AlbumListAdapter();
        mAlbumListAdapter.setAlbumItemClickListener(this);
        mAlbumListAdapter.setOnAlbumItemLongClickListener(this);
        mSubListView.setAdapter(mAlbumListAdapter);
        mSubListView.addItemDecoration(new RecyclerView.ItemDecoration() {
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

        mSubscriptionPresenter = SubscriptionPresenter.getInstance();
        mSubscriptionPresenter.registerViewCallback(this);
        mSubscriptionPresenter.loadSubscriptionList();
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
        }
        return itemView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSubscriptionPresenter != null) {
            mSubscriptionPresenter.unRegisterViewCallback(this);
        }
        if (mAlbumListAdapter != null) {
            mAlbumListAdapter.setAlbumItemClickListener(null);
            mAlbumListAdapter.setOnAlbumItemLongClickListener(null);
        }
    }

    @Override
    public void onItemClick(int position, Album album) {
        AlbumDetailPresenter.getInstance().setTargetAlbum(album);
        //Item被点击了，跳转到详情界面
        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.main_contain, new DetailFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onItemLongClick(Album album) {
        mCurrentClickAlbum = album;
        //订阅的item被长按了
        ConfirmDialog confirmDialog = new ConfirmDialog(getActivity());
        confirmDialog.setOnDialogActionClickListener(this);
        confirmDialog.show();
    }


    @Override
    public void onAddResult(boolean isSuccess) {

    }

    @Override
    public void onDeleteResult(boolean isSuccess) {
        Toast.makeText(getActivity(), isSuccess ? R.string.cancel_sub_success : R.string.cancel_sub_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubscriptionsLoaded(List<Album> albums) {
        if (albums.size() == 0) {
            if (mUiLoader != null) {
                mUiLoader.upDateStatus(UILoader.UIStatus.EMPTY);
            }
        } else {
            if (mUiLoader != null) {
                mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
            }
        }
        if (mAlbumListAdapter != null) {
            mAlbumListAdapter.setData(albums);
        }
    }

    @Override
    public void onSubFull() {
        Toast.makeText(getActivity(), "订阅数量不得超过" + Constants.MAX_SUB_COUNT, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancelSubClick() {
        //取消订阅内容
        if (mCurrentClickAlbum != null && mSubscriptionPresenter != null) {
            mSubscriptionPresenter.deleteSubscription(mCurrentClickAlbum);
            //onRefresh();
            //给出取消订阅的提示
            Toast.makeText(BaseApplication.getAppContext(), R.string.cancel_sub_success, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGiveUpClick() {

    }
}