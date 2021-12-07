package com.audiobook.interfaces;

import com.ximalaya.ting.android.opensdk.model.album.Album;

public interface ISubDao {

    /**
     * 添加专辑订阅
     */
    void addAlbum(Album album);

    /**
     * 删除订阅内容
     */
    void delAlbum(Album album);

    /**
     * 获取订阅内容
     */
    void listAlbum();


    void setCallBack(ISubDaoCallBack callback);

}
