# react-native-uapp
react native 版本的友盟统计/推送

# 安装

`yarn add react-native-uapp`


# Android 配置

## 1. `android/build.gradle`

```
...
allprojects {
    repositories {
        ...
        # 添加友盟仓库地址
        maven { url 'https://dl.bintray.com/umsdk/release' }
    }
}
...
```

## 2. `android/gradle.properties`

```
....

# 设置设备类型 (UMENG_DEVICE_TYPE, 1-手机, 2-盒子, 默认为1[可不添加])
#UMENG_DEVICE_TYPE=1

# 设置友盟推送的 SECRET 值
UMENG_PUSH_SECRET="xxxxx"

# 厂商推送集成 (TODO: 以下暂未完成)
#UMENT_PUSH_XIAOMI_CHANNEL=xiaomi
#UMENT_PUSH_XIAOMI_ID="xiaomi"
#UMENT_PUSH_XIAOMI_KEY="xiaomi"

#UMENT_PUSH_HUAWEI_CHANNEL=huawei

#UMENT_PUSH_MEIZU_CHANNEL=meizu
#UMENT_PUSH_MEIZU_ID="meizu"
#UMENT_PUSH_MEIZU_KEY="meizu"

#UMENT_PUSH_OPPO_CHANNEL=oppo
#UMENT_PUSH_OPPO_ID="oppo"
#UMENT_PUSH_OPPO_KEY="oppo"

#UMENT_PUSH_VIVO_CHANNEL=vivo
```

## 3.`android/app/src/main/androidManifest.xml`

```
<manifest ..>

    <!--友盟所需基本权限-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>

    <!-- 建议添加权限，以便使用更多的第三方SDK和更精准的统计数据 -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />


    <application ...>
        ....

        <!--添加友盟应用的 key 和 渠道-->
        <!--TODO:当渠道名和 gradle.properties 中设置的名称相同时,会自动集成厂商推送-->
        <meta-data android:value="xxxxxx" android:name="UMENG_APPKEY"/>
        <meta-data android:value="xiaomi" android:name="UMENG_CHANNEL"/>

        <!--TODO:集成厂商推送通道, 需添加-->
        <activity
            android:exported="true"
            android:launchMode="singleTask"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.Translucent"
            android:name="com.umreact.uapp.UappActivity"
        />

        <!--TODO:集成华为推送通道, 需添加-->
        <meta-data
            android:name="com.huawei.hms.client.appid"
            android:value="appid=xxxxxx" 
        />

        <!--TODO:集成魅族推送通道, 需添加-->
        <receiver ...>
          ....
        </receiver>

    </application>

</manifest>
```

## 4.若开启了打包混淆

需修改 `android/app/proguard-rules.pro`

```

# 添加 （TODO:待确认）
-keep class com.umeng.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public class [您的应用包名].R$*{
    public static final int *;
}

-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.meizu.**

-dontwarn com.vivo.push.**
-keep class com.vivo.push.**{*; }
-keep class com.vivo.vms.**{*; }
-keep class xxx.xxx.xxx.PushMessageReceiverImpl{*;}

```


## 5.初始化
`android/app/src/main/java/[project]/MainApplication.java`

```
....

import com.umreact.uapp.UappModule;

....


  @Override
  public void onCreate() {
    super.onCreate();

    // 载入 
    // 第二个参数:附送的启动屏功能，不使用设置为 false 即可
    // 第三个参数:是否开启友盟的日志
    UappModule.init(this, true, false);
    .....
  }

....
```


## 6. 优化
在 `android/app/src/main/res` 下创建 `drawable`、`layout`、`raw` 目录 (若已存在或不需要，在无需)

- 在 `drawable` 下添加 `umeng_push_notification_default_large_icon.png` 和 `umeng_push_notification_default_small_icon.png` 作为推送通知的图标

- 在 `raw` 下添加 `umeng_push_notification_default_sound.mp3` 作为推送通知的声音

若使用了启动屏功能

- 默认情况下自带了一个 `launch_screen.xml` 作为启动屏UI （为一个 LOGO 居中的画面）， 只需在 `drawable` 下添加 `launch_screen.png` 作为启动屏 LOGO

- 完全自定义启动屏，只需在 `layout` 下添加 `launch_screen.xml` 自行制作（需要一定的 android 开发知识）

备注：以上步骤都有默认值，属于优化范畴，可省略的


## 7. 备注

Android 已可用，暂未整合厂商推送通道




# iOS 配置

还未开发 iOS 版本，TODO


