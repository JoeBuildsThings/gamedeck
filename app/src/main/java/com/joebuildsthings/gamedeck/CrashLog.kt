package com.joebuildsthings.gamedeck

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Saves the stack trace of any uncaught crash to app storage, then lets Android
 * kill the process as usual. MainActivity shows the saved trace on next launch,
 * so no logcat or adb is needed to see what went wrong.
 */
object CrashLog {

    private const val FILE_NAME = "last_crash.txt"
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val writer = StringWriter()
                error.printStackTrace(PrintWriter(writer))
                File(appContext.filesDir, FILE_NAME)
                    .writeText("Thread: ${thread.name}\n\n$writer")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun consume(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull()
        file.delete()
        return text
    }
}
