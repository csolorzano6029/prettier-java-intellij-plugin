package com.prettierjavaplugin

import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.options.NodeRuntimeOptions
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.file.Files
import java.util.EnumSet
import java.util.zip.ZipInputStream

/**
 * Core formatting service that integrates Prettier with IntelliJ's Reformat Code (Ctrl+Alt+L).
 *
 * Uses Javet to embed an in-memory V8 Node.js runtime, bypassing the need for an external node process.
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
        val filePath = try {
            request.context.virtualFile?.path ?: ""
        } catch (_: Exception) { "" }
        val projectBasePath = try {
            request.context.project.basePath
        } catch (_: Exception) { null }

        return object : FormattingTask {
            @Volatile
            private var cancelled = false

            override fun run() {
                if (cancelled) return
                try {
                    val result = runPrettier(code, settings, filePath, projectBasePath)
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

        private val v8Executor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            // Use a larger stack size (4MB) for the native V8 engine
            Thread(null, r, "PrettierJava-V8-Worker", 4 * 1024 * 1024).apply { isDaemon = true }
        }

        /**
         * Runs Prettier on [code] using the given [settings] inside a transient Javet V8 engine.
         * Executes on a dedicated background thread to prevent native thread affinity crashes and concurrency issues.
         */
        fun runPrettier(code: String, settings: PrettierJavaSettings.State, filePath: String = "", projectBasePath: String? = null): String? {
            log.info("Prettier Java: Scheduling formatting for '$filePath'")
            
            return try {
                // Remove the .get() timeout to avoid unsafe native thread interruption
                v8Executor.submit(java.util.concurrent.Callable {
                    runPrettierInternal(code, settings, filePath, projectBasePath)
                }).get() 
            } catch (t: Throwable) {
                log.error("Prettier Java: Error during formatting task", t)
                null
            }
        }

        private fun runPrettierInternal(code: String, settings: PrettierJavaSettings.State, filePath: String, projectBasePath: String?): String? {
            log.info("Prettier Java: Starting runPrettierInternal on ${Thread.currentThread().name}")
            System.err.println("Prettier Java [NATIVE TRACE]: Starting runPrettierInternal")
            
            // --- Custom Profile Auto-Generation Logic --- //
            if (settings.globalProfile == "Custom" && projectBasePath != null) {
                try {
                    val rootDir = File(projectBasePath)
                    if (rootDir.exists() && rootDir.isDirectory) {
                        // Check if any prettierrc file exists
                        val hasPrettierRc = rootDir.listFiles()?.any { it.name.startsWith(".prettierrc") || it.name == "prettier.config.js" } ?: false
                        
                        if (!hasPrettierRc) {
                            val newRcFile = File(rootDir, ".prettierrc")
                            val content = """
                                {
                                  "printWidth": ${settings.customPrintWidth},
                                  "tabWidth": ${settings.customTabWidth}
                                }
                            """.trimIndent()
                            newRcFile.writeText(content)
                            log.info("Prettier Java: Auto-created .prettierrc at ${newRcFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    log.error("Prettier Java: Failed to auto-generate .prettierrc", e)
                }
            }
            // -------------------------------------------- //
            
            val optionsJson = buildOptionsJson(settings, filePath)
            val prettierDir = extractPrettierResources()
            val formatScript = File(prettierDir, "format.js").absolutePath.replace("\\", "/")

            log.info("Prettier Java: Creating V8 Runtime (Node mode)...")
            System.err.println("Prettier Java [NATIVE TRACE]: Creating V8 Runtime")
            
            // Basic runtime creation
            val runtime: com.caoccao.javet.interop.V8Runtime = V8Host.getNodeInstance().createV8Runtime()
            var result: String? = null
            
            try {
                log.info("Prettier Java: Initializing format.js...")
                System.err.println("Prettier Java [NATIVE TRACE]: Initializing format.js")
                
                val initScript = """
                    const formatModule = require('$formatScript');
                    globalThis.formatCode = formatModule.formatCode;
                """.trimIndent()
                
                runtime.getExecutor(initScript).executeVoid()

                log.info("Prettier Java: Calling formatCode JS function...")
                System.err.println("Prettier Java [NATIVE TRACE]: Calling formatCode")
                val globalObj = runtime.getGlobalObject()
                
                // Get the function as a V8Value and cast it
                val formatFn = globalObj.get<com.caoccao.javet.values.reference.V8ValueFunction>("formatCode")
                
                try {
                    val promise = formatFn.call<com.caoccao.javet.values.reference.V8ValuePromise>(null, code as Any, optionsJson as Any)
                    try {
                        log.info("Prettier Java: Awaiting JS Promise...")
                        System.err.println("Prettier Java [NATIVE TRACE]: Awaiting JS Promise")
                        runtime.await() // Process Node.js Event Loop
                        
                        System.err.println("Prettier Java [NATIVE TRACE]: Promise resolved")
                        if (promise.isRejected) {
                            val errorValue = promise.getResult<com.caoccao.javet.values.V8Value>()
                            val error = errorValue.toString()
                            errorValue.close()
                            log.warn("Prettier Java: JS Promise REJECTED: $error")
                        } else {
                            val v8Result = promise.getResult<com.caoccao.javet.values.V8Value>()
                            val formatted = v8Result.toString()
                            v8Result.close()
                            
                            log.info("Prettier Java: Format successful, result length: ${formatted.length}")
                            if (formatted.isNotBlank()) {
                                result = formatted
                            }
                        }
                    } finally {
                        promise.close()
                    }
                } finally {
                    formatFn.close()
                    globalObj.close()
                }
            } catch (t: Throwable) {
                log.error("Prettier Java: V8 Internal Error", t)
                System.err.println("Prettier Java [NATIVE TRACE]: ERROR: ${t.message}")
                t.printStackTrace()
            } finally {
                log.info("Prettier Java: Closing V8 Runtime...")
                System.err.println("Prettier Java [NATIVE TRACE]: Closing V8 Runtime")
                try {
                    runtime.close()
                    log.info("Prettier Java: Runtime closed successfully.")
                } catch (e: Exception) {
                    log.error("Prettier Java: Error closing runtime", e)
                }
            }
            
            return result
        }

        private fun buildOptionsJson(settings: PrettierJavaSettings.State, filePath: String = ""): String {
            val escapedPath = filePath.replace("\\", "/")
            
            return when (settings.globalProfile) {
                "Enterprise/Spring" -> {
                    """{"filePath":"$escapedPath","printWidth":120,"tabWidth":4,"useTabs":true,"trailingComma":"none","semi":true,"singleQuote":false}"""
                }
                "Google Style" -> {
                    """{"filePath":"$escapedPath","printWidth":100,"tabWidth":2,"useTabs":false,"trailingComma":"none","semi":true,"singleQuote":false}"""
                }
                else -> {
                    // "Custom" or unknown profile -> rely on .prettierrc in the project
                    """{"filePath":"$escapedPath"}"""
                }
            }
        }

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

            var extractedCount = 0
            ZipInputStream(nodeModulesZipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(nodeModulesDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val prettierIndex = File(nodeModulesDir, "prettier/index.js")
            val prettierExists = prettierIndex.exists()
            
            val msg = "Prettier Java [NATIVE TRACE]: [V5] Resources extracted. Total files: $extractedCount. Prettier index exists: $prettierExists"
            log.info(msg)
            System.err.println(msg)
            
            if (!prettierExists) {
                System.err.println("Prettier Java [NATIVE TRACE]: CRITICAL - prettier index NOT FOUND at ${prettierIndex.absolutePath}")
            }

            log.info("Prettier Java: Resources extracted to ${dir.absolutePath}")
            tempDir = dir
            return dir
        }
    }
}
