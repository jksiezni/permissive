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

import android.content.pm.PackageManager;

import java.util.ArrayList;

/**
 *
 */
class RequestPermissionsResult {

  final String[] grantedPermissions;
  final String[] refusedPermissions;

  RequestPermissionsResult(String[] permissions, int[] grantResults) {
    if(permissions.length != grantResults.length) {
      throw new IllegalArgumentException("Both arrays of permissions and grantResults must have equal lengths.");
    }
    ArrayList<String> granted = new ArrayList<>();
    ArrayList<String> refused = new ArrayList<>();
    for (int i = 0; i < permissions.length; ++i) {
      if (PackageManager.PERMISSION_GRANTED == grantResults[i]) {
        granted.add(permissions[i]);
      } else {
        refused.add(permissions[i]);
      }
    }
    grantedPermissions = granted.toArray(new String[granted.size()]);
    refusedPermissions = refused.toArray(new String[refused.size()]);
  }

  boolean hasAnyRefusedPermissions() {
    return refusedPermissions.length > 0;
  }
}
