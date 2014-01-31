# Kickflip SDK for Android

**note:** This is a pre-release preview. Consider nothing stable.

The Kickflip platform is a complete video streaming solution for your Android application. You can use our pre-built `BroadcastActivity` to stream live video to your Kickflip account starting with **one line of code**.

## Quickstart

1. Start Kickflip's `BroadcastActivity` to instantly stream live video from your application:

```java
Kickflip.startBroadcastActivity(this, "API_KEY", "API_SECRET");
```
    	
   You may optionally provide a callback to handle messages from the Kickflip Broadcaster:


```java
Kickflip.startBroadcastActivity(this, "API_KEY", "API_SECRET", new 	KickflipCallback(){
	public void onMessage(KickflipMessage msg){
	
	switch(msg.getType()){
		case Kickflip.LIVE:
			Log.i("Whoo!", "This phone is live @ " + msg.getBroadcastURL());
	   		// do stuff
	   		break;
	   	case Kickflip.COMPLETE:
	   		Log.i("Whoo!", "The user stopped recording, and the entire broadcast is synced!");
	   		break;
	   	case Kickflip.ERROR:
	   		Log.e("Boo!", "An error occurred:" + msg.getError());
	   		break;
	}
);
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

MIT