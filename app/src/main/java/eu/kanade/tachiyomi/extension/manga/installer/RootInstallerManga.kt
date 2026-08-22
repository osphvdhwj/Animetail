package eu.kanade.tachiyomi.extension.manga.installer

import android.app.Service
import android.os.Process
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.util.system.getUriSize
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream

class RootInstallerManga(private val service: Service) : InstallerManga(service) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override var ready = false

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        scope.launch {
            var sessionId: String? = null
            try {
                val size = service.getUriSize(entry.uri) ?: throw IllegalStateException()
                service.contentResolver.openInputStream(entry.uri)!!.use { apkStream ->
                    val userId = Process.myUserHandle().hashCode()
                    val createCommand = "pm install-create --user $userId -r -i ${service.packageName} -S $size\n"
                    val createResult = execRoot(createCommand)
                    sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
                        ?: throw RuntimeException("Failed to create root install session: ${createResult.out}")

                    val writeCommand = "pm install-write -S $size $sessionId base -\n"
                    val writeResult = execRoot(writeCommand, apkStream)
                    if (writeResult.resultCode != 0) {
                        throw RuntimeException("Failed to write APK to root session $sessionId")
                    }

                    val commitResult = execRoot("pm install-commit $sessionId\n")
                    if (commitResult.resultCode != 0) {
                        throw RuntimeException("Failed to commit root install session $sessionId")
                    }

                    continueQueue(InstallStep.Installed)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to root-install extension ${entry.downloadId} ${entry.uri}" }
                if (sessionId != null) {
                    try {
                        execRoot("pm install-abandon $sessionId\n")
                    } catch (_: Exception) {}
                }
                continueQueue(InstallStep.Error)
            }
        }
    }

    override fun cancelEntry(entry: Entry): Boolean = getActiveEntry() != entry

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun execRoot(command: String, stdin: InputStream? = null): ShellResult {
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { out ->
            if (stdin != null) {
                out.write(command.toByteArray())
                out.flush()
                stdin.copyTo(out)
            } else {
                out.write(command.toByteArray())
            }
            out.flush()
        }
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val resultCode = process.waitFor()
        return ShellResult(resultCode, output)
    }

    private data class ShellResult(val resultCode: Int, val out: String)

    init {
        scope.launch {
            try {
                val testProcess = Runtime.getRuntime().exec("su")
                testProcess.outputStream.use { out ->
                    out.write("id\n".toByteArray())
                    out.flush()
                }
                val output = testProcess.inputStream.bufferedReader().use(BufferedReader::readText)
                val resultCode = testProcess.waitFor()
                if (resultCode == 0 && (output.contains("uid=0") || output.contains("root"))) {
                    ready = true
                    checkQueue()
                } else {
                    throw Exception("Su returned non-zero code or is not root")
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Root is not ready or su permission denied" }
                service.toast("Root access denied or su binary missing")
                service.stopSelf()
            }
        }
    }
}

private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
