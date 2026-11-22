
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

            if (call.method == "discover") {
                discoverReaders(result)
            } else {
                result.notImplemented()
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

        try {
            val executor = Executors.newSingleThreadExecutor()

            SEService(
                this,
                executor
            ) { service ->

                try {
                    val readers: List<String> = if (service.isConnected) {
    service.getReaders().map { reader -> reader.name }
} else {
    emptyList()
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
            result.error(
                "OMAPI_ERROR",
                e.message,
                null
            )
        }
    }
}
