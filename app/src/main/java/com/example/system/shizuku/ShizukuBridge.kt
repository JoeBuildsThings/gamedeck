package com.example.system.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

enum class ShizukuStatus {
    NOT_INSTALLED,
    DEAD,
    PERMISSION_DENIED,
    READY
}

class ShizukuBridge(private val context: Context) {

    private val _status = MutableStateFlow(ShizukuStatus.DEAD)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var newProcessMethod: Method? = null
    private var monitorJob: Job? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus.DEAD
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            _status.value = ShizukuStatus.READY
        } else {
            _status.value = ShizukuStatus.PERMISSION_DENIED
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            initReflection()
            updateStatus()
        } catch (_: Throwable) {
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    private fun initReflection() {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            newProcessMethod = method
        } catch (_: Throwable) {}
    }

    fun startContinuousMonitor(scope: CoroutineScope) {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val prev = _status.value
                    val current = updateStatus()
                    // If Shizuku was dead but now binder is responsive, try re-attaching
                    if (prev == ShizukuStatus.DEAD && current == ShizukuStatus.READY) {
                        initReflection()
                    }
                } catch (_: Throwable) {}
                delay(3000L) // Poll every 3 seconds for background connection recovery
            }
        }
    }

    fun stopContinuousMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun updateStatus(): ShizukuStatus {
        val newStatus = try {
            if (!isShizukuInstalled()) {
                ShizukuStatus.NOT_INSTALLED
            } else if (!Shizuku.pingBinder()) {
                ShizukuStatus.DEAD
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                ShizukuStatus.PERMISSION_DENIED
            } else {
                ShizukuStatus.READY
            }
        } catch (_: Throwable) {
            ShizukuStatus.DEAD
        }
        _status.value = newStatus
        return newStatus
    }

    fun isAvailableAndGranted(): Boolean {
        return updateStatus() == ShizukuStatus.READY
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Throwable) {}
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Open Play Store
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            }
        } catch (_: Throwable) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun openWirelessDebuggingSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
    }

    /**
     * Executes a shell command via Shizuku with a timeout. Never blocks main thread.
     */
    suspend fun exec(command: String, timeoutMs: Long = 3000L): Result<String> = withContext(Dispatchers.IO) {
        if (!isAvailableAndGranted()) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not running or permission is denied"))
        }

        if (newProcessMethod == null) {
            initReflection()
        }

        val method = newProcessMethod
            ?: return@withContext Result.failure(IllegalStateException("Shizuku newProcess method unavailable"))

        try {
            withTimeout(timeoutMs) {
                val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                reader.close()
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    Result.success(output.toString())
                } else {
                    Result.failure(RuntimeException("Command failed with exit code $exitCode: $command"))
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun cleanUp() {
        stopContinuousMonitor()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {}
    }
}
