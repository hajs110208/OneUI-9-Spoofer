package com.HeheJuice.OneUINineSpoof

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XC_MethodHook
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

class HeheJuiceSpoof : IXposedHookLoadPackage {

    // Dynamic map loaded from assets/prop.txt
    private val spoofedProps = mutableMapOf<String, String>()

    // Fallback static fields populated dynamically from the loaded props
    private var spoofModel = "SM-L715U"
    private var spoofBrand = "samsung"
    private var spoofDevice = "projectv2ul"
    private var spoofProduct = "projectv2ulue"
    private var spoofFingerprint = "samsung/projectv2ulue/projectv2ul:17/CP2A.260330.023/L715USQU1AZFL:user/release-keys"
    private var spoofDisplay = "CP2A.260330.023.L715USQU1AZFL"
    private var spoofTags = "release-keys"
    private var spoofType = "user"
    private var spoofDescription = "projectv2ulue-user 17 CP2A.260330.023 L715USQU1AZFL release-keys"
    private var spoofRelease = "17"
    private var spoofSecurityPatch = "2026-05-05"
    private var spoofSdkInt = 37
    private var spoofSemInt = 180000

    private val customXmlContent = """
        <?xml version="1.0" encoding="utf-8"?>
        <permissions>
         <feature name="com.samsung.android.oneui.version.10000" />
         <feature name="com.samsung.android.oneui.version.10100" />
         <feature name="com.samsung.android.oneui.version.10200" />
         <feature name="com.samsung.android.oneui.version.10500" />
         <feature name="com.samsung.android.oneui.version.20000" />
         <feature name="com.samsung.android.oneui.version.20100" />
         <feature name="com.samsung.android.oneui.version.20500" />
         <feature name="com.samsung.android.oneui.version.30000" />
         <feature name="com.samsung.android.oneui.version.30100" />
         <feature name="com.samsung.android.oneui.version.30101" />
         <feature name="com.samsung.android.oneui.version.40000" />
         <feature name="com.samsung.android.oneui.version.40100" />
         <feature name="com.samsung.android.oneui.version.40101" />
         <feature name="com.samsung.android.oneui.version.50000" />
         <feature name="com.samsung.android.oneui.version.50100" />
         <feature name="com.samsung.android.oneui.version.50101" />
         <feature name="com.samsung.android.oneui.version.60000" />
         <feature name="com.samsung.android.oneui.version.60100" /> 
         <feature name="com.samsung.android.oneui.version.60101" /> 
         <feature name="com.samsung.android.oneui.version.70000" />
         <feature name="com.samsung.android.oneui.version.80000" /> 
         <feature name="com.samsung.android.oneui.version.90000" /> 
         <feature name="com.samsung.android.oneui.version.90000" /> 
        </permissions>
    """.trimIndent()

    private val targetPath = "/system/etc/permissions/com.samsung.android.oneui.version.xml"
    private val xmlBytes = customXmlContent.toByteArray(Charsets.UTF_8)
    private var buildFieldsSpoofed = false

    private fun loadPropsFromAssets(lpparam: LoadPackageParam) {
        if (spoofedProps.isNotEmpty()) return
        try {
            val modRes = XModuleResources.createInstance(lpparam.classLoader.getResource("")?.path, null)
            val inputStream = modRes.assets.open("prop.txt")
            val properties = Properties()
            properties.load(InputStreamReader(inputStream, Charsets.UTF_8))
            inputStream.close()

            for (key in properties.stringPropertyNames()) {
                properties.getProperty(key)?.let { value ->
                    spoofedProps[key] = value
                }
            }

            // Map loaded properties to local variables for Build fields fallback
            spoofModel = spoofedProps["ro.product.model"] ?: spoofModel
            spoofBrand = spoofedProps["ro.product.brand"] ?: spoofBrand
            spoofDevice = spoofedProps["ro.product.device"] ?: spoofDevice
            spoofProduct = spoofedProps["ro.product.name"] ?: spoofProduct
            spoofFingerprint = spoofedProps["ro.build.fingerprint"] ?: spoofFingerprint
            spoofDisplay = spoofedProps["ro.build.display.id"] ?: spoofDisplay
            spoofTags = spoofedProps["ro.build.tags"] ?: spoofTags
            spoofType = spoofedProps["ro.build.type"] ?: spoofType
            spoofDescription = spoofedProps["ro.build.description"] ?: spoofDescription
            spoofRelease = spoofedProps["ro.build.version.release"] ?: spoofRelease
            spoofSecurityPatch = spoofedProps["ro.build.version.security_patch"] ?: spoofSecurityPatch
            spoofSdkInt = spoofedProps["ro.build.version.sdk"]?.toIntOrNull() ?: spoofSdkInt
            spoofSemInt = spoofedProps["ro.build.version.sep"]?.toIntOrNull() ?: spoofSemInt

        } catch (t: Throwable) {
            // Fallback or handle error if asset cannot be read
        }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == null) return

