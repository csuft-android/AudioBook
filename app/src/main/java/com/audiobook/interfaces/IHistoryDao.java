package com.audiobook.interfaces;

import com.ximalaya.ting.android.opensdk.model.track.Track;

public interface IHistoryDao {

    /**
     * 回调
     * @param callback
     */
    void CallBack(IHistoryDaoCallBack callback);


    /**
     * 添加历史
     * @param track
     */
    void addHistory(Track track);

    /**
     * 删除历史
     */
    /**
     *
     * @param track
     */
    void delHistory(Track track);

    /**
     * 清除内容
     */

    void clearHistory();

    /**
     * 获取历史内容
     */
    void listHistories();
}
