package com.prettierjavaplugin

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel

/**
 * Swing panel for the Prettier Java settings UI.
 * Shown in Settings → Tools → Prettier Java.
 */
class PrettierJavaSettingsPanel {

    val panel: JPanel = JPanel(GridBagLayout())

    // Controls
    private val enabledCheckBox     = JBCheckBox("Enable Prettier Java formatter")
    private val formatOnSaveCheckBox = JBCheckBox("Format on Save (runs Prettier every time you save a .java file)")
    private val profileCombo        = ComboBox(arrayOf("Enterprise/Spring", "Google Style", "Custom"))

    init {
        buildUI()
    }

    private fun buildUI() {
        val gbc = GridBagConstraints()
        gbc.fill   = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(4, 8, 4, 8)

        var row = 0

        // Helper: full-width row (spans 2 columns)
        fun addFullRow(component: javax.swing.JComponent) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0
            panel.add(component, gbc)
            gbc.gridwidth = 1
            row++
        }

        // Helper: label + control row
        fun addRow(label: String, component: javax.swing.JComponent) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35
            panel.add(JBLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 0.65
            panel.add(component, gbc)
            row++
        }

        addFullRow(enabledCheckBox)
        addFullRow(formatOnSaveCheckBox)
        addRow("Formatting Profile:", profileCombo)
        
        // Note: explanation label
        val explanationLabel = JBLabel("<html><small>" +
                "<b>Enterprise/Spring:</b> printWidth=120, tabWidth=4, useTabs=true<br>" +
                "<b>Google Style:</b> printWidth=100, tabWidth=2, useTabs=false<br>" +
                "<b>Custom:</b> Relies on your local .prettierrc configuration" +
                "</small></html>")
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        panel.add(explanationLabel, gbc)
        row++

        // Vertical spacer
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(JPanel(), gbc)
    }

    /** Applies UI values → settings state. */
    fun apply(settings: PrettierJavaSettings.State) {
        settings.enabled       = enabledCheckBox.isSelected
        settings.formatOnSave  = formatOnSaveCheckBox.isSelected
        settings.globalProfile = profileCombo.selectedItem as String
    }

    /** Resets UI from settings state. */
    fun reset(settings: PrettierJavaSettings.State) {
        enabledCheckBox.isSelected      = settings.enabled
        formatOnSaveCheckBox.isSelected = settings.formatOnSave
        profileCombo.selectedItem       = settings.globalProfile
    }

    /** Returns true if UI values differ from the saved settings state. */
    fun isModified(settings: PrettierJavaSettings.State): Boolean =
        enabledCheckBox.isSelected      != settings.enabled ||
        formatOnSaveCheckBox.isSelected != settings.formatOnSave ||
        profileCombo.selectedItem as String != settings.globalProfile
}
