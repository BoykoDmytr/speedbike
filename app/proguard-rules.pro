# Default ProGuard rules.

# Keep osmdroid classes (uses reflection for tile sources).
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
