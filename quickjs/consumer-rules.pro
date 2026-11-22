-keep class com.berryos.music.quickjs.QuickJsNative { *; }
-keep interface com.berryos.music.quickjs.HostCallback { *; }
-keepclassmembers class * implements com.berryos.music.quickjs.HostCallback {
    public java.lang.String call(java.lang.String, java.lang.String);
}
