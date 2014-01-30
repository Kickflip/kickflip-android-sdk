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

1. Create a file named `local.properties` in this directory with the following contents:
    
	    sdk.dir=/path/to/android-sdk

2. From this directory run:

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