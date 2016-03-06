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
package com.github.jksiezni.permissive.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.github.jksiezni.permissive.PermissionsResultListener;
import com.github.jksiezni.permissive.PermissiveMessenger;
import com.github.jksiezni.permissive.Rationale;

/**
 *
 */
public class RationaleFragment extends Fragment implements Rationale, PermissionsResultListener {

  private String[] permissions;
  private PermissiveMessenger permissiveMessenger;

  private boolean doFinish;

  public String[] getPermissions() {
    return permissions;
  }

  public PermissiveMessenger getPermissiveMessenger() {
    return permissiveMessenger;
  }

  @Override
  public void onShowRationale(Activity activity, String[] permissions, PermissiveMessenger messenger) {
    this.permissions = permissions;
    this.permissiveMessenger = messenger;

    if (isAdded()) {
      return;
    }
    if (activity instanceof FragmentActivity) {
      FragmentActivity fa = (FragmentActivity) activity;
      fa.getSupportFragmentManager().beginTransaction()
          .add(android.R.id.content, this, null)
          .commit();
    }
  }

  @Override
  public void onPermissionsResult(String[] grantedPermissions, String[] refusedPermissions) throws SecurityException {
    Log.i(getClass().getSimpleName(), "onPermissionsResult(): this=" + this);
    finishFragment();
  }

  public void finishFragment() {
    if (!isResumed()) {
      Log.e(getClass().getSimpleName(), "onPermissionsResult(): failed to exit. this=" + this);
      doFinish = true;
      return;
    }
    getFragmentManager().beginTransaction()
        .remove(this)
        .commit();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(getClass().getSimpleName(), "onCreate(): this=" + this + ", savedInstanceState=" + savedInstanceState);

    if (savedInstanceState != null) {
      permissions = savedInstanceState.getStringArray("permissions");
      permissiveMessenger = savedInstanceState.getParcelable("messenger");
    }
    if (!permissiveMessenger.updatePermissionsResultListener(this)) {
      permissiveMessenger.rebuildRequest()
          .withRationale(this)
          .whenPermissionsResultReceived(this)
          .execute(getActivity());
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.i(getClass().getSimpleName(), "onSaveInstanceState(): this=" + this);
    super.onSaveInstanceState(outState);
    outState.putStringArray("permissions", permissions);
    outState.putParcelable("messenger", permissiveMessenger);
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.i(getClass().getSimpleName(), "onStop(): this=" + this);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(getClass().getSimpleName(), "onResume(): this=" + this);
    if (doFinish) {
      finishFragment();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(getClass().getSimpleName(), "onPause(): this=" + this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(getClass().getSimpleName(), "onDestroy(): this=" + this);
  }
}
