# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# --- General rules for libraries using reflection ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses


# --- Gson rules ---
# Keep Gson internals needed for reflection
-keep class com.google.gson.reflect.TypeToken {*;} 
-keep class * extends com.google.gson.reflect.TypeToken {*;} 
-keep class com.google.gson.stream.** { *; }

# Keep fields that are annotated with SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


# --- Retrofit rules ---
# Keep Retrofit's Response class
-keep class retrofit2.Response { *; }

# Keep suspend functions and their Continuation parameter
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep class kotlin.coroutines.Continuation


# --- OkHttp3 / Okio rules ---
-dontwarn okio.**
-dontwarn okhttp3.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase


# --- App specific rules ---
# Keep your data model classes
-keep class com.example.myapplication.feature.chat.model.** { *; }

# Keep your Room DAO interfaces
-keep interface com.example.myapplication.feature.chat.persistence.** { *; }

