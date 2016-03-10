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

/**
 * The interface for displaying a rationale to the user.
 */
public interface Rationale {

  /**
   * The method provides an entry point to displaying a rationale to the user.
   *
   * Each rationale is bound to the request via {@link PermissiveMessenger},
   * which allows to control request's flow while a rationale is displayed.
   *
   * Thus, the developer is responsible for preserving {@code messenger},
   * until the request is <i>canceled</i> or <i>repeated</i>.
   *
   * <p><b>Note:</b> <i>Make sure, that you always invoke one of {@link PermissiveMessenger#cancelRequest()}
   * or {@link PermissiveMessenger#repeatRequest()} methods when you are finished with the rationale.
   * Otherwise, Permissive library won't be able to work!</i></p>
   *
   * @param activity Current activity context.
   * @param allowablePermissions An array of permissions, that still can be changed by the user. It corresponds to 'never ask again' feature.
   * @param messenger Provides methods for sending important messages related to current request.
   */
  void onShowRationale(Activity activity, String[] allowablePermissions, PermissiveMessenger messenger);
}
