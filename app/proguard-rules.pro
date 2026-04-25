# AdMob
-keep class com.google.android.gms.ads.** { *; }
# UMP
-keep class com.google.android.ump.** { *; }
# Play Billing
-keep class com.android.billingclient.** { *; }
# CameraX
-keep class androidx.camera.** { *; }
# Hilt
-keep class dagger.hilt.** { *; }
# Room entities
-keep class com.lumiai.flashlight.core.data.local.entity.** { *; }
# Firebase
-keep class com.google.firebase.** { *; }
# Keep Kotlin metadata for reflection
-keepattributes *Annotation*
-keepattributes Signature
