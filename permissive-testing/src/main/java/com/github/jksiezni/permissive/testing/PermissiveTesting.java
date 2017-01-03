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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.IntDef;
import android.support.test.espresso.intent.ResettingStubber;
import android.support.test.runner.intent.IntentCallback;
import android.support.test.runner.intent.IntentMonitorRegistry;
import android.support.test.runner.intent.IntentStubberRegistry;

import org.hamcrest.Matcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * <p>The <b>Permissive</b> testing framework enables safe testing of runtime permissions
 * by faking process of requesting and checking runtime permissions.
 *
 * <p>The class provides methods, which allow to create a fake response for permission requests
 * performed by the application under test.
 *
 * <p><i>Note: Faking runtime permissions only works for activities, that were started by {@link PermissiveTestRule} rule.</i>
 *
 * @see PermissiveTestRule
 */
public class PermissiveTesting {

  /**
   * The action used to request that the user approve a permission request
   * from the application.
   */
  private static final String ACTION_REQUEST_PERMISSIONS =
      "android.content.pm.action.REQUEST_PERMISSIONS";

  /**
   * The names of the requested permissions.
   * <p>
   * <strong>Type:</strong> String[]
   * </p>
   */
  private static final String EXTRA_REQUEST_PERMISSIONS_NAMES =
      "android.content.pm.extra.REQUEST_PERMISSIONS_NAMES";

  /**
   * The results from the permissions request.
   * <p>
   * <strong>Type:</strong> int[] of #PermissionResult
   * </p>
   */
  private static final String EXTRA_REQUEST_PERMISSIONS_RESULTS
      = "android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS";

  /**
   * Enables faking responses for permission requests.
   *
   * <p>In this case, the test author can make a fake response for single permission request.
   * When the activity requests a permission, then it would receive this (faked) response.
   *
   * <p><i>Works only with {@link PermissiveTestRule}</i>
   *
   * @param permission    the permission, that is expected in the request
   * @return {@link PermissionResponse} object to set the response
   */
  public static PermissionResponse onPermissionRequest(String permission) {
    return defaultInstance.new PermissionResponse(permission);
  }

  /**
   * Enables faking responses for multiple permissions request.
   *
   * <p>In this case, the test author can make a fake response for single permission request.
   * When the activity requests a permission, then it would receive this (faked) response.
   *
   * <p><i>Works only with {@link PermissiveTestRule}</i>
   *
   * @return {@link PermissionResponse} object to set the response
   */
  public static MultiPermissionsResponse onMultiplePermissionsRequest() {
    return defaultInstance.new MultiPermissionsResponse();
  }

  private static PermissiveTesting defaultInstance;

  private final Set<String> grantedFakePermissions = new HashSet<>();
  private MultiPermissionsResponse multiplePermissionsRequest;
  private boolean hasStubbedIntent;

  private final IntentCallback intentCallback = new IntentCallback() {
    @Override
    public void onIntentSent(Intent intent) {
      Matcher<Intent> intentMatcher = hasAction(PermissiveTesting.ACTION_REQUEST_PERMISSIONS);
      if (intentMatcher.matches(intent)) {
        ResettingStubber resettingStubber = (ResettingStubber) IntentStubberRegistry.getInstance();
        String[] permissions = intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES);
        if (hasStubbedIntent) {
          if (multiplePermissionsRequest != null) {
            multiplePermissionsRequest.update(resettingStubber, intentMatcher);
            multiplePermissionsRequest = null;
          }
          Instrumentation.ActivityResult result = resettingStubber.getActivityResultForIntent(intent);
          assertThat("Requested permissions should be exactly the same to the results",
              result.getResultData().getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES),
              arrayContainingInAnyOrder(permissions));
          applyActivityResult(result);
        } else {
          resettingStubber.setActivityResultForIntent(intentMatcher, createResult(permissions, PackageManager.PERMISSION_DENIED));
        }
      }
    }

    private void applyActivityResult(Instrumentation.ActivityResult result) {
      String[] permissions = result.getResultData().getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES);
      int[] resultPermissions = result.getResultData().getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS);
      for (int i = 0; i < permissions.length; ++i) {
        if (resultPermissions[i] == PackageManager.PERMISSION_GRANTED) {
          grantFakePermission(permissions[i]);
        } else {
          revokeFakePermission(permissions[i]);
        }
      }
    }
  };

  private PermissiveTesting() {
  }

  static PermissiveTesting init() {
    assertNull(defaultInstance);
    defaultInstance = new PermissiveTesting();
    IntentMonitorRegistry.getInstance().addIntentCallback(defaultInstance.intentCallback);
    return defaultInstance;
  }

  static void release() {
    IntentMonitorRegistry.getInstance().removeIntentCallback(defaultInstance.intentCallback);
    defaultInstance = null;
  }

  void grantFakePermission(String permission) {
    grantedFakePermissions.add(permission);
  }

  void revokeFakePermission(String permission) {
    grantedFakePermissions.remove(permission);
  }

  @IntDef({PackageManager.PERMISSION_GRANTED,
      PackageManager.PERMISSION_DENIED})
  @Retention(RetentionPolicy.SOURCE)
  @interface PermissionResult {
  }

  @PermissionResult
  static int checkFakePermission(String permission) {
    assertNotNull(defaultInstance);
    return defaultInstance.grantedFakePermissions.contains(permission) ?
        PackageManager.PERMISSION_GRANTED :
        PackageManager.PERMISSION_DENIED;
  }

  private static Instrumentation.ActivityResult createResult(String permission, int grantPermission) {
    return createResult(new String[]{permission}, grantPermission);
  }

  private static Instrumentation.ActivityResult createResult(String[] permissions, int grantPermission) {
    final int[] grants = new int[permissions.length];
    Arrays.fill(grants, grantPermission);
    return createResult(permissions, grants);
  }

  private static Instrumentation.ActivityResult createResult(String[] permissions, int[] grantPermissions) {
    Intent data = new Intent();
    data.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions);
    data.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, grantPermissions);
    return new Instrumentation.ActivityResult(Activity.RESULT_OK, data);
  }

  /**
   * Provides methods to setup a response for future permission request.
   */
  public class PermissionResponse {

    private final String permission;

    private PermissionResponse(String permission) {
      this.permission = permission;
    }

    /**
     * Grants permission when requested.
     */
    public void grant() {
      intending(hasAction(ACTION_REQUEST_PERMISSIONS))
          .respondWith(createResult(permission, PackageManager.PERMISSION_GRANTED));
      hasStubbedIntent = true;
    }

    /**
     * Revokes permission when requested.
     */
    public void revoke() {
      intending(hasAction(ACTION_REQUEST_PERMISSIONS))
          .respondWith(createResult(permission, PackageManager.PERMISSION_DENIED));
      hasStubbedIntent = true;
    }
  }

  /**
   * Provides methods to setup a response for multiple permissions request.
   *
   * <p>Supports method chaining after {@link PermissiveTesting#onMultiplePermissionsRequest()} call.
   */
  public class MultiPermissionsResponse {

    private final Map<String, Integer> permissions = new HashMap<>();

    private MultiPermissionsResponse() {
      multiplePermissionsRequest = this;
    }

    /**
     * Grants permission when requested.
     *
     * @param permission the permission to be granted
     */
    public MultiPermissionsResponse grant(String permission) {
      permissions.put(permission, PackageManager.PERMISSION_GRANTED);
      hasStubbedIntent = true;
      return this;
    }

    /**
     * Revokes permission when requested.
     *
     * @param permission the permission to be revoked
     */
    public MultiPermissionsResponse revoke(String permission) {
      permissions.put(permission, PackageManager.PERMISSION_DENIED);
      hasStubbedIntent = true;
      return this;
    }

    void update(ResettingStubber resettingStubber, Matcher<Intent> intentMatcher) {
      String[] names = new String[permissions.size()];
      int[] results = new int[permissions.size()];
      int i = 0;
      for (Entry<String, Integer> entry : permissions.entrySet()) {
        names[i] = entry.getKey();
        results[i] = entry.getValue();
        ++i;
      }
      resettingStubber.setActivityResultForIntent(intentMatcher, createResult(names, results));
    }
  }
}
