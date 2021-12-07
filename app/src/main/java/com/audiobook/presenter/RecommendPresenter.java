package com.audiobook.presenter;

import android.content.Context;

import com.audiobook.data.AudioBookApi;
import com.audiobook.interfaces.IRecommendPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.IRecommendViewCallback;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.GussLikeAlbumList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.presenter
 * @Date 2021/10/26 0:06
 */
public class RecommendPresenter implements IRecommendPresenter {

    private static final String TAG = "RecommendPresenter";
    private List<IRecommendViewCallback> mCallBacks = new ArrayList<>();
    private List<Album> mCurrentRecommend = null;

    private RecommendPresenter() {
    }

    private static RecommendPresenter sInstance ;

    /**
     * 获得单例对象
     *
     * @return
     */
    public static RecommendPresenter getInstance() {
        if (sInstance == null) {
            synchronized (RecommendPresenter.class) {
                if (sInstance == null) {
                    sInstance = new RecommendPresenter();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void getRecommendList() {
        upDateLoading();
        AudioBookApi ximalayaApi = new AudioBookApi();
        ximalayaApi.getRecommendList(new IDataCallBack<GussLikeAlbumList>() {
            @Override
            public void onSuccess(GussLikeAlbumList gussLikeAlbumList) {
                if (gussLikeAlbumList != null) {
                    List<Album> albumList = gussLikeAlbumList.getAlbumList();
                    //通知UI更新
                    handleRecommendResult(albumList);
                }
            }

            @Override
            public void onError(int i, String s) {
                LogUtil.d(TAG, "error ==>" + i);
                LogUtil.d(TAG, "msg ==>" + s);
                handlerError();
            }
        });
    }

    private void handlerError() {
        if (mCallBacks != null) {
            for (IRecommendViewCallback callBack : mCallBacks) {
                callBack.onNetworkError();
            }
        }
    }

    /**
     * 获取当前的推荐专辑列表
     *
     * @return 推荐专辑列表，使用之前要判空
     */
    public List<Album> getCurrentRecommend() {
        return mCurrentRecommend;
    }

    /**
     * 通知UI更新
     *
     * @param albumList
     */
    private void handleRecommendResult(List<Album> albumList) {
        //通知UI更新
        if (albumList != null) {
            if (albumList.size() == 0) {
                for (IRecommendViewCallback callback : mCallBacks) {
                    callback.onEmpty();
                }
            } else {
                for (IRecommendViewCallback callback : mCallBacks) {
                    callback.onRecommendListLoad(albumList);
                }
                mCurrentRecommend = albumList;
            }
        }
    }

    private void upDateLoading() {
        for (IRecommendViewCallback callback : mCallBacks) {
            callback.onLoading();
        }
    }

    @Override
    public void pull2RefreshMore() {

    }

    @Override
    public void loadMore() {

    }

    @Override
    public void registerViewCallback(IRecommendViewCallback callback) {
        if (mCallBacks != null && !mCallBacks.contains(callback)) {
            mCallBacks.add(callback);
        }
    }

    @Override
    public void unRegisterViewCallback(IRecommendViewCallback callback) {
        if (mCallBacks != null) {
            mCallBacks.remove(callback);
        }
    }

}
