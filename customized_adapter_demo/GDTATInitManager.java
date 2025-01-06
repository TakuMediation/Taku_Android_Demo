

package com.anythink.custom.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.MediationInitCallback;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.setting.GlobalSetting;
import com.qq.e.comm.managers.status.SDKStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GDTATInitManager extends ATInitMediation {

    public static final String TAG = GDTATInitManager.class.getSimpleName();
    private volatile static GDTATInitManager sInstance;
    int personAdStatus = 0;
    private boolean mHasInit;
    private String mLocalInitAppId;
    private final AtomicBoolean mIsIniting;
    private final Object mLock = new Object();
    private List<MediationInitCallback> mListeners;

    private GDTATInitManager() {
        mIsIniting = new AtomicBoolean(false);
    }

    public static GDTATInitManager getInstance() {
        if (sInstance == null) {
            synchronized (GDTATInitManager.class) {
                if (sInstance == null) sInstance = new GDTATInitManager();
            }
        }
        return sInstance;
    }

    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras) {
        initSDK(context, serviceExtras, null);
    }

    @Override
    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras, MediationInitCallback onInitCallback) {
        try {
            personAdStatus = ATSDK.getPersionalizedAdStatus();
        } catch (Throwable ignored) {

        }
        if (personAdStatus == ATAdConst.PRIVACY.PERSIONALIZED_LIMIT_STATUS) {
            GlobalSetting.setPersonalizedState(1);
        } else {
            GlobalSetting.setPersonalizedState(0);
        }

        if (ATSDK.isNetworkLogDebug()) {
            Log.i(TAG, "GlobalSetting.getPersonalizedState():" + GlobalSetting.getPersonalizedState());
        }

        if (mHasInit) {
            if (onInitCallback != null) {
                onInitCallback.onSuccess();
            }
            return;
        }

        synchronized (mLock) {

            if (mIsIniting.get()) {
                if (onInitCallback != null) {
                    mListeners.add(onInitCallback);
                }
                return;
            }

            if (mListeners == null) {
                mListeners = new ArrayList<>();
            }

            mIsIniting.set(true);
        }

        String app_id = getStringFromMap(serviceExtras, "app_id");

        if (onInitCallback != null) {
            mListeners.add(onInitCallback);
        }
        if (serviceExtras.containsKey(ATInitMediation.KEY_LOCAL)) {
            mLocalInitAppId = app_id;
        } else if (mLocalInitAppId != null && !TextUtils.equals(mLocalInitAppId, app_id)) {
            checkToSaveInitData(getNetworkName(), serviceExtras, mLocalInitAppId);
            mLocalInitAppId = null;
        }

        GDTAdSdk.initWithoutStart(context.getApplicationContext(), app_id);
        GDTAdSdk.start(new GDTAdSdk.OnStartListener() {
            @Override
            public void onStartSuccess() {
                mHasInit = true;
                callbackResult(true, null, null);
            }

            @Override
            public void onStartFailed(Exception e) {
                callbackResult(false, "", "GDT initSDK failed." + e.getMessage());
            }
        });
    }

    private void callbackResult(boolean success, String errorCode, String errorMsg) {
        synchronized (mLock) {
            int size = mListeners.size();
            MediationInitCallback initListener;
            for (int i = 0; i < size; i++) {
                initListener = mListeners.get(i);
                if (initListener != null) {
                    if (success) {
                        initListener.onSuccess();
                    } else {
                        initListener.onFail(errorCode + " | " + errorMsg);
                    }
                }
            }
            mListeners.clear();

            mIsIniting.set(false);
        }
    }

    @Override
    public String getNetworkName() {
        return "Tencent";
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    @Override
    public String getNetworkSDKClass() {
        return "com.qq.e.ads.ADActivity";
    }

    @Override
    public List getActivityStatus() {
        ArrayList<String> list = new ArrayList<>();
        list.add("com.qq.e.ads.ADActivity");
        list.add("com.qq.e.ads.PortraitADActivity");
        list.add("com.qq.e.ads.LandscapeADActivity");
        list.add("com.qq.e.ads.RewardvideoPortraitADActivity");
        list.add("com.qq.e.ads.RewardvideoLandscapeADActivity");
        return list;
    }

    @Override
    public List getServiceStatus() {
        ArrayList<String> list = new ArrayList<>();
        list.add("com.qq.e.comm.DownloadService");
        return list;
    }

    protected int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / (scale <= 0 ? 1 : scale) + 0.5f);
    }

    protected LoadAdParams getLoadAdParams() {
        LoadAdParams loadAdParams = new LoadAdParams();
        HashMap<String, String> loadMap = new HashMap<>();
        loadAdParams.setDevExtra(loadMap);
        return loadAdParams;
    }

}
