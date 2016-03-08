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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.jksiezni.permissive.Permissive;
import com.github.jksiezni.permissive.fragments.RationaleFragment;

/**
 *
 */
public class EducateUpFrontFragment extends RationaleFragment {

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.educate_up_front_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    final boolean firstTimeAsk = sharedPrefs.getBoolean("first_time_ask", true);

    Button tryItBtn = (Button) view.findViewById(R.id.tryItBtn);
    if (!firstTimeAsk && !isAnyAllowablePermission()) {
      tryItBtn.setText("SETTINGS");
      tryItBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", v.getContext().getPackageName(), null));
          startActivity(i);
        }
      });
    } else {
      view.findViewById(R.id.tryItBtn).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          getPermissiveMessenger().repeatRequest();
          sharedPrefs.edit().putBoolean("first_time_ask", false).apply();
        }
      });
    }

    view.findViewById(R.id.exitBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getActivity().finish();
      }
    });

  }

  @Override
  public void onResume() {
    super.onResume();
    if (Permissive.filterPermissions(getContext(),
            getPermissiveMessenger().getRequestedPermissions(),
            PackageManager.PERMISSION_GRANTED).length > 0) {
      // if user granted permission via Settings, then finish this fragment
      finishFragment();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPermissiveMessenger().cancelRequest();
  }

  @Override
  public void onPermissionsResult(String[] grantedPermissions, String[] refusedPermissions) throws SecurityException {
    if (getActivity() == null || getActivity().isFinishing()) {
      return;
    }
    if (refusedPermissions.length > 0) {
      getActivity().finish();
    } else {
      super.onPermissionsResult(grantedPermissions, refusedPermissions);
    }
  }
}
