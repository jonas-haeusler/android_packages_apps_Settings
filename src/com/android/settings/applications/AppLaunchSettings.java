/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.ArraySet;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;

import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener,
        Preference.OnPreferenceChangeListener {

    private static final String KEY_OPEN_DOMAIN_URLS = "app_launch_open_domain_urls";
    private static final String KEY_SUPPORTED_DOMAIN_URLS = "app_launch_supported_domain_urls";
    private static final String KEY_CLEAR_DEFAULTS = "app_launch_clear_defaults";

    private PackageManager mPm;

    private SwitchPreference mOpenDomainUrls;
    private AppDomainsPreference mAppDomainUrls;
    private ClearDefaultsPreference mClearDefaultsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.installed_app_launch_settings);

        mPm = getActivity().getPackageManager();
        final int myUserId = UserHandle.myUserId();

        mOpenDomainUrls = (SwitchPreference) findPreference(KEY_OPEN_DOMAIN_URLS);
        mOpenDomainUrls.setOnPreferenceChangeListener(this);

        final int status = mPm.getIntentVerificationStatus(mPackageName, myUserId);
        boolean checked = status == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
        mOpenDomainUrls.setChecked(checked);

        mAppDomainUrls = (AppDomainsPreference) findPreference(KEY_SUPPORTED_DOMAIN_URLS);
        CharSequence[] entries = getEntries(mPackageName);
        mAppDomainUrls.setTitles(entries);
        mAppDomainUrls.setValues(new int[entries.length]);

        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(KEY_CLEAR_DEFAULTS);
    }

    private CharSequence[] getEntries(String packageName) {
        ArraySet<String> result = new ArraySet<>();

        List<IntentFilterVerificationInfo> list =
                mPm.getIntentFilterVerifications(packageName);
        for (IntentFilterVerificationInfo ivi : list) {
            for (String host : ivi.getDomains()) {
                result.add(host);
            }
        }

        return result.toArray(new CharSequence[0]);
    }

    @Override
    protected boolean refreshUi() {
        mClearDefaultsPreference.setPackageName(mPackageName);
        mClearDefaultsPreference.setAppEntry(mAppEntry);

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for preferred launch settings.
        return null;
    }


    @Override
    public void onClick(View v) {
        // Nothing to do
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean ret = false;
        final String key = preference.getKey();
        if (KEY_OPEN_DOMAIN_URLS.equals(key)) {
            SwitchPreference pref = (SwitchPreference) preference;
            int status = !pref.isChecked() ?
                    PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS :
                    PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;
            ret = mPm.updateIntentVerificationStatus(mPackageName, status, UserHandle.myUserId());
        }
        return ret;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_APP_LAUNCH;
    }
}
