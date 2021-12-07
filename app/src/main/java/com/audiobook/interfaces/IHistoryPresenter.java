package com.audiobook.interfaces;

import com.audiobook.base.IBasePresenter;
import com.audiobook.view.IHistoryCallBack;
import com.ximalaya.ting.android.opensdk.model.track.Track;

public interface IHistoryPresenter extends IBasePresenter<IHistoryCallBack> {
    /**
     * 获取历史内容
     */
    void listHistory();

    /**
     * 添加历史
     * @param track
     */
    void addHistory(Track track);

    /**
     * 删除历史
     */
    void delHistory(Track track);

    /**
     * 清除历史
     */
    void cleanHistory();
}
