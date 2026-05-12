# ---------------------------------
# Release shrinking + obfuscation
# ---------------------------------

# Keep metadata required by reflection and useful crash stack traces.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------
# Android entry points (manifest + framework)
# ---------------------------------

-keep class id.harissabil.hayah.HayahApplication { *; }
-keep class id.harissabil.hayah.MainActivity { *; }
-keep class id.harissabil.hayah.service.ActivityTransitionReceiver { *; }
-keep class id.harissabil.hayah.service.OnBootReceiver { *; }
-keep class id.harissabil.hayah.service.HayahAccessibilityService { *; }

# Keep all app service-package classes (orchestrators/helpers/services/receivers).
-keep class id.harissabil.hayah.service.** { *; }

# ---------------------------------
# Gson reflection safety
# ---------------------------------

# Preserve generic type metadata for TypeToken parsing.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep classes/fields used via Gson reflection in network/cache payloads.
-keep class id.harissabil.hayah.data.model.** { *; }
-keep class id.harissabil.hayah.ui.screens.settings.ReciterOption { *; }

# AI response models parsed with Gson in VerseRecommendationService.
-keep class id.harissabil.hayah.data.ai.VerseRecommendationService$ReflectionResult { *; }
-keep class id.harissabil.hayah.data.ai.VerseRecommendationService$ReflectionItem { *; }
-keep class id.harissabil.hayah.data.ai.** { *; }

# MCP Kotlin SDK and Ktor HTTP client used for MCP/AI calls.
-keep class io.modelcontextprotocol.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.modelcontextprotocol.**
-dontwarn io.ktor.**

# ---------------------------------
# MediaPipe Tasks Text (semantic detection)
# ---------------------------------

# MediaPipe uses JNI and reflection internally; the AAR ships no consumer rules.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# DetectionMode and AppTheme are persisted by name to DataStore and read back via
# .entries.find { it.name == ... }. Without these rules, R8 obfuscates constant names
# and breaks mode/theme restoration when minification is re-enabled.
-keep enum id.harissabil.hayah.ui.screens.settings.DetectionMode { *; }
-keep enum id.harissabil.hayah.ui.screens.settings.AppTheme { *; }

# Preserve fields annotated with @SerializedName when obfuscating other classes.
-keepclassmembers,allowobfuscation class * {
	@com.google.gson.annotations.SerializedName <fields>;
}
