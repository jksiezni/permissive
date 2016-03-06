[![Release](https://jitpack.io/v/jksiezni/permissive.svg)](https://jitpack.io/#jksiezni/permissive)

# Permissive

Permissive is a lightweight, yet powerful Android library, that helps you restore control over permissions handling introduced in Android Marshmallow (API 23).

The _Permissive_ library perfectly fits into [Material Design - Permissions](https://www.google.com/design/spec/patterns/permissions.html) pattern, by providing a simple API to accomplish tasks requiring sensitive permissions.

<img src="/../gh-pages/images/screenshots/educate_up_front.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/ask_up_front.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/educate_in_context.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/ask_in_context.png?raw=true" width="24%"/>

All screenshots were taken from __sample__ app, which provides exemplary implementation of popular use-cases using the library.

### Features

* Backward compatibility down to Android API 8 (not tested on earlier versions)
* __Java8__ ready (supports __retrolambda__ expressions)
* Thread-safe API
* Queueing __permissive__ ```Requests``` and ```Actions```
* Not using transparent *magic* Activities
* No messing with your Activities (well, actually adds a fragment, but it should not bother you in most cases)
* No additional dependencies
* No code generation

## Getting started

### Setup
- Add it in your `build.gradle` at the end of repositories:
```gradle
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
```
- Add the core dependency:
```gradle
	dependencies {
		compile 'com.github.jksiezni.permissive:permissive:+'
	}
```
- (Optional) Add an extra library with helper fragments:
```gradle
	dependencies {
		// includes helper fragments based on Android Support v4 implementation (minSdkVersion 8)
		compile 'com.github.jksiezni.permissive:permissive-fragments:+'
	}
```
or
```gradle
	dependencies {
		// includes helper fragments based on native implementation (minSdkVersion 11)
		compile 'com.github.jksiezni.permissive:permissive-fragments-v13:+'
	}
```

### Usage

#### Requesting permissions
```Permissive.Request``` allows to simply ask user for a permission and then (if allowed) do the task.
To request a permission you just need to create and execute ```Permissive.Request```:
```java
new Permissive.Request(Manifest.permission.ACCESS_FINE_LOCATION).execute(getActivity());
```

Then, you can add callback listeners, that return results of the request. You can also ask for more permissions at once:
```java
    new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .whenPermissionsGranted(new PermissionsGrantedListener() {
          @Override
          public void onPermissionsGranted(String[] permissions) throws SecurityException {
            // given permissions are granted
          }
        })
        .whenPermissionsRefused(new PermissionsRefusedListener() {
          @Override
          public void onPermissionsRefused(String[] permissions) {
            // given permissions are refused
          }
        })
        .execute(getActivity());
```

#### Providing rationale
There are two ways to provide a rationale for a requested permission:

- Register a global ```Rationale```, which is executed every time, the permission request is denied:

  **Note**: *Use ```PermissiveMessenger``` repeatPermissionsRequest() or cancelPermissionsRequest() methods to ask again for permissions or cancel ongoing request.
  **Note 2**: If you forget to call one of ```PermissiveMessenger``` methods, then no more ```Requests``` or ```Actions``` will be processed.*
```java
    Permissive.registerGlobalRationale(Manifest.permission.ACCESS_FINE_LOCATION, new Rationale() {
      @Override
      public void onShowRationaleForRequest(Activity activity, String[] permissions, PermissiveMessenger messenger) {
        new AlertDialog.Builder(activity)
          .setTitle("LOCATION Rationale")
          .setMessage("Location is required to track your position via GPS.")
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              messenger.repeatPermissionsRequest();
            }
          })
          .setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog1) {
              messenger.cancelPermissionsRequest();
            }
          })
          .show();
      }
    });
```

- Add ```Rationale``` to the ```Permissive.Request```:
```java
    new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
      .withRationale(new Rationale() {
        @Override
        public void onShowRationaleForRequest(Activity activity, String[] permissions, PermissiveMessenger messenger) {
          // show rationale in a dialog, fragment, activity, etc.
        }
      })
      .execute(getActivity());
```

#### Running a ```Permissive.Action```

The ```Permissive.Action``` simply checks what permissions are granted or refused to the app. So, the main difference from ```Permissive.Request``` is that it doesn't show any dialogs to the user.
Also, it is executed in a queue as every other ```Permissive.Request```, what provides a big advantage when used with requests. In effect, every action will wait until other Actions or Requests are completed.
Actions can be used in background tasks (like _Services_), where no Activity exists, but a sensitive permission is still required.

**Note**: *It's reasonable to not present the request dialog out of blue when running in background, but instead handle denied permission.*
```java
    // here using Java 8 lambdas
    new Permissive.Action<>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .whenPermissionsGranted(this::onPermissionsGranted)
        .whenPermissionsRefused(this::onPermissionsRefused)
        .execute(this);
```

## Advanced usage

#### Showing rationale first

For some use-cases the ```Rationale``` should be displayed before asking for a permission. It can be done simply by adding _showRationaleFirst()_ call to the ```Permissive.Request```:
```java
    new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .showRationaleFirst(true)
        .withRationale(new Rationale() {/*...*/})
        .execute(getActivity());
```

#### Checking permission in-place
Basically, it's a clone of _Context.checkSelfPermission()_ method:
```java
if(Permissive.checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
    // permission is granted
}
```

## License
    Copyright 2016 Jakub Księżniak
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.