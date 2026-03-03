package com.prettierjavaplugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import java.io.File
import javax.swing.JComponent

/**
 * Connects the Settings panel to IntelliJ's Settings system.
 * Appears as Settings → Tools → Prettier Java.
 */
class PrettierJavaSettingsConfigurable : Configurable {

    private var myPanel: PrettierJavaSettingsPanel? = null

    override fun getDisplayName(): String = "Prettier Java"

    override fun createComponent(): JComponent {
        myPanel = PrettierJavaSettingsPanel()
        reset()
        return myPanel!!.panel
    }

    override fun isModified(): Boolean {
        val settings = PrettierJavaSettings.getInstance().state
        return myPanel?.isModified(settings) ?: false
    }

    override fun apply() {
        val settings = PrettierJavaSettings.getInstance().state
        myPanel?.apply(settings)
        
        // Reactive update: update printWidth and tabWidth in existing .prettierrc files across open projects
        if (settings.globalProfile == "Custom") {
            try {
                val openProjects = ProjectManager.getInstance().openProjects
                for (project in openProjects) {
                    val basePath = project.basePath ?: continue
                    val rootDir = File(basePath)
                    if (rootDir.exists() && rootDir.isDirectory) {
                        val rcFile = File(rootDir, ".prettierrc")
                        if (rcFile.exists() && rcFile.isFile) {
                            var content = rcFile.readText()
                            
                            // Replace printWidth and tabWidth using regex
                            val newPrintWidth = settings.customPrintWidth
                            val newTabWidth = settings.customTabWidth
                            
                            content = content.replace(Regex("\"printWidth\"\\s*:\\s*\\d+"), "\"printWidth\": $newPrintWidth")
                            content = content.replace(Regex("\"tabWidth\"\\s*:\\s*\\d+"), "\"tabWidth\": $newTabWidth")
                            
                            rcFile.writeText(content)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent update errors at Settings
            }
        }
    }

    override fun reset() {
        val settings = PrettierJavaSettings.getInstance().state
        myPanel?.reset(settings)
    }

    override fun disposeUIResources() {
        myPanel = null
    }
}
