package com.example.rulemaker.ui.components

import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * Panel for displaying and editing screen information
 */
class ScreenInfoPanel(
    private val onApply: (screenId: String, packageName: String) -> Unit = { _, _ -> },
    private val onCapture: () -> Unit = {},
    private val onRecord: () -> Unit = {}
) : JPanel(GridBagLayout()) {

    // Screen information fields
    private val screenIdField = JBTextField().apply {
        isEditable = true
        preferredSize = Dimension(160, 32)
    }
    
    private val packageNameField = JBTextField().apply {
        isEditable = true
        preferredSize = Dimension(160, 32)
    }
    
    init {
        // Set background color for the panel
        background = Color(60, 63, 65)
        
        // Create screen info box
        val screenInfoBox = createScreenInfoBox()
        
        // Create control panel with Capture and Record buttons
        val controlPanel = createControlPanel()
        
        // Layout components vertically
        val gbc = GridBagConstraints()
        
        // Add screen info box
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.NONE
        add(screenInfoBox, gbc)
        
        // Add control panel
        val gbc2 = GridBagConstraints()
        gbc2.gridx = 0
        gbc2.gridy = 1
        gbc2.weightx = 1.0
        gbc2.weighty = 0.0
        gbc2.anchor = GridBagConstraints.SOUTH
        gbc2.fill = GridBagConstraints.NONE
        add(controlPanel, gbc2)
        
        // Add space below control panel
        val gbc3 = GridBagConstraints()
        gbc3.gridx = 0
        gbc3.gridy = 2
        gbc3.weightx = 1.0
        gbc3.weighty = 0.0
        gbc3.anchor = GridBagConstraints.SOUTH
        gbc3.fill = GridBagConstraints.NONE
        add(Box.createVerticalStrut(30), gbc3)
        
        // Set preferred size
        preferredSize = Dimension(290, 300)
    }
    
    /**
     * Create the screen info box with form fields
     */
    private fun createScreenInfoBox(): JPanel {
        val box = JPanel()
        box.layout = GroupLayout(box)
        box.background = Color(60, 63, 65)
        box.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Screen information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        
        val layout = box.layout as GroupLayout
        layout.autoCreateGaps = true
        layout.autoCreateContainerGaps = true
        
        // Create labels
        val screenIdLabel = JLabel("screen id:")
        screenIdLabel.foreground = Color.WHITE
        val packageNameLabel = JLabel("package name:")
        packageNameLabel.foreground = Color.WHITE
        
        // Create Apply button
        val applyButton = JButton("Apply")
        applyButton.preferredSize = Dimension(100, 32)
        applyButton.addActionListener {
            onApply(screenIdField.text, packageNameField.text)
        }
        
        // Set horizontal group
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(
                    layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addComponent(screenIdLabel)
                            .addComponent(packageNameLabel)
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(screenIdField)
                            .addComponent(packageNameField)
                        )
                )
                .addComponent(applyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        )
        
        // Set vertical group
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(screenIdLabel)
                    .addComponent(screenIdField)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(packageNameLabel)
                    .addComponent(packageNameField)
                )
                .addGap(12)
                .addComponent(applyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        )
        
        // Set size
        box.preferredSize = Dimension(260, 160)
        box.maximumSize = Dimension(320, 180)
        
        return box
    }
    
    /**
     * Create the control panel with Capture and Record buttons
     */
    private fun createControlPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10))
        panel.background = Color(60, 63, 65)
        
        val captureButton = JButton("Capture")
        captureButton.addActionListener { onCapture() }
        
        val recordButton = JButton("Record")
        recordButton.addActionListener { onRecord() }
        
        panel.add(captureButton)
        panel.add(recordButton)
        
        return panel
    }
    
    /**
     * Get the current screen ID
     */
    fun getScreenId(): String = screenIdField.text
    
    /**
     * Get the current package name
     */
    fun getPackageName(): String = packageNameField.text
    
    /**
     * Set the screen ID
     */
    fun setScreenId(screenId: String) {
        screenIdField.text = screenId
    }
    
    /**
     * Set the package name
     */
    fun setPackageName(packageName: String) {
        packageNameField.text = packageName
    }
} 