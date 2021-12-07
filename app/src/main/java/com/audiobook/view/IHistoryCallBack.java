package com.audiobook.view;

import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.util.List;

public interface IHistoryCallBack {
    /**
     * 历史内容加载结果
     * @param tracks
     */
    void onHistoryLoaded(List<Track> tracks);

}
