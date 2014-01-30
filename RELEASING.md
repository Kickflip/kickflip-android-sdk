# Kickflip SDK for Android - Releasing

1. Create a file named `gradle.properties` in this directory with the following contents:

		storeFile=/path/to/keystore.keystore
		storePassword=yourStorePassword
		keyAlias=yourKeyAlias
		keyPassword=yourKeyPassword
		
2. From this directory run:

		$ ./gradlew assembleRelease