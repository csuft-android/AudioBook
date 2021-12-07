package com.audiobook.presenter;

import com.audiobook.base.BaseApplication;
import com.audiobook.data.SubscriptionDao;
import com.audiobook.interfaces.ISubDaoCallBack;
import com.audiobook.interfaces.ISubscriptionPresenter;
import com.audiobook.utils.Constants;
import com.audiobook.utils.LogUtil;
import com.audiobook.view.ISubscriptionCallback;
import com.ximalaya.ting.android.opensdk.model.album.Album;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.presenter
 * @Date 2021/10/29 19:32
 */

public class SubscriptionPresenter implements ISubscriptionPresenter, ISubDaoCallBack {

    private static final String TAG = "SubscriptionPresenter";
    private Map<Long, Album> mData = new HashMap<>();
    private List<ISubscriptionCallback> mCallbacks = new ArrayList<>();
    private List<Album> mSubscriptions = new ArrayList<>();
    private final SubscriptionDao mSubscriptionDao;

    private SubscriptionPresenter() {
        mSubscriptionDao = SubscriptionDao.getInstance();
        mSubscriptionDao.setCallBack(this);
    }

    private void listSubscription() {
        LogUtil.d(TAG,"listSubscription.............");
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                //只调用不处理结果
                if (mSubscriptionDao != null) {
                    mSubscriptionDao.listAlbum();
                    LogUtil.d(TAG,"subscribe.............");
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private static SubscriptionPresenter sSubscriptionPresenter = null;

    public static SubscriptionPresenter getInstance() {
        if (sSubscriptionPresenter == null) {
            synchronized (SubscriptionPresenter.class) {
                sSubscriptionPresenter = new SubscriptionPresenter();
            }
        }
        return sSubscriptionPresenter;
    }


    @Override
    public void addSubscription(Album album) {
        //判断当前的订阅数量，不能超过100
        if (mData.size() >= Constants.MAX_SUB_COUNT) {
            //给出提示
            for (ISubscriptionCallback callback : mCallbacks) {
                callback.onSubFull();
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                //只调用不处理结果
                if (mSubscriptionDao != null) {
                    mSubscriptionDao.addAlbum(album);
                    LogUtil.d(TAG,"addAlbum===>"+album.getAlbumTitle());
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void deleteSubscription(Album album) {
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Throwable {
                //只调用不处理结果
                if (mSubscriptionDao != null) {
                    mSubscriptionDao.delAlbum(album);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void loadSubscriptionList() {
        listSubscription();
    }

    @Override
    public boolean isSub(Album album) {
        Album result = mData.get(album.getId());
        return result != null;
    }

    @Override
    public void registerViewCallback(ISubscriptionCallback iSubscriptionCallback) {
        if (!mCallbacks.contains(iSubscriptionCallback)) {
            mCallbacks.add(iSubscriptionCallback);
        }
    }

    @Override
    public void unRegisterViewCallback(ISubscriptionCallback iSubscriptionCallback) {
        mCallbacks.remove(iSubscriptionCallback);
    }

    @Override
    public void onAddResult(Boolean isSuccess) {
        listSubscription();
        LogUtil.d(TAG,"onAddResult............");
        //添加成功的回调
        BaseApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                for (ISubscriptionCallback callback : mCallbacks) {
                    callback.onAddResult(isSuccess);
                }
            }
        });
    }

    @Override
    public void onDelResult(Boolean isSuccess) {
        listSubscription();
        //删除的回调
        BaseApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                for (ISubscriptionCallback callback : mCallbacks) {
                    callback.onDeleteResult(isSuccess);
                }
            }
        });
    }

    @Override
    public void onSubListLoaded(List<Album> result) {
        mData.clear();
        //加载数据的回调
        for (Album album : result) {
            mData.put(album.getId(), album);
        }
        //通知UI更新
        BaseApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                for (ISubscriptionCallback callback : mCallbacks) {
                    callback.onSubscriptionsLoaded(result);
                }
            }
        });
    }
}