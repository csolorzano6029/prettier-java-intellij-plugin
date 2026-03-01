package com.prettierjavaplugin

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComboBox
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
    private val enabledCheckBox     = JBCheckBox("Enable Prettier Java formatter")
    private val formatOnSaveCheckBox = JBCheckBox("Format on Save (runs Prettier every time you save a .java file)")
    private val nodePathField       = JBTextField()
    private val printWidthSpinner   = JSpinner(SpinnerNumberModel(80, 1, 500, 1))
    private val tabWidthSpinner     = JSpinner(SpinnerNumberModel(4, 1, 16, 1))
    private val useTabsCheckBox     = JBCheckBox("Use Tabs instead of spaces")
    private val semiCheckBox        = JBCheckBox("Add semicolons at end of statements")
    private val singleQuoteCheckBox = JBCheckBox("Use single quotes")
    private val trailingCommaCombo  = JComboBox(arrayOf("all", "es5", "none"))

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
        addRow("Node.js executable path:", nodePathField)
        addRow("Print Width:", printWidthSpinner)
        addRow("Tab Width:", tabWidthSpinner)
        addFullRow(useTabsCheckBox)
        addFullRow(semiCheckBox)
        addFullRow(singleQuoteCheckBox)
        addRow("Trailing Comma:", trailingCommaCombo)

        // Vertical spacer
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(JPanel(), gbc)
    }

    /** Applies UI values → settings state. */
    fun apply(settings: PrettierJavaSettings.State) {
        settings.enabled       = enabledCheckBox.isSelected
        settings.formatOnSave  = formatOnSaveCheckBox.isSelected
        settings.nodePath      = nodePathField.text.trim().ifBlank { "node" }
        settings.printWidth    = printWidthSpinner.value as Int
        settings.tabWidth      = tabWidthSpinner.value as Int
        settings.useTabs       = useTabsCheckBox.isSelected
        settings.semi          = semiCheckBox.isSelected
        settings.singleQuote   = singleQuoteCheckBox.isSelected
        settings.trailingComma = trailingCommaCombo.selectedItem as String
    }

    /** Resets UI from settings state. */
    fun reset(settings: PrettierJavaSettings.State) {
        enabledCheckBox.isSelected      = settings.enabled
        formatOnSaveCheckBox.isSelected = settings.formatOnSave
        nodePathField.text              = settings.nodePath
        printWidthSpinner.value         = settings.printWidth
        tabWidthSpinner.value           = settings.tabWidth
        useTabsCheckBox.isSelected      = settings.useTabs
        semiCheckBox.isSelected         = settings.semi
        singleQuoteCheckBox.isSelected  = settings.singleQuote
        trailingCommaCombo.selectedItem = settings.trailingComma
    }

    /** Returns true if UI values differ from the saved settings state. */
    fun isModified(settings: PrettierJavaSettings.State): Boolean =
        enabledCheckBox.isSelected      != settings.enabled ||
        formatOnSaveCheckBox.isSelected != settings.formatOnSave ||
        nodePathField.text.trim()       != settings.nodePath ||
        printWidthSpinner.value as Int  != settings.printWidth ||
        tabWidthSpinner.value as Int    != settings.tabWidth ||
        useTabsCheckBox.isSelected      != settings.useTabs ||
        semiCheckBox.isSelected         != settings.semi ||
        singleQuoteCheckBox.isSelected  != settings.singleQuote ||
        trailingCommaCombo.selectedItem as String != settings.trailingComma
}
