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
public final class Permissive {
  public static final String PERMISSIVE_FRAGMENT_TAG = "permissive_fragment";

  private static final PermissiveHandler permissiveHandler = new PermissiveHandler();
  private static final Map<String, Rationale> globalRationaleMap = new HashMap<>();

  private Permissive() { /* never instantiated */ }

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

  static boolean restoreActivityToCurrentRequest(Activity activity) {
    return permissiveHandler.restoreActivityToCurrentRequest(activity);
  }

  static int getPermissionGrant(Context context, String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(permission, Process.myPid(), Process.myUid());
  }

  public static boolean checkPermission(Context context, String permission) {
    return getPermissionGrant(context, permission) == PackageManager.PERMISSION_GRANTED;
  }

  public static String[] filterPermissions(Context context, String[] permissions, int filter) {
    final ArrayList<String> filtered = new ArrayList<>();
    for (String permission : permissions) {
      if (getPermissionGrant(context, permission) == filter) {
        filtered.add(permission);
      }
    }
    return filtered.toArray(new String[filtered.size()]);
  }

  public static String[] getPermissionsRequiringRationale(Activity activity, String[] permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final List<String> rationalePermissions = new ArrayList<>();
      for (String permission : permissions) {
        if (getPermissionGrant(activity, permission) == PackageManager.PERMISSION_DENIED
            && activity.shouldShowRequestPermissionRationale(permission)) {
          rationalePermissions.add(permission);
        }
      }
      return rationalePermissions.toArray(new String[rationalePermissions.size()]);
    }
    return new String[0];
  }


  public static class Action<T extends Context> {

    private final String[] permissions;
    private WeakReference<PermissionsGrantedListener> permissionsGrantedRef;
    private WeakReference<PermissionsRefusedListener> permissionsRefusedRef;
    private WeakReference<PermissionsResultListener> permissionsResultRef;

    protected WeakReference<T> activityRef;

    public Action(String... permissions) {
      this.permissions = permissions;
    }

    public Action<T> whenPermissionsGranted(PermissionsGrantedListener listener) {
      this.permissionsGrantedRef = new WeakReference<>(listener);
      return this;
    }

    public Action<T> whenPermissionsRefused(PermissionsRefusedListener listener) {
      this.permissionsRefusedRef = new WeakReference<>(listener);
      return this;
    }

    public Action<T> whenGotPermissionsResult(PermissionsResultListener listener) {
      this.permissionsResultRef = new WeakReference<>(listener);
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
      final PermissionsGrantedListener listener = permissionsGrantedRef != null ? permissionsGrantedRef.get() : null;
      if (listener != null) {
        listener.onPermissionsGranted(grantedPermissions);
      }
    }

    protected void firePermissionsRefusedListener(String[] refusedPermissions) {
      final PermissionsRefusedListener listener = permissionsRefusedRef != null ? permissionsRefusedRef.get() : null;
      if (listener != null) {
        listener.onPermissionsRefused(refusedPermissions);
      }
    }

    protected void firePermissionsResultListener(String[] grantedPermissions, String[] refusedPermissions) {
      final PermissionsResultListener listener = permissionsResultRef != null ? permissionsResultRef.get() : null;
      if (listener != null) {
        listener.onPermissionsResult(grantedPermissions, refusedPermissions);
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

    private WeakReference<Rationale> rationaleRef;

    private boolean shouldDisplayRationale;
    private boolean showRationaleFirst;

    public Request(String... permissions) {
      super(permissions);
    }

    public Request withRationale(Rationale rationale) {
      this.rationaleRef = new WeakReference<>(rationale);
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
    public void execute(Activity activity) {
      shouldDisplayRationale = true;
      super.execute(activity);
    }

    void updateActivityRef(Activity activity) {
      activityRef = new WeakReference<>(activity);
    }

    protected boolean showRationale(String[] permissions, PermissiveMessenger messenger) {
      shouldDisplayRationale = false;
      Rationale rationale;
      if (rationaleRef != null && (rationale = rationaleRef.get()) != null) {
        rationale.onShowRationaleForRequest(getContext(), permissions, messenger);
        return true;
      }
      // show globally registered rationale, if any
      return Permissive.fireGlobalRationale(getContext(), permissions, messenger);
    }

  }

}
