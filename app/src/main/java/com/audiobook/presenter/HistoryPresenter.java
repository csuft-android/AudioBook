package com.audiobook.presenter;

import com.audiobook.base.BaseApplication;
import com.audiobook.data.HistoryDao;
import com.audiobook.interfaces.IHistoryDaoCallBack;
import com.audiobook.interfaces.IHistoryPresenter;
import com.audiobook.utils.Constants;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.IHistoryCallBack;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.presenter
 * @Date 2021/10/31 0:53
 */

/**
 * 历史记录数量最多100条，超过则将最前面的删除
 */
public class HistoryPresenter implements IHistoryPresenter, IHistoryDaoCallBack {
    private static final String TAG = "HistoryFragment";

    private List<Track> mHistories = new ArrayList<>();
    private final Object mLock = new Object();
    private final HistoryDao mHistoryDao ;
    private List<IHistoryCallBack> mCallBacks = new ArrayList<>();
    private List<Track> mCurrentHistories = null;
    private Track mCurrentAddTrack = null;

    private HistoryPresenter() {
        mHistoryDao = new HistoryDao();
        mHistoryDao.CallBack(this);
        listHistory();
    }

    private volatile static HistoryPresenter mHistoryPresenter;

    public static HistoryPresenter getHistoryPresenter() {
        if (mHistoryPresenter == null) {
            synchronized (HistoryPresenter.class) {
                if (mHistoryPresenter == null) {
                    mHistoryPresenter = new HistoryPresenter();
                }
            }
        }
        return mHistoryPresenter;
    }

    @Override
    public void listHistory() {
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                //只调用，不处理结果
                if (mHistoryDao != null) {
                    mHistoryDao.listHistories();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private boolean isDoDelOutOfSize = false;

    @Override
    public void addHistory(Track track) {
        //需要判断数量>=100
        if (mCurrentHistories != null && mCurrentHistories.size() >= Constants.MAX_HISTORY_COUNT) {
            //先删除最前面的一条记录，再添加
            isDoDelOutOfSize = true;
            this.mCurrentAddTrack = track;
            delHistory(mCurrentHistories.get(mCurrentHistories.size() - 1));
        } else {
            doAddHistory(track);
        }

    }

    private void doAddHistory(final Track track) {
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.addHistory(track);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void delHistory(Track track) {
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.delHistory(track);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void cleanHistory() {
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                if (mHistoryDao != null) {
                    mHistoryDao.clearHistory();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void registerViewCallback(IHistoryCallBack iHistoryCallBack) {
        //UI注册过来的
        if (!mCallBacks.contains(iHistoryCallBack)) {
            mCallBacks.add(iHistoryCallBack);
        }
    }

    @Override
    public void unRegisterViewCallback(IHistoryCallBack iHistoryCallBack) {
        mCallBacks.remove(iHistoryCallBack);
    }

    @Override
    public void onHistoryAdd(boolean isSuccess) {
        listHistory();
    }

    @Override
    public void onHistoryDel(boolean isSuccess) {
        if (isDoDelOutOfSize && mCurrentAddTrack != null) {
            isDoDelOutOfSize = false;
            //添加当前的数据到数据库
            addHistory(mCurrentAddTrack);
        } else {
            listHistory();
        }
    }

    /**
     * 从HistoryDao回来的数据
     *
     * @param track
     */
    @Override
    public void onHistoryLoaded(List<Track> track) {
        LogUtil.d(TAG, "历史记录 的大小" + track.size());
        this.mCurrentHistories = track;
        //通知UI更新数据
        BaseApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                for (IHistoryCallBack callBack : mCallBacks) {
                    callBack.onHistoryLoaded(track);
                }
            }
        });
    }

    @Override
    public void onHistoryClean(boolean isSuccess) {
        listHistory();
    }
}
