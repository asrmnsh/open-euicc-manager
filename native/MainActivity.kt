
package org.openeuicc.manager

import android.os.Build
import android.content.pm.PackageManager
import android.se.omapi.SEService
import android.se.omapi.Session
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.security.MessageDigest
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {

    private val channelName =
        "org.openeuicc.manager/bridge"

    override fun configureFlutterEngine(
        flutterEngine: FlutterEngine
    ) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            channelName
        ).setMethodCallHandler { call, result ->

            when (call.method) {
                "discover" -> discoverReaders(result)
                "testSim1" -> testSim1(result)
                "getAppCertificate" -> getAppCertificate(result)
                else -> result.notImplemented()
            }
        }
    }

    // استخراج شهادات توقيع التطبيق
    
private fun getAppCertificate(
    result: MethodChannel.Result
) {
    try {
        val signatures = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        ) {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )

            info.signingInfo
                ?.apkContentsSigners
                ?: throw Exception("SigningInfo غير متاح")
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
                ?: throw Exception("Signatures غير متاحة")
        }

        if (signatures.isEmpty()) {
            throw Exception("لا توجد شهادة توقيع")
        }

        val certificates = signatures.map { signature ->
            val bytes = signature.toByteArray()

            fun fingerprint(algorithm: String): String {
                return MessageDigest
                    .getInstance(algorithm)
                    .digest(bytes)
                    .joinToString(":") {
                        "%02X".format(it.toInt() and 0xFF)
                    }
            }

            mapOf(
                "SHA1" to fingerprint("SHA-1"),
                "SHA256" to fingerprint("SHA-256")
            )
        }

        result.success(
            mapOf(
                "packageName" to packageName,
                "certificates" to certificates
            )
        )

    } catch (e: Exception) {
        result.error(
            "CERT_ERROR",
            e.message ?: "فشل استخراج شهادة التطبيق",
            null
        )
    }
}

    // اكتشاف قارئات OMAPI
    private fun discoverReaders(
        result: MethodChannel.Result
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            result.success(
                mapOf(
                    "omapiSupported" to false,
                    "readers" to emptyList<String>()
                )
            )
            return
        }

        val executor = Executors.newSingleThreadExecutor()

        try {
            lateinit var service: SEService

            service = SEService(
                this,
                executor
            ) {
                try {
                    val readers: List<String> =
                        service.readers.map { reader ->
                            reader.name
                        }

                    runOnUiThread {
                        result.success(
                            mapOf(
                                "omapiSupported" to true,
                                "readers" to readers
                            )
                        )
                        service.shutdown()
                        executor.shutdown()
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        result.error(
                            "OMAPI_ERROR",
                            e.message,
                            null
                        )
                        service.shutdown()
                        executor.shutdown()
                    }
                }
            }

        } catch (e: Exception) {
            executor.shutdown()

            result.error(
                "OMAPI_ERROR",
                e.message,
                null
            )
        }
    }

    // اختبار فتح وإغلاق Session على SIM1
    private fun testSim1(
        result: MethodChannel.Result
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            result.success(
                mapOf(
                    "success" to false,
                    "message" to "OMAPI غير مدعوم"
                )
            )
            return
        }

        val executor = Executors.newSingleThreadExecutor()

        try {
            lateinit var service: SEService

            service = SEService(
                this,
                executor
            ) {
                var session: Session? = null

                try {
                    val reader = service.readers.firstOrNull {
                        it.name == "SIM1"
                    }

                    if (reader == null) {
                        runOnUiThread {
                            result.success(
                                mapOf(
                                    "success" to false,
                                    "message" to "لم يتم العثور على SIM1"
                                )
                            )

                            service.shutdown()
                            executor.shutdown()
                        }

                        return@SEService
                    }

                    session = reader.openSession()

                    session?.close()
                    session = null

                    runOnUiThread {
                        result.success(
                            mapOf(
                                "success" to true,
                                "message" to
                                    "نجح فتح Session وإغلاقها على SIM1"
                            )
                        )

                        service.shutdown()
                        executor.shutdown()
                    }

                } catch (e: Exception) {
                    try {
                        session?.close()
                    } catch (_: Exception) {
                    }

                    runOnUiThread {
                        result.success(
                            mapOf(
                                "success" to false,
                                "message" to (
                                    e.message ?: "فشل فتح Session"
                                )
                            )
                        )

                        service.shutdown()
                        executor.shutdown()
                    }
                }
            }

        } catch (e: Exception) {
            executor.shutdown()

            result.success(
                mapOf(
                    "success" to false,
                    "message" to (
                        e.message ?: "خطأ في OMAPI"
                    )
                )
            )
        }
    }
}
