/*
 *    Copyright 2017 Jakub Księżniak
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.jksiezni.permissive.testing;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.List;

/**
 *
 */
@TargetApi(Build.VERSION_CODES.M)
class PermissiveContext extends ContextWrapper {

  static void attachTo(ContextWrapper context) {
    try {
      Field mBase = ContextWrapper.class.getDeclaredField("mBase");
      mBase.setAccessible(true);
      mBase.set(context, new PermissiveContext(context.getBaseContext()));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Permissive was unable to replace base context. Please report it to the library maintainer.", e);
    }
  }

  private PermissiveContext(Context base) {
    super(base);
  }

  @Override
  public int checkPermission(String permission, int pid, int uid) {
    return PermissiveTesting.checkFakePermission(permission);
  }

  @Override
  public int checkSelfPermission(String permission) {
    return PermissiveTesting.checkFakePermission(permission);
  }

  @Override
  public PackageManager getPackageManager() {
    return new PackageManager() {
      final PackageManager pm = getBaseContext().getPackageManager();

      public boolean shouldShowRequestPermissionRationale(String permission) {
        return true;
      }

      public String getPermissionControllerPackageName() {
        return "com.google.android.packageinstaller";
      }

      @Override
      public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        return pm.getPackageInfo(packageName, flags);
      }

      @Override
      public String[] currentToCanonicalPackageNames(String[] names) {
        return pm.currentToCanonicalPackageNames(names);
      }

      @Override
      public String[] canonicalToCurrentPackageNames(String[] names) {
        return pm.canonicalToCurrentPackageNames(names);
      }

      @Override
      public Intent getLaunchIntentForPackage(String packageName) {
        return pm.getLaunchIntentForPackage(packageName);
      }

      @Override
      public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        return pm.getLeanbackLaunchIntentForPackage(packageName);
      }

      @Override
      public int[] getPackageGids(String packageName) throws NameNotFoundException {
        return pm.getPackageGids(packageName);
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
        return pm.getPackageGids(packageName, flags);
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
        return pm.getPackageUid(packageName, flags);
      }

