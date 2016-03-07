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

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

/**
 *
 */
public class PermissiveMessenger implements Parcelable {
  private static final String TAG = PermissiveMessenger.class.getSimpleName();
  private static final boolean DEBUG = BuildConfig.DEBUG;

  public static final Parcelable.Creator<PermissiveMessenger> CREATOR
      = new Parcelable.Creator<PermissiveMessenger>() {
    public PermissiveMessenger createFromParcel(Parcel in) {
      return new PermissiveMessenger(in);
    }

    public PermissiveMessenger[] newArray(int size) {
      return new PermissiveMessenger[size];
    }
  };

  private final Messenger messenger;
  private final String[] permissions;

  private boolean messageSent;

  PermissiveMessenger(Handler target, String[] permissions) {
    this.messenger = new Messenger(target);
    this.permissions = permissions;
  }

  private PermissiveMessenger(Parcel in) {
    this.messenger = in.readParcelable(getClass().getClassLoader());
    this.permissions = in.createStringArray();
  }

  public String[] getRequestedPermissions() {
    return permissions;
  }

  public synchronized boolean repeatRequest() {
    return repeatRequest(false);
  }

  public synchronized boolean repeatRequest(boolean showRationale) {
    if (messageSent) {
      return false;
    }
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.REPEAT_REQUEST;
      msg.arg1 = showRationale ? 1 : 0;
      messenger.send(msg);
      messageSent = true;
      return true;
    } catch (RemoteException e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  public synchronized boolean cancelRequest() {
    if (messageSent) {
      return false;
    }
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.CANCEL_REQUEST;
      messenger.send(msg);
      messageSent = true;
      return true;
    } catch (RemoteException e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  public boolean updatePermissionsResultListener(PermissionsResultListener listener) {
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.UPDATE_LISTENER;
      msg.obj = listener;
      messenger.send(msg);
      return true;
    } catch (Exception e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  public boolean restoreActivity(Activity activity) {
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.RESTORE_ACTIVITY;
      msg.obj = activity;
      messenger.send(msg);
      return true;
    } catch (Exception e) {
      if (DEBUG) {
        Log.w(TAG, e);
      }
      return false;
    }
  }

  public Permissive.Request rebuildRequest() {
    return new Permissive.Request(true, permissions);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(messenger, flags);
    dest.writeStringArray(permissions);
  }
}
