
package com.anythink.custom.adapter;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.rewardvideo.unitgroup.api.CustomRewardVideoAdapter;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.ads.rewardvideo.RewardVideoAD;
import com.qq.e.ads.rewardvideo.RewardVideoADListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.listeners.ADRewardListener;
import com.qq.e.comm.util.AdError;

import java.util.HashMap;
import java.util.Map;

public class GDTATRewardedVideoAdapter extends CustomRewardVideoAdapter {

    private static final String TAG = GDTATRewardedVideoAdapter.class.getSimpleName();
    RewardVideoAD mRewardVideoAD;
    UnifiedInterstitialAD mInterstitialRVAD;
    String mAppId;
    String mUnitId;
    String mPayload;
    private int mVideoMuted = 0;
    private Map<String, Object> extraMap;

    private int mUnitType = 1;//1：GDTRewardedVideo,2；GDT Interstitla RewardedVideo

    private boolean isC2SBidding = false;

    @Override
    public void loadCustomNetworkAd(final Context context, final Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra, localExtra);

        if (TextUtils.isEmpty(mAppId) || TextUtils.isEmpty(mUnitId)) {
            notifyATLoadFail("", "GTD appId or unitId is empty.");
            return;
        }

        final Context applicationContext = context.getApplicationContext();
        GDTATInitManager.getInstance().initSDK(applicationContext, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
                startLoadAd(context, serverExtra);
            }

