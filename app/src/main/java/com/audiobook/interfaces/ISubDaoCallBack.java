package com.audiobook.interfaces;

import com.ximalaya.ting.android.opensdk.model.album.Album;

import java.util.List;

public interface ISubDaoCallBack {


    /**
     * 添加的结果
     */
    void onAddResult(Boolean isSuccess);

    /**
     * 删除
     */

    void onDelResult(Boolean isSuccess);

    /**
     * 结果
     */

    void onSubListLoaded(List<Album> album);

}