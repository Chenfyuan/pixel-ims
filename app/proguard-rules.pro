# R8 handles most optimizations automatically; keep only what's needed.

-keepattributes *Annotation*

# Android components (AGP usually handles this, but explicit for safety)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Shizuku instrumentation uses reflection
-keep class io.github.vvb2060.ims.privileged.** { *; }

# Window extensions (jetpack)
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
