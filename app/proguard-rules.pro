# Keep kotlinx.serialization / OkHttp reflection-free; nothing special needed yet.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# AndroidX Security / Tink compile-only annotations
-dontwarn com.google.errorprone.annotations.**
