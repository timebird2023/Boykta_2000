###############################################
# boykta net — ProGuard / R8 Release Rules
###############################################

# Keep app entry points
-keep class com.boykta.net.MainActivity { *; }

# Obfuscate everything else aggressively
-dontskipnonpubliclibraryclasses
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''

# Keep Compose internals
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Retrofit + OkHttp
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson / JSON models — keep field names so serialisation works
-keepclassmembers class com.boykta.net.data.models.** { *; }
-keep class com.boykta.net.data.models.** { *; }
-keepclassmembers class com.boykta.net.data.model.** { *; }
-keep class com.boykta.net.data.model.** { *; }
-keepattributes *Annotation*

# DataStore
-keep class androidx.datastore.** { *; }

# Start.io SDK
-keep class com.startapp.** { *; }
-dontwarn com.startapp.**
-keep class com.android.installreferrer.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
