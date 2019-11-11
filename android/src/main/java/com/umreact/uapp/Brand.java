package com.umreact.uapp;

import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class Brand {
    private static String brand = null;

    public static String get() {
        if (brand == null) {
            brand = getBrand();
        }
        return brand;
    }

    private static String getBrand() {
        String[] props = {
                "ro.build.version.emui",
                "ro.build.hw_emui_api_level",
                "ro.miui.ui.version.name",
                "ro.miui.ui.version.code",
                "ro.build.version.opporom",
                "ro.vivo.os.name",
                "ro.vivo.os.version",
                "ro.vivo.rom.version",
                "ro.meizu.product.model",
        };
        String brand = getProperty(props);
        if (brand == null) {
            return getByBuild();
        }
        if (brand.contains("emui")) {
            return "huawei";
        } else if (brand.contains("miui")) {
            return "xiaomi";
        } else if (brand.contains("opporom")) {
            return "oppo";
        } else if (brand.contains("vivo")) {
            return "vivo";
        } else if (brand.contains("meizu")) {
            return "meizu";
        }
        return "other";
    }

    private static String getByBuild() {
        String brand = Build.BRAND.toLowerCase();
        if ("huawei".equals(brand) || "honor".equals(brand)) {
            return "huawei";
        } else if ("xiaomi".equals(brand)) {
            return "xiaomi";
        } else if ("oppo".equals(brand)) {
            return "oppo";
        } else if ("vivo".equals(brand)) {
            return "vivo";
        } else if ("meizu".equals(brand) || "22c4185e".equals(brand)) {
            return "meizu";
        }
        return "other";
    }

    private static String getProperty(String[] props) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method method = cls.getDeclaredMethod("get", new Class[] {
                    String.class
            });
            for (String prop: props) {
                if (!TextUtils.isEmpty((String) method.invoke(cls, new Object[] {
                        prop
                }))) {
                    return prop;
                }
            }
        } catch (Throwable e) {
            return getPropertyFromFile(props);
        }
        return null;
    }

    private static String getPropertyFromFile(String[] props) {
        for (String prop: props) {
            if (!TextUtils.isEmpty(getPropertyByRead(prop))) {
                return prop;
            }
        }
        return null;
    }

    private static String getPropertyByRead(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
        return line;
    }
}
