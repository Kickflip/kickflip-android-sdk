# Kickflip SDK for Android

**note:** This is a pre-release preview. Consider nothing stable.

The Kickflip platform is a complete video streaming solution for your Android application. You can use our pre-built `BroadcastActivity` to stream live video to your Kickflip account starting with a few lines of code.

## Quickstart using BroadcastActivity
0. Add `BroadcastActivity` to your `AndroidManifest.xml`:

       <activity
            android:name="io.kickflip.sdk.BroadcastActivity"
            android:screenOrientation="landscape">
       </activity>
   
1. Provide your Kickflip keys and start `BroadcastActivity` to instantly stream live video from your application:

```java
	Kickflip.initWithApiKey(API_KEY, API_SECRET);
	Kickflip.startBroadcastActivity(this, mBroadcastListener);
```
    	
   BroadcastListener currently has the following definition:


```java
public interface BroadcastListener {
    public void onBroadcastStart();
    public void onBroadcastLive(String watchUrl);
    public void onBroadcastStop();
    public void onBroadcastError();
}

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

The Kickflip SDK will be available from the Maven Central Repository, and will be incorporated as a dependency in your project's `build.gradle`:

    # Coming Soon! Not yet published
    dependencies {
	   compile 'io.kickflip:sdk:0.1'
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