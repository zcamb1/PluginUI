package com.example.rulemaker.ui.components

import com.example.rulemaker.ui.logic.TopToolbarLogic
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * UI Panel for the top toolbar containing title, action buttons and user info
 */
class TopToolbarPanel(
    private val topToolbarLogic: TopToolbarLogic,
    private val userName: String = "abcxyz"
) : JPanel(BorderLayout()) {
    
    init {
        // Set background color for the toolbar
        background = Color(60, 63, 65)
        
        // Title on the left
        val titleLabel = JLabel("IUG Rule Maker Tool")
        titleLabel.foreground = Color.WHITE
        titleLabel.border = JBUI.Borders.empty(5, 10)
        titleLabel.font = titleLabel.font.deriveFont(titleLabel.font.size + 2f)
        
        // Buttons in the center
        val buttonsPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonsPanel.background = Color(60, 63, 65)
        
        val exportButton = JButton("Export")
        val importButton = JButton("Import")
        val exitButton = JButton("Exit")
        
        // Add action listeners
        exportButton.addActionListener { topToolbarLogic.exportRule(this) }
        importButton.addActionListener { topToolbarLogic.importRule() }
        exitButton.addActionListener { topToolbarLogic.exitApplication() }
        
        // Add buttons to panel
        buttonsPanel.add(exportButton)
        buttonsPanel.add(importButton)
        buttonsPanel.add(exitButton)
        
        // User info on the right
        val userPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        userPanel.background = Color(60, 63, 65)
        
        val userLabel = JLabel("Hello, $userName ▼")
        userLabel.foreground = Color.WHITE
        userLabel.border = JBUI.Borders.empty(5, 10)
        userPanel.add(userLabel)
        
        // Add components to main panel
        add(titleLabel, BorderLayout.WEST)
        add(buttonsPanel, BorderLayout.CENTER)
        add(userPanel, BorderLayout.EAST)
    }
} 