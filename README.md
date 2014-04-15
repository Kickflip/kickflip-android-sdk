# Kickflip SDK for Android

![kickflip live broadcast screenshot](http://i.imgur.com/BZy4qSF.jpg)

**The Kickflip Android SDK manages all the plumbing for your cloud video application**. With this SDK you can broadcast Live, High Definition [HTTP-HLS](http://en.wikipedia.org/wiki/HTTP_Live_Streaming) video, associate these broadcasts with users, and query broadcasts made by your users. All you need is a [Kickflip.io](https://kickflip.io) account.

The Kickflip Android SDK requires Android 4.3+ (API 18+).

Check out our [Android example application](https://github.com/Kickflip/kickflip-android-example) to see how to easily put this SDK to work.

Also check out our slick [iOS SDK](https://github.com/Kickflip/kickflip-ios-sdk) and [iOS Example application](https://github.com/Kickflip/kickflip-ios-example)

## Features

+ **High Definition [HTTP Live Streaming](http://en.wikipedia.org/wiki/HTTP_Live_Streaming)**
+ **Background recording**
+ **OpenGL Video Effects**



## Quickstart

0. Make a [kickflip.io](https://kickflip.io) account to register an Application and receive your **Client Key** and **Client Secret**. You'll need these later.

1. Ensure the `minSdkVersion` of your application is **18** (Android 4.3) and the `compileSdkVersion` is **19** (Android 4.4).

	```groovy
	android {
        compileSdkVersion 19
        ...
        defaultConfig {
            minSdkVersion 18
            targetSdkVersion 19
            ...
        }
        ...
    }
    ```

2. Add Kickflip to your app's `build.gradle`:

    **Dependencies:**
	```groovy
	dependencies {
   		compile 'io.kickflip:sdk:0.9.10'
	}
	```


3. Add the following to your app's `AndroidManifest.xml`:

    **Permissions:**
	```xml	       
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
	```
	
	**BroadcastActivity:**	
	
	```xml
    <activity
         android:name="io.kickflip.sdk.activity.BroadcastActivity"
         android:keepScreenOn="true"
         android:screenOrientation="landscape">
    </activity>
	```

	**MediaPlayerActivity** (Optional):
	
	MediaPlayerActivity handles playing HLS streams with some additional features over Android's MediaPlayer (like indicating when a stream is "Live" and more accurately inferring the stream duration).
	
	```xml
	<activity
            android:name="io.kickflip.sdk.activity.MediaPlayerActivity"
            android:screenOrientation="landscape" >
    </activity>
	```
4. Provide your Kickflip keys and start `BroadcastActivity` when appropriate:

	```java
	Kickflip.setup(this, CLIENT_ID, CLIENT_SECRET);
	Kickflip.startBroadcastActivity(this, new BroadcastListener() {
        @Override
        public void onBroadcastStart() {
        
        }

        @Override
        public void onBroadcastLive(String watchUrl) { 
        	Log.i("Kickflip", "This phone is live at " + watchUrl);       
        }

        @Override
        public void onBroadcastStop() {
        
        }

        @Override
        public void onBroadcastError() {
        
        }
    });
	```
   	
`BroadcastActivity` provides a pre-built UI including a camera preview and controls for starting, stopping, and sharing the broadcast.

## Developing on the bleeding edge

If you'd like to modify the sdk or just develop on the bleeding edge,
add the kickflip-android-sdk to your project as a git submodule. See the [kickflip-android-example dev branch](https://github.com/Kickflip/kickflip-android-example/tree/dev)
repo for an example application configured with the Kickflip SDK as a submodule.

1. Clone the kickflip-android-sdk repo:

        $ cd /path/to/yourapp
		$ git submodule add https://github.com/Kickflip/kickflip-android-sdk ./submodules/kickflip-android-sdk/

2. Add the sdk to your top-level `settings.gradle`:

		// settings.gradle
		include ':yourapp'
		include ':submodules:kickflip-android-sdk:sdk'

3. Add the sdk as a dependency to your app module's `build.gradle`:

		// ./yourapp/build.gradle
		...
		dependencies {
				...
				compile project(':submodules:kickflip-android-sdk:sdk')
		}

## Building the Kickflip Android SDK

### Set up Java, the Android SDK & Build-Tools

1. [Download and install JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
2. [Download and install the Android SDK](http://developer.android.com/sdk/)
3. Run `/android-sdk/tools/android` to install the following packages:

![Installing packages from the Android SDK Manager](http://i.imgur.com/PuWsBEB.png)

**ProTip**: You should have the Android SDK root, along with the `/tools` and `/platform-tools` sub-directories added to your PATH.

### Building

1. Define the Android SDK location as $ANDROID_HOME in your environment, or create a file named `local.properties` in this directory with the following contents:

	    sdk.dir=/path/to/android-sdk

2. From this directory run:

	    $ ./gradlew assembleDebug

The Kickflip SDK .aar will be in `./sdk/build/libs`.

## License

Apache 2.0

	Copyright 2014 OpenWatch, Inc.
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.