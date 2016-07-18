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
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * One of core classes, that implements a state machine for permissions handling.
 */
class PermissiveHandler {
  private static final String TAG = PermissiveHandler.class.getSimpleName();
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int REQUEST_PERMISSIONS = 1;

  static final int PERMISSIONS_RESULT = 2;
  static final int RESTORE_ACTIVITY = 3;
  static final int REPEAT_REQUEST = 4;
  static final int CANCEL_REQUEST = 5;
  static final int UPDATE_LISTENER = 6;

  static String getMessageString(int what) {
    switch (what) {
      case REQUEST_PERMISSIONS:
        return "REQUEST_PERMISSIONS";
      case PERMISSIONS_RESULT:
        return "PERMISSIONS_RESULT";
      case RESTORE_ACTIVITY:
        return "RESTORE_ACTIVITY";
      case REPEAT_REQUEST:
        return "REPEAT_REQUEST";
      case CANCEL_REQUEST:
        return "CANCEL_REQUEST";
      case UPDATE_LISTENER:
        return "UPDATE_LISTENER";
      default:
        return "Unknown msg: " + what;
    }
  }

  private final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
    private Permissive.Action currentAction = null;
    private final Queue<Permissive.Action> pendingActions = new LinkedList<>();

    @Override
    public boolean handleMessage(Message msg) {
      if(DEBUG) {
        Log.v(TAG, "handleMessage: " + getMessageString(msg.what));
      }
      switch (msg.what) {
        case REQUEST_PERMISSIONS:
          pendingActions.add((Permissive.Action) msg.obj);
          if(DEBUG) {
            dumpPendingActions();
          }
          if (currentAction == null) {
            currentAction = processPendingActions();
          }
          break;
        case PERMISSIONS_RESULT:
          if (currentAction == null) {
            Log.e(TAG, "Unable to process result for non-existent action.");
            // FIXME: find out when it happens
            break;
          }
          if (!processPermissionsResultFor(currentAction, (RequestPermissionsResult) msg.obj)) {
            currentAction = processPendingActions();
          }
          break;
        case REPEAT_REQUEST:
          if (currentAction instanceof Permissive.Request) {
            final Permissive.Request request = (Permissive.Request) currentAction;
            request.shouldDisplayRationale(msg.arg1 > 0);
            if (!requestPermissions(request)) {
              processAction(currentAction);
              currentAction = processPendingActions();
            }
          }
          break;
        case CANCEL_REQUEST:
          if (currentAction == null) {
            Log.e(TAG, "Unable to cancel a non-existent action.");
            // TODO: add identifiers to running actions, so incoming messages can be matched with actions
            break;
          }
          processAction(currentAction);
          currentAction = processPendingActions();
          break;
        case RESTORE_ACTIVITY:
          if (currentAction instanceof Permissive.Request) {
            Permissive.Request request = (Permissive.Request) currentAction;
            request.updateActivityRef((Activity) msg.obj);
          }
          break;
        case UPDATE_LISTENER:
          if (currentAction == null) {
            Log.e(TAG, "Unable to update listener for non-existent action: " + msg.obj);
            // TODO: add identifiers to running actions, so incoming messages can be matched with actions
            break;
          }
          if (msg.obj instanceof PermissionsResultListener) {
            currentAction.whenPermissionsResultReceived((PermissionsResultListener) msg.obj);
          }
          break;
      }
      return true;
    }

    private void dumpPendingActions() {
      StringBuilder builder = new StringBuilder("current) " + currentAction + "\n");
      int count = 0;
      for (Permissive.Action action : pendingActions) {
        builder.append(++count);
        builder.append(") ");
        builder.append(action.toString());
        builder.append('\n');
      }
      Log.v(TAG, builder.toString());
    }

    private Permissive.Action processPendingActions() {
      Permissive.Action action;
      while ((action = pendingActions.poll()) != null) {
        if(DEBUG) {
          Log.v(TAG, "processing: " + action);
        }
        if (action instanceof Permissive.Request
            && requestPermissions((Permissive.Request) action)) {
          return action;
        } else {
          processAction(action);
        }
      }
      return null;
    }
  });

  void enqueueAction(Permissive.Action action) {
    handler.obtainMessage(REQUEST_PERMISSIONS, action).sendToTarget();
  }

  private boolean requestPermissions(Permissive.Request request) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false;
    }

    final Activity activity = request.getContext();
    if (!isValidActivity(activity)) {
      return false;
    }
    if(DEBUG) {
      Log.v(TAG, "requestPermissions(): " + request);
    }

    final String[] permissionsToAsk = request.getRefusedPermissions(activity);
    if (permissionsToAsk.length > 0) {
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
      fireActionCallbacks(action, new RequestPermissionsResult(action.getPermissions(), grants));
    }
  }

  private boolean processPermissionsResultFor(Permissive.Action action, RequestPermissionsResult result) {
    if (action instanceof Permissive.Request) {
      Permissive.Request request = (Permissive.Request) action;
      if (result.hasAnyRefusedPermissions() && showRationaleForRequest(request)) {
        return true;
      }
    } else {
      Log.e(TAG, "No request could receive result: " + action);
    }
    processAction(action);
    return false;
  }

  private boolean showRationaleForRequest(Permissive.Request request) {
    Activity activity = request.getContext();
    if (null == activity) {
      return false;
    }
    final String[] rationalePermissions = Permissive.getPermissionsRequiringRationale(activity, request.getPermissions());
    if (request.shouldDisplayRationale()) {
      final PermissiveMessenger messenger = new PermissiveMessenger(handler, request.getPermissions());
      return request.showRationale(rationalePermissions, messenger) && !request.rebuild;
    }
    return false;
  }

  private void fireActionCallbacks(Permissive.Action action, RequestPermissionsResult result) {
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

  @TargetApi(Build.VERSION_CODES.M)
  private boolean isValidActivity(Activity activity) {
    return activity != null && !activity.isDestroyed() && !activity.isFinishing();
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void askForPermissions(final Activity activity, final String... permissions) {
    final FragmentManager fm = activity.getFragmentManager();
    PermissiveFragment frag = (PermissiveFragment) fm.findFragmentByTag(Permissive.PERMISSIVE_FRAGMENT_TAG);
    if (frag != null) {
      Log.e(TAG, "A previous request fragment still exists!");
      frag.setMessenger(new Messenger(handler));
      return;
    }
    fm.beginTransaction()
        .add(PermissiveFragment.create(permissions, handler), Permissive.PERMISSIVE_FRAGMENT_TAG)
        .commit();
  }

}
