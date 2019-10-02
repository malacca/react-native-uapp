package com.umreact.uapp;

import org.json.JSONObject;

import android.content.Context;
import android.app.Application;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.LifecycleEventListener;
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
import com.umeng.message.common.inter.ITagManager;
import com.umeng.message.UmengNotificationClickHandler;

import com.umeng.commonsdk.UMConfigure;
import com.umeng.analytics.MobclickAgent;

public class UappModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static PushAgent mPushAgent;
    private static String deviceToken = "empty";
    private static Boolean pushRegistered = false;
    private static ReactApplicationContext reactContext = null;
    private static List<WritableMap> launchMessage = new ArrayList<>();
    private static List<Promise> deviceTokenListener = new ArrayList<>();
    private static final String messageEvent = "UmengMessage";

    // 在主工程 MainApplication.onCreate 函数中调用初始化
    public static void init(Application application) {
        init(application, false);
    }
    public static void init(Application application, boolean logEnabled) {
        // 调试
        UMConfigure.setLogEnabled(logEnabled);

        // 初始化
        UMConfigure.init(application, BuildConfig.UMENG_DEVICE_TYPE, BuildConfig.UMENG_PUSH_SECRET);
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
        
        // todo 通过反射方式  调用厂商推送通道的注册函数

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

    private static UmengMessageHandler onMessage() {
        return new UmengMessageHandler() {
            // 不显示通知栏的 自定义消息回调
            @Override
            public void dealWithCustomMessage(final Context context, final UMessage msg) {
                onPushMessage(context, msg);
            }
        };
    }

    private static UmengNotificationClickHandler onNotify() {
        return new UmengNotificationClickHandler() {
            @Override
            public void launchApp(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(context, msg);
            }

            @Override
            public void openUrl(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(context, msg);
            }

            @Override
            public void openActivity(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(context, msg);
            }

            @Override
            public void dealWithCustomAction(final Context context, final UMessage msg) {
                super.launchApp(context, msg);
                onPushMessage(context, msg);
            }
        };
    }

    private static void onPushMessage(final Context context, final UMessage msg) {
        String key;
        JSONObject jsonObject = msg.getRaw();
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

    @Override
    public void onHostResume() {
        MobclickAgent.onResume(getCurrentActivity());
    }

    @Override
    public void onHostPause() {
        MobclickAgent.onPause(getCurrentActivity());
    }

    @Override
    public void onHostDestroy() {
        //do nothing
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
     * 载入 push 消息, 在绑定好 message 回调函数后载入
     */
    @ReactMethod
    public void initPush() {
        pushRegistered = true;
        emitMessageCache();
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
}