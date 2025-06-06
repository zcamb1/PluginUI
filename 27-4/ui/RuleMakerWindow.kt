package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.model.LayoutMatcher
import com.example.rulemaker.service.RuleParser
import com.example.rulemaker.ui.components.TopToolbarPanel
import com.example.rulemaker.ui.components.ScreenInfoPanel
import com.example.rulemaker.ui.logic.TopToolbarLogic
import com.example.rulemaker.ui.logic.ScreenInfoLogic
import com.example.rulemaker.ui.logic.NodeInteractionLogic
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.FlowLayout
import java.awt.Color
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import java.util.ArrayDeque
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel

/**
 * Main window for the Rule Maker plugin.
 */
class RuleMakerWindow(private val project: Project) : JPanel(BorderLayout()) {
    
    private val ruleParser = RuleParser()
    private var currentRule: Rule? = null
    private val ruleHistory = ArrayDeque<Rule>()
    
    // UI components - Replace EditorPanel with EditorPanelLogic and StepInfoPanel
    private val editorLogic: EditorPanelLogic = EditorPanelLogic(::onStepUpdated)
    private val stepInfoPanel: StepInfoPanel = StepInfoPanel(editorLogic)
    
    // Node Interaction Logic
    private val nodeLogic: NodeInteractionLogic = NodeInteractionLogic(
        getCurrentRule = { currentRule },
        setRule = { rule -> currentRule = rule },
        refreshGraph = { graphPanel.refreshGraph() },
        showStepInEditor = { step -> stepInfoPanel.setStep(step) },
        createNewStep = { isSubStep -> editorLogic.createNewStep(isSubStep) },
        getCellForStep = { stepId -> graphPanel.getCellForStep(stepId) },
        getCellGeometry = { cell -> graphPanel.getCellGeometry(cell) },
        setCellGeometry = { cell, geo -> graphPanel.setCellGeometry(cell, geo) },
        showMessage = { msg, title, type -> JOptionPane.showMessageDialog(this, msg, title, type) }
    )
    
    private val graphPanel: GraphPanel = GraphPanel(
        onStepSelected = { step -> nodeLogic.onStepSelected(step) },
        onAddStep = { parentStep, parentCell, parentGeo -> 
            nodeLogic.onAddStep(parentStep, parentCell, parentGeo) 
        },
        onAddSubStep = { parentStep -> nodeLogic.onAddSubStep(parentStep) },
        onRemoveStep = { step -> nodeLogic.onRemoveStep(step) },
        onSwapNode = { stepA, swapId -> nodeLogic.onSwapNode(stepA, swapId) }
    )
    
    // Screen Info Panel
    private val screenInfoLogic = ScreenInfoLogic()
    
    private val screenInfoPanel = ScreenInfoPanel(
        onApply = { screenId, packageName -> screenInfoLogic.applyScreenInfo(screenId, packageName) },
        onCapture = { screenInfoLogic.captureScreen() },
        onRecord = { screenInfoLogic.recordScreen() }
    )
    
    // Logic components
    private val topToolbarLogic = TopToolbarLogic(
        project,
        ruleParser,
        ::setRule,
        { currentRule }
    )
    
