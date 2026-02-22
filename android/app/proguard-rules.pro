# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.w3nto.app.**$$serializer { *; }
-keepclassmembers class com.w3nto.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.w3nto.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# JNA / Lazysodium — required for Phantom X25519 encryption
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn com.sun.jna.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# MWA (Mobile Wallet Adapter)
-keep class com.solana.mobilewalletadapter.** { *; }
