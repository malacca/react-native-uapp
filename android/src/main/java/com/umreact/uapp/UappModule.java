package com.umreact.uapp;

import org.json.JSONObject;

import android.os.Build;
import android.os.Bundle;
import android.content.Context;

import android.app.Dialog;
import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.umeng.message.UTrack;
import com.umeng.message.PushAgent;
import com.umeng.message.tag.TagManager;
import com.umeng.message.IUmengCallback;
import com.umeng.message.entity.UMessage;
import com.umeng.message.UmengMessageHandler;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.UmengNotifyClickActivity;
import com.umeng.message.common.inter.ITagManager;
import com.umeng.message.UmengNotificationClickHandler;

import com.umeng.commonsdk.UMConfigure;
import com.umeng.commonsdk.utils.UMUtils;
import com.umeng.analytics.MobclickAgent;

public class UappModule extends ReactContextBaseJavaModule {
    private static PushAgent mPushAgent;
    private static String deviceToken = "empty";
    private static Boolean pushRegistered = false;
    private static ReactApplicationContext reactContext = null;
    private static List<WritableMap> launchMessage = new ArrayList<>();
    private static List<Promise> deviceTokenListener = new ArrayList<>();
    private static final String messageEvent = "UmengMessage";

    private static Dialog mSplashDialog;
    private static Boolean isEnableSplashScreen = false;
    private static WeakReference<Activity> mActivity;


    // 启动屏, 由于该功能代码量较少, 且 uapp 也要监听 activity 生命周期
    // 所以一并放到模块内, 若要启用该功能, 应在调用 init() 函数前设置
    public static void enableSplashScreen(boolean enableSplashScreen) {
        isEnableSplashScreen = enableSplashScreen;
    }

