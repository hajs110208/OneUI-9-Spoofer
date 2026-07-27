package com.HeheJuice.OneUINineSpoof

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XC_MethodHook
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class HeheJuiceSpoof : IXposedHookLoadPackage {

    // =========================================================
    // Spoofed system properties (values from prop.txt)
    // =========================================================
    private val spoofedProps = mapOf(
        // Device identification
        "ro.product.brand" to "samsung",
        "ro.product.device" to "projectv2ul",
        "ro.product.manufacturer" to "samsung",
        "ro.product.model" to "SM-L715U",
        "ro.product.name" to "projectv2ulue",
        // Build fingerprint & metadata
        "ro.build.fingerprint" to "samsung/projectv2ulue/projectv2ul:17/CP2A.260330.023/L715USQU1AZFL:user/release-keys",
        "ro.build.id" to "CP2A.260330.023",
        "ro.build.display.id" to "CP2A.260330.023.L715USQU1AZFL",
        "ro.build.version.incremental" to "L715USQU1AZFL",
        "ro.build.version.release" to "17",
        "ro.build.version.sdk" to "37",
        "ro.build.version.security_patch" to "2026-05-05",
        "ro.build.date.utc" to "1782469254",
        "ro.build.tags" to "release-keys",
        "ro.build.type" to "user",
        "ro.build.description" to "projectv2ulue-user 17 CP2A.260330.023 L715USQU1AZFL release-keys",
        // OneUI & SEP versions (already present, kept for completeness)
        "ro.build.version.oneui" to "90000",
        "ro.build.version.sep" to "180000",
        // Additional optional props from prop.txt (can be extended)
        "ro.product.locale" to "en-GB",
        "ro.wifi.channels" to "",
        "ro.carrier" to "unknown",
        "ro.cw_build.wear_sdk.version" to "7"
    )

    // =========================================================
    // Custom OneUI feature XML (unchanged)
    // =========================================================
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

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == null) return

        // =========================================================
        // LAYER 1: SYSTEM PROPERTIES & INT OVERRIDES (All Apps)
        // =========================================================
        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            // Hook get(String)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            // Hook get(String, String)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            // Hook getInt(String, int)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { str ->
                            try {
                                param.result = str.toInt()
                            } catch (_: NumberFormatException) {
                                // keep default
                            }
                        }
                    }
                })

            // Hook getLong(String, long) if needed
            try {
                XposedHelpers.findAndHookMethod(systemPropertiesClass, "getLong", String::class.java, Long::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as? String ?: return
                            spoofedProps[key]?.let { str ->
                                try {
                                    param.result = str.toLong()
                                } catch (_: NumberFormatException) {
                                    // keep default
                                }
                            }
                        }
                    })
            } catch (_: Throwable) {}

        } catch (t: Throwable) {
            // Ignore if class not found
        }

        // 1B. Spoof Samsung's proprietary SemSystemProperties wrapper
        try {
            val semSystemPropertiesClass = XposedHelpers.findClass("android.os.SemSystemProperties", lpparam.classLoader)

            // Hook get(String)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            // Hook get(String, String)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { param.result = it }
                    }
                })

            // Hook getInt(String, int)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        spoofedProps[key]?.let { str ->
                            try {
                                param.result = str.toInt()
                            } catch (_: NumberFormatException) {
                                // keep default
                            }
                        }
                    }
                })

            // Hook getLong if exists
            try {
                XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getLong", String::class.java, Long::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as? String ?: return
                            spoofedProps[key]?.let { str ->
                                try {
                                    param.result = str.toLong()
                                } catch (_: NumberFormatException) {
                                    // keep default
                                }
                            }
                        }
                    })
            } catch (_: Throwable) {}

        } catch (t: Throwable) {
            // Ignore if class not found
        }

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
                // android.os.Build
                val buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader)
                XposedHelpers.setStaticObjectField(buildClass, "MODEL", "SM-L715U")
                XposedHelpers.setStaticObjectField(buildClass, "BRAND", "samsung")
                XposedHelpers.setStaticObjectField(buildClass, "DEVICE", "projectv2ul")
                XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", "projectv2ulue")
                XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT", "samsung/projectv2ulue/projectv2ul:17/CP2A.260330.023/L715USQU1AZFL:user/release-keys")
                XposedHelpers.setStaticObjectField(buildClass, "DISPLAY", "CP2A.260330.023.L715USQU1AZFL")
                XposedHelpers.setStaticObjectField(buildClass, "TAGS", "release-keys")
                XposedHelpers.setStaticObjectField(buildClass, "TYPE", "user")
                XposedHelpers.setStaticObjectField(buildClass, "DESCRIPTION", "projectv2ulue-user 17 CP2A.260330.023 L715USQU1AZFL release-keys")

                // android.os.Build.VERSION
                val versionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
                XposedHelpers.setStaticObjectField(versionClass, "RELEASE", "17")
                XposedHelpers.setStaticObjectField(versionClass, "SECURITY_PATCH", "2026-05-05")
                XposedHelpers.setStaticIntField(versionClass, "SDK_INT", 37)
                // Also set SEM fields (already done in original code, but we keep)
                XposedHelpers.setStaticIntField(versionClass, "SEM_PLATFORM_INT", 180000)
                XposedHelpers.setStaticIntField(versionClass, "SEM_INT", 180000)

                buildFieldsSpoofed = true
            } catch (t: Throwable) {
                // Ignore
            }
        }

        // =========================================================
        // LAYER 3: VIRTUAL FILE SIMULATION FOR OneUI XML (unchanged)
        // =========================================================

        // 3A. Mock existence
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "exists", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = true
                    }
                }
            })
        } catch (t: Throwable) {}

        // 3B. Mock file length
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "length", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = xmlBytes.size.toLong()
                    }
                }
            })
        } catch (t: Throwable) {}

        // 3C. Intercept FileInputStream constructors to serve custom XML
        try {
            val fileStreamHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args[0]
                    val path = if (arg is File) arg.absolutePath else arg as? String

                    if (path == targetPath) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "isOneUISpoofStream", true)
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "spoofStream", ByteArrayInputStream(xmlBytes))
                        if (arg is File) {
                            param.args[0] = File("/dev/null")
                        } else {
                            param.args[0] = "/dev/null"
                        }
                    }
                }
            }
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, File::class.java, fileStreamHook)
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, String::class.java, fileStreamHook)
        } catch (t: Throwable) {}

        // 3D. Redirect read() methods to the spoofed stream
        try {
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

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        val off = param.args[1] as Int
                        val len = param.args[2] as Int
                        param.result = bis.read(b, off, len)
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