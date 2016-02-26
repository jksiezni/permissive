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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Permissive {
  public static final String PERMISSIVE_FRAGMENT_TAG = "permissive_fragment";

  private static final PermissiveHandler permissiveHandler = new PermissiveHandler();
  private static final Map<String, Rationale> globalRationaleMap = new HashMap<>();

  public static void registerGlobalRationale(String permission, Rationale rationale) {
    synchronized (globalRationaleMap) {
      globalRationaleMap.put(permission, rationale);
    }
  }


  private static boolean fireGlobalRationale(Activity activity, String[] permissions, PermissiveMessenger messenger) {
    synchronized (globalRationaleMap) {
      for (String permission : permissions) {
        if (globalRationaleMap.containsKey(permission)) {
          globalRationaleMap.get(permission).onShowRationaleForRequest(activity, new String[]{permission}, messenger);
          return true;
        }
      }
    }
    return false;
  }

  static void dispatchRequestPermissionsResult(String[] permissions, int[] grantResults) {
    permissiveHandler.dispatchRequestPermissionsResult(new RequestPermissionsResult(permissions, grantResults));
  }

  public static int checkPermission(Context context, String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(permission, Process.myPid(), Process.myUid());
  }

  public static String[] filterPermissions(Context context, String[] permissions, int filter) {
    final ArrayList<String> filtered = new ArrayList<>();
    for (int i = 0; i < permissions.length; ++i) {
      if (checkPermission(context, permissions[i]) == filter) {
        filtered.add(permissions[i]);
      }
    }
    return filtered.toArray(new String[filtered.size()]);
  }

  public static String[] getPermissionsRequiringRationale(Activity activity, String[] permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final List<String> rationalePermissions = new ArrayList<>();
      for (int i = 0; i < permissions.length; ++i) {
        if (checkPermission(activity, permissions[i]) == PackageManager.PERMISSION_DENIED
            && activity.shouldShowRequestPermissionRationale(permissions[i])) {
          rationalePermissions.add(permissions[i]);
        }
      }
      return rationalePermissions.toArray(new String[rationalePermissions.size()]);
    }
    return new String[0];
  }


  public static class Action<T extends Context> {

    private final String[] permissions;
    private PermissionsGrantedListener permissionsGrantedListener;
    private PermissionsRefusedListener permissionsRefusedListener;


    private WeakReference<T> activityRef;

    public Action(String... permissions) {
      this.permissions = permissions;
    }

    public Action<T> whenPermissionsGranted(PermissionsGrantedListener listener) {
      this.permissionsGrantedListener = listener;
      return this;
    }

    public Action<T> whenPermissionsRefused(PermissionsRefusedListener listener) {
      this.permissionsRefusedListener = listener;
      return this;
    }

    public String[] getPermissions() {
      return permissions;
    }

    public T getContext() {
      return activityRef.get();
    }

    public void execute(T context) {
      if (context == null) {
        throw new IllegalArgumentException("context is null");
      }
      activityRef = new WeakReference<>(context);
      permissiveHandler.enqueueAction(this);
    }

    protected void firePermissionsGrantedListener(String[] grantedPermissions) {
      if (permissionsGrantedListener != null) {
        permissionsGrantedListener.onPermissionsGranted(grantedPermissions);
      }
    }

    protected void firePermissionsRefusedListener(String[] refusedPermissions) {
      if (permissionsRefusedListener != null) {
        permissionsRefusedListener.onPermissionsRefused(refusedPermissions);
      }
    }

    public String[] getRefusedPermissions(T context) {
      if (context == null) {
        throw new IllegalArgumentException("context is null");
      }
      return filterPermissions(context, permissions, PackageManager.PERMISSION_DENIED);
    }

  }

  public static class Request extends Action<Activity> {

    private Rationale rationale;

    private boolean shouldDisplayRationale;
    private boolean showRationaleFirst;

    public Request(String... permissions) {
      super(permissions);
    }

    public Request withRationale(Rationale rationale) {
      this.rationale = rationale;
      return this;
    }

    public Request showRationaleFirst(boolean enable) {
      showRationaleFirst = enable;
      return this;
    }

    public boolean shouldDisplayRationale() {
      return shouldDisplayRationale;
    }

    public boolean shouldDisplayRationaleFirst() {
      return showRationaleFirst && shouldDisplayRationale;
    }

    @Override
    public void execute(Activity context) {
      shouldDisplayRationale = true;
      super.execute(context);
    }

    protected boolean showRationale(String[] permissions, PermissiveMessenger messenger) {
      shouldDisplayRationale = false;
      if (rationale != null) {
        rationale.onShowRationaleForRequest(getContext(), permissions, messenger);
        return true;
      }
      // show globally registered rationale, if any
      return Permissive.fireGlobalRationale(getContext(), permissions, messenger);
    }

  }

}
