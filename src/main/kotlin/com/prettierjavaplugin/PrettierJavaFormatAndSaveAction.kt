package com.prettierjavaplugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager

/**
 * Replaces IntelliJ's built-in "SaveAll" action (Ctrl+S) using overrides="true" in plugin.xml.
 *
 * When "Format on Save" is enabled and the active editor is a Java file:
 *   1. Runs Prettier synchronously to format the document
 *   2. Then saves all documents to disk
 *
 * When format on save is disabled or the file is not Java:
 *   → Saves all documents normally (same as built-in Ctrl+S)
 */
class PrettierJavaFormatAndSaveAction : AnAction("Save All") {

    private val log = thisLogger()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        // Always visible and enabled — we handle all Ctrl+S cases
        e.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val settings = PrettierJavaSettings.getInstance().state
        val project = e.project

        // Step 1: Save immediately — no freeze, the user gets instant feedback
        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        // Step 2: If conditions met, format in background and auto-save again silently
        if (settings.enabled && settings.formatOnSave && project != null) {
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            if (file.extension?.lowercase() != "java") return

            val document = editor.document
            val code = document.text
            val filePath = file.path

            // Background thread — does NOT block the EDT
            ApplicationManager.getApplication().executeOnPooledThread {
                val formatted = try {
                    PrettierJavaFormattingService.runPrettier(code, settings, filePath, project.basePath)
                } catch (ex: Exception) {
                    log.warn("Prettier Java: Format on Save failed for '${file.name}'", ex)
                    null
                }

                if (formatted == null || formatted == code) return@executeOnPooledThread

                // Back on EDT: update document + silent second save
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(
                        project, "Prettier Java: Format", null,
                        { document.setText(formatted) }
                    )
                    // Auto-save the formatted version silently
                    ApplicationManager.getApplication().runWriteAction {
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                }
            }
        }
    }
}
