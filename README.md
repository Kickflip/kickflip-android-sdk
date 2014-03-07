# Kickflip SDK for Android

**note:** This is a pre-release preview. Consider nothing stable.

The Kickflip platform is a complete video streaming solution for your Android 4.3+ (API 18+) application. Our built-in `BroadcastActivity` makes broadcasting live HD video to your Kickflip account possible with a few lines of code.

Besides live broadcasting, Kickflip supports an array of output formats beyond the capabilities of Android's [MediaRecorder](http://developer.android.com/reference/android/media/MediaRecorder.html) and [MediaMuxer](https://developer.android.com/reference/android/media/MediaMuxer.html) with a dead-simple API.

## Features

+ **High Definition [HTTP Live Streaming](http://en.wikipedia.org/wiki/HTTP_Live_Streaming)**
+ **Background recording**
+ **OpenGL Video Effects**



## Quickstart

0. Ensure the `minSdkVersion` of your application is **18** (Android 4.3) and the `compileSdkVersion` is **19** (Android 4.4).

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

1. Add Kickflip to your app's `build.gradle`:

    **Dependencies:**
	```groovy
	dependencies {
   		compile 'io.kickflip:sdk:0.9'
	}
	```


2. Add the following to your app's `AndroidManifest.xml`:

    **Permissions:**
	```xml	       
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
	```
	
	**BroadcastActivity:**
	
	```xml
    <activity
         android:name="io.kickflip.sdk.BroadcastActivity"
         android:screenOrientation="landscape">
    </activity>
	```

4. Provide your Kickflip keys and start `BroadcastActivity` when appropriate:

	```java
	Kickflip.setupWithApiKey(API_KEY, API_SECRET);
	Kickflip.startBroadcastActivity(this, new BroadcastListener() {
        @Override
        public void onBroadcastStart() {
        
        }

        @Override
        public void onBroadcastLive(String watchUrl) { 
        	Log.i("Kickflp", "This phone is live at " + watchUrl);       
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

## Building this project

### Set up Java, the Android SDK & Build-Tools

1. [Download and install JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
2. [Download and install the Android SDK](http://developer.android.com/sdk/)
3. Run `/android-sdk/tools/android` to install the following packages:
    
![Installing packages from the Android SDK Manager](http://i.imgur.com/PuWsBEB.png)

**ProTip**: You should have the Android SDK root, along with the `/tools` and `/platform-tools` sub-directories added to your PATH.

### Build

1. Define the Android SDK location as $ANDROID_HOME in your environment, or create a file named `local.properties` in this directory with the following contents:
    
	    sdk.dir=/path/to/android-sdk
	    
2. Create a file named `SECRETS.java` in `./sample/src/main/java/io/kickflip/sample/`:

		package io.kickflip.sample;
		public class SECRETS {
		    public static final String CLIENT_KEY = "YourKickflipKey";
		    public static final String CLIENT_SECRET = "YourKickflipSecret";
		}


3. From this directory run:

	    $ ./gradlew assembleDebug

The Kickflip Sample .apk will be in `./sample/build/apk`. 

The Kickflip SDK .aar will be in `./sdk/build/libs`.

## Using the library

The Kickflip SDK is available from the [Maven Central Repository](http://search.maven.org/), and can be easily added to your project's `build.gradle`:

    dependencies {
	   compile 'io.kickflip:sdk:0.9'
	}


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