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
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <b>Permissive</b> class provides core API methods to the whole library.
 * Usually, you will start from here when working with the library.
 *
 * Also, it provides two nested classes {@link Action} and {@link Request},
 * which together allow to build a chain of synchronous requests and actions.
 *
 * @see Action
 * @see Request
 */
public final class Permissive {

  /**
   * A tag used to identify the {@link PermissiveFragment}.
   */
  public static final String PERMISSIVE_FRAGMENT_TAG = "com.github.jksiezni.permissive.request_fragment";

  private static final PermissiveHandler permissiveHandler = new PermissiveHandler();
  private static final Map<String, Rationale> globalRationaleMap = new HashMap<>();

  private Permissive() { /* never instantiated */ }

  /**
   * Registers a global rationale for a given permission.
   *
   * This rationale will be used every time, the {@link Request} with the same
   * permission is executed.
   *
   * <p>The best place to use this method is during app initialization.
   * For example in {@link Application#onCreate()}</p>
   *
   * <p>It can be really useful for commonly used permissions in the app.</p>
   *
   * @param permission One of permissions from {@link android.Manifest.permission}
   * @param rationale A rationale that will be used
   */
  public static void registerGlobalRationale(String permission, Rationale rationale) {
    synchronized (globalRationaleMap) {
      globalRationaleMap.put(permission, rationale);
    }
  }

  private static boolean fireGlobalRationale(Activity activity, String[] permissions, PermissiveMessenger messenger) {
    synchronized (globalRationaleMap) {
      for (String permission : permissions) {
        if (globalRationaleMap.containsKey(permission)) {
          globalRationaleMap.get(permission).onShowRationale(activity, new String[]{permission}, messenger);
          return true;
        }
      }
    }
    return false;
  }

  static int getPermissionGrant(Context context, String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(permission, Process.myPid(), Process.myUid());
  }

