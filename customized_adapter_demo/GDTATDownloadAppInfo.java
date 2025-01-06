
package com.anythink.custom.adapter;

import com.anythink.core.api.ATAdAppInfo;
import com.qq.e.ads.nativ.NativeUnifiedADAppMiitInfo;

public class GDTATDownloadAppInfo extends ATAdAppInfo {
    public String publisher;
    public String appVersion;
    public String appPrivacyLink;
    public String appPermissionLink;
    public String appName;
    public long appSize;
    public String appDownloadCount;

    public GDTATDownloadAppInfo(NativeUnifiedADAppMiitInfo nativeUnifiedADAppMiitInfo, String downloadCount) {
        publisher = nativeUnifiedADAppMiitInfo.getAuthorName();
        appVersion = nativeUnifiedADAppMiitInfo.getVersionName();
        appPrivacyLink = nativeUnifiedADAppMiitInfo.getPrivacyAgreement();
        appPermissionLink = nativeUnifiedADAppMiitInfo.getPermissionsUrl();
        appName = nativeUnifiedADAppMiitInfo.getAppName();
        appSize = nativeUnifiedADAppMiitInfo.getPackageSizeBytes();
        appDownloadCount = downloadCount;
    }

    @Override
    public String getPublisher() {
        return publisher;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public String getAppPrivacyUrl() {
        return appPrivacyLink;
    }

    @Override
    public String getAppPermissonUrl() {
        return appPermissionLink;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getAppPackageName() {
        return "";
    }

    @Override
    public String getDownloadCount() {
        return appDownloadCount;
    }

    @Override
    public long getAppSize() {
        return appSize;
    }
}
