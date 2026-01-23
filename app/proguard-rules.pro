# Keep Kotlin metadata
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Room database
-keep class androidx.room.** { *; }

# Keep Hilt
-keep class dagger.** { *; }
-keep class com.google.dagger.** { *; }
-keep class * extends com.google.dagger.internal.Binding
-keep class * extends com.google.dagger.internal.Factory

# Keep AI model classes
-keep class com.aihos.ai.** { *; }
-keep class com.aihos.memory.** { *; }
-keep class com.aihos.reasoning.** { *; }
-keep class com.aihos.reflection.** { *; }
-keep class com.aihos.evolution.** { *; }

# Keep serialization classes
-keep class com.aihos.**.* extends * { <fields>; }
-keepclassmembers class com.aihos.** {
    *** *;
}

# Remove logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
