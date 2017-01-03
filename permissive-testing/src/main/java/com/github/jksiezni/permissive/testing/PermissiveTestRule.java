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
import android.app.Activity;
import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleCallback;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * This rule adds support for Runtime Permissions in functional UI tests. This class is an
 * extension of {@link IntentsTestRule}, which initializes fake permissions before each test
 * annotated with
 * <a href="http://junit.org/javadoc/latest/org/junit/Test.html"><code>Test</code></a> and releases
 * clears permissions after each test run.
 * This rule can replace directly the {@link ActivityTestRule}.
 *
 * <p>By default, all permissions are denied. Chain one of <code>granted*</code> methods
 * to make some permissions granted before the test is started.</p>
 *
 * <p>
 * Later runtime permissions can be granted in two ways:
 * <ul>
 * <li>Faking single permission request, using the {@link PermissiveTesting#onPermissionRequest(String)} API</li>
 * <li>Faking multiple permissions request, using the {@link PermissiveTesting#onMultiplePermissionsRequest()} API</li>
 * </ul>
 *
 * @param <T> The activity to test
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissiveTestRule<T extends Activity> extends IntentsTestRule<T> {

  private static final String TAG = PermissiveTestRule.class.getSimpleName();

  private final ActivityLifecycleCallback activityLifecycleCallback = new ActivityLifecycleCallback() {
    @Override
    public void onActivityLifecycleChanged(Activity activity, Stage stage) {
      if (stage == Stage.PRE_ON_CREATE) {
        PermissiveContext.attachTo(activity);
      }
    }
  };

  private final Set<String> fakePermissions = new HashSet<>();

  private boolean alreadyInitialized;

  /**
   * {@inheritDoc}
   */
  public PermissiveTestRule(Class<T> activityClass) {
    super(activityClass);
  }

  /**
   * {@inheritDoc}
   */
  public PermissiveTestRule(Class<T> activityClass, boolean initialTouchMode) {
    super(activityClass, initialTouchMode);
  }

  /**
   * {@inheritDoc}
   */
  public PermissiveTestRule(Class<T> activityClass, boolean initialTouchMode,
                            boolean launchActivity) {
    super(activityClass, initialTouchMode, launchActivity);
  }

  @Override
  protected void beforeActivityLaunched() {
    if (alreadyInitialized) return;
    grantAllPermissions();
    PermissiveTesting permissiveTesting = PermissiveTesting.init();
    for(String fakePerm : fakePermissions) {
      permissiveTesting.grantFakePermission(fakePerm);
    }
    ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(activityLifecycleCallback);
    super.beforeActivityLaunched();
  }

  @Override
  protected void afterActivityLaunched() {
    if (!alreadyInitialized) {
      alreadyInitialized = true;
      super.afterActivityLaunched();
    }
  }

  @Override
  protected void afterActivityFinished() {
    super.afterActivityFinished();
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(activityLifecycleCallback);
    PermissiveTesting.release();
    alreadyInitialized = false;
  }

  /**
   * Makes given permission granted at the start of each test.
   *
   * @param permission the permission to be granted
   */
  public PermissiveTestRule<T> granted(String permission) {
    fakePermissions.add(permission);
    return this;
  }

  /**
   * Makes all permissions declared in Android Manifest to be granted at the start of each test.
   */
  public PermissiveTestRule<T> grantedAll() {
    Context context = InstrumentationRegistry.getTargetContext();
    try {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
      Collections.addAll(fakePermissions, packageInfo.requestedPermissions);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, "packageInfo not found for: " + context.getPackageName());
    }
    return this;
  }

  /**
   * Makes given permission denied at the start of each test.
   *
   * @param permission the permission to be denied
   */
  public PermissiveTestRule<T> denied(String permission) {
    fakePermissions.remove(permission);
    return this;
  }

  /**
   * Makes all permissions declared in Android Manifest to be denied at the start of each test.
   */
  public PermissiveTestRule<T> deniedAll() {
    fakePermissions.clear();
    return this;
  }

  private void grantAllPermissions() {
    Context context = InstrumentationRegistry.getTargetContext();
    try {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
      UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
      for (int i = 0; i < packageInfo.requestedPermissions.length; ++i) {
        if ((packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
          grantReal(uiAutomation, packageInfo.requestedPermissions[i]);
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, "packageInfo not found for: " + context.getPackageName());
    }
  }

  private static void grantReal(UiAutomation automation, String permission) {
    try {
      String targetPackageName = InstrumentationRegistry.getTargetContext().getPackageName();
      ParcelFileDescriptor pfn = automation.executeShellCommand("pm grant " + targetPackageName + " " + permission);
      pfn.close();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }
}