            @Override
            public void onFail(String errorMsg) {
                notifyATLoadFail("", errorMsg);
            }
        });
    }

    private void initRequestParams(Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        mAppId = ATInitMediation.getStringFromMap(serverExtra, "app_id");
        mUnitId = ATInitMediation.getStringFromMap(serverExtra, "unit_id");
        mPayload = ATInitMediation.getStringFromMap(serverExtra, "payload");

        mVideoMuted = ATInitMediation.getIntFromMap(serverExtra, "video_muted", 0);
        mUnitType = ATInitMediation.getIntFromMap(serverExtra, "unit_type", 1);
    }

    private void startLoadAd(Context context, Map<String, Object> serverExtra) {
        if (mUnitType == 2) {
            loadInterstitialRewardVideoAd(context, serverExtra);
        } else {
            loadRewardVideoAD(context, serverExtra);
        }
    }

    private void loadRewardVideoAD(Context context, Map<String, Object> serverExtra) {
        RewardVideoADListener rewardVideoADListener = new RewardVideoADListener() {
            @Override
            public void onADLoad() {
                try {
                    Map<String, Object> tempMap = mRewardVideoAD.getExtraInfo();
                    if (tempMap != null) {
                        if (extraMap == null) {
                            extraMap = new HashMap<>();
                        }
                        extraMap.putAll(tempMap);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                if (mLoadListener != null) {
                    mLoadListener.onAdDataLoaded();
                }
            }

            @Override
            public void onVideoCached() {
                if (isC2SBidding) {
                    if (mBiddingListener != null) {
                        if (mRewardVideoAD != null) {
                            int ecpm = mRewardVideoAD.getECPM();
                            double price = ecpm;
                            GDTATBiddingNotice biddingNotice = new GDTATBiddingNotice(mRewardVideoAD);
                            mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(price, System.currentTimeMillis() + "", biddingNotice, ATAdConst.CURRENCY.RMB_CENT), null);
                        }
                    }
                } else {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                }
            }

            @Override
            public void onADShow() {
            }

            @Override
            public void onADExpose() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayStart();
                }
            }

            @Override
            public void onReward(Map<String, Object> map) {
                if (extraMap == null) {
                    extraMap = new HashMap<>();
                }
                extraMap.put("gdt_trans_id", map.get(ServerSideVerificationOptions.TRANS_ID));
                if (mImpressionListener != null) {
                    mImpressionListener.onReward();
                }
            }

            @Override
            public void onADClick() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayClicked();
                }
            }

            @Override
            public void onVideoComplete() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayEnd();
                }
            }

            @Override
            public void onADClose() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdClosed();
                }
            }

            @Override
            public void onError(AdError adError) {
                notifyATLoadFail(adError.getErrorCode() + "", adError.getErrorMsg());
            }
        };

        if (TextUtils.isEmpty(mPayload) || isC2SBidding) {
            mRewardVideoAD = new RewardVideoAD(context.getApplicationContext(), mUnitId, rewardVideoADListener, mVideoMuted != 1);
            mRewardVideoAD.setLoadAdParams(GDTATInitManager.getInstance().getLoadAdParams());
        } else {
            mRewardVideoAD = new RewardVideoAD(context.getApplicationContext(), mUnitId, rewardVideoADListener, mVideoMuted != 1, mPayload);
        }

        try {
            ServerSideVerificationOptions.Builder builder = new ServerSideVerificationOptions.Builder();
            builder.setUserId(mUserId);
            if (!TextUtils.isEmpty(mUserData) && mUserData.contains(ATAdConst.REWARD_EXTRA_REPLACE_HODLER_KEY.NETWORK_PLACEMENT_ID_HOLDER_NAME)) {
                mUserData = mUserData.replace(ATAdConst.REWARD_EXTRA_REPLACE_HODLER_KEY.NETWORK_PLACEMENT_ID_HOLDER_NAME, mUnitId);
            }
            builder.setCustomData(mUserData);

            mRewardVideoAD.setServerSideVerificationOptions(builder.build());
        } catch (Throwable ignored) {
        }

        mRewardVideoAD.loadAD();
    }

    private void loadInterstitialRewardVideoAd(Context context, Map<String, Object> serverExtra) {
        if (!(context instanceof Activity)) {
            notifyATLoadFail("", "GDT UnifiedInterstitial's context must be activity.");
            return;
        }

        UnifiedInterstitialADListener interstitialADListener = new UnifiedInterstitialADListener() {
            @Override
            public void onADReceive() {

                try {
                    Map<String, Object> tempMap = mInterstitialRVAD.getExtraInfo();
                    if (tempMap != null) {
                        if (extraMap == null) {
                            extraMap = new HashMap<>();
                        }
                        extraMap.putAll(tempMap);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                mInterstitialRVAD.setRewardListener(new ADRewardListener() {
                    @Override
                    public void onReward(Map<String, Object> map) {
                        if (extraMap == null) {
                            extraMap = new HashMap<>();
                        }
                        extraMap.put("gdt_trans_id", map.get(ServerSideVerificationOptions.TRANS_ID));
                        if (mImpressionListener != null) {
                            mImpressionListener.onReward();
                        }
                    }
                });

                if (mLoadListener != null) {
                    mLoadListener.onAdDataLoaded();
                }
            }

            @Override
            public void onVideoCached() {

            }

            @Override
            public void onNoAD(AdError adError) {
                notifyATLoadFail(String.valueOf(adError.getErrorCode()), adError.getErrorMsg());
            }

            @Override
            public void onADOpened() {

            }

            @Override
            public void onADExposure() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayStart();
                }
            }

            @Override
            public void onADClicked() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayClicked();
                }
            }

            @Override
            public void onADLeftApplication() {

            }

            @Override
            public void onADClosed() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdClosed();
                }
            }

            @Override
            public void onRenderSuccess() {
                if (isC2SBidding) {
                    if (mBiddingListener != null) {
                        if (mInterstitialRVAD != null) {
                            int ecpm = mInterstitialRVAD.getECPM();
                            double price = ecpm;
                            GDTATBiddingNotice biddingNotice = new GDTATBiddingNotice(mInterstitialRVAD);
                            mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(price, System.currentTimeMillis() + "", biddingNotice, ATAdConst.CURRENCY.RMB_CENT), null);
                        } else {
                            notifyATLoadFail("", "GDT : UnifiedInterstitialAD had been destroyed.");
                        }
                    }
                } else {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                }
            }

            @Override
            public void onRenderFail() {
                notifyATLoadFail("", "GDT: onRenderFail()");
            }
        };

        if (TextUtils.isEmpty(mPayload) || isC2SBidding) {
            mInterstitialRVAD = new UnifiedInterstitialAD((Activity) context, mUnitId, interstitialADListener);
            mInterstitialRVAD.setLoadAdParams(GDTATInitManager.getInstance().getLoadAdParams());
        } else {
            mInterstitialRVAD = new UnifiedInterstitialAD((Activity) context, mUnitId, interstitialADListener, null, mPayload);
        }

        setVideoOption(mInterstitialRVAD, serverExtra);

        mInterstitialRVAD.setMediaListener(new UnifiedInterstitialMediaListener() {
            @Override
            public void onVideoInit() {

            }

            @Override
            public void onVideoLoading() {

            }

            @Override
            public void onVideoReady(long l) {

            }

            @Override
            public void onVideoStart() {

            }

            @Override
            public void onVideoPause() {

            }

            @Override
            public void onVideoComplete() {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayEnd();
                }
            }

            @Override
            public void onVideoError(AdError adError) {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayFailed(adError.getErrorCode() + "", adError.getErrorMsg());
                }
            }

            @Override
            public void onVideoPageOpen() {

            }

            @Override
            public void onVideoPageClose() {

            }
        });

        mInterstitialRVAD.loadFullScreenAD();
    }

    /**
     * set video option
     */
    private void setVideoOption(UnifiedInterstitialAD interstitialAD, Map<String, Object> serverExtra) {

        int isVideoMuted = 0;
        int isVideoAutoPlay = 1;
        int videoDuration = -1;
        if (serverExtra.containsKey("video_muted")) {
            isVideoMuted = Integer.parseInt(serverExtra.get("video_muted").toString());
        }
        if (serverExtra.containsKey("video_autoplay")) {
            isVideoAutoPlay = Integer.parseInt(serverExtra.get("video_autoplay").toString());
        }
        if (serverExtra.containsKey("video_duration")) {
            videoDuration = Integer.parseInt(serverExtra.get("video_duration").toString());
        }

        if (interstitialAD != null) {
            VideoOption option = new VideoOption.Builder().setAutoPlayMuted(isVideoMuted == 1).setDetailPageMuted(isVideoMuted == 1).setAutoPlayPolicy(isVideoAutoPlay).build();
            interstitialAD.setVideoOption(option);
            if (videoDuration != -1) {
                interstitialAD.setMaxVideoDuration(videoDuration);
            }
        }
    }

    @Override
    public String getNetworkName() {
        return GDTATInitManager.getInstance().getNetworkName();
    }

    @Override
    public void destory() {
        if (mRewardVideoAD != null) {
            mRewardVideoAD = null;
        }
        if (mInterstitialRVAD != null) {
            mInterstitialRVAD = null;
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
    public boolean isAdReady() {
        if (mRewardVideoAD != null) {
            return mRewardVideoAD.isValid();
        }

        if (mInterstitialRVAD != null) {
            return mInterstitialRVAD.isValid();
        }

        return false;
    }

    @Override
    public void show(Activity activity) {

        if (mRewardVideoAD != null) {
            try {
                if (activity != null) {
                    mRewardVideoAD.showAD(activity);
                } else {
                    mRewardVideoAD.showAD();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (mInterstitialRVAD != null) {
            try {
                mInterstitialRVAD.showFullScreenAD(activity);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Map<String, Object> getNetworkInfoMap() {
        return extraMap;
    }

    @Override
    public ATInitMediation getMediationInitManager() {
        return GDTATInitManager.getInstance();
    }

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        isC2SBidding = true;
        loadCustomNetworkAd(context, serverExtra, localExtra);
        return true;
    }

}