    // New UI components for improved layout
    private val logMessagePanel = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Log messages will appear here..."
        background = Color(45, 45, 45)
        foreground = Color.WHITE
    }
    
    private val mirrorScreenPanel = JPanel(BorderLayout()).apply {
        add(JBLabel("No screen connected", SwingConstants.CENTER), BorderLayout.CENTER)
        background = Color(45, 45, 45)
    }
    
    // Common info text area
    private val commonInfoArea = JTextArea("{\n\n}").apply {
        lineWrap = true
        wrapStyleWord = true
    }

    
    init {
        // Set background color for the main panel
        background = Color(60, 63, 65)
        
        // Create toolbar with title and actions at the top
        val topToolbarPanel = TopToolbarPanel(topToolbarLogic)
        add(topToolbarPanel, BorderLayout.NORTH)
        
        // Main content panel
        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.background = Color(60, 63, 65)
        
        // Top section with Common Info/Step Info (tab) và Mirror Screen bằng nhau, Screen Info bên phải
        val topSection = JPanel(BorderLayout())
        topSection.background = Color(60, 63, 65)
        
        val combinedTabPanel = createCombinedTabPanel()
        val mirrorPanel = createMirrorPanel()
        val screenInfoPanel = createScreenInfoPanel()
        
        // Đặt cùng kích thước preferredSize cho hai panel
        combinedTabPanel.preferredSize = Dimension(400, 300)
        mirrorPanel.preferredSize = Dimension(400, 300)
        
        // Tạo container ngang cho tab + mirror
        val tabAndMirrorContainer = JPanel(GridLayout(1, 2, 10, 0))
        tabAndMirrorContainer.background = Color(60, 63, 65)
        tabAndMirrorContainer.add(combinedTabPanel)
        tabAndMirrorContainer.add(mirrorPanel)
        
        // Tạo container ngang cho tab+mirror và screen info
        val topPanelsLayout = JPanel()
        topPanelsLayout.layout = BoxLayout(topPanelsLayout, BoxLayout.X_AXIS)
        topPanelsLayout.background = Color(60, 63, 65)
        topPanelsLayout.add(tabAndMirrorContainer)
        topPanelsLayout.add(screenInfoPanel)
        
        // Add the horizontal layout to the top section
        topSection.add(topPanelsLayout, BorderLayout.CENTER)
        
        // Bottom section with Graph and Log
        val bottomSection = JPanel(BorderLayout())
        bottomSection.background = Color(60, 63, 65)
        
        // Create Graph and Log panels
        val graphPanelContainer = createGraphPanel()
        val logPanelContainer = createLogPanel()
        
        // Bottom panels layout with Graph and Log side by side
        val bottomPanelsLayout = JPanel()
        bottomPanelsLayout.layout = BoxLayout(bottomPanelsLayout, BoxLayout.X_AXIS)
        bottomPanelsLayout.background = Color(60, 63, 65)
        
        // Add graph panel (wide) and log panel (narrow) side by side
        bottomPanelsLayout.add(graphPanelContainer)
        bottomPanelsLayout.add(logPanelContainer)
        
        // Add the bottom panels layout to the bottom section
        bottomSection.add(bottomPanelsLayout, BorderLayout.CENTER)
        
        // Use a vertical split pane to divide top and bottom sections with preferred ratios
        val mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, topSection, bottomSection)
        mainSplitPane.resizeWeight = 0.5 // Top panel gets 50% of space, bottom gets 50%
        mainSplitPane.border = null
        mainSplitPane.dividerSize = 5
        
        // Add the split pane to main content
        mainContentPanel.add(mainSplitPane, BorderLayout.CENTER)
        
        // Add main content to window
        add(mainContentPanel, BorderLayout.CENTER)
        
        // Set default size
        preferredSize = Dimension(1200, 700)
    }
    

    
    /**
     * Create a combined panel with Common Info and Step Info in tabs
     */
    private fun createCombinedTabPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)
        
        // Create tabbed pane
        val tabbedPane = JTabbedPane()
        tabbedPane.background = Color(60, 63, 65)
        tabbedPane.foreground = Color.WHITE
        
        // Create Common Info content
        val commonInfoContent = JPanel(BorderLayout())
        commonInfoContent.background = Color(60, 63, 65)
        commonInfoContent.add(JBScrollPane(commonInfoArea), BorderLayout.CENTER)
        
        // Create Step Info content
        val stepInfoContent = createStepInfoContent()
        
        // Add tabs
        tabbedPane.addTab("Common Info", commonInfoContent)
        tabbedPane.addTab("Step Info", stepInfoContent)
        
        // Set Common Info as default tab
        tabbedPane.selectedIndex = 0
        
        panel.add(tabbedPane, BorderLayout.CENTER)
        panel.preferredSize = Dimension(580, 300)
        
        return panel
    }



    /**
     * Create the Step Info content panel
     */
    private fun createStepInfoContent(): JPanel {
        // Now we simply return the already instantiated StepInfoPanel
        return stepInfoPanel
    }

    /**
     * Create the Mirror Screen panel
     */
    private fun createMirrorPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Mirror screen",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        panel.background = Color(60, 63, 65)
        panel.add(mirrorScreenPanel, BorderLayout.CENTER)
        panel.preferredSize = Dimension(290, 300)
        return panel
    }
    
    /**
     * Create the Screen Information panel
     */
    private fun createScreenInfoPanel(): JPanel {
        return screenInfoPanel
    }
    
    /**
     * Create the Graph panel
     */
    private fun createGraphPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Step graph",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        panel.background = Color(60, 63, 65)
        
        // Make sure the graph is visible
        graphPanel.preferredSize = Dimension(950, 220)
        panel.add(graphPanel, BorderLayout.CENTER)
        
        // Set preferred size for the graph panel container
        panel.preferredSize = Dimension(900, 250)
        
        return panel
    }
    
    /**
     * Create the Log panel
     */
    private fun createLogPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Log",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        panel.background = Color(60, 63, 65)
        
        panel.add(JBScrollPane(logMessagePanel), BorderLayout.CENTER)
        panel.preferredSize = Dimension(300, 250)
        
        return panel
    }
    

    
    /**
     * Set the rule for editing and display.
     */
    private fun setRule(rule: Rule) {
        currentRule = rule
        
        // Pass rule to editor panel
        editorLogic.setRule(rule)
        
        // Display rule in graph panel
        graphPanel.displayRule(rule)
        
        // Apply layout for better visualization
        graphPanel.applyLayout()
        
        // Reset panels
        editorLogic.reset()
        stepInfoPanel.reset()
    }
    
    /**
     * Count the total number of connections in a rule
     */
    private fun countConnections(rule: Rule): Int {
        return nodeLogic.countConnections(rule)
    }
    
    /**
     * Callback when a step is updated in the editor.
     */
    private fun onStepUpdated(step: Step) {
        nodeLogic.onStepUpdated(step)
    }
    
    /**
     * Get the main component.
     */
    fun getComponent(): JComponent = this
    


} 