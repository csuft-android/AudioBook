package com.audiobook.interfaces;

import com.audiobook.base.IBasePresenter;
import com.audiobook.view.IAlbumDetailViewCallback;

public interface IAlbumDetailPresenter extends IBasePresenter<IAlbumDetailViewCallback> {


    /**
     * 获取专辑详情
     *
     * @param albumId
     * @param page
     */
    void getAlbumDetail(int albumId, int page);

    /**
     * 下拉刷新更多内容
     */
    void pull2RefreshMore();

    /**
     * 上拉加载更多
     */
    void loadMore();
}