    private static void showLaunchScreenInActivity(final Activity activity) {
        if (activity == null) {
            return;
        }
        mActivity = new WeakReference<Activity>(activity);
        final int resourceId = R.style.SplashScreen_Theme;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!activity.isFinishing()) {
                    mSplashDialog = new Dialog(activity, resourceId);
                    mSplashDialog.setContentView(R.layout.launch_screen);
                    mSplashDialog.setCancelable(false);
                    if (!mSplashDialog.isShowing()) {
                        mSplashDialog.show();
                    }
                }
            }
        });
    }

    private static void hideLaunchScreenInActivity(Activity activity) {
        if (activity == null) {
            if (mActivity == null) {
                return;
            }
            activity = mActivity.get();
        }
        if (activity == null) {
            return;
        }
        final Activity _activity = activity;
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSplashDialog != null && mSplashDialog.isShowing()) {
                    boolean isDestroyed = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        isDestroyed = _activity.isDestroyed();
                    }
                    if (!_activity.isFinishing() && !isDestroyed) {
                        mSplashDialog.dismiss();
                    }
                    mSplashDialog = null;
                }
            }
        });
    }

    // 在主工程 MainApplication.onCreate 函数中调用初始化
    public static void init(Application application) {
        init(application, null);
    }

    public static void init(Application application, @Nullable String channel) {
        init(application, channel, false);
    }

    public static void init(Application application, @Nullable String channel, boolean logEnabled) {
        // active 监听
        application.registerActivityLifecycleCallbacks(new activeListener());

        // 调试
        UMConfigure.setLogEnabled(logEnabled);

        // 初始化
        if (channel == null) {
            UMConfigure.init(application, BuildConfig.UMENG_DEVICE_TYPE, BuildConfig.UMENG_PUSH_SECRET);
        } else {
            UMConfigure.init(
                    application,
                    UMUtils.getAppkeyByXML(application),
                    channel,
                    BuildConfig.UMENG_DEVICE_TYPE,
                    BuildConfig.UMENG_PUSH_SECRET
            );
        }

        // 设置统计模式
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL);

        // push 注册
        mPushAgent = PushAgent.getInstance(application);
        mPushAgent.register(new IUmengRegisterCallback() {
            @Override
            public void onSuccess(String deviceToken) {
                afterUmRegister(deviceToken);
            }
            @Override
            public void onFailure(String s, String s1) {
                afterUmRegister(null);
            }
        });

        // 注册厂商通道
        registerVendor(application);

        // push 监听(消息&通知)
        mPushAgent.setMessageHandler(onMessage());
        mPushAgent.setNotificationClickHandler(onNotify());
    }

    private static void afterUmRegister(String token) {
        deviceToken = token;
        Iterator<Promise> i = deviceTokenListener.iterator();
        while (i.hasNext()) {
            i.next().resolve(deviceToken);
            i.remove();
        }
    }

    // 通过反射方式  调用厂商推送通道的注册函数
    private static void registerVendor(Application application) {
        String channel = BuildConfig.UMENG_PUSH_VENDOR,
                param = BuildConfig.UMENG_PUSH_PARAM;
        String[] auth = null;

        if ("complex".equals(channel)) {
            String brand = Brand.get();
            if ("other".equals(brand)) {
                return;
            }
            int key = "xiaomi".equals(brand) ? 0 : (
                    "oppo".equals(brand) ? 1 : (
                            "meizu".equals(brand) ? 2 : -1
                    )
            );
            if (key > -1) {
                String[] params = param.split("#");
                auth = params[key].split("_");
                if (TextUtils.isEmpty(auth[0])) {
                    return;
                }
            }
            channel = brand;
        } else if ("xiaomi".equals(channel) || "oppo".equals(channel) || "meizu".equals(channel)) {
            auth = param.split("_");
        }

        String driver = null;
        boolean isVivo = false;
        boolean isHuawei = false;
        switch (channel) {
            case "xiaomi":
                driver = "org.android.agoo.xiaomi.MiPushRegistar";
                break;
            case "huawei":
                isHuawei = true;
                driver = "org.android.agoo.huawei.HuaWeiRegister";
                break;
            case "meizu":
                driver = "org.android.agoo.mezu.MeizuRegister";
                break;
            case "oppo":
                driver = "org.android.agoo.oppo.OppoRegister";
                break;
            case "vivo":
                isVivo = true;
                driver = "org.android.agoo.vivo.VivoRegister";
                break;
        }
        if (driver == null) {
            return;
        }

        try {
            Class<?> cls = Class.forName(driver);
            if (isHuawei || isVivo) {
                Method method = cls.getDeclaredMethod(
                        "register",
                        isVivo ? Context.class : Application.class
                );
                method.invoke(cls, application);
            } else {
                Method method = cls.getDeclaredMethod(
                        "register",
                        Context.class,
                        String.class,
                        String.class
                );
                method.invoke(cls, application, auth[0], auth[1]);
            }
        } catch (Throwable e) {
            // do nothing
        }
    }

    // 监听 mainActivity 生命周期
    private static class activeListener implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (!(activity instanceof UmengNotifyClickActivity)) {
                mPushAgent.onAppStart();
            }
            if (isEnableSplashScreen) {
                isEnableSplashScreen = false;
                showLaunchScreenInActivity(activity);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            MobclickAgent.onResume(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            MobclickAgent.onPause(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }

    private static UmengMessageHandler onMessage() {
        return new UmengMessageHandler() {
            // 不显示通知栏的 自定义消息回调
            @Override
            public void dealWithCustomMessage(final Context context, final UMessage msg) {
                onPushMessage(msg);
            }
        };
    }

    private static UmengNotificationClickHandler onNotify() {
        return new UmengNotificationClickHandler() {
            @Override
            public void launchApp(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(msg);
            }

            @Override
            public void openUrl(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(msg);
            }

            @Override
            public void openActivity(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(msg);
            }

            @Override
            public void dealWithCustomAction(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(msg);
            }
        };
    }

    private static void onPushMessage(final UMessage msg) {
        handlePushMessage(msg.getRaw());
    }

    // 处理推送消息: js未准备好,java缓存,待js准备好后进行通知; 否则直接通知
    public static void handlePushMessage(JSONObject jsonObject) {
        String key;
        WritableMap map = Arguments.createMap();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            key = keys.next();
            try {
                map.putString(key, jsonObject.get(key).toString());
            } catch (Exception e) {
                //Log.e("ReactNative", "putString fail");
            }
        }
        if (pushRegistered) {
            emitToJs(map);
        } else {
            launchMessage.add(map);
        }
    }

    private static void emitToJs(WritableMap map) {
        if (reactContext != null && reactContext.hasActiveCatalystInstance()) {
            reactContext.getJSModule(
                    DeviceEventManagerModule.RCTDeviceEventEmitter.class
            ).emit(messageEvent, map);
        }
    }

    private static void emitMessageCache() {
        Iterator<WritableMap> i = launchMessage.iterator();
        while (i.hasNext()) {
            emitToJs(i.next());
            i.remove();
        }
    }

    /**
     * 以上为静态函数, 从这里开始扩展实例化
     * @param context ReactApplicationContext
     */
    public UappModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public String getName() {
        return "Uapp";
    }

    /**
     * 自动载入推送功能有一个获取设备品牌的接口, 不浪费, 暴露给 js
     */
    @ReactMethod
    public void getBrand(Promise promise) {
        promise.resolve(Brand.get());
    }

    /**
     * 启动屏 显示/隐藏
     */
    @ReactMethod
    public void showLaunchScreen() {
        showLaunchScreenInActivity(getCurrentActivity());
    }

    @ReactMethod
    public void hideLaunchScreen() {
        hideLaunchScreenInActivity(getCurrentActivity());
    }

    /**
     * 设置是否打开系统日志
     */
    @ReactMethod
    public void setLogEnabled(boolean isEnabled) {
        UMConfigure.setLogEnabled(isEnabled);
    }

    /**
     * 获取统计用的 测试设备信息
     */
    @ReactMethod
    public void getTestDeviceInfo(Promise promise) {
        String[] devices = UMConfigure.getTestDeviceInfo(reactContext);
        WritableMap info = Arguments.createMap();
        info.putString("device_id", devices[0]);
        info.putString("mac", devices[1]);
        promise.resolve(info);
    }

    /**
     * rn 就一个 active, 若统计页面, 需要在 js 中手动调用
     */
    @ReactMethod
    public void onPageStart(String viewName) {
        MobclickAgent.onPageStart(viewName);
    }

    @ReactMethod
    public void onPageEnd(String viewName) {
        MobclickAgent.onPageEnd(viewName);
    }

    /**
     * 绑定站内自有账号, 统计更容易追踪
     */
    @ReactMethod
    public void onProfileSignIn(String ID) {
        MobclickAgent.onProfileSignIn(ID);
    }

    @ReactMethod
    public void onProviderSignIn(String ID, String Provider) {
        MobclickAgent.onProfileSignIn(Provider, ID);
    }

    @ReactMethod
    public void onProfileSignOff() {
        MobclickAgent.onProfileSignOff();
    }

    /**
     * 自定义事件打点
     */
    @ReactMethod
    public void onEventTrick(String eventId) {
        MobclickAgent.onEvent(reactContext, eventId);
    }

    @ReactMethod
    public void onEventLib(String eventId, String eventLabel) {
        MobclickAgent.onEvent(reactContext, eventId, eventLabel);
    }

    @ReactMethod
    public void onEventObject(String eventId, ReadableMap map) {
        onEventDuration(eventId, map, 0);
    }

    @ReactMethod
    public void onEventDuration(String eventId, ReadableMap map, int duration) {
        boolean hasNumber = false;
        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            if (ReadableType.Number == map.getType(key)) {
                hasNumber = true;
                break;
            }
        }
        if (hasNumber) {
            eventObject(eventId, map, duration);
        } else {
            eventValue(eventId, map, duration);
        }
    }

    private void eventObject(String eventId, ReadableMap map, int duration) {
        String key, value;
        Map<String, Object> rMap = new HashMap<String, Object>();
        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            key = iterator.nextKey();
            if (ReadableType.Boolean == map.getType(key)) {
                rMap.put(key, map.getBoolean(key) ? 1 : 0);
            } else if (ReadableType.String == map.getType(key)) {
                value = map.getString(key);
                if (value != null && value.length() > 0) {
                    rMap.put(key, value);
                }
            } else if (ReadableType.Number == map.getType(key)) {
                rMap.put(key, map.getDouble(key));
            }
        }
        // MobclickAgent.onEventObject 是没有 duration 参数的
        // 但是看 MobclickAgent.onEventValue 源码, 只是加了个 __ct__ 值, 这里也带上
        if (duration > 0) {
            rMap.put("__ct__", duration);
        }
        MobclickAgent.onEventObject(reactContext, eventId, rMap);
    }

    private void eventValue(String eventId, ReadableMap map, int duration) {
        String key, value;
        Map<String, String> rMap = new HashMap<String, String>();
        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            key = iterator.nextKey();
            if (ReadableType.Boolean == map.getType(key)) {
                rMap.put(key, String.valueOf(map.getBoolean(key)));
            } else if (ReadableType.Number == map.getType(key)) {
                rMap.put(key, String.valueOf(map.getInt(key)));
            } else if (ReadableType.String == map.getType(key)) {
                value = map.getString(key);
                if (value != null && value.length() > 0) {
                    rMap.put(key, value);
                }
            }
        }
        MobclickAgent.onEventValue(reactContext, eventId, rMap, duration);
    }

    /**
     * 设置 统计 session 的心跳时长
     */
    @ReactMethod
    public void setSessionContinueMillis(long millis) {
        MobclickAgent.setSessionContinueMillis(millis);
    }

    /**
     * 设置是否上报异常到友盟服务器
     */
    @ReactMethod
    public void setCatchUncaughtExceptions(boolean isEnable) {
        MobclickAgent.setCatchUncaughtExceptions(isEnable);
    }

    /**
     * 手动上报自己捕获的异常到友盟服务器
     */
    @ReactMethod
    public void reportError(String error) {
        MobclickAgent.reportError(reactContext, error);
    }

    /**
     * 手动设置用户 经纬度
     */
    @ReactMethod
    public void reportError(double latitude, double longitude) {
        MobclickAgent.setLocation(latitude, longitude);
    }

    /**
     * 统计相关 api 结束, 以下为推送相关的 api
     * 获取推送用的 DeviceToken
     */
    @ReactMethod
    public void getDeviceToken(Promise promise) {
        if (deviceToken.equals("empty")) {
            deviceTokenListener.add(promise);
        } else {
            promise.resolve(deviceToken);
        }
    }

    /**
     * 设置/获取 当应用处于前台时, 是否在通知栏显示通知
     */
    @ReactMethod
    public void setNotificaitonOnForeground(boolean show) {
        mPushAgent.setNotificaitonOnForeground(show);
    }

    @ReactMethod
    public void getNotificationOnForeground(Promise promise) {
        promise.resolve(mPushAgent.getNotificationOnForeground());
    }

    /**
     * 设置/获取 通知的冷却时长, 在指定时长内, 仅显示一条通知, 后来通知会替换前面的通知
     */
    @ReactMethod
    public void setMuteDurationSeconds(int seconds) {
        mPushAgent.setMuteDurationSeconds(seconds);
    }

    @ReactMethod
    public void getMuteDurationSeconds(Promise promise) {
        promise.resolve(mPushAgent.getMuteDurationSeconds());
    }

    /**
     * 设置/获取 勿扰时间段
     */
    @ReactMethod
    public void setNoDisturbMode(int startHour, int startMinute, int endHour, int endMinute) {
        mPushAgent.setNoDisturbMode(startHour, startMinute, endHour, endMinute);
    }

    @ReactMethod
    public void getNoDisturbMode(Promise promise) {
        WritableArray times = Arguments.createArray();
        times.pushInt(mPushAgent.getNoDisturbStartHour());
        times.pushInt(mPushAgent.getNoDisturbStartMinute());
        times.pushInt(mPushAgent.getNoDisturbEndHour());
        times.pushInt(mPushAgent.getNoDisturbEndMinute());
        promise.resolve(times);
    }

    /**
     * 开启/关闭 推送
     */
    @ReactMethod
    public void disable(final Promise promise) {
        mPushAgent.disable(new IUmengCallback() {
            @Override
            public void onSuccess() {
                promise.resolve(true);
            }
            @Override
            public void onFailure(String s, String s1) {
                promise.resolve(false);
            }
        });
    }

    @ReactMethod
    public void enable(final Promise promise) {
        mPushAgent.enable(new IUmengCallback() {
            @Override
            public void onSuccess() {
                promise.resolve(true);
            }
            @Override
            public void onFailure(String s, String s1) {
                promise.resolve(false);
            }
        });
    }

    /**
     * 管理 deviceToken tag 标签
     */
    @ReactMethod
    public void addTags(ReadableArray tags, final Promise promise) {
        mPushAgent.getTagManager().addTags(new TagManager.TCallBack() {
            @Override
            public void onMessage(boolean isSuccess, ITagManager.Result result) {
                promise.resolve(isSuccess);
            }
        }, readableArrayToArrayList(tags).toArray(new String[0]));
    }

    @ReactMethod
    public void deleteTags(ReadableArray tags, final Promise promise) {
        mPushAgent.getTagManager().deleteTags(new TagManager.TCallBack() {
            @Override
            public void onMessage(boolean isSuccess, ITagManager.Result result) {
                promise.resolve(isSuccess);
            }
        }, readableArrayToArrayList(tags).toArray(new String[0]));
    }

    @ReactMethod
    public void getTags(final Promise promise) {
        mPushAgent.getTagManager().getTags(new TagManager.TagListCallBack() {
            @Override
            public void onMessage(boolean isSuccess, List<String> result) {
                promise.resolve(listToWritableArray(isSuccess, result));
            }
        });
    }

    private ArrayList<String> readableArrayToArrayList(ReadableArray tags) {
        ArrayList<String> tagList = new ArrayList<>();
        readableArrayToArrayListDeep(tags, tagList);
        return tagList;
    }

    private void readableArrayToArrayListDeep(ReadableArray tags, ArrayList<String> tagList) {
        ReadableType type;
        for (int i = 0; i < tags.size(); i++) {
            type = tags.getType(i);
            if (ReadableType.Array == type) {
                ReadableArray value = tags.getArray(i);
                if (value != null) {
                    readableArrayToArrayListDeep(value, tagList);
                }
            } else if (ReadableType.String == type) {
                tagList.add(tags.getString(i));
            }
        }
    }

    private WritableArray listToWritableArray(boolean isSuccess, List<String> lists) {
        WritableArray arr = Arguments.createArray();
        if (!isSuccess) {
            return arr;
        }
        for (String str:lists) {
            arr.pushString(str);
        }
        return arr;
    }

    /**
     * 管理 deviceToken alias 别名
     */
    @ReactMethod
    public void addAlias(String type, String alias, final Promise promise) {
        mPushAgent.addAlias(alias, type, new UTrack.ICallBack() {
            @Override
            public void onMessage(boolean isSuccess, String message) {
                promise.resolve(isSuccess);
            }
        });
    }

    @ReactMethod
    public void setAlias(String type, String alias, final Promise promise) {
        mPushAgent.setAlias(alias, type, new UTrack.ICallBack() {
            @Override
            public void onMessage(boolean isSuccess, String message) {
                promise.resolve(isSuccess);
            }
        });
    }

    @ReactMethod
    public void deleteAlias(String type, String alias, final Promise promise) {
        mPushAgent.deleteAlias(alias, type, new UTrack.ICallBack() {
            @Override
            public void onMessage(boolean isSuccess, String message) {
                promise.resolve(isSuccess);
            }
        });
    }

    /**
     * 载入 push 消息, 在绑定好 message 回调函数后载入
     */
    @ReactMethod
    public void initPush() {
        pushRegistered = true;
        emitMessageCache();
    }

    // 在 js bundle 载入后自动发送消息, 但考虑到 js 的异步特性
    // 如果绑定监听函数是在异步操作中, 就收不到消息了, 所以把收消息的方式改为手动模式
    // 在已绑定好消息接收函数后, 手动调用 initPush()
    // @Override
    // public void initialize() {
    //    pushRegistered = true;
    //    emitMessageCache();
    // }
}
