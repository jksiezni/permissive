[![Release](https://jitpack.io/v/jksiezni/permissive.svg)](https://jitpack.io/#jksiezni/permissive)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Permissive-green.svg?style=flat)](http://android-arsenal.com/details/1/4316)

# Permissive

Permissive is a lightweight, yet powerful Android library, that helps you restore control over permissions handling introduced in Android Marshmallow (API 23).

The _Permissive_ library perfectly fits into [Material Design - Permissions](https://www.google.com/design/spec/patterns/permissions.html) pattern, by providing a simple API to accomplish tasks requiring sensitive permissions.

<img src="/../gh-pages/images/screenshots/educate_up_front.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/ask_up_front.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/educate_in_context.png?raw=true" width="24%"/>
<img src="/../gh-pages/images/screenshots/ask_in_context.png?raw=true" width="24%"/>

All screenshots were taken from __sample__ app, which provides exemplary implementation of popular use-cases using the _Permissive_ library.

### Features

* Backward compatibility down to Android API 8 (not tested on earlier versions)
* __Java8__ ready (supports __retrolambda__ expressions)
* Thread-safe API
* Queueing __permissive__ ```Requests``` and ```Actions```
* Not using transparent *magic* Activities
* No messing with your Activities (well, actually adds a fragment, but it should not bother you in most cases)
* No additional dependencies
* No code generation
* Unique __Espresso__ compatible [Permissive Testing](https://github.com/jksiezni/permissive/wiki/Testing-Runtime-Permissions) library *(experimental)*

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
		compile 'com.github.jksiezni.permissive:permissive:0.1'
	}
```
 - (Optional) Add an extra library with helper fragments:
```gradle
	dependencies {
		// includes helper fragments based on Android Support v4 implementation (minSdkVersion 8)
		compile 'com.github.jksiezni.permissive:permissive-fragments:0.1'

		and/or

		// includes helper fragments based on native implementation (minSdkVersion 11)
		compile 'com.github.jksiezni.permissive:permissive-fragments-v13:0.1'
	}
```

### Usage

#### Requesting permissions
```Permissive.Request``` allows to simply ask user for a permission and (if allowed) do the task.
To request a permission you just need to create and execute ```Permissive.Request```:
```java
new Permissive.Request(Manifest.permission.ACCESS_FINE_LOCATION).execute(getActivity());
```

Add callback listeners that return results of the request. Also, you can ask for more permissions with a single _Request_:
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
When requesting permissions, a rationale may be required. According to Material Design guidelines,
you should provide a proper rationale depending on clarity and importance of permission type you are requesting.
_Permissive_ API is very flexible and allows you to implement all strategies you may need when requesting permissions.
See [Request patterns](https://www.google.com/design/spec/patterns/permissions.html#permissions-request-patterns) suggested by Google.

##### Executing requests with ```Rationale```
To show a rationale simply add a new ```Rationale``` callback to the ```Permissive.Request```:
```java
new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          .withRationale(/*YourRationale*/)
          .whenPermissionsGranted(/*listener*/)
          .whenPermissionsGranted(/*listener*/)
          .execute(getActivity());
```

You can also register a _global_ ```Rationale```, which will be used automatically when requesting permission:
```java
// registar global rationale
Permissive.registerGlobalRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, /*YourRationale*/);
...
// perform a request
new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE).execute(getActivity());
```
>  **Note**: *A locally added rationale callback always takes precedence over global rationales.*

##### Building ```Rationale```
```Rationale``` implementation depends on the request pattern you choose to follow. Of course, it may vary from app to app,
so _Permissive_ library does not enforce you to use any of them. Instead, it provides some helpers, that make it easier to build
a well tailored ```Rationale```.

- This is an example of ```Rationale``` implementation using _AlertDialog_:
```java
    new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      .withRationale(new Rationale() {
        @Override
        public void onShowRationale(Activity activity, String[] allowablePermissions, PermissiveMessenger messenger) {
          new AlertDialog.Builder(activity)
              .setTitle("Rationale title...")
              .setMessage("A rationale message.")
              .setPositiveButton(android.R.string.ok, null)
              .setOnDismissListener(new DialogInterface.OnDismissListener() {
                  @Override
                  public void onDismiss(DialogInterface dialog1) {
                    messenger.cancelPermissionsRequest();
                  }
              })
              .show();
        }
      }).execute(getActivity());
```
The ```onShowRationale(Activity activity, String[] allowablePermissions, PermissiveMessenger messenger)``` 
method is called when a rationale should be shown.
The '''allowablePermissions''' argument gives you a hint, what permissions you can still ask for.
It's a different approach to **'never ask again'** problem, where you would expect to have a list of blocked permissions.

The ```PermissiveMessenger``` object allows you to control current ```Request```. Use _repeatPermissionsRequest()_ or _cancelPermissionsRequest()_ methods to ask again for permissions or cancel ongoing request.
> **Note**: *Remember to call one of _repeatPermissionsRequest()_ or _cancelPermissionsRequest()_ methods, in order to continue processing requests. Otherwise your ```Requests``` and ```Actions``` will be dead-locked.*
  
Additionally, __Permissive__ comes with **permissive-fragments** library providing specialized fragments, that help simplify building rationales.
The main advantage of using those fragments, is when you want to preserve your request and rationale across [runtime changes](http://developer.android.com/guide/topics/resources/runtime-changes.html).
Here's an example of a previous AlertDialog which is encapsulated in ```RationaleDialogFragment```:
```java
public class ExampleRationaleFragment extends RationaleDialogFragment implements DialogInterface.OnClickListener {
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
        .setTitle("Rationale title...")
        .setMessage("A rationale message.")
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
}
...
new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    .withRationale(new ExampleRationaleFragment())
    .execute(getActivity());
```

#### Running a ```Permissive.Action```

The ```Permissive.Action``` simply checks what permissions are granted or refused to the app. So, the main difference from ```Permissive.Request``` is that it doesn't show any dialogs to the user.
Also, it is executed in a queue as every other ```Permissive.Request```, what provides a big advantage when used with requests. In effect, every action will wait until other Actions or Requests are completed.
Actions can be used in background tasks (like _Services_), where no Activity exists, but a sensitive permission is still required.

```java
// here using Java 8 lambdas
new Permissive.Action<>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    .whenPermissionsGranted(this::onPermissionsGranted)
    .whenPermissionsRefused(this::onPermissionsRefused)
    .execute(getActivity());
```

## Advanced usage

#### Showing rationale first

For some use-cases the ```Rationale``` should be displayed before asking for a permission, for instance when educating users about unclear permissions. It can be done simply by adding _showRationaleFirst()_ call to the ```Permissive.Request```:
```java
new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    .showRationaleFirst(true)
    .withRationale(new Rationale() {/*...*/})
    .execute(getActivity());
```

#### Checking permission in-place
Basically, it's a clone of _[Context.checkSelfPermission()](http://developer.android.com/reference/android/content/Context.html#checkSelfPermission%28java.lang.String%29)_ method:
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