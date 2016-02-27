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

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import com.github.jksiezni.permissive.Permissive;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

  static {
    Permissive.registerGlobalRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, (activity, permissions, messenger) -> {
      new AlertDialog.Builder(activity)
          .setTitle("Storage Rationale")
          .setMessage("Storage is required to access files on your device.")
          .setPositiveButton(android.R.string.ok, (dialog, which) -> {
            messenger.repeatPermissionsRequest();
          })
          .setOnDismissListener(dialog1 -> messenger.cancelPermissionsRequest())
          .show();
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void askForLocationPermission(View view) {
    new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .showRationaleFirst(true)
        .withRationale((activity, permissions, messenger) -> {
          new AlertDialog.Builder(activity)
              .setTitle("LOCATION Rationale")
              .setMessage("Location is required to track your position via GPS.")
              .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                messenger.repeatPermissionsRequest();
              })
              .setOnDismissListener(dialog1 -> messenger.cancelPermissionsRequest())
              .show();

        })
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
  }

  public void askForStoragePermission(View view) {
    new Permissive.Action<>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
  }

  public void askForAllPermissions(View view) {
    new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION)
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
  }

  private void onPermissionsGranted(String[] permissions) throws SecurityException {
    Toast.makeText(MainActivity.this,
        Html.fromHtml("Permission <font color=green>GRANTED</font> for " + Arrays.toString(permissions)),
        Toast.LENGTH_SHORT)
        .show();
  }

  private void onPermissionsRefused(String[] permissions) throws SecurityException {
    Toast.makeText(MainActivity.this,
        Html.fromHtml("Permission <font color=red>REFUSED</font> for " + Arrays.toString(permissions)),
        Toast.LENGTH_SHORT)
        .show();
  }

}

