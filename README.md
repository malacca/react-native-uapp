# react-native-uapp
react native 版本的友盟统计/推送

# 安装

`yarn add react-native-uapp`


# Android 配置

## 1.`android/app/src/main/androidManifest.xml`

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
        <meta-data android:value="xxxxxx" android:name="UMENG_APPKEY"/>
        <meta-data android:value="xiaomi" android:name="UMENG_CHANNEL"/>

    </application>

</manifest>
```

## 2. `android/gradle.properties`

```
....
# 重要提示：以下配置值都不要加引号, 直接在 = 后面设置


# 设置设备类型 (UMENG_DEVICE_TYPE, 1-手机, 2-盒子, 默认为1[可不添加])
#UMENG_DEVICE_TYPE=1

# 设置友盟推送的 SECRET 值
UMENG_PUSH_SECRET=xxxxx


# 当 manifest 的 UMENG_CHANNEL 等于以下值, 将集成对应的厂商
# 该方式适合打包 发布到应用市场的 apk, 仅针对单一厂商集成
# 配置可省略, 会使用默认值
#UMENG_PUSH_XIAOMI_CHANNEL=xiaomi
#UMENG_PUSH_HUAWEI_CHANNEL=huawei
#UMENG_PUSH_MEIZU_CHANNEL=meizu
#UMENG_PUSH_OPPO_CHANNEL=oppo
#UMENG_PUSH_VIVO_CHANNEL=vivo


# 当 manifest 的 UMENG_CHANNEL 等于该值, 将集成所有厂商通道
# 会根据手机 rom 自初始化对应厂商的 sdk
# 该方式适合打包发布到厂商应用市场之外的市场, 如应用宝
#UMENG_PUSH_COMPLEX_CHANNEL=complex


# 厂商配置, 可缺省, 没有设置的, 打包时会自动忽略
# 即使设置了 UMENG_CHANNEL, 也会忽略
#UMENG_PUSH_HUAWEI_ID=huawei_id

#UMENG_PUSH_XIAOMI_ID=xiaomi_id
#UMENG_PUSH_XIAOMI_KEY=xiaomi_key

#UMENG_PUSH_MEIZU_ID=meizu_id
#UMENG_PUSH_MEIZU_KEY=meizu_key

#UMENG_PUSH_OPPO_ID=oppo_key
#UMENG_PUSH_OPPO_KEY=oppo_secret

#UMENG_PUSH_VIVO_ID=vivo_id
#UMENG_PUSH_VIVO_KEY=vivo_key

```


## 3.初始化
`android/app/src/main/java/[project]/MainApplication.java`

```
....

import com.umreact.uapp.UappModule;

....


  @Override
  public void onCreate() {
    super.onCreate();

    # 额外附带的一个小功能,  是否使用启动屏, 默认未开启
    UappModule.enableSplashScreen(true);

    # 第二个参数: 渠道, 设置为 null 则使用 manifest 的 UMENG_CHANNEL
    # 第三个参数: 是否开启 android 日志
    UappModule.init(this, null|string, bool);
    .....
  }

....
```



## 4. 其他配置

在 `android/app/src/main/res` 下创建 `drawable`、`layout`、`raw` 目录 (若已存则无需)

### 推送优化

- 在 `drawable` 下添加 `umeng_push_notification_default_large_icon.png` 和 `umeng_push_notification_default_small_icon.png` 作为推送通知的图标，不设置则使用系统默认

- 若集成魅族厂商通道，还需在 `drawable` 目录下添加 `stat_sys_third_app_notify.png` 作为离线推送图标，建议 64x64

- 在 `raw` 下添加 `umeng_push_notification_default_sound.mp3` 作为推送通知的声音，不设置则使用系统默认


### 启动屏

因为启动屏的代码量较少, 且可以与友盟使用同一个监听函数, 所以附带着集成在一起了，默认未启用；
如果启用该功能，必须完成以下操作的一个（否则会报错）

- 在 `drawable` 下添加 `launch_screen.png` 作为启动屏 LOGO；为保持不同手机都能清晰显示，建议创建 `drawable-hdpi`、 `drawable-mdpi`、 `drawable-xdpi`、`drawable-xxdpi`、`drawable-xxxdpi` 文件夹，并放置不同尺寸的 log 图片

- 完全自定义启动屏，在 `layout` 下添加自定义的 `launch_screen.xml` 即可（需要一定的 android 开发知识）



# iOS 配置

还未开发 iOS 版本，TODO


