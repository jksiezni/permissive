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
 * The class allows to control a current request while a rationale is displayed.
 *
 * <p>When a {@link Rationale} is displayed, then the processing of other actions and requests are stopped,
 * until a current request is canceled or repeated. As a developer you have to carefully design the flow
 * of your rationale, so you won't omit a required call to {@link #repeatRequest()} or {@link #cancelRequest()}.
 * Otherwise, a deadlock in your App may occur.
 * </p>
 *
 * <p>Fortunately, the class provides useful methods, that will help you to maintain control over request,
 * and allow to rebuild your request in case of destroyed process.
 * </p>
 *
 * @see #repeatRequest()
 * @see #cancelRequest()
 * @see #rebuildRequest()
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

  /**
   * Constructs the messenger for given requested permissions.
   *
   * @param target A permissive handler, where all messages are sent.
   * @param permissions  Requested permissions.
   */
  PermissiveMessenger(Handler target, String[] permissions) {
    this.messenger = new Messenger(target);
    this.permissions = permissions;
  }

  private PermissiveMessenger(Parcel in) {
    this.messenger = in.readParcelable(getClass().getClassLoader());
    this.permissions = in.createStringArray();
    this.messageSent = in.readInt() > 0;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(messenger, flags);
    dest.writeStringArray(permissions);
    dest.writeInt(messageSent ? 1 : 0);
  }

  /**
   * @return An array with requested permissions.
   */
  public String[] getRequestedPermissions() {
    return permissions;
  }

  /**
   * Sends a message to repeat current request.
   *
   * @return {@code true} if the message was sent. Otherwise is {@code false}.
   */
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

}