        // Load properties from assets/prop.txt first
        loadPropsFromAssets(lpparam)

        // =========================================================
        // LAYER 1: SYSTEM PROPERTIES & INT OVERRIDES (All Apps)
        // =========================================================
        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            XposedHelpers.findAndHookMethod(systemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { str ->
                            try {
                                param.result = str.toInt()
                            } catch (_: NumberFormatException) {}
                        }
                    }
                })

            try {
                XposedHelpers.findAndHookMethod(systemPropertiesClass, "getLong", String::class.java, Long::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as? String ?: return
                            spoofedProps[key]?.let { str ->
                                try {
                                    param.result = str.toLong()
                                } catch (_: NumberFormatException) {}
                            }
                        }
                    })
            } catch (_: Throwable) {}

        } catch (t: Throwable) {}

        // 1B. Spoof Samsung's proprietary SemSystemProperties wrapper
        try {
            val semSystemPropertiesClass = XposedHelpers.findClass("android.os.SemSystemProperties", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { str ->
                            try {
                                param.result = str.toInt()
                            } catch (_: NumberFormatException) {}
                        }
                    }
                })
        } catch (t: Throwable) {}

        // 1C. Spoof PackageManager.hasSystemFeature for OneUI versions
        try {
            val featureHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val featureName = param.args[0] as? String ?: return
                    if (featureName.startsWith("com.samsung.android.oneui.version")) {
                        param.result = true
                    }
                }
            }
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader,
                "hasSystemFeature", String::class.java, featureHook)
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader,
                "hasSystemFeature", String::class.java, Int::class.javaPrimitiveType, featureHook)
        } catch (t: Throwable) {}

        // =========================================================
        // LAYER 2: STATIC Build FIELDS (Set once per process)
        // =========================================================
        if (!buildFieldsSpoofed) {
            try {
                val buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader)
                XposedHelpers.setStaticObjectField(buildClass, "MODEL", spoofModel)
                XposedHelpers.setStaticObjectField(buildClass, "BRAND", spoofBrand)
                XposedHelpers.setStaticObjectField(buildClass, "DEVICE", spoofDevice)
                XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", spoofProduct)
                XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT", spoofFingerprint)
                XposedHelpers.setStaticObjectField(buildClass, "DISPLAY", spoofDisplay)
                XposedHelpers.setStaticObjectField(buildClass, "TAGS", spoofTags)
                XposedHelpers.setStaticObjectField(buildClass, "TYPE", spoofType)
                XposedHelpers.setStaticObjectField(buildClass, "DESCRIPTION", spoofDescription)

                val versionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                XposedHelpers.setStaticObjectField(versionClass, "RELEASE", spoofRelease)
                XposedHelpers.setStaticObjectField(versionClass, "SECURITY_PATCH", spoofSecurityPatch)
                XposedHelpers.setStaticIntField(versionClass, "SDK_INT", spoofSdkInt)
                XposedHelpers.setStaticIntField(versionClass, "SEM_PLATFORM_INT", spoofSemInt)
                XposedHelpers.setStaticIntField(versionClass, "SEM_INT", spoofSemInt)

                buildFieldsSpoofed = true
            } catch (t: Throwable) {}
        }

        // =========================================================
        // LAYER 3: VIRTUAL FILE SIMULATION FOR OneUI XML
        // =========================================================
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "exists", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = true
                    }
                }
            })

            XposedHelpers.findAndHookMethod(File::class.java, "length", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = xmlBytes.size.toLong()
                    }
                }
            })

            val fileStreamHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args[0]
                    val path = if (arg is File) arg.absolutePath else arg as? String

                    if (path == targetPath) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "isOneUISpoofStream", true)
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "spoofStream", ByteArrayInputStream(xmlBytes))
                        param.args[0] = if (arg is File) File("/dev/null") else "/dev/null"
                    }
                }
            }
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, File::class.java, fileStreamHook)
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, String::class.java, fileStreamHook)

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.read()
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        param.result = bis.read(b)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "available", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.available()
                    }
                }
            })
        } catch (t: Throwable) {}
    }
}
