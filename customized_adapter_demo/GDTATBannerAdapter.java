
package com.anythink.custom.adapter;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.anythink.banner.unitgroup.api.CustomBannerAdapter;
import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.MediationInitCallback;
import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.ads.banner2.UnifiedBannerView;

import java.util.Map;

public class GDTATBannerAdapter extends CustomBannerAdapter {
    private final String TAG = GDTATBannerAdapter.class.getSimpleName();

    String mAppId;
    String mUnitId;
    String mPayload;
    UnifiedBannerView mBannerView;

    int mUnitVersion = 0;
    int mRefreshTime;

    boolean isC2SBidding;

    private void startLoadAd(Activity activity, Map<String, Object> serverMap) {
        UnifiedBannerView unifiedBannerView = null;
        //2.0
        UnifiedBannerADListener unifiedBannerADListener = new UnifiedBannerADListener() {
            @Override
            public void onNoAD(com.qq.e.comm.util.AdError adError) {
                mBannerView = null;
                notifyATLoadFail(String.valueOf(adError.getErrorCode()), adError.getErrorMsg());
            }

            @Override
            public void onADReceive() {
                if (isC2SBidding) {
                    if (mBiddingListener != null) {
                        if (mBannerView != null) {
                            int ecpm = mBannerView.getECPM();
                            double price = ecpm;
                            GDTATBiddingNotice biddingNotice = new GDTATBiddingNotice(mBannerView);
                            mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(price, System.currentTimeMillis() + "", biddingNotice, ATAdConst.CURRENCY.RMB_CENT), null);

                        } else {
                            notifyATLoadFail("", "GDT: Offer had been destroy.");
                        }
                    }
                } else {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                }
            }

            @Override
            public void onADExposure() {
                if (mImpressionEventListener != null) {
                    mImpressionEventListener.onBannerAdShow();
                }
            }

            @Override
            public void onADClosed() {
                if (mImpressionEventListener != null) {
                    mImpressionEventListener.onBannerAdClose();
                }
            }

            @Override
            public void onADClicked() {
                if (mImpressionEventListener != null) {
                    mImpressionEventListener.onBannerAdClicked();
                }
            }

            @Override
            public void onADLeftApplication() {

            }

        };

        if (TextUtils.isEmpty(mPayload) || isC2SBidding) {
            unifiedBannerView = new UnifiedBannerView(activity, mUnitId, unifiedBannerADListener);
            unifiedBannerView.setLoadAdParams(GDTATInitManager.getInstance().getLoadAdParams());
        } else {
            unifiedBannerView = new UnifiedBannerView(activity, mUnitId, unifiedBannerADListener, null, mPayload);
        }

        if (mRefreshTime > 0) {
            unifiedBannerView.setRefresh(mRefreshTime);
        } else {
            unifiedBannerView.setRefresh(0);
        }

        if (unifiedBannerView.getLayoutParams() == null) {
            unifiedBannerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        mBannerView = unifiedBannerView;

        unifiedBannerView.loadAD();
    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }

    @Override
    public String getNetworkName() {
        return GDTATInitManager.getInstance().getNetworkName();
    }

    @Override
    public void loadCustomNetworkAd(final Context context, final Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra, localExtra);

        if (TextUtils.isEmpty(mAppId) || TextUtils.isEmpty(mUnitId)) {
            notifyATLoadFail("", "GTD appid or unitId is empty.");
            return;
        }

        if (!(context instanceof Activity)) {
            notifyATLoadFail("", "Context must be activity.");
            return;
        }

        runOnNetworkRequestThread(new Runnable() {
            @Override
            public void run() {
                GDTATInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
                    @Override
                    public void onSuccess() {
                        startLoadAd((Activity) context, serverExtra);
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        notifyATLoadFail("", errorMsg);
                    }
                });
            }
        });
    }

    private void initRequestParams(Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        mAppId = ATInitMediation.getStringFromMap(serverExtra, "app_id");
        mUnitId = ATInitMediation.getStringFromMap(serverExtra, "unit_id");
        mUnitVersion = ATInitMediation.getIntFromMap(serverExtra, "unit_version");
        mPayload = ATInitMediation.getStringFromMap(serverExtra, "payload");
        mRefreshTime = 0;
        try {
            if (serverExtra.containsKey("nw_rft")) {
                mRefreshTime = ATInitMediation.getIntFromMap(serverExtra, "nw_rft");
                mRefreshTime /= (int) 1000f;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destory() {
        if (mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }
    }

    @Override
    public String getNetworkPlacementId() {
        return mUnitId;
    }

    @Override
    public String getNetworkSDKVersion() {
        return GDTATInitManager.getInstance().getNetworkVersion();
    }

    @Override
    public ATInitMediation getMediationInitManager() {
        return GDTATInitManager.getInstance();
    }

    @Override
    public boolean startBiddingRequest(Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        isC2SBidding = true;
        loadCustomNetworkAd(context, serverExtra, localExtra);
        return true;
    }
}