      @Override
      public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
        return pm.getPermissionInfo(name, flags);
      }

      @Override
      public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
        return pm.queryPermissionsByGroup(group, flags);
      }

      @Override
      public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
        return pm.getPermissionGroupInfo(name, flags);
      }

      @Override
      public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return pm.getAllPermissionGroups(flags);
      }

      @Override
      public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        return pm.getApplicationInfo(packageName, flags);
      }

      @Override
      public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
        return pm.getActivityInfo(component, flags);
      }

      @Override
      public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
        return pm.getReceiverInfo(component, flags);
      }

      @Override
      public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
        return pm.getServiceInfo(component, flags);
      }

      @Override
      public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
        return pm.getProviderInfo(component, flags);
      }

      @Override
      public List<PackageInfo> getInstalledPackages(int flags) {
        return pm.getInstalledPackages(flags);
      }

      @Override
      public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
        return pm.getPackagesHoldingPermissions(permissions, flags);
      }

      @Override
      public int checkPermission(String permName, String pkgName) {
        if (getPackageName().equals(pkgName)) {
          return PermissiveTesting.checkFakePermission(permName);
        }
        return pm.checkPermission(permName, pkgName);
      }

      @Override
      public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
        return pm.isPermissionRevokedByPolicy(permName, pkgName);
      }

      @Override
      public boolean addPermission(PermissionInfo info) {
        return pm.addPermission(info);
      }

      @Override
      public boolean addPermissionAsync(PermissionInfo info) {
        return pm.addPermissionAsync(info);
      }

      @Override
      public void removePermission(String name) {
        pm.removePermission(name);
      }

      @Override
      public int checkSignatures(String pkg1, String pkg2) {
        return pm.checkSignatures(pkg1, pkg2);
      }

      @Override
      public int checkSignatures(int uid1, int uid2) {
        return pm.checkSignatures(uid1, uid2);
      }

      @Nullable
      @Override
      public String[] getPackagesForUid(int uid) {
        return pm.getPackagesForUid(uid);
      }

      @Nullable
      @Override
      public String getNameForUid(int uid) {
        return pm.getNameForUid(uid);
      }

      @Override
      public List<ApplicationInfo> getInstalledApplications(int flags) {
        return pm.getInstalledApplications(flags);
      }

      @Override
      public String[] getSystemSharedLibraryNames() {
        return pm.getSystemSharedLibraryNames();
      }

      @Override
      public FeatureInfo[] getSystemAvailableFeatures() {
        return pm.getSystemAvailableFeatures();
      }

      @Override
      public boolean hasSystemFeature(String name) {
        return pm.hasSystemFeature(name);
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public boolean hasSystemFeature(String name, int version) {
        return pm.hasSystemFeature(name, version);
      }

      @Override
      public ResolveInfo resolveActivity(Intent intent, int flags) {
        return pm.resolveActivity(intent, flags);
      }

      @Override
      public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        return pm.queryIntentActivities(intent, flags);
      }

      @Override
      public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
        return pm.queryIntentActivityOptions(caller, specifics, intent, flags);
      }

      @Override
      public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        return pm.queryBroadcastReceivers(intent, flags);
      }

      @Override
      public ResolveInfo resolveService(Intent intent, int flags) {
        return pm.resolveService(intent, flags);
      }

      @Override
      public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        return pm.queryIntentServices(intent, flags);
      }

      @Override
      public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        return pm.queryIntentContentProviders(intent, flags);
      }

      @Override
      public ProviderInfo resolveContentProvider(String name, int flags) {
        return pm.resolveContentProvider(name, flags);
      }

      @Override
      public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        return pm.queryContentProviders(processName, uid, flags);
      }

      @Override
      public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException {
        return pm.getInstrumentationInfo(className, flags);
      }

      @Override
      public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return pm.queryInstrumentation(targetPackage, flags);
      }

      @Override
      public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        return pm.getDrawable(packageName, resid, appInfo);
      }

      @Override
      public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
        return pm.getActivityIcon(activityName);
      }

      @Override
      public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
        return pm.getActivityIcon(intent);
      }

      @Override
      public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
        return pm.getActivityBanner(activityName);
      }

      @Override
      public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
        return pm.getActivityBanner(intent);
      }

      @Override
      public Drawable getDefaultActivityIcon() {
        return pm.getDefaultActivityIcon();
      }

      @Override
      public Drawable getApplicationIcon(ApplicationInfo info) {
        return pm.getApplicationIcon(info);
      }

      @Override
      public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
        return pm.getApplicationIcon(packageName);
      }

      @Override
      public Drawable getApplicationBanner(ApplicationInfo info) {
        return pm.getApplicationBanner(info);
      }

      @Override
      public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
        return pm.getApplicationBanner(packageName);
      }

      @Override
      public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
        return pm.getActivityLogo(activityName);
      }

      @Override
      public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
        return pm.getActivityLogo(intent);
      }

      @Override
      public Drawable getApplicationLogo(ApplicationInfo info) {
        return pm.getApplicationLogo(info);
      }

      @Override
      public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
        return pm.getApplicationLogo(packageName);
      }

      @Override
      public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return pm.getUserBadgedIcon(icon, user);
      }

      @Override
      public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        return pm.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
      }

      @Override
      public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return pm.getUserBadgedLabel(label, user);
      }

      @Override
      public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
        return pm.getText(packageName, resid, appInfo);
      }

      @Override
      public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
        return pm.getXml(packageName, resid, appInfo);
      }

      @Override
      public CharSequence getApplicationLabel(ApplicationInfo info) {
        return pm.getApplicationLabel(info);
      }

      @Override
      public Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
        return pm.getResourcesForActivity(activityName);
      }

      @Override
      public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
        return pm.getResourcesForApplication(app);
      }

      @Override
      public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
        return pm.getResourcesForApplication(appPackageName);
      }

      @Override
      public void verifyPendingInstall(int id, int verificationCode) {
        pm.verifyPendingInstall(id, verificationCode);
      }

      @Override
      public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        pm.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
      }

      @Override
      public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        pm.setInstallerPackageName(targetPackage, installerPackageName);
      }

      @Override
      public String getInstallerPackageName(String packageName) {
        return pm.getInstallerPackageName(packageName);
      }

      @SuppressWarnings("deprecation")
      @Override
      public void addPackageToPreferred(String packageName) {
        pm.addPackageToPreferred(packageName);
      }

      @SuppressWarnings("deprecation")
      @Override
      public void removePackageFromPreferred(String packageName) {
        pm.removePackageFromPreferred(packageName);
      }

      @Override
      public List<PackageInfo> getPreferredPackages(int flags) {
        return pm.getPreferredPackages(flags);
      }

      @SuppressWarnings("deprecation")
      @Override
      public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
        pm.addPreferredActivity(filter, match, set, activity);
      }

      @Override
      public void clearPackagePreferredActivities(String packageName) {
        pm.clearPackagePreferredActivities(packageName);
      }

      @Override
      public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
        return pm.getPreferredActivities(outFilters, outActivities, packageName);
      }

      @Override
      public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
        pm.setComponentEnabledSetting(componentName, newState, flags);
      }

      @Override
      public int getComponentEnabledSetting(ComponentName componentName) {
        return pm.getComponentEnabledSetting(componentName);
      }

      @Override
      public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
        pm.setApplicationEnabledSetting(packageName, newState, flags);
      }

      @Override
      public int getApplicationEnabledSetting(String packageName) {
        return pm.getApplicationEnabledSetting(packageName);
      }

      @Override
      public boolean isSafeMode() {
        return pm.isSafeMode();
      }

      @NonNull
      @Override
      public PackageInstaller getPackageInstaller() {
        return pm.getPackageInstaller();
      }
    };
  }
}
