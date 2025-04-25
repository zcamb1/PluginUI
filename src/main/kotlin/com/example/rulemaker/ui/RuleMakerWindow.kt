package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.service.RuleParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
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
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import java.util.ArrayDeque
import java.awt.GridBagLayout
import java.awt.GridBagConstraints

/**
 * Main window for the Rule Maker plugin.
 */
class RuleMakerWindow(private val project: Project) : JPanel(BorderLayout()) {
    
    private val LOG = Logger.getInstance(RuleMakerWindow::class.java)
    
    private val ruleParser = RuleParser()
    private var currentRule: Rule? = null
    private val ruleHistory = ArrayDeque<Rule>()
    
    // UI components
    private val editorPanel = EditorPanel(::onStepUpdated, null)
    private val graphPanel = GraphPanel(
        onStepSelected = ::onStepSelected,
        onAddStep = ::onAddStep, 
        onAddSubStep = ::onAddSubStep,
        onRemoveStep = ::onRemoveStep,
        onSwapNode = ::onSwapNode
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
    
    // Screen Information Panel
    private val screenIdField = JBTextField().apply {
        isEditable = true
    }
    
    private val packageNameField = JBTextField().apply {
        isEditable = true
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
        val topToolbarPanel = createTopToolbar()
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
     * Create the top toolbar with title and actions
     */
    private fun createTopToolbar(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)
        
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
        
        importButton.addActionListener { openRuleFile() }
        
        // Add buttons to panel
        buttonsPanel.add(exportButton)
        buttonsPanel.add(importButton)
        buttonsPanel.add(exitButton)
        
        // User info on the right
        val userPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        userPanel.background = Color(60, 63, 65)
        
        val userLabel = JLabel("Hello, abcxyz ▼")
        userLabel.foreground = Color.WHITE
        userLabel.border = JBUI.Borders.empty(5, 10)
        userPanel.add(userLabel)
        
        // Add components to main panel
        panel.add(titleLabel, BorderLayout.WEST)
        panel.add(buttonsPanel, BorderLayout.CENTER)
        panel.add(userPanel, BorderLayout.EAST)
        
        return panel
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
        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)

        // Custom step editor panel
        val customStepPanel = JPanel(BorderLayout())
        customStepPanel.background = Color(60, 63, 65)

        // Tạo các trường layoutMatchers
        val matchTargetLabel = JLabel("matchTarget:")
        matchTargetLabel.foreground = Color.WHITE
        val matchTargetCombo = JComboBox(arrayOf("text", "content_description", "class_name"))
        matchTargetCombo.preferredSize = Dimension(140, 28)

        val matchOperandLabel = JLabel("matchOperand:")
        matchOperandLabel.foreground = Color.WHITE
        val matchOperandField = JTextField()
        matchOperandField.preferredSize = Dimension(140, 28)

        val matchCriteriaLabel = JLabel("matchCriteria:")
        matchCriteriaLabel.foreground = Color.WHITE
        val matchCriteriaCombo = JComboBox(arrayOf("equals", "contains"))
        matchCriteriaCombo.preferredSize = Dimension(140, 28)

        val highlightTypeLabel = JLabel("highlightType:")
        highlightTypeLabel.foreground = Color.WHITE
        val highlightTypeCombo = JComboBox(arrayOf("none", "click", "scroll_down", "scroll_right", "scroll_left_right", "invisible"))
        highlightTypeCombo.preferredSize = Dimension(140, 28)

        val transitionConditionLabel = JLabel("transitionCondition:")
        transitionConditionLabel.foreground = Color.WHITE
        val transitionConditionCombo = JComboBox(arrayOf("layout_match", "tts_end"))
        transitionConditionCombo.preferredSize = Dimension(140, 28)

        // Panel nhóm layoutMatchers
        val layoutMatchersPanel = JPanel(GridBagLayout())
        layoutMatchersPanel.background = Color(60, 63, 65)
        layoutMatchersPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Layout Matchers",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        val gbc = GridBagConstraints()
        gbc.insets = JBUI.insets(4, 4, 4, 4)
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridx = 0
        gbc.gridy = 0
        layoutMatchersPanel.add(matchTargetLabel, gbc)
        gbc.gridx = 1
        layoutMatchersPanel.add(matchTargetCombo, gbc)
        gbc.gridx = 0; gbc.gridy++
        layoutMatchersPanel.add(matchOperandLabel, gbc)
        gbc.gridx = 1
        layoutMatchersPanel.add(matchOperandField, gbc)
        gbc.gridx = 0; gbc.gridy++
        layoutMatchersPanel.add(matchCriteriaLabel, gbc)
        gbc.gridx = 1
        layoutMatchersPanel.add(matchCriteriaCombo, gbc)
        gbc.gridx = 0; gbc.gridy++
        layoutMatchersPanel.add(highlightTypeLabel, gbc)
        gbc.gridx = 1
        layoutMatchersPanel.add(highlightTypeCombo, gbc)
        gbc.gridx = 0; gbc.gridy++
        layoutMatchersPanel.add(transitionConditionLabel, gbc)
        gbc.gridx = 1
        layoutMatchersPanel.add(transitionConditionCombo, gbc)
        layoutMatchersPanel.preferredSize = Dimension(340, 180)

        // Tạo form với GroupLayout
        val formPanel = JPanel()
        formPanel.background = Color(60, 63, 65)
        val layout = GroupLayout(formPanel)
        formPanel.layout = layout
        layout.autoCreateGaps = true
        layout.autoCreateContainerGaps = true

        val stepIdLabel = JLabel("Step ID:")
        stepIdLabel.foreground = Color.WHITE
        val screenIdLabel = JLabel("Screen ID:")
        screenIdLabel.foreground = Color.WHITE
        val guideContentLabel = JLabel("Guide Content:")
        guideContentLabel.foreground = Color.WHITE
        val nextStepIdsLabel = JLabel("Next Step IDs:")
        nextStepIdsLabel.foreground = Color.WHITE
        val isSubStepCheckbox = editorPanel.getIsSubStepCheckbox()

        val idField = editorPanel.getIdField()
        val screenIdField = editorPanel.getScreenIdField()
        val guideContentArea = JBScrollPane(editorPanel.getGuideContentArea())
        val nextStepsField = editorPanel.getNextStepsField()

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addComponent(stepIdLabel)
                            .addComponent(screenIdLabel)
                            .addComponent(guideContentLabel)
                            .addComponent(nextStepIdsLabel)
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(idField)
                            .addComponent(screenIdField)
                            .addComponent(guideContentArea)
                            .addComponent(nextStepsField)
                        )
                )
                .addComponent(layoutMatchersPanel)
                .addComponent(isSubStepCheckbox)
        )
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(stepIdLabel)
                    .addComponent(idField)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(screenIdLabel)
                    .addComponent(screenIdField)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(guideContentLabel)
                    .addComponent(guideContentArea)
                )
                .addComponent(layoutMatchersPanel)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(nextStepIdsLabel)
                    .addComponent(nextStepsField)
                )
                .addComponent(isSubStepCheckbox)
        )

        // Bọc formPanel trong JScrollPane để có thể cuộn nếu không đủ chỗ
        val scrollPane = JScrollPane(formPanel)
        scrollPane.border = null
        scrollPane.background = Color(60, 63, 65)
        customStepPanel.add(scrollPane, BorderLayout.CENTER)

        // Navigation buttons
        val navPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        navPanel.background = Color(60, 63, 65)
        navPanel.border = JBUI.Borders.empty(10)
        val prevButton = JButton("← Previous Step")
        val saveButton = JButton("Save Changes")
        val nextButton = JButton("Next Step →")
        prevButton.addActionListener { editorPanel.onPreviousStep() }
        saveButton.addActionListener { editorPanel.saveChanges() }
        nextButton.addActionListener { editorPanel.onNextStep() }
        navPanel.add(prevButton)
        navPanel.add(saveButton)
        navPanel.add(nextButton)
        customStepPanel.add(navPanel, BorderLayout.SOUTH)
        panel.add(customStepPanel, BorderLayout.CENTER)
        return panel
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
        // Box nhỏ chứa form và border
        val screenInfoBox = JPanel()
        screenInfoBox.layout = GroupLayout(screenInfoBox)
        screenInfoBox.background = Color(60, 63, 65)
        screenInfoBox.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Screen information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        val layout = screenInfoBox.layout as GroupLayout
        layout.autoCreateGaps = true
        layout.autoCreateContainerGaps = true

        val screenIdLabel = JLabel("screen id:")
        screenIdLabel.foreground = Color.WHITE
        val packageNameLabel = JLabel("package name:")
        packageNameLabel.foreground = Color.WHITE
        screenIdField.preferredSize = Dimension(160, 32)
        packageNameField.preferredSize = Dimension(160, 32)
        val applyButton = JButton("Apply")
        applyButton.preferredSize = Dimension(100, 32)

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
        screenInfoBox.preferredSize = Dimension(260, 160)
        screenInfoBox.maximumSize = Dimension(320, 180)

        // Control buttons căn giữa và lên cao một chút
        val controlPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10))
        controlPanel.background = Color(60, 63, 65)
        val captureButton = JButton("Capture")
        val recordButton = JButton("Record")
        controlPanel.add(captureButton)
        controlPanel.add(recordButton)

        // Panel phải dùng GridBagLayout để căn giữa box và đẩy controlPanel xuống cuối
        val columnPanel = JPanel(GridBagLayout())
        columnPanel.background = Color(60, 63, 65)
        val gbc = java.awt.GridBagConstraints()

        // Thêm box ở giữa cột
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = java.awt.GridBagConstraints.CENTER
        gbc.fill = java.awt.GridBagConstraints.NONE
        columnPanel.add(screenInfoBox, gbc)

        // Thêm controlPanel căn giữa và lên cao một chút
        val gbc2 = java.awt.GridBagConstraints()
        gbc2.gridx = 0
        gbc2.gridy = 1
        gbc2.weightx = 1.0
        gbc2.weighty = 0.0
        gbc2.anchor = java.awt.GridBagConstraints.SOUTH
        gbc2.fill = java.awt.GridBagConstraints.NONE
        columnPanel.add(controlPanel, gbc2)
        // Thêm khoảng cách phía dưới controlPanel
        val gbc3 = java.awt.GridBagConstraints()
        gbc3.gridx = 0
        gbc3.gridy = 2
        gbc3.weightx = 1.0
        gbc3.weighty = 0.0
        gbc3.anchor = java.awt.GridBagConstraints.SOUTH
        gbc3.fill = java.awt.GridBagConstraints.NONE
        columnPanel.add(Box.createVerticalStrut(30), gbc3)

        columnPanel.preferredSize = Dimension(290, 300)
        return columnPanel
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
     * Open a rule JSON file.
     */
    private fun openRuleFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
        descriptor.title = "Select Rule JSON File"
        
        val fileChooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val files = fileChooser.choose(project)
        
        if (files.isNotEmpty()) {
            val file = File(files[0].path)
            try {
                val rules = ruleParser.parseRulesFromFile(file)
                if (rules.isNotEmpty()) {
                    setRule(rules[0])
                    
                    // Add log message
                    logMessagePanel.text = "Successfully loaded rule: ${rules[0].id}\n"
                    
                    Messages.showInfoMessage(project, "Successfully loaded rule: ${rules[0].id}", "Rule Loaded")
                } else {
                    logMessagePanel.text = "No rules found in the file\n"
                    Messages.showWarningDialog(project, "No rules found in the file", "No Rules")
                }
            } catch (e: Exception) {
                logMessagePanel.text = "Error loading rule file: ${e.message}\n"
                LOG.error("Error loading rule file", e)
                Messages.showErrorDialog(project, "Error loading rule file: ${e.message}", "Error")
            }
        }
    }
    
    /**
     * Set the rule for editing and display.
     */
    private fun setRule(rule: Rule) {
        currentRule = rule
        
        // Pass rule to editor panel
        editorPanel.setRule(rule)
        
        // No need to calculate main paths anymore since isSubStep is explicitly set in JSON
        // Just display the rule with its existing isSubStep values
        
        // Display rule in graph panel
        graphPanel.displayRule(rule)
        
        // Apply layout for better visualization
        graphPanel.applyLayout()
        
        // Reset editor panel
        editorPanel.reset()
        
        // Log loaded rule structure
        LOG.info("Loaded rule with ${rule.steps.size} steps and ${countConnections(rule)} connections")
        logMessagePanel.text += "Loaded rule with ${rule.steps.size} steps and ${countConnections(rule)} connections\n"
    }
    
    /**
     * Count the total number of connections in a rule
     */
    private fun countConnections(rule: Rule): Int {
        return rule.steps.sumOf { it.nextStepIds.size }
    }
    
    /**
     * Handle swapping a node with another node
     */
    private fun onSwapNode(stepA: Step, swapId: String) {
        val rule = currentRule ?: return
        val stepB = rule.steps.find { it.id == swapId }
        if (stepB == null) {
            JOptionPane.showMessageDialog(this, "Node with ID '$swapId' not found.", "Swap Node", JOptionPane.ERROR_MESSAGE)
            return
        }
        // Cho phép swap giữa mọi loại node (main <-> sub)
        swapNodes(rule, stepA, stepB)
        graphPanel.refreshGraph()
        logMessagePanel.text += "Swapped node ${stepA.id} with ${stepB.id}\n"
    }

    private fun swapNodes(rule: Rule, stepA: Step, stepB: Step) {
        // 1. Swap thuộc tính isSubStep
        val tmpIsSub = stepA.isSubStep
        stepA.isSubStep = stepB.isSubStep
        stepB.isSubStep = tmpIsSub
    
        // 2. Swap nextStepIds
        val tmpNext = stepA.nextStepIds.toList()
        stepA.nextStepIds.clear()
        stepA.nextStepIds.addAll(stepB.nextStepIds)
        stepB.nextStepIds.clear()
        stepB.nextStepIds.addAll(tmpNext)
    
        // 3. Cập nhật các node khác trỏ tới A hoặc B
        for (step in rule.steps) {
            for (i in step.nextStepIds.indices) {
                if (step.nextStepIds[i] == stepA.id) step.nextStepIds[i] = stepB.id
                else if (step.nextStepIds[i] == stepB.id) step.nextStepIds[i] = stepA.id
            }
        }
    
        // 4. Swap vị trí hiển thị (geometry)
        val cellA = graphPanel.getCellForStep(stepA.id)
        val cellB = graphPanel.getCellForStep(stepB.id)
        if (cellA != null && cellB != null) {
            val geoA = graphPanel.getCellGeometry(cellA)
            val geoB = graphPanel.getCellGeometry(cellB)
            if (geoA != null && geoB != null) {
                val newGeoA = geoB.clone() as mxGeometry
                val newGeoB = geoA.clone() as mxGeometry
                graphPanel.setCellGeometry(cellA, newGeoA)
                graphPanel.setCellGeometry(cellB, newGeoB)
            }
        }
    }
    
    /**
     * This method is no longer needed since isSubStep is explicitly set in JSON
     * Kept for backward compatibility with older rule files that might not have isSubStep
     */
    
    /**
     * Validate the current rule.
     */
    private fun validateRule() {
        val rule = currentRule ?: return
        
        val invalidReferences = rule.validateNextStepReferences()
        val isolatedSteps = rule.findIsolatedSteps()
        
        val sb = StringBuilder()
        
        if (invalidReferences.isEmpty() && isolatedSteps.isEmpty()) {
            logMessagePanel.text += "Rule validation passed!\n"
            Messages.showInfoMessage(project, "Rule validation passed!", "Validation")
            return
        }
        
        if (invalidReferences.isNotEmpty()) {
            sb.append("Invalid step references:\n")
            invalidReferences.forEach { sb.append("- $it\n") }
            sb.append("\n")
            
            logMessagePanel.text += "Invalid step references found\n"
        }
        
        if (isolatedSteps.isNotEmpty()) {
            sb.append("Isolated steps (no incoming or outgoing connections):\n")
            isolatedSteps.forEach { sb.append("- ${it.id}\n") }
            
            logMessagePanel.text += "Isolated steps found\n"
        }
        
        Messages.showWarningDialog(project, sb.toString(), "Validation Issues")
    }
    
    /**
     * Callback when a step is selected in the graph.
     */
    private fun onStepSelected(step: Step) {
        // No need to recalculate isSubStep status anymore
        if (currentRule != null) {
            editorPanel.setRule(currentRule!!)
        }
        editorPanel.setStep(step)
        logMessagePanel.text += "Selected step: ${step.id}\n"
    }
    
    /**
     * Callback when a step is updated in the editor.
     */
    private fun onStepUpdated(step: Step) {
        graphPanel.refreshGraph()
        logMessagePanel.text += "Updated step: ${step.id}\n"
    }
    
    /**
     * Callback to add a new step.
     */
    private fun onAddStep(
        parentStep: Step?, 
        parentCell: com.mxgraph.model.mxCell?, 
        parentGeo: com.mxgraph.model.mxGeometry?
    ) {
        val rule = currentRule ?: return
        editorPanel.setRule(rule)
        val newStep = editorPanel.createNewStep()
        rule.addStep(newStep)
    
        if (parentStep != null) {
            // 1. Tìm node main phía sau (nếu có)
            val oldNextMainId = parentStep.nextStepIds.firstOrNull { id ->
                val s = rule.steps.find { it.id == id }
                s != null && !s.isSubStep
            }
            if (oldNextMainId != null) {
                // 2. Ngắt kết nối X → Y
                parentStep.removeNextStep(oldNextMainId)
                // 3. Nối X → Z
                parentStep.addNextStep(newStep.id)
                // 4. Nối Z → Y
                newStep.addNextStep(oldNextMainId)
            } else {
                parentStep.addNextStep(newStep.id)
            }
        }
    
        // Không cần set thủ công vị trí, chỉ cần layout lại
        graphPanel.refreshGraph()
        logMessagePanel.text += "Added new step: ${newStep.id}\n"
    }
    
    /**
     * Callback to add a sub-step to a parent step.
     */
    private fun onAddSubStep(parentStep: Step) {
        val rule = currentRule ?: return
        
        // Make sure editor has reference to current rule
        editorPanel.setRule(rule)
        
        // Create new sub-step
        val subStep = editorPanel.createNewStep(isSubStep = true)
        
        // Add to rule
        rule.addStep(subStep)
        
        // Connect parent to sub-step
        parentStep.addNextStep(subStep.id)
        
        // Refresh graph
        graphPanel.refreshGraph()
        
        logMessagePanel.text += "Added sub-step ${subStep.id} to parent ${parentStep.id}\n"
    }
    
    /**
     * Callback to remove a step.
     */
    private fun onRemoveStep(step: Step): Boolean {
        val rule = currentRule ?: return false
        
        // Check if step has children
        if (step.hasChildren()) {
            logMessagePanel.text += "Cannot remove step '${step.id}' because it has next steps\n"
            Messages.showWarningDialog(
                project,
                "Cannot remove step '${step.id}' because it has next steps. Remove the connections first.",
                "Cannot Remove Step"
            )
            return false
        }
        
        // Remove step from rule
        if (rule.removeStep(step.id)) {
            editorPanel.reset()
            logMessagePanel.text += "Removed step: ${step.id}\n"
            return true
        }
        
        return false
    }
    
    /**
     * Get the main component.
     */
    fun getComponent(): JComponent = this
} 
