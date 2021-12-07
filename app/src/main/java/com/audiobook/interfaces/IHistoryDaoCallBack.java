package com.audiobook.interfaces;

import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.util.List;

public interface IHistoryDaoCallBack {

    /**
     * 添加历史结果
     * @param isSuccess
     */
    void onHistoryAdd(boolean isSuccess);

    /**
     * 删除历史
     * @param isSuccess
     */
    void onHistoryDel(boolean isSuccess);

    /**
     * 历史数据的结果
     * @param track
     */
    void onHistoryLoaded(List<Track> track);

    /**
     *  历史内容清除
     * @param isSuccess
     */
    void onHistoryClean(boolean isSuccess);
}
