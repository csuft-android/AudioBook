package com.audiobook.base;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.audiobook.utils.LogUtil;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.DeviceInfoProviderDefault;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDeviceInfoProvider;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.util.BaseUtil;
import com.ximalaya.ting.android.opensdk.util.SharedPreferencesUtil;


/**
 * @author 优雅永不过时
 * @Package com.audiobook.base
 * @Date 2021/10/17 18:00
 */
public class BaseApplication extends Application {
    private static Handler sHandler = null;
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = null;
    private static String moaid;
    public static final String KEY_LAST_OAID = "last_oaid";

    @Override
    public void onCreate() {
        super.onCreate();
        if (BaseUtil.isMainProcess(this)) {
            moaid = SharedPreferencesUtil.getInstance(getApplicationContext()).getString(KEY_LAST_OAID);
        }
        CommonRequest mXimalaya = CommonRequest.getInstanse();
        if (DTransferConstants.isRelease) {
            String mAppSecret = "8646d66d6abe2efd14f2891f9fd1c8af";
            mXimalaya.setAppkey("9f9ef8f10bebeaa83e71e62f935bede8");
            mXimalaya.setPackid("com.app.test.android");
            mXimalaya.init(this, mAppSecret, getDeviceInfo(this));
        } else {
            String mAppSecret = "0a09d7093bff3d4947a5c4da0125972e";
            mXimalaya.setAppkey("f4d8f65918d9878e1702d49a8cdf0183");
            mXimalaya.setPackid("com.ximalaya.qunfeng");
            mXimalaya.init(this, mAppSecret, getDeviceInfo(this));
        }

        //初始化播放器
        XmPlayerManager.getInstance(this).init();

        //初始化LogUtil
        LogUtil.init(this.getPackageName(), false);
        sHandler = new Handler();
        sContext = getBaseContext();
    }

    private IDeviceInfoProvider getDeviceInfo(Context context) {
        return new DeviceInfoProviderDefault(context) {
            @Override
            public String oaid() {
                return moaid;
            }
        };
    }


    public static Context getAppContext() {
        return sContext;
    }

    public static Handler getHandler() {
        return sHandler;
    }


}
