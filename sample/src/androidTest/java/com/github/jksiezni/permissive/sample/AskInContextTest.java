/*
 *    Copyright 2017 Jakub Księżniak
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
import android.support.test.runner.AndroidJUnit4;

import com.github.jksiezni.permissive.testing.PermissiveTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.github.jksiezni.permissive.testing.PermissiveMatchers.denied;
import static com.github.jksiezni.permissive.testing.PermissiveMatchers.granted;
import static com.github.jksiezni.permissive.testing.PermissiveTesting.onPermissionRequest;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 */
@RunWith(AndroidJUnit4.class)
public class AskInContextTest {

  @Rule
  public PermissiveTestRule<MainActivity> mActivityTestRule = new PermissiveTestRule<>(MainActivity.class)
      .granted(Manifest.permission.WRITE_EXTERNAL_STORAGE);

  @Test
  public void grant_permission() {
    assertThat(Manifest.permission.CAMERA, is(denied()));
    onPermissionRequest(Manifest.permission.CAMERA).grant();

    onView(withId(R.id.askInContextButton))
        .check(matches(isDisplayed()))
        .perform(click());

    assertThat(Manifest.permission.CAMERA, is(granted()));
  }

  @Test
  public void deny_permission_and_show_rationale() {
    assertThat(Manifest.permission.CAMERA, is(denied()));
    onPermissionRequest(Manifest.permission.CAMERA).revoke();

    onView(withId(R.id.askInContextButton))
        .check(matches(isDisplayed()))
        .perform(click());

    onView(withText("Ask in context..."))
        .check(matches(isDisplayed()));

    onView(withText(android.R.string.ok))
        .check(matches(isDisplayed()))
        .perform(click());

    assertThat(Manifest.permission.CAMERA, is(denied()));
  }
}
