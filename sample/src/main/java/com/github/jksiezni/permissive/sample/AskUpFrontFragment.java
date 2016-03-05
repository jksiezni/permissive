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
package com.github.jksiezni.permissive.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.jksiezni.permissive.fragments.RationaleFragment;

/**
 *
 */
public class AskUpFrontFragment extends RationaleFragment {

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    getPermissiveMessenger().repeatRequest();
    return inflater.inflate(R.layout.ask_up_front_fragment, container, false);
  }

  @Override
  public void onPermissionsResult(String[] grantedPermissions, String[] refusedPermissions) throws SecurityException {
    super.onPermissionsResult(grantedPermissions, refusedPermissions);
    if (refusedPermissions.length > 0) {
      getActivity().finish();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPermissiveMessenger().cancelRequest();
  }
}
