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

package com.github.jksiezni.permissive.testing;

import android.content.pm.PackageManager;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A collection of hamcrest matchers that match runtime permissions.
 */
public class PermissiveMatchers {

  /**
   * Returns a matcher that matches permissions that are granted.
   */
  public static Matcher<String> granted() {
    return new TypeSafeMatcher<String>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("permission granted");
      }

      @Override
      protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendValue(item).appendText(" was denied");
      }

      @Override
      protected boolean matchesSafely(String item) {
        return PermissiveTesting.checkFakePermission(item) == PackageManager.PERMISSION_GRANTED;
      }

    };
  }

  /**
   * Returns a matcher that matches permissions that are denied.
   */
  public static Matcher<String> denied() {
    return new TypeSafeMatcher<String>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("permission denied");
      }

      @Override
      protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendValue(item).appendText(" was granted");
      }

      @Override
      protected boolean matchesSafely(String item) {
        return PermissiveTesting.checkFakePermission(item) == PackageManager.PERMISSION_DENIED;
      }

    };
  }
}
