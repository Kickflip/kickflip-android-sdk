# Kickflip SDK for Android - Releasing

1. Create a file named `gradle.properties` in this directory with the following contents:

		# APK Signing
		
		storeFile=/path/to/keystore.keystore
		storePassword=yourStorePassword
		keyAlias=yourKeyAlias
		keyPassword=yourKeyPassword
		
		# Maven
		
		signing.keyId=gpg-id
		signing.password=gpg-password
		signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
				
		sonatypeUsername=sonaUser
		sonatypePassword=sonaPass
		
		artifactGroupId=io.kickflip
		artifactName=sdk
		projectTitle=Kickflip SDK
		projectDesc=Kickflip Live video broadcasting SDK for Android
		
		htmlUrl=https://github.com/Kickflip/kickflip-android-sdk
		gitUrl=https://github.com/Kickflip/kickflip-android-sdk.git
		sshUrl=scm:git@github.com:Kickflip/kickflip-android-sdk.git
		
		devId=devId
		devName=Your Name
		

## Build a Signed APK for the Play Store etc.

	$ ./gradlew assembleRelease
		
Signed .apk will be in `./sample/build/apk/`.

Signed .aar will be in `./sdk/build/libs/`.
		
## Publish the SDK to Maven

Remember to update the `versionName` in `./sdk/build.gradle`!

	$ ./gradlew clean && ./gradlew build && ./gradlew uploadArchives
	
	