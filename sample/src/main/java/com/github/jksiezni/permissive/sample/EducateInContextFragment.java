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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.github.jksiezni.permissive.fragments.RationaleDialogFragment;

/**
 *
 */
public class EducateInContextFragment extends RationaleDialogFragment implements DialogInterface.OnClickListener {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity(), R.style.EducateInContextDialogTheme)
        .setTitle("Educate in context...")
        .setMessage("Location is required to track your position via GPS.")
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.no, this)
        .create();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    switch (which) {
      case DialogInterface.BUTTON_POSITIVE:
        getPermissiveMessenger().repeatRequest();
        break;
      case DialogInterface.BUTTON_NEGATIVE:
        getPermissiveMessenger().cancelRequest();
        break;
    }
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    super.onDismiss(dialog);
    getPermissiveMessenger().cancelRequest();
  }
}
