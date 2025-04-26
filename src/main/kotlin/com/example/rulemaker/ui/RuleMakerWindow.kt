package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.model.LayoutMatcher
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
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel

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
    
    // Table Model cho layoutMatchers
    class LayoutMatchersTableModel : AbstractTableModel() {
        val columns = arrayOf("matchTarget", "matchOperand", "matchCriteria", "highlightType")
        val data = mutableListOf<Array<String?>>()

        override fun getRowCount() = data.size
        override fun getColumnCount() = columns.size
        override fun getColumnName(col: Int) = columns[col]
        override fun getValueAt(row: Int, col: Int) = data[row][col]
        override fun isCellEditable(row: Int, col: Int) = true
        override fun setValueAt(value: Any?, row: Int, col: Int) {
            data[row][col] = value as? String
            fireTableCellUpdated(row, col)
        }
        fun addEmptyRow() {
            data.add(arrayOfNulls<String>(columns.size))
            fireTableRowsInserted(data.size-1, data.size-1)
        }
        fun removeRow(row: Int) {
            data.removeAt(row)
            fireTableRowsDeleted(row, row)
        }
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
     * Hàm fixWidth cho JComponent
     */
    private fun fixWidth(c: JComponent) {
        c.maximumSize = Dimension(Int.MAX_VALUE, c.preferredSize.height)
    }

    /**
     * Create the Step Info content panel
     */
    private fun createStepInfoContent(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)

        // 1. FormBuilder cho phần nhập liệu
        val idField = editorPanel.getIdField()
        val screenIdField = editorPanel.getScreenIdField()
        val guideContentArea = JBScrollPane(editorPanel.getGuideContentArea())
        guideContentArea.preferredSize = Dimension(300, 60)
        val nextStepsField = editorPanel.getNextStepsField()
        nextStepsField.preferredSize = Dimension(nextStepsField.preferredSize.width, 32)
        val isSubStepCheckbox = editorPanel.getIsSubStepCheckbox()
        val formPanel = com.intellij.util.ui.FormBuilder.createFormBuilder()
            .addLabeledComponent("Step ID:", idField)
            .addLabeledComponent("Screen ID:", screenIdField)
            .addLabeledComponent("Guide Content:", guideContentArea)
            .addLabeledComponent("Next Step IDs:", nextStepsField)
            .panel.apply {
                border = JBUI.Borders.empty(10, 10, 0, 10)
            }
        // Panel riêng cho checkbox isSubStep, căn trái, spacing trên
        val isSubStepPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = Color(60, 63, 65)
            add(Box.createVerticalStrut(10))
            add(isSubStepCheckbox)
        }

        // 2. Panel chứa bảng layout matchers và nút
        val layoutMatchersModel = LayoutMatchersTableModel()
        layoutMatchersTable = JTable(layoutMatchersModel)
        layoutMatchersTable.background = Color(60, 63, 65)
        layoutMatchersTable.foreground = Color.WHITE
        layoutMatchersTable.selectionBackground = Color(90, 90, 120)
        layoutMatchersTable.selectionForeground = Color.WHITE
        layoutMatchersTable.columnModel.getColumn(0).cellEditor = DefaultCellEditor(JComboBox(arrayOf("text", "content_description", "class_name")))
        layoutMatchersTable.columnModel.getColumn(2).cellEditor = DefaultCellEditor(JComboBox(arrayOf("equals", "contains")))
        layoutMatchersTable.columnModel.getColumn(3).cellEditor = DefaultCellEditor(JComboBox(arrayOf("none", "click", "scroll_down", "scroll_right", "scroll_left_right", "invisible")))
        layoutMatchersTable.columnModel.getColumn(0).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(1).preferredWidth = 120
        layoutMatchersTable.columnModel.getColumn(2).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(3).preferredWidth = 120
        layoutMatchersTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
        val matchersScrollPane = JScrollPane(layoutMatchersTable)
        matchersScrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        matchersScrollPane.preferredSize = Dimension(750, 240)

        // Panel chứa 2 nút icon nhỏ
        val matcherBtnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        matcherBtnPanel.background = Color(60, 63, 65)
        val addMatcherBtn = JButton(AllIcons.General.Add)
        addMatcherBtn.toolTipText = "Thêm dòng Matcher"
        addMatcherBtn.preferredSize = Dimension(28, 28)
        addMatcherBtn.maximumSize = Dimension(28, 28)
        val removeMatcherBtn = JButton(AllIcons.General.Remove)
        removeMatcherBtn.toolTipText = "Xóa dòng Matcher"
        removeMatcherBtn.preferredSize = Dimension(28, 28)
        removeMatcherBtn.maximumSize = Dimension(28, 28)
        matcherBtnPanel.add(addMatcherBtn)
        matcherBtnPanel.add(removeMatcherBtn)
        addMatcherBtn.addActionListener { layoutMatchersModel.addEmptyRow() }
        removeMatcherBtn.addActionListener {
            val row = layoutMatchersTable.selectedRow
            if (row >= 0) layoutMatchersModel.removeRow(row)
        }

        // Panel chứa bảng và nút, có border tiêu đề
        val layoutMatchersPanel = JPanel(BorderLayout())
        layoutMatchersPanel.background = Color(60, 63, 65)
        layoutMatchersPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Layout Matchers",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        layoutMatchersPanel.add(matcherBtnPanel, BorderLayout.NORTH)
        layoutMatchersPanel.add(matchersScrollPane, BorderLayout.CENTER)
        layoutMatchersPanel.preferredSize = Dimension(750, 300)

        // 3. Gộp lại vào mainPanel
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.background = Color(60, 63, 65)
        mainPanel.maximumSize = Dimension(800, Int.MAX_VALUE)
        mainPanel.preferredSize = Dimension(800, 500)
        mainPanel.add(Box.createVerticalStrut(10))
        mainPanel.add(formPanel)
        mainPanel.add(Box.createVerticalStrut(10))
        mainPanel.add(layoutMatchersPanel)
        mainPanel.add(Box.createVerticalStrut(10))
        mainPanel.add(isSubStepPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Add vào customStepPanel
        val customStepPanel = JPanel(BorderLayout())
        customStepPanel.background = Color(60, 63, 65)
        customStepPanel.add(mainPanel, BorderLayout.CENTER)

        // Navigation buttons giữ nguyên
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
     * Update layoutMatchers table with data from the given step.
     */
    private fun updateLayoutMatchersTable(step: Step) {
        val model = layoutMatchersTable.model as LayoutMatchersTableModel
        model.data.clear()
        
        step.layoutMatchers.forEach { matcher ->
            model.data.add(arrayOf(
                matcher.matchTarget,
                matcher.matchOperand,
                matcher.matchCriteria,
                matcher.highlightType,
                null // empty transitionCondition cell
            ))
        }
        
        model.fireTableDataChanged()
    }
    
    /**
     * Update the current step with data from layoutMatchersTable and save to currentRule.
     */
    private fun updateStepWithLayoutMatchersData(currentStep: Step) {
        val tableModel = layoutMatchersTable.model as LayoutMatchersTableModel
        val matchers = mutableListOf<LayoutMatcher>()
        
        for (row in 0 until tableModel.rowCount) {
            val matchTarget = tableModel.getValueAt(row, 0)?.toString() ?: ""
            val matchOperand = tableModel.getValueAt(row, 1)?.toString() ?: ""
            val matchCriteria = tableModel.getValueAt(row, 2)?.toString()
            val highlightType = tableModel.getValueAt(row, 3)?.toString()
            
            matchers.add(LayoutMatcher(matchTarget, matchOperand, matchCriteria, highlightType))
        }
        
        // Tạo step mới với layoutMatchers mới và các giá trị khác từ currentStep
        val updatedStep = Step(
            id = currentStep.id,
            screenId = currentStep.screenId,
            guideContent = currentStep.guideContent,
            layoutMatchers = matchers,
            nextStepIds = currentStep.nextStepIds,
            screenMatcher = currentStep.screenMatcher,
            transitionCondition = currentStep.transitionCondition,
            isSubStep = currentStep.isSubStep
        )
        
        // Cập nhật step mới vào currentRule
        updateCurrentRuleWithStep(currentStep, updatedStep)
    }
    
    /**
     * Update a step in the current rule.
     */
    private fun updateCurrentRuleWithStep(oldStep: Step, newStep: Step) {
        val rule = currentRule ?: return
        val index = rule.steps.indexOfFirst { it.id == oldStep.id }
        if (index >= 0) {
            // Tạo danh sách steps mới có chứa newStep thay vì oldStep
            val updatedSteps = rule.steps.toMutableList()
            updatedSteps[index] = newStep
            
            // Cập nhật currentRule với danh sách steps mới
            currentRule = rule.copy(steps = updatedSteps)
            
            // Cập nhật lại các tham chiếu
            editorPanel.setRule(currentRule!!)
            graphPanel.displayRule(currentRule!!)
            
            // Lưu lại step mới vào biến currentStep trong EditorPanel
            editorPanel.setStep(newStep)
        }
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
        updateLayoutMatchersTable(step) // Update the table when a step is selected
        logMessagePanel.text += "Selected step: ${step.id}\n"
    }
    
    /**
     * Callback when a step is updated in the editor.
     */
    private fun onStepUpdated(step: Step) {
        updateStepWithLayoutMatchersData(step) // Get data from table before updating the graph
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

    // Declare the layoutMatchersTable as a class member for access
    private lateinit var layoutMatchersTable: JTable
} 
