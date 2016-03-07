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
package com.github.jksiezni.permissive.fragments.v13;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.github.jksiezni.permissive.PermissiveMessenger;
import com.github.jksiezni.permissive.Rationale;


/**
 *
 */
public class RationaleDialogFragment extends DialogFragment implements Rationale {
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private String[] allowablePermissions;
  private PermissiveMessenger permissiveMessenger;

  public String[] getPermissions() {
    return allowablePermissions;
  }

  public PermissiveMessenger getPermissiveMessenger() {
    return permissiveMessenger;
  }

  public boolean isAnyAllowablePermission() {
    return allowablePermissions.length > 0;
  }

  @Override
  public void onShowRationale(Activity activity, String[] allowablePermissions, PermissiveMessenger messenger) {
    this.allowablePermissions = allowablePermissions;
    this.permissiveMessenger = messenger;

    show(activity.getFragmentManager(), null);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (DEBUG) {
      Log.d(getClass().getSimpleName(), "onSaveInstanceState(): this=" + this);
    }
    super.onSaveInstanceState(outState);
    outState.putStringArray("permissions", allowablePermissions);
    outState.putParcelable("messenger", permissiveMessenger);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (DEBUG) {
      Log.d(getClass().getSimpleName(), "onCreate(): this=" + this + ", savedInstanceState=" + savedInstanceState);
    }

    if (savedInstanceState != null) {
      allowablePermissions = savedInstanceState.getStringArray("permissions");
      permissiveMessenger = savedInstanceState.getParcelable("messenger");
    }
    permissiveMessenger.restoreActivity(getActivity());
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);
    permissiveMessenger.cancelRequest();
  }

}
