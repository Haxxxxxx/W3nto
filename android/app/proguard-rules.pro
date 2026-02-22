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
