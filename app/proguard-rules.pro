# FocusTap is shipped unminified in the alpha release. These rules keep things
# sane if minification is enabled later.
-keepattributes *Annotation*
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-dontwarn com.google.zxing.**
