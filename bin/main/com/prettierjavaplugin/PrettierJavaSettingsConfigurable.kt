package com.prettierjavaplugin

import com.intellij.openapi.options.Configurable
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
    }

    override fun reset() {
        val settings = PrettierJavaSettings.getInstance().state
        myPanel?.reset(settings)
    }

    override fun disposeUIResources() {
        myPanel = null
    }
}
