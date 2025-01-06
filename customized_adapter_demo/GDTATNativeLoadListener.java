

package com.anythink.custom.adapter;

import com.anythink.nativead.unitgroup.api.CustomNativeAd;

public interface GDTATNativeLoadListener {
    void notifyLoaded(CustomNativeAd... customNativeAds);

    void notifyError(String errorCode, String errorMsg);
}
