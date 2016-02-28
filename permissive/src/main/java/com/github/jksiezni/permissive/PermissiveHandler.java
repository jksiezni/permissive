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
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
class PermissiveHandler implements Handler.Callback {

  private static final int REQUEST_PERMISSIONS = 1;
  private static final int REQUEST_PERMISSIONS_RESULT = 2;

  static final int REPEAT_REQUEST = 3;
  static final int CANCEL_REQUEST = 4;

  private final Queue<Permissive.Action> pendingActions = new ConcurrentLinkedQueue<>();
  private final Handler handler = new Handler(Looper.getMainLooper(), this);

  private boolean isRequestingPermission = false;


  void enqueueAction(Permissive.Action action) {
    handler.obtainMessage(REQUEST_PERMISSIONS, action).sendToTarget();
  }

  void dispatchRequestPermissionsResult(RequestPermissionsResult result) {
    handler.obtainMessage(REQUEST_PERMISSIONS_RESULT, result).sendToTarget();
  }

  @Override
  public boolean handleMessage(Message msg) {
    switch (msg.what) {
      case REQUEST_PERMISSIONS:
        pendingActions.add((Permissive.Action) msg.obj);
        if (!isRequestingPermission) {
          isRequestingPermission = processPendingActions();
        }
        break;
      case REQUEST_PERMISSIONS_RESULT:
        if(!processPermissionsResult((RequestPermissionsResult) msg.obj)) {
          isRequestingPermission = processPendingActions();
        }
        break;
      case REPEAT_REQUEST:
        Log.w("tag", "REPEAT_REQUEST");
        isRequestingPermission = processPendingActions();
        break;
      case CANCEL_REQUEST:
        Log.w("tag", "CANCEL_REQUEST");
        pendingActions.remove();
        isRequestingPermission = processPendingActions();
        break;
    }
    return true;
  }

  private boolean processPendingActions() {
    Permissive.Action action;
    while ((action = currentAction()) != null) {
      if (action instanceof Permissive.Request
          && requestPermissions((Permissive.Request) action)) {
        return true;
      } else {
        processAction(action);
        pendingActions.remove();
      }
    }
    return false;
  }

  private Permissive.Action currentAction() {
    return pendingActions.peek();
  }

  private boolean requestPermissions(Permissive.Request request) {
    final Activity activity = request.getContext();
    if (null == activity) {
      return false;
    }
    final String[] permissionsToAsk = request.getRefusedPermissions(activity);
    if (permissionsToAsk.length > 0
        && Permissive.getPermissionsRequiringRationale(activity, permissionsToAsk).length > 0) {

      if (request.shouldDisplayRationaleFirst() && showRationaleForRequest(request)) {
        return true;
      }
      askForPermissions(activity, permissionsToAsk);
      return true;
    }
    return false;
  }

  private void processAction(Permissive.Action action) {
    Context context = action.getContext();
    if (context != null) {
      final int[] grants = getPermissionGrants(context, action.getPermissions());
      processPermissionsResult(new RequestPermissionsResult(action.getPermissions(), grants), false);
    }
  }

  private boolean processPermissionsResult(RequestPermissionsResult result) {
    Permissive.Action action = pendingActions.peek();
    if (action instanceof Permissive.Request) {
      Permissive.Request request = (Permissive.Request) action;
      if (request.shouldDisplayRationale() && showRationaleForRequest(request)) {
        return true;
      }
    }
    Context context = action.getContext();
    if (context != null) {
      final int[] grants = getPermissionGrants(context, action.getPermissions());
      processPermissionsResult(new RequestPermissionsResult(action.getPermissions(), grants), true);
    }
    return false;
  }

  private boolean showRationaleForRequest(Permissive.Request request) {
    Activity activity = request.getContext();
    if (null == activity) {
      return false;
    }
    final String[] rationalePermissions = Permissive.getPermissionsRequiringRationale(activity, request.getPermissions());
    if (rationalePermissions.length == 0) {
      return false;
    }
    final PermissiveMessenger messenger = new PermissiveMessenger(handler);
    return request.showRationale(rationalePermissions, messenger);
  }

  private void processPermissionsResult(RequestPermissionsResult result, boolean removeAction) {
    Permissive.Action action = removeAction ? pendingActions.poll() : pendingActions.peek();
    if (action != null) {
      if (result.grantedPermissions.length > 0) {
        action.firePermissionsGrantedListener(result.grantedPermissions);
      }
      if (result.refusedPermissions.length > 0) {
        action.firePermissionsRefusedListener(result.refusedPermissions);
      }
      action.firePermissionsResultListener(result.grantedPermissions, result.refusedPermissions);
    }
  }

  private static int[] getPermissionGrants(Context context, String[] permissions) {
    final int[] grantResults = new int[permissions.length];

    final int permissionCount = permissions.length;
    for (int i = 0; i < permissionCount; i++) {
      grantResults[i] = Permissive.getPermissionGrant(context, permissions[i]);
    }
    return grantResults;
  }

  private static void askForPermissions(final Activity activity, final String... permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      activity.getFragmentManager().beginTransaction()
          .add(PermissiveFragment.create(permissions), Permissive.PERMISSIVE_FRAGMENT_TAG)
          .commit();
    }
  }

}
