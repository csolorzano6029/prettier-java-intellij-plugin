package com.prettierjavaplugin

import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.EnumSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Core formatting service that integrates Prettier with IntelliJ's Reformat Code (Ctrl+Alt+L).
 *
 * On first use, extracts format.js and node_modules.zip from the plugin JAR to a temp directory.
 * Then calls Node.js as a subprocess passing Java code via stdin and reading formatted code from stdout.
 */
class PrettierJavaFormattingService : AsyncDocumentFormattingService() {

    private val log = thisLogger()

    override fun getNotificationGroupId(): String = "Prettier Java"

    override fun getName(): String = "Prettier Java"

    override fun getFeatures(): Set<FormattingService.Feature> =
        EnumSet.noneOf(FormattingService.Feature::class.java)

    override fun canFormat(file: PsiFile): Boolean {
        val settings = PrettierJavaSettings.getInstance().state
        return settings.enabled && file.name.endsWith(".java")
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val settings = PrettierJavaSettings.getInstance().state
        val code = request.documentText
        // Get the file path so format.js can call prettier.resolveConfig() for .prettierrc
        val filePath = try {
            request.context.virtualFile?.path ?: ""
        } catch (_: Exception) { "" }

        return object : FormattingTask {
            @Volatile
            private var cancelled = false

            override fun run() {
                if (cancelled) return
                try {
                    val result = runPrettier(code, settings, filePath)
                    if (result != null) {
                        request.onTextReady(result)
                    } else {
                        request.onError(
                            "Prettier Java",
                            "Prettier returned empty output. Check Settings → Tools → Prettier Java."
                        )
                    }
                } catch (e: Exception) {
                    log.warn("Prettier Java formatting failed", e)
                    request.onError("Prettier Java", e.message ?: "Unknown error.")
                }
            }

            override fun cancel(): Boolean {
                cancelled = true
                return true
            }
        }
    }

    companion object {
        private val log = thisLogger()

        @Volatile
        private var tempDir: File? = null

        /**
         * Runs Prettier on [code] using the given [settings].
         * Reads stdout/stderr concurrently to prevent deadlocks and capture real error messages.
         */
        fun runPrettier(code: String, settings: PrettierJavaSettings.State, filePath: String = ""): String? {
            val prettierDir = extractPrettierResources()
            val formatScript = File(prettierDir, "format.js")
            val optionsJson = buildOptionsJson(settings, filePath)

            // New protocol: first line of stdin = JSON options, rest = Java code
            // This avoids Windows CLI argument quoting issues with JSON double-quotes
            val input = (optionsJson + "\n" + code).toByteArray(Charsets.UTF_8)

            val process = ProcessBuilder(settings.nodePath, formatScript.absolutePath)
                .directory(prettierDir)
                .redirectErrorStream(false)
                .start()

            // Start reading stdout/stderr concurrently BEFORE writing stdin (prevents deadlock)
            val stdoutFuture = CompletableFuture.supplyAsync {
                process.inputStream.readBytes().toString(Charsets.UTF_8)
            }
            val stderrFuture = CompletableFuture.supplyAsync {
                process.errorStream.readBytes().toString(Charsets.UTF_8)
            }

            // Write options + code to stdin
            try {
                process.outputStream.use { it.write(input) }
            } catch (_: IOException) {
                // Node exited early — stderr will explain why
            }

            val exited = process.waitFor(60, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                throw RuntimeException("Prettier timed out after 60 seconds")
            }

            val stdout = stdoutFuture.get(5, TimeUnit.SECONDS)
            val stderr = stderrFuture.get(5, TimeUnit.SECONDS)

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                throw RuntimeException(
                    if (stderr.isNotBlank()) stderr.trim()
                    else "Prettier exited with code $exitCode"
                )
            }

            return stdout.ifBlank { null }
        }

        private fun buildOptionsJson(settings: PrettierJavaSettings.State, filePath: String = ""): String {
            // filePath is passed so format.js can call prettier.resolveConfig() for .prettierrc
            val escapedPath = filePath.replace("\\", "/")
            return """{"filePath":"$escapedPath","printWidth":${settings.printWidth},"tabWidth":${settings.tabWidth},"useTabs":${settings.useTabs},"trailingComma":"${settings.trailingComma}","semi":${settings.semi},"singleQuote":${settings.singleQuote}}"""
        }

        /**
         * Extracts plugin resources (format.js + node_modules.zip) to a temp dir on first use.
         */
        @Synchronized
        fun extractPrettierResources(): File {
            val existing = tempDir
            if (existing != null && existing.exists() && File(existing, "format.js").exists()) {
                return existing
            }

            log.info("Prettier Java: Extracting resources to temp directory...")
            val dir = Files.createTempDirectory("prettier-java-intellij").toFile()

            // Copy format.js
            val formatJsStream = PrettierJavaFormattingService::class.java
                .getResourceAsStream("/prettier-node/format.js")
                ?: throw RuntimeException("format.js not found in plugin JAR.")
            File(dir, "format.js").writeBytes(formatJsStream.readBytes())

            // Extract node_modules.zip → node_modules/
            val nodeModulesZipStream = PrettierJavaFormattingService::class.java
                .getResourceAsStream("/prettier-node/node_modules.zip")
                ?: throw RuntimeException("node_modules.zip not found in plugin JAR.")

            val nodeModulesDir = File(dir, "node_modules")
            nodeModulesDir.mkdirs()

            ZipInputStream(nodeModulesZipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(nodeModulesDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        destFile.writeBytes(zis.readBytes())
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            log.info("Prettier Java: Resources extracted to ${dir.absolutePath}")
            tempDir = dir
            return dir
        }
    }
}
