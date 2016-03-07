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
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;

/**
 * This fragment is used to request permissions on Android Marshmallow (>23).
 *
 * It's designed to retain its state across activities and processes,
 * so we can be sure, that the request result is received and correctly dispatched.
 *
 * Unfortunately, the request itself may not be preserved across processes, then the request
 * can be rebuilt with {@link PermissiveMessenger#rebuildRequest()}.
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissiveFragment extends Fragment {
  private static final String TAG = PermissiveFragment.class.getSimpleName();
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final String PERMISSIONS = "permissions";
  private static final String MESSENGER = "messenger";
  private static final String WAITING_FOR_RESULT = "waiting_for_result";

  private String[] permissions;
  private Messenger messenger;

  public static PermissiveFragment create(String[] permissions, Handler handler) {
    final PermissiveFragment f = new PermissiveFragment();
    final Bundle bundle = new Bundle();
    bundle.putStringArray(PERMISSIONS, permissions);
    bundle.putParcelable(MESSENGER, new Messenger(handler));
    f.setArguments(bundle);
    return f;
  }

  private boolean waitingForResult;
  private boolean hasResult;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    permissions = getArguments().getStringArray(PERMISSIONS);
    messenger = getArguments().getParcelable(MESSENGER);
    if (DEBUG) {
      Log.v(TAG, "onCreate(): " + Arrays.toString(permissions));
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      waitingForResult = savedInstanceState.getBoolean(WAITING_FOR_RESULT);
      if (!restoreActivity() && !waitingForResult) {
        Log.e(TAG, "It should never happen, that we close this fragment before any results are received!");
        closeFragment();
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (DEBUG) {
      Log.v(TAG, "onStart(): requestingPermission=" + (permissions != null && !waitingForResult));
    }
    if (permissions != null && !waitingForResult) {
      waitingForResult = true;
      requestPermissions(permissions, 42);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (DEBUG) {
      Log.v(TAG, "onResume():");
    }
    if (hasResult) {
      closeFragment();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (DEBUG) {
      Log.v(TAG, "onDestroy(): isRemoving=" + isRemoving());
    }
    if (hasResult) {
      sendMsg(PermissiveHandler.PERMISSIONS_RESULT);
    } else if (!isRemoving()) {
      sendMsg(PermissiveHandler.CANCEL_REQUEST);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (DEBUG) {
      Log.v("PermissiveFragment", "Results: " + Arrays.toString(permissions) + " = " + Arrays.toString(grantResults));
    }
    waitingForResult = false;
    hasResult = true; // postpone sending this event until this fragment is resumed
    if (isResumed()) {
      Log.e(TAG, "It's in resumed state, so we should close it immediately.");
      //closeFragment();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(WAITING_FOR_RESULT, waitingForResult);
    if (DEBUG) {
      Log.v("PermissiveFragment", "onSaveInstanceState(): " + waitingForResult);
    }
  }

  private void closeFragment() {
    getFragmentManager().beginTransaction()
        .remove(this)
        .commit();
  }

  private boolean sendMsg(int what) {
    try {
      Message msg = Message.obtain();
      msg.what = what;
      messenger.send(msg);
      return true;
    } catch (RemoteException e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  private boolean restoreActivity() {
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.RESTORE_ACTIVITY;
      msg.obj = getActivity();
      messenger.send(msg);
      return true;
    } catch (Exception e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  void setMessenger(Messenger messenger) {
    this.messenger = messenger;
  }
}
