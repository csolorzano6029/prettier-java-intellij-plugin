package com.prettierjavaplugin

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Swing panel for the Prettier Java settings UI.
 * Shown in Settings → Tools → Prettier Java.
 */
class PrettierJavaSettingsPanel {

    val panel: JPanel = JPanel(GridBagLayout())

    // Controls
    private val enabledCheckBox         = JBCheckBox("Enable Prettier Java formatter")
    private val formatOnSaveCheckBox     = JBCheckBox("Format on Save (runs Prettier every time you save a .java file)")
    private val profileCombo            = ComboBox(arrayOf("Enterprise/Spring", "Google Style", "Custom"))
    
    // Custom Profile Controls
    private val customPrintWidthSpinner = JSpinner(SpinnerNumberModel(110, 1, 500, 1))
    private val customTabWidthSpinner   = JSpinner(SpinnerNumberModel(2, 1, 16, 1))
    
    // Labels for custom components to toggle visibility
    private val customPrintWidthLabel   = JBLabel("Custom Print Width:")
    private val customTabWidthLabel     = JBLabel("Custom Tab Width:")

    init {
        buildUI()
        setupListeners()
    }

    private fun buildUI() {
        val gbc = GridBagConstraints()
        gbc.fill   = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(4, 8, 4, 8)

        var row = 0

        // Helper: full-width row (spans 2 columns)
        fun addFullRow(component: JComponent) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0
            panel.add(component, gbc)
            gbc.gridwidth = 1
            row++
        }

        // Helper: label + control row
        fun addRow(label: JComponent, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35
            panel.add(label, gbc)
            gbc.gridx = 1; gbc.weightx = 0.65
            panel.add(component, gbc)
            row++
        }

        addFullRow(enabledCheckBox)
        addFullRow(formatOnSaveCheckBox)
        addRow(JBLabel("Formatting Profile:"), profileCombo)
        
        // Custom rows
        addRow(customPrintWidthLabel, customPrintWidthSpinner)
        addRow(customTabWidthLabel, customTabWidthSpinner)
        
        // Note: explanation label
        val explanationLabel = JBLabel("<html><small>" +
                "<b>Enterprise/Spring:</b> printWidth=120, tabWidth=4, useTabs=true<br>" +
                "<b>Google Style:</b> printWidth=100, tabWidth=2, useTabs=false<br>" +
                "<b>Custom:</b> Uses custom inputs above or relies on your local .prettierrc configuration" +
                "</small></html>")
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        panel.add(explanationLabel, gbc)
        row++

        // Vertical spacer
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(JPanel(), gbc)
    }
    
    private fun setupListeners() {
        profileCombo.addActionListener {
            val isCustom = profileCombo.selectedItem == "Custom"
            customPrintWidthLabel.isVisible = isCustom
            customPrintWidthSpinner.isVisible = isCustom
            customTabWidthLabel.isVisible = isCustom
            customTabWidthSpinner.isVisible = isCustom
        }
    }

    /** Applies UI values → settings state. */
    fun apply(settings: PrettierJavaSettings.State) {
        settings.enabled          = enabledCheckBox.isSelected
        settings.formatOnSave     = formatOnSaveCheckBox.isSelected
        settings.globalProfile    = profileCombo.selectedItem as String
        settings.customPrintWidth = customPrintWidthSpinner.value as Int
        settings.customTabWidth   = customTabWidthSpinner.value as Int
    }

    /** Resets UI from settings state. */
    fun reset(settings: PrettierJavaSettings.State) {
        enabledCheckBox.isSelected      = settings.enabled
        formatOnSaveCheckBox.isSelected = settings.formatOnSave
        profileCombo.selectedItem       = settings.globalProfile
        customPrintWidthSpinner.value   = settings.customPrintWidth
        customTabWidthSpinner.value     = settings.customTabWidth
        
        // Trigger visibility update
        val isCustom = settings.globalProfile == "Custom"
        customPrintWidthLabel.isVisible = isCustom
        customPrintWidthSpinner.isVisible = isCustom
        customTabWidthLabel.isVisible = isCustom
        customTabWidthSpinner.isVisible = isCustom
    }

    /** Returns true if UI values differ from the saved settings state. */
    fun isModified(settings: PrettierJavaSettings.State): Boolean =
        enabledCheckBox.isSelected         != settings.enabled ||
        formatOnSaveCheckBox.isSelected    != settings.formatOnSave ||
        profileCombo.selectedItem as String != settings.globalProfile ||
        customPrintWidthSpinner.value as Int != settings.customPrintWidth ||
        customTabWidthSpinner.value as Int   != settings.customTabWidth
}
