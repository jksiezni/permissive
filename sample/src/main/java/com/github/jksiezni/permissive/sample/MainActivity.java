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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.jksiezni.permissive.Permissive;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

  static {
    Permissive.registerGlobalRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskUpFrontFragment());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(getClass().getSimpleName(), "onCreate(): " + savedInstanceState);
    setContentView(R.layout.activity_main);

    if (null == savedInstanceState) {
      new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          .withRationale(new EducateUpFrontFragment())
          .showRationaleFirst(true)
          .execute(this);
    } else {
      new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          .showRationaleFirst(true)
          .execute(this);
    }
  }

  public void askForCameraPermission(View view) {
    new Permissive.Request(Manifest.permission.CAMERA)
        .withRationale(new AskInContextFragment())
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
  }

  public void educateForLocationPermission(View view) {
    new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .showRationaleFirst(true)
        .withRationale(new EducateInContextFragment())
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
  }

  public void openSettings(View view) {
    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
    startActivity(i);
  }

  private void onPermissionsGranted(String[] permissions) throws SecurityException {
    Toast.makeText(MainActivity.this,
        Html.fromHtml("Permission <font color=green>GRANTED</font> for " + Arrays.toString(permissions)),
        Toast.LENGTH_SHORT)
        .show();
  }

  private void onPermissionsRefused(String[] permissions) {
    Toast.makeText(MainActivity.this,
        Html.fromHtml("Permission <font color=red>REFUSED</font> for " + Arrays.toString(permissions)),
        Toast.LENGTH_SHORT)
        .show();
  }

}

