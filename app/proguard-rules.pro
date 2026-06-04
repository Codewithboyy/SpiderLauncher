# ── SpiderLauncher ProGuard / R8 ─────────────────────────────────────────────

-keep class com.spiderlauncher.android.SpiderApp { *; }
-keep class com.spiderlauncher.android.ui.*Activity { *; }
-keep class com.spiderlauncher.android.viewmodel.** { *; }

# CRITICAL — keep ALL model/data classes for Gson (release build fix)
-keep class com.spiderlauncher.android.model.** { *; }
-keepclassmembers class com.spiderlauncher.android.model.** { <init>(...); <fields>; }
-keep class com.spiderlauncher.android.data.** { *; }
-keep class com.spiderlauncher.android.runtime.** { *; }
-keep class com.spiderlauncher.android.settings.** { *; }
-keep class com.spiderlauncher.android.game.** { *; }
-keep class com.spiderlauncher.android.network.** { *; }

-keepattributes Signature
-keepattributes SourceFile
-keepattributes SourceDir
-keepattributes LocalVariableTable
-keepattributes LocalVariableTypeTable
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson - keep generic signatures for deserialization
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent obfuscation of classes with generics used by Gson
-keepnames class com.spiderlauncher.android.model.**
-keepnames class com.spiderlauncher.android.runtime.**
-keepnames class com.spiderlauncher.android.network.**

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep class okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# DataStore
-keep class androidx.datastore.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Strip debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
