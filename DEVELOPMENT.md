Kickflip Android SDK
=============

This document provides a more in-depth look at the Kickflip Android SDK for those wishing to modify it.

**Ready to Develop with the Kickflip SDK?**
See the [kickflip-android-sdk](https://github.com/Kickflip/kickflip-android-sdk) project for [QuickStart](https://github.com/Kickflip/kickflip-android-sdk#quickstart) and [Build instructions](https://github.com/Kickflip/kickflip-android-sdk#building-this-project) if you're ready to start developing.

## What is the Kickflip Android SDK?

We designed the Kickflip Android SDK to make cloud video applications for Android a joy. When paired with a [Kickflip.io](https://kickflip.io) account **this SDK can manage all the plumbing for your cloud video application**: It broadcasts live HD [HTTP-HLS](http://en.wikipedia.org/wiki/HTTP_Live_Streaming) video, manages user profiles, and allows you to query broadcasts made by your users.

See the [Kickflip Example Android App](https://github.com/Kickflip/kickflip-android-example) for a basic live video social network written with this SDK.

Beyond a Kickflip.io client, this SDK is a collection of modular audio and video components that enable powerful hardware-accelerated video products for the Android platform. At the top level, you can use this SDK and a Kickflip account to create a live video social network without having to worry about video encoding or storage. With the lower-level classes you could create a unique kind of camera with realtime OpenGL shaders.


## Setting up

Before you use the Kickflip API, you must first get your access tokens from the Kickflip dashboard.

### Get the SDK

See the [kickflip-android-sdk quickstart guide](https://github.com/kickflip/kickflip-android-sdk#quickstart).	

## Architecture

### Kickflip

[`Kickflip`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/Kickflip.java) is the top-level class for easy interaction with the SDK features.

The first thing you'll do is register your application with Kickflip:

```java
	Kickflip.setup(this, CLIENT_ID, CLIENT_SECRET);
```
This method returns a [`KickflipApiClient`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/api/KickflipApiClient.java) ready to perform API actions on behalf of your account.

```java
	KickflipApiClient apiClient = Kickflip.setup(this, CLIENT_ID, CLIENT_SECRET);
```

To start broadcasting video:

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
	
To play a Kickflip broadcast within your app:

```java
	Kickflip.setup(this, "CLIENT_ID, CLIENT_SECRET);
	Kickflip.startMediaPlayerActivity(this, "http://example.com/stream.m3u8");
```

You can obtain a Kickflip [`Stream`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/api/json/Stream.java) raw media url with `stream.getStreamUrl()`. For an HLS broadcast this is a url of form https://xxx.xxx/xxx.m3u8
 


### Activities and Fragments

[`BroadcastActivity`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/BroadcastActivity.java) hosts a single [`BroadcastFragment`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/fragment/BroadcastFragment.java) which manages every aspect of the broadcasting use-case for you. `BroadcastFragment` take care of passing Activity lifecycle hooks to the underlying classes and is a great reference if you decide to directly interface with the base classes.

### Recording (The Full Stack)

[`Broadcaster`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/av/Broadcaster.java) and [`AVRecorder`](https://github.com/Kickflip/kickflip-android-sdk/blob/preview/sdk/src/main/java/io/kickflip/sdk/av/AVRecorder.java) are the high-level recording classes with APIs that are limited to starting, stopping, and a few other configuration methods. Think of `AVRecorder` as a supercharged version of Android's [`MediaRecorder`](http://developer.android.com/reference/android/media/MediaRecorder.html). `Broadcaster` extends `AVRecorder` and handles streaming to your Kickflip account transparently. 

### Encoding

`CameraEncoder` and `MicrophoneEncoder` abstract the management of device hardware like the Camera and Microphone behind a simple API for configuring, starting, and stopping.

Their parent classes are `VideoEncoderCore` and `AudioEncoderCore`, respectively. These two classes handle video/audio specific configuration of `AndroidEncoder`: our wrapper around Android's `MediaCodec`.

Under the hood, we use Android's `MediaCodec` class for encoding H.264 Video and AAC audio with hardware acceleration. Due to the performance requirements of real-time HD encoding, it's not currently feasible to leverage FFmpeg as a software encoder.

### Muxing

The Muxer is the heart of any recording configuration. Every recording session synchronizes on a single Muxer, which handles combining all Encoder outputs into a comprehensible output format.

Kickflip currently includes `FFmpegMuxer` and `AndroidMuxer`, which both implement the `Muxer` interface and thus can be used interchangeably. `AndroidMuxer` employs Android's built-in `MediaMuxer` and supports only MPEG-4 output. `FFmpegMuxer` harnesses the power of FFmpeg to write Encoder data to a variety of outputs such as HLS streams (MPEG-TS segments and a .m3u8 manifest file) as well as local MPEG-4 files. `FFmpegMuxer` can be further developed to write most any output format that supports H.264 video and AAC audio.