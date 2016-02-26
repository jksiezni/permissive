/*
 *    Copyright 2016 Jakub Księżniak
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

package com.github.jksiezni.permissive;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

/**
 *
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissiveFragment extends Fragment {
  private static final String TAG = PermissiveFragment.class.getSimpleName();
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final String PERMISSIONS_EXTRA = "permissions";
  private static final String WAITING_FOR_RESULT = "waiting_for_result";


  public static PermissiveFragment create(String[] permissions) {
    final PermissiveFragment f = new PermissiveFragment();
    final Bundle bundle = new Bundle();
    bundle.putStringArray(PERMISSIONS_EXTRA, permissions);
    f.setArguments(bundle);
    return f;
  }

  private boolean waitingForResult;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final String[] permissions = getArguments().getStringArray(PERMISSIONS_EXTRA);
    if (DEBUG) {
      Log.v(TAG, "onCreate(): " + Arrays.toString(permissions));
    }

    if (savedInstanceState != null) {
      waitingForResult = savedInstanceState.getBoolean(WAITING_FOR_RESULT);
    }

    if (permissions != null && !waitingForResult) {
      waitingForResult = true;
      requestPermissions(permissions, 42);
    } else {
      closeFragment();
    }
  }

  @Override
  public void onDestroy() {
    if (DEBUG) {
      Log.v(TAG, "onDestroy()");
    }
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (DEBUG) {
      Log.v("PermissiveFragment", "Results: " + Arrays.toString(permissions) + " = " + Arrays.toString(grantResults));
    }
    waitingForResult = false;
    closeFragment();
    Permissive.dispatchRequestPermissionsResult(permissions, grantResults);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(WAITING_FOR_RESULT, waitingForResult);
  }

  private void closeFragment() {
    getFragmentManager().beginTransaction()
        .remove(this)
        .commit();
  }
}
