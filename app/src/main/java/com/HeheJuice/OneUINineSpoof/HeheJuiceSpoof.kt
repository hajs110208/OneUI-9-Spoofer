package com.HeheJuice.OneUINineSpoof

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XC_MethodHook

class HeheJuiceSpoof : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == null) return

        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            val propHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.product.device") param.result = "q7q"
                    if (key == "ro.build.characteristics") param.result = "tablet"
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, propHookString)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java, propHookString)

            val propHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, propHookInt)

            // Hook boolean properties like SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR and force them to true
            val propHookBoolean = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR") param.result = true
                }
            }
            try {
                XposedHelpers.findAndHookMethod(systemPropertiesClass, "getBoolean", String::class.java, Boolean::class.javaPrimitiveType, propHookBoolean)
            } catch (t: Throwable) {}
        } catch (t: Throwable) {}

        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
        } catch (t: Throwable) {}

        try {
            val semSystemPropertiesClass = XposedHelpers.findClass("android.os.SemSystemProperties", lpparam.classLoader)

            val semPropHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.product.device") param.result = "q7q"
                    if (key == "ro.build.characteristics") param.result = "tablet"
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, semPropHookString)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, String::class.java, semPropHookString)

            val semPropHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, semPropHookInt)

            // Hook boolean properties on SemSystemProperties as well
            val semPropHookBoolean = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR") param.result = true
                }
            }
            try {
                XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getBoolean", String::class.java, Boolean::class.javaPrimitiveType, semPropHookBoolean)
            } catch (t: Throwable) {}
        } catch (t: Throwable) {}

        // Additionally hook the SemFloatingFeature class used by SystemUI (com.samsung.android.feature.SemFloatingFeature)
        try {
            val semFloatingFeatureClass = XposedHelpers.findClass("com.samsung.android.feature.SemFloatingFeature", lpparam.classLoader)

            val semFloatingHookNoDefault = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR") param.result = true
                }
            }
            // getBoolean(String)
            XposedHelpers.findAndHookMethod(semFloatingFeatureClass, "getBoolean", String::class.java, semFloatingHookNoDefault)

            val semFloatingHookWithDefault = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "SEC_FLOATING_FEATURE_LAUNCHER_SUPPORT_TASKBAR") param.result = true
                }
            }
            // getBoolean(String, boolean)
            try {
                XposedHelpers.findAndHookMethod(semFloatingFeatureClass, "getBoolean", String::class.java, Boolean::class.javaPrimitiveType, semFloatingHookWithDefault)
            } catch (t: Throwable) {}
        } catch (t: Throwable) {}
    }
}
