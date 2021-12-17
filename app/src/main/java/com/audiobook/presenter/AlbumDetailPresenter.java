package com.audiobook.presenter;

import com.audiobook.data.AudioBookApi;
import com.audiobook.interfaces.IAlbumDetailPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.IAlbumDetailViewCallback;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.presenter
 * @Date 2021/10/26 12:21
 */
public class AlbumDetailPresenter implements IAlbumDetailPresenter {

    private static final String TAG = "AlbumDetailPresenter";
    private List<IAlbumDetailViewCallback> mCallbacks = new ArrayList<>();
    private List<Track> mTracks = new ArrayList<>();

    private Album mTargetAlum = null;
    //当前的专辑id
    private int mCurrentAlbumId = -1;
    //当前页
    private int mCurrentPageIndex = 0;

    private AlbumDetailPresenter() { }

    private volatile static AlbumDetailPresenter sAlbumDetailPresenter ;

    public static AlbumDetailPresenter getInstance() {
        if (sAlbumDetailPresenter == null) {
            synchronized (AlbumDetailPresenter.class) {
                if (sAlbumDetailPresenter == null) {
                    sAlbumDetailPresenter = new AlbumDetailPresenter();
                }
            }
        }
        return sAlbumDetailPresenter;
    }

    @Override
    public void getAlbumDetail(int albumId, int page) {
        mTracks.clear();
        mCurrentAlbumId = albumId;
        mCurrentPageIndex = page;
        //根据页码和专辑id获取列表
        doLoaded(false);
    }

    private void doLoaded(final boolean isLoadMore) {
        AudioBookApi ximalayaApi = AudioBookApi.getInstance();
        ximalayaApi.getAlbumDetail(new IDataCallBack<TrackList>() {
            @Override
            public void onSuccess(TrackList trackList) {
                if (trackList != null) {
                    List<Track> tracks = trackList.getTracks();
                    LogUtil.d(TAG, "tracks size -- >" + tracks.size());
                    if (isLoadMore) {
                        //上拉加载，结果放到后面去
                        mTracks.addAll(tracks);
                        int size = tracks.size();
                        handlerLoaderMoreResult(size);
                    } else {
                        //这个是下拉加载，结果放到前面去
                        mTracks.addAll(0, tracks);
                    }
                    handlerAlbumDetailResult(mTracks);
                }
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                if (isLoadMore) {
                    mCurrentPageIndex--;
                }
                LogUtil.d(TAG, "errorCode --> " + errorCode);
                LogUtil.d(TAG, "errorMsg --> " + errorMsg);
                handlerError(errorCode, errorMsg);
            }
        }, mCurrentAlbumId,mCurrentPageIndex);
    }

    /**
     * 如果是发生错误，那么就通知UI
     *
     * @param errorCode
     * @param errorMsg
     */
    private void handlerError(int errorCode, String errorMsg) {
        for (IAlbumDetailViewCallback callback : mCallbacks) {
            callback.onNetworkError(errorCode, errorMsg);
        }
    }

    /**
     * 处理加载更多的结果
     *
     * @param size
     */
    private void handlerLoaderMoreResult(int size) {
        for (IAlbumDetailViewCallback callback : mCallbacks) {
            callback.onLoaderMoreFinished(size);
        }
    }

    private void handlerAlbumDetailResult(List<Track> tracks) {
        for (IAlbumDetailViewCallback mCallback : mCallbacks) {
            mCallback.onDetailListLoaded(tracks);
        }
    }
    @Override
    public void pull2RefreshMore() {

    }

    @Override
    public void loadMore() {
        //去加载更多内容
        mCurrentPageIndex++;
        //传入true，表示结果会追加到列表的后方。
        doLoaded(true);
    }

    @Override
    public void registerViewCallback(IAlbumDetailViewCallback detailViewCallback) {
        if (!mCallbacks.contains(detailViewCallback)) {
            mCallbacks.add(detailViewCallback);
            if (mTargetAlum != null) {
                detailViewCallback.onAlbumLoaded(mTargetAlum);
            }
        }
    }

    @Override
    public void unRegisterViewCallback(IAlbumDetailViewCallback detailViewCallback) {
        mCallbacks.remove(detailViewCallback);
    }
    public void setTargetAlbum(Album targetAlbum) {
        this.mTargetAlum = targetAlbum;
    }
}
