# dApp Store Submission Assets

## Required Assets

| Asset | Specification | Status |
|-------|--------------|--------|
| App Icon | 512x512 PNG | TODO |
| Screenshots | 1920x1080, min 4, same orientation | TODO |
| Banner | 1200x600 | TODO |
| Feature Graphic | 1200x1200 | TODO |
| Privacy Policy URL | Public URL | TODO |
| License URL | Public URL | TODO |

## Release Signing

1. Generate a **fresh signing key** (not debug, not Play Store):
   ```bash
   keytool -genkeypair -v -keystore w3nto-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias w3nto -storepass <password> -keypass <password>
   ```

2. Create `android/keystore.properties` (gitignored):
   ```properties
   storeFile=w3nto-release.jks
   storePassword=<your-store-password>
   keyAlias=w3nto
   keyPassword=<your-key-password>
   ```

3. Place the `.jks` file in `android/` (also gitignored).

## Build Release APK

```bash
cd android && ./gradlew assembleRelease
```

The signed APK will be at `android/app/build/outputs/apk/release/app-release.apk`.
