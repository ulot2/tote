# kotlinx.serialization route classes for Navigation
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class app.tote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
