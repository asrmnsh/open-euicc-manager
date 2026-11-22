
package org.openeuicc.manager

import android.os.Build
import android.se.omapi.SEService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
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
    else -> result.notImplemented()
            }
        }
    }

    
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
                    executor.shutdown()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    result.error(
                        "OMAPI_ERROR",
                        e.message,
                        null
                    )
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

private fun testSim1(result: MethodChannel.Result) {
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

        service = SEService(this, executor) {
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
                        executor.shutdown()
                    }
                    return@SEService
                }

                val session = reader.openSession()
                val channel = session.openLogicalChannel(
                    byteArrayOf(
                        0xA0.toByte(),
                        0x00,
                        0x00,
                        0x00,
                        0x01
                    )
                )

                val opened = channel != null

                channel?.close()
                session.close()

                runOnUiThread {
                    result.success(
                        mapOf(
                            "success" to opened,
                            "message" to if (opened)
                                "تم فتح القناة بنجاح"
                            else
                                "تعذر فتح القناة"
                        )
                    )
                    executor.shutdown()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    result.success(
                        mapOf(
                            "success" to false,
                            "message" to (e.message ?: "فشل الاتصال")
                        )
                    )
                    executor.shutdown()
                }
            }
        }
    } catch (e: Exception) {
        executor.shutdown()
        result.success(
            mapOf(
                "success" to false,
                "message" to (e.message ?: "خطأ في OMAPI")
            )
        )
    }
}
}