  /**
   * Checks whether you have been granted a particular permission.
   * This method is similar to {@link Context#checkSelfPermission(String)},
   * but returns a boolean value instead of integer value.
   *
   * @param context Provide a context. Can't be {@code null}.
   * @param permission A permission that should be checked. Can't be {@code null}.
   * @return {@code true} when permission is granted, otherwise {@code false}.
   */
  public static boolean checkPermission(Context context, String permission) {
    return getPermissionGrant(context, permission) == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Filters all provided permissions and returns only granted or denied.
   *
   * @param context Provide a context. Can't be {@code null}.
   * @param permissions Permissions that should be checked. Can't be {@code null}.
   * @param filter One of: {@link PackageManager#PERMISSION_GRANTED} or {@link PackageManager#PERMISSION_DENIED}
   * @return Permissions that match the given filter flag.
   */
  public static String[] filterPermissions(Context context, String[] permissions, int filter) {
    final ArrayList<String> filtered = new ArrayList<>();
    for (String permission : permissions) {
      if (getPermissionGrant(context, permission) == filter) {
        filtered.add(permission);
      }
    }
    return filtered.toArray(new String[filtered.size()]);
  }

  /**
   * @param activity An Activity is required here.
   * @param permissions An array of permissions to be checked.
   * @return An array of permissions that may require a rationale to be shown.
   */
  static String[] getPermissionsRequiringRationale(Activity activity, String[] permissions) {
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

  /**
   * The Action class is designed to perform tasks when a given set of permissions are granted.
   * It does not requests any permissions from user, but only checks existing permissions status.
   *
   * <p>Each action is enqueued and executed on a main thread, so keep in mind to not add time
   * consuming tasks, like accessing storage, networking, etc.</p>
   *
   * @param <T> A type of {@link Context} in which an action will be executed.
   */
  public static class Action<T extends Context> {

    private final String[] permissions;
    private WeakReference<PermissionsGrantedListener> permissionsGrantedRef;
    private WeakReference<PermissionsRefusedListener> permissionsRefusedRef;
    private WeakReference<PermissionsResultListener> permissionsResultRef;

    protected WeakReference<T> activityRef;

    /**
     * Constructs a new Action.
     *
     * @param permissions  A list of permissions, that are required by the action.
     */
    public Action(String... permissions) {
      this.permissions = permissions;
    }

    /**
     * Registers a callback for granted permissions.
     * The callback is called when at least one permission has been granted.
     *
     * @param listener  A listener object. Keep it's reference somewhere, otherwise it will be GCed.
     * @return {@code this} object, for method chaining.
     */
    public Action<T> whenPermissionsGranted(PermissionsGrantedListener listener) {
      this.permissionsGrantedRef = new WeakReference<>(listener);
      return this;
    }

    /**
     * Registers a callback for refused permissions.
     * The callback is called when at least one permission has been refused.
     *
     * @param listener  A listener object. Keep it's reference somewhere, otherwise it will be GCed.
     * @return {@code this} object, for method chaining.
     */
    public Action<T> whenPermissionsRefused(PermissionsRefusedListener listener) {
      this.permissionsRefusedRef = new WeakReference<>(listener);
      return this;
    }

    /**
     * Registers a callback for both granted and refused permissions.
     * The callback is always called and provides a combined result of checking permissions.
     *
     * @param listener  A listener object. Keep it's reference somewhere, otherwise it will be GCed.
     * @return {@code this} object, for method chaining.
     */
    public Action<T> whenPermissionsResultReceived(PermissionsResultListener listener) {
      this.permissionsResultRef = new WeakReference<>(listener);
      return this;
    }

    /**
     * Gets permissions that were provided during initialization.
     * @return An array of requested permissions.
     */
    public String[] getPermissions() {
      return permissions;
    }

    /**
     * Gets a context that was provided when this Action was executed.
     * @return The context.
     */
    public T getContext() {
      return activityRef.get();
    }

    public PermissionsGrantedListener getPermissionsGrantedListener() {
      return permissionsGrantedRef != null ? permissionsGrantedRef.get() : null;
    }

    public PermissionsRefusedListener getPermissionsRefusedListener() {
      return permissionsRefusedRef != null ? permissionsRefusedRef.get() : null;
    }

    public PermissionsResultListener getPermissionsResultListener() {
      return permissionsResultRef != null ? permissionsResultRef.get() : null;
    }

    /**
     * Executes this Action with a given context.
     *
     * <p>Keep in mind, that if the context disappears (for example when an Activity is finished),
     * then the Action will not be executed.</p>
     *
     * @param context  The context which is saved as weak reference.
     */
    public void execute(T context) {
      if (context == null) {
        throw new IllegalArgumentException("context is null");
      }
      activityRef = new WeakReference<>(context);
      permissiveHandler.enqueueAction(this);
    }

    protected void firePermissionsGrantedListener(String[] grantedPermissions) {
      final PermissionsGrantedListener listener = getPermissionsGrantedListener();
      if (listener != null) {
        listener.onPermissionsGranted(grantedPermissions);
      }
    }

    protected void firePermissionsRefusedListener(String[] refusedPermissions) {
      final PermissionsRefusedListener listener = getPermissionsRefusedListener();
      if (listener != null) {
        listener.onPermissionsRefused(refusedPermissions);
      }
    }

    protected void firePermissionsResultListener(String[] grantedPermissions, String[] refusedPermissions) {
      final PermissionsResultListener listener = getPermissionsResultListener();
      if (listener != null) {
        listener.onPermissionsResult(grantedPermissions, refusedPermissions);
      }
    }

    /**
     * Quickly provides an array of refused permissions from a list of permissions provided during initialization.
     *
     * @param context  The context to be used.
     * @return An array of refused permissions.
     */
    public String[] getRefusedPermissions(Context context) {
      if (context == null) {
        throw new IllegalArgumentException("context is null");
      }
      return filterPermissions(context, permissions, PackageManager.PERMISSION_DENIED);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()) +
          '{' +
          Arrays.toString(permissions) +
          ", pGrantedListener=" + getPermissionsGrantedListener() +
          ", pRefusedListener=" + getPermissionsRefusedListener() +
          ", pResultListener=" + getPermissionsResultListener() +
          '}';
    }
  }

  /**
   * Allows to build a request where you can ask user for dangerous permissions.
   * It's designed in a way, to create a user friendly and repetitive requests,
   * that can be easily used in any context or {@link Activity} to be more specific.
   *
   * <p>Each {@code Request} is enqueued and executed on a main thread, so keep in mind to not add time
   * consuming tasks, like accessing storage, networking, etc.</p>
   */
  public static class Request extends Action<Activity> {

    private WeakReference<Rationale> rationaleRef;

    private boolean shouldDisplayRationale = true;
    private boolean showRationaleFirst = false;
    final boolean rebuild;

    /**
     * Constructs a new Request.
     *
     * @param permissions  A list of permissions, that are required by the request.
     */
    public Request(String... permissions) {
      this(false, permissions);
    }

    /* Inner constructor used to rebuild the Request. See PermissiveMessenger for details. */
    Request(boolean rebuild, String[] permissions) {
      super(permissions);
      this.rebuild = rebuild;
    }

    /**
     * Registers a callback for rationale handling.
     * The callback is called when a rationale should be presented to the user.
     *
     * @param rationale  A listener object. Keep it's reference somewhere, otherwise it will be GCed.
     * @return {@code this} object, for method chaining.
     */
    public Request withRationale(Rationale rationale) {
      this.rationaleRef = new WeakReference<>(rationale);
      return this;
    }

    /**
     * Forces the rationale to be shown first, before requesting any permissions.
     *
     * @param enable When {@code true}, show the rationale first. Default is {@code false}.
     * @return {@code this} object, for method chaining.
     */
    public Request showRationaleFirst(boolean enable) {
      showRationaleFirst = enable;
      return this;
    }

    /**
     * @return {@code true} if a rationale should be displayed for this request.
     */
    public boolean shouldDisplayRationale() {
      return shouldDisplayRationale;
    }

    /**
     * @return {@code true} if a rationale should be displayed first for this request.
     */
    public boolean shouldDisplayRationaleFirst() {
      return showRationaleFirst && shouldDisplayRationale;
    }

    /**
     * A package protected method to override status of displayed rationale.
     * In rare cases it's reasonable to show rationale more than once.
     *
     * @see PermissiveMessenger#repeatRequest(boolean)
     *
     * @param displayRationale  {@code true} when rationale should be displayed. The value is reset once the rationale is displayed.
     */
    void shouldDisplayRationale(boolean displayRationale) {
      this.shouldDisplayRationale = displayRationale;
    }

    /**
     * Executes this Request with a given Activity context.
     *
     * <p>Keep in mind, that if the activity disappears, then the Request will not be executed.</p>
     *
     * @param activity  The Activity context which is saved as weak reference.
     */
    @Override
    public void execute(Activity activity) {
      super.execute(activity);
    }

    /**
     * @return a Rationale listener, registered with {@linkplain #withRationale(Rationale)}.
     */
    public Rationale getRationale() {
      return rationaleRef != null ? rationaleRef.get() : null;
    }

    @Override
    public String toString() {
      String str = super.toString();
      return str.substring(0, str.length() - 1) +
          ", rationaleListener=" + getRationale()
          + '}';
    }

    protected boolean showRationale(String[] permissions, PermissiveMessenger messenger) {
      shouldDisplayRationale = false;
      Rationale rationale;
      if (rationaleRef != null && (rationale = rationaleRef.get()) != null) {
        rationale.onShowRationale(getContext(), permissions, messenger);
        return true;
      }
      // show globally registered rationale, if any
      return Permissive.fireGlobalRationale(getContext(), permissions, messenger);
    }

    void updateActivityRef(Activity activity) {
      activityRef = new WeakReference<>(activity);
    }
  }

}
