-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn com.ut.**
-dontwarn com.ta.**
-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn com.alibaba.sdk.android.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.coloros.**
-dontwarn com.vivo.**
-dontwarn com.meizu.**

-keepattributes *Annotation*

-keep class com.ut.**{*;}
-keep class com.ta.**{*;}
-keep class com.umeng.** {*;}
-keep class com.taobao.** {*;}
-keep class com.alibaba.sdk.android.**{*;}
-keep class anet.channel.** {*;}
-keep class anetwork.channel.** {*;}
-keep class org.android.** {*;}
-keep class org.apache.thrift.** {*;}

# huawei
-keep class com.huawei.** {*;}
-keep class com.coloros.** {*;}
-keep class com.vivo.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.meizu.** {*;}

-keep public class * extends android.app.Service
-keep public class * extends org.android.agoo.mezu.MeizuPushReceiver
-keep public class * extends com.umeng.message.UmengNotifyClickActivity

-keep public class **.R$*{
   public static final int *;
}