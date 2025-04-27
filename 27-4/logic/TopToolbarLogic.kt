package com.example.rulemaker.ui.logic

import com.example.rulemaker.model.Rule
import com.example.rulemaker.service.RuleParser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*

/**
 * Logic for the top toolbar functionality
 */
class TopToolbarLogic(
    private val project: Project,
    private val ruleParser: RuleParser,
    private val onRuleLoaded: (Rule) -> Unit,
    private val getCurrentRule: () -> Rule?
) {
    /**
     * Handle export button click
     */
    fun exportRule(parentComponent: java.awt.Component) {
        val rule = getCurrentRule()
        if (rule == null) {
            Messages.showWarningDialog(project, "No rule is currently loaded to export.", "No Rule")
            return
        }
        showExportDialog(rule, parentComponent)
    }
    
    /**
     * Show the export dialog
     */
    private fun showExportDialog(rule: Rule, parentComponent: java.awt.Component) {
        val window = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame ?: return
        val dialog = JDialog(window, "Export Rule", true)
        dialog.layout = BorderLayout()
        dialog.preferredSize = Dimension(500, 180)
        
        // Main content panel with form layout
        val contentPanel = JPanel(GridBagLayout())
        contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        contentPanel.background = Color(60, 63, 65)
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        // Filename input
        val filenameLabel = JLabel("Filename:")
        filenameLabel.foreground = Color.WHITE
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        contentPanel.add(filenameLabel, gbc)
        
        val filenameField = JTextField("${rule.id}.json")
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.gridwidth = 2
        contentPanel.add(filenameField, gbc)
        
        // Export path
        val pathLabel = JLabel("Export to:")
        pathLabel.foreground = Color.WHITE
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        gbc.gridwidth = 1
        contentPanel.add(pathLabel, gbc)
        
        val pathField = JTextField(System.getProperty("user.home"))
        pathField.isEditable = false
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.weightx = 1.0
        contentPanel.add(pathField, gbc)
        
        val browseButton = JButton("Browse...")
        gbc.gridx = 2
        gbc.gridy = 1
        gbc.weightx = 0.0
        contentPanel.add(browseButton, gbc)
        
        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = Color(60, 63, 65)
        
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")
        
        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)
        
        // Add panels to dialog
        dialog.add(contentPanel, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)
        
        // Configure browse button
        browseButton.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "Select Export Directory"
            
            val fileChooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
            val folders = fileChooser.choose(project)
            
            if (folders.isNotEmpty()) {
                pathField.text = folders[0].path
            }
        }
        
        // Variable to track if export was completed
        var exportCompleted = false
        
        // Configure OK button
        okButton.addActionListener {
            // Ensure filename has .json extension
            var filename = filenameField.text.trim()
            if (!filename.endsWith(".json", ignoreCase = true)) {
                filename += ".json"
            }
            
            val exportDir = pathField.text.trim()
            if (exportDir.isEmpty()) {
                Messages.showWarningDialog(dialog, "Please select an export directory", "Export Error")
                return@addActionListener
            }
            
            val file = File(exportDir, filename)
            
            val result = ruleParser.exportRuleToJsonFile(rule, file)
            
            if (result.first) {
                Messages.showInfoMessage(project, "Successfully exported rule to ${file.absolutePath}", "Export Successful")
                exportCompleted = true
                dialog.dispose()
            } else {
                Messages.showErrorDialog(project, "Failed to export rule: ${result.second ?: "Unknown error"}", "Export Failed")
            }
        }
        
        // Configure Cancel button
        cancelButton.addActionListener {
            dialog.dispose()
        }
        
        // Show dialog
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }
    
    /**
     * Handle import button click
     */
    fun importRule() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
        descriptor.title = "Select Rule JSON File"
        
        val fileChooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val files = fileChooser.choose(project)
        
        if (files.isNotEmpty()) {
            val file = File(files[0].path)
            try {
                val rules = ruleParser.parseRulesFromFile(file)
                if (rules.isNotEmpty()) {
                    onRuleLoaded(rules[0])
                    Messages.showInfoMessage(project, "Successfully loaded rule: ${rules[0].id}", "Rule Loaded")
                } else {
                    Messages.showWarningDialog(project, "No rules found in the file", "No Rules")
                }
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Error loading rule file: ${e.message}", "Error")
            }
        }
    }
    
    /**
     * Handle exit button click
     */
    fun exitApplication() {
        // Ask for confirmation
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to exit the application?",
            "Exit Confirmation",
            "Yes",
            "No",
            null
        )
        
        if (result == Messages.YES) {
            System.exit(0)
        }
    }
} 