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

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 *
 */
public class PermissiveMessenger implements Parcelable {

  public static final Parcelable.Creator<PermissiveMessenger> CREATOR
      = new Parcelable.Creator<PermissiveMessenger>() {
    public PermissiveMessenger createFromParcel(Parcel in) {
      Messenger messenger = in.readParcelable(getClass().getClassLoader());
      return new PermissiveMessenger(messenger);
    }

    public PermissiveMessenger[] newArray(int size) {
      return new PermissiveMessenger[size];
    }
  };

  private final Messenger messenger;
  private boolean messageSent;

  PermissiveMessenger(Handler target) {
    this.messenger = new Messenger(target);
  }

  private PermissiveMessenger(Messenger messenger) {
    this.messenger = messenger;
  }

  public synchronized boolean repeatPermissionsRequest() {
    if (messageSent) {
      return false;
    }
    try {
      Message msg = Message.obtain();
      msg.what = PermissiveHandler.REPEAT_REQUEST;
      messenger.send(msg);
      messageSent = true;
      return true;
    } catch (RemoteException e) {
      e.printStackTrace();
      return false;
    }
  }

  public synchronized boolean cancelPermissionsRequest() {
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
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(messenger, flags);
  }
}
