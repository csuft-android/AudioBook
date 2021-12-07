package com.audiobook.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.Activity.DetailActivity;
import com.audiobook.R;
import com.audiobook.adapter.AlbumListAdapter;
import com.audiobook.base.BaseFragment;
import com.audiobook.presenter.AlbumDetailPresenter;
import com.audiobook.presenter.RecommendPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.utils.UILoader;
import com.audiobook.view.IRecommendViewCallback;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.ximalaya.ting.android.opensdk.model.album.Album;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2021/10/25 21:23
 */
public class RecommendFragment extends BaseFragment implements IRecommendViewCallback, AlbumListAdapter.onAlbumItemClickListener {
    private static final String TAG = "RecommendFragment";
    private View mRootView;
    private RecyclerView mRecommendRv;
    private AlbumListAdapter mAlbumListAdapter;
    private RecommendPresenter mRecommendPresenter;
    private UILoader mUiLoader;

    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        mUiLoader = new UILoader(getContext()) {
            @Override
            protected View getSuccessView(ViewGroup container) {
                return createSuccessView(layoutInflater, container);
            }
        };
        mRecommendPresenter = RecommendPresenter.getInstance();
        //注册接口
        mRecommendPresenter.registerViewCallback(this);
        //请求推荐数据
        mRecommendPresenter.getRecommendList();
        //解绑
        if (mUiLoader.getParent() instanceof ViewGroup) {
            ((ViewGroup) mUiLoader.getParent()).removeView(mUiLoader);
        }
        //重新加载数据
        mUiLoader.setOnRetryClickListener(new UILoader.OnRetryClickListener() {
            @Override
            public void onRetryClick() {
                if (mRecommendPresenter != null) {
                    mRecommendPresenter.getRecommendList();
                }
            }
        });
        return mUiLoader;
    }

    private View createSuccessView(LayoutInflater layoutInflater, ViewGroup container) {
        mRootView = layoutInflater.inflate(R.layout.fragment_recommend, container, false);
        //recycleView的使用
        //1.找到控件
        mRecommendRv = mRootView.findViewById(R.id.recommend_list);
        TwinklingRefreshLayout twinklingRefreshLayout = mRootView.findViewById(R.id.over_scroll_view);
        twinklingRefreshLayout.setPureScrollModeOn();
        //2.设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(linearLayoutManager.VERTICAL);
        mRecommendRv.setLayoutManager(linearLayoutManager);
        mRecommendRv.addItemDecoration(new RecyclerView.ItemDecoration() {
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
        //3.设置适配器
        mAlbumListAdapter = new AlbumListAdapter();
        mRecommendRv.setAdapter(mAlbumListAdapter);
        mAlbumListAdapter.setAlbumItemClickListener(this);
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //取消注册
        if (mRecommendPresenter != null) {
            mRecommendPresenter.unRegisterViewCallback(this);
        }
        if (mAlbumListAdapter!=null){
            mAlbumListAdapter.setAlbumItemClickListener(null);
            mAlbumListAdapter.setOnAlbumItemLongClickListener(null);
        }
    }

  /*  @Override
    public void onDestroy() {
        super.onDestroy();
        //取消注册
        if (mRecommendPresenter != null) {
            mRecommendPresenter.unRegisterViewCallback(this);
        }
        if (mAlbumListAdapter!=null){
            mAlbumListAdapter.setAlbumItemClickListener(null);
            mAlbumListAdapter.setOnAlbumItemLongClickListener(null);
        }

    }*/

    @Override
    public void onRecommendListLoad(List<Album> result) {
        //当获取到推荐内容时，方法被调用，数据回来
        //更新UI
        LogUtil.d(TAG, "size==>" + result.size());
        mAlbumListAdapter.setData(result);
        mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
    }

    @Override
    public void onNetworkError() {
        mUiLoader.upDateStatus(UILoader.UIStatus.NETWORK_ERROR);
    }

    @Override
    public void onEmpty() {
        mUiLoader.upDateStatus(UILoader.UIStatus.EMPTY);
    }

    @Override
    public void onLoading() {
        mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
    }

    @Override
    public void onItemClick(int position, Album album) {
        //把数据传到AlbumDetailPresenter层去
        AlbumDetailPresenter.getInstance().setTargetAlbum(album);
        //Item被点击了，跳转到详情界面
        startActivity(new Intent(getContext(), DetailActivity.class));
    }
}
