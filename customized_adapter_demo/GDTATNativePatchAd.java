package com.anythink.custom.adapter;

import android.content.Context;

import com.qq.e.ads.nativ.NativeUnifiedADData;

public class GDTATNativePatchAd extends GDTATNativeAd {

    protected GDTATNativePatchAd(Context context, NativeUnifiedADData gdtAd, int videoMuted, int videoAutoPlay, int videoDuration) {
        super(context, gdtAd, videoMuted, videoAutoPlay, videoDuration);
    }

    @Override
    public int getNativeType() {
        return NativeType.PATCH;
    }
}