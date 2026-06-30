# Android Studio default ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.persistence.db.SupportDatabase <methods>;
}

-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }

# Keep all Room entity classes and their fields
-keep class com.jv.loyaltycardswallet.** { *; }
