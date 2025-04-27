package com.example.rulemaker.ui

import com.example.rulemaker.model.LayoutMatcher
import com.example.rulemaker.model.Step
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Color
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * Panel for displaying and editing step information.
 * Contains all UI components and uses EditorPanelLogic for data handling.
 */
class StepInfoPanel(private val editorLogic: EditorPanelLogic) : JPanel(BorderLayout()) {
    
    // UI components
    private val idField = JBTextField()
    private val screenIdField = JBTextField()
    private val guideContentArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val nextStepsField = JBTextField()
    private val isSubStepCheckbox = JCheckBox("Is Sub-Step")
    
    // LayoutMatchers table
    private val layoutMatchersModel = LayoutMatchersTableModel()
    private val layoutMatchersTable = JTable(layoutMatchersModel)
    
    // Table model for layout matchers
    class LayoutMatchersTableModel : AbstractTableModel() {
        val columns = arrayOf("matchTarget", "matchOperand", "matchCriteria", "highlightType", "transitionCondition")
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
        background = Color(60, 63, 65)
        
        // Create main panel with form and table
        val mainPanel = createMainPanel()
        add(mainPanel, BorderLayout.CENTER)
        
        // Create navigation buttons panel
        val navPanel = createNavigationPanel()
        add(navPanel, BorderLayout.SOUTH)
        
        // Configure layout matchers table
        configureLayoutMatchersTable()
    }
    
    /**
     * Create the main panel with form and layout matchers table
     */
    private fun createMainPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = Color(60, 63, 65)
        
        // Form for basic step information
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Step ID:", idField)
            .addLabeledComponent("Screen ID:", screenIdField)
            .addLabeledComponent("Guide Content:", JBScrollPane(guideContentArea).apply {
                preferredSize = Dimension(300, 60)
            })
            .addLabeledComponent("Next Step IDs:", nextStepsField)
            .panel.apply {
                border = JBUI.Borders.empty(10, 10, 0, 10)
                background = Color(60, 63, 65)
            }
        
        // Panel for the isSubStep checkbox
        val isSubStepPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = Color(60, 63, 65)
            add(Box.createVerticalStrut(10))
            add(isSubStepCheckbox)
        }
        
        // Create layout matchers panel with table and buttons
        val layoutMatchersPanel = createLayoutMatchersPanel()
        
        // Add components to main panel
        panel.add(formPanel)
        panel.add(layoutMatchersPanel)
        panel.add(isSubStepPanel)
        
        return panel
    }
    
    /**
     * Create the layout matchers panel with table and buttons
     */
    private fun createLayoutMatchersPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Layout Matchers",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            null,
            Color.WHITE
        )
        
        // Panel for add/remove buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.background = Color(60, 63, 65)
        
        val addButton = JButton(AllIcons.General.Add)
        addButton.toolTipText = "Add Matcher"
        addButton.preferredSize = Dimension(28, 28)
        
        val removeButton = JButton(AllIcons.General.Remove)
        removeButton.toolTipText = "Remove Matcher"
        removeButton.preferredSize = Dimension(28, 28)
        
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        
        // Scroll pane for table
        val scrollPane = JScrollPane(layoutMatchersTable)
        scrollPane.preferredSize = Dimension(750, 240)
        
        // Add listeners to buttons
        addButton.addActionListener { layoutMatchersModel.addEmptyRow() }
        removeButton.addActionListener {
            val selectedRow = layoutMatchersTable.selectedRow
            if (selectedRow >= 0) {
                layoutMatchersModel.removeRow(selectedRow)
            }
        }
        
        // Add components to panel
        panel.add(buttonPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * Configure the layout matchers table
     */
    private fun configureLayoutMatchersTable() {
        layoutMatchersTable.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            selectionBackground = Color(90, 90, 120)
            selectionForeground = Color.WHITE
            autoResizeMode = JTable.AUTO_RESIZE_OFF
        }
        
        // Set column widths and editors
        layoutMatchersTable.columnModel.getColumn(0).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(1).preferredWidth = 120
        layoutMatchersTable.columnModel.getColumn(2).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(3).preferredWidth = 120
        layoutMatchersTable.columnModel.getColumn(4).preferredWidth = 120
        
        // Set ComboBox editors for specific columns
        layoutMatchersTable.columnModel.getColumn(0).cellEditor = DefaultCellEditor(
            JComboBox(arrayOf("text", "content_description", "class_name"))
        )
        layoutMatchersTable.columnModel.getColumn(2).cellEditor = DefaultCellEditor(
            JComboBox(arrayOf("equals", "contains"))
        )
        layoutMatchersTable.columnModel.getColumn(3).cellEditor = DefaultCellEditor(
            JComboBox(arrayOf("none", "click", "scroll_down", "scroll_right", "scroll_left_right", "invisible"))
        )
        layoutMatchersTable.columnModel.getColumn(4).cellEditor = DefaultCellEditor(
            JComboBox(arrayOf("layout_match", "tts_end"))
        )
    }
    
    /**
     * Create the navigation panel with previous, save, and next buttons
     */
    private fun createNavigationPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER))
        panel.background = Color(60, 63, 65)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            JBUI.Borders.empty(8)
        )
        
        val prevButton = JButton("← Previous Step")
        val saveButton = JButton("Save Changes")
        val nextButton = JButton("Next Step →")
        
        prevButton.addActionListener { onPreviousStep() }
        saveButton.addActionListener { saveChanges() }
        nextButton.addActionListener { onNextStep() }
        
        panel.add(prevButton)
        panel.add(saveButton)
        panel.add(nextButton)
        
        return panel
    }
    
    /**
     * Update the UI with data from a step
     */
    fun updateUIFromStep(step: Step) {
        idField.text = step.id
        screenIdField.text = step.screenId
        guideContentArea.text = step.guideContent
        nextStepsField.text = step.nextStepIds.joinToString(", ")
        isSubStepCheckbox.isSelected = step.isSubStep
        
        // Update layout matchers table
        updateLayoutMatchersTable(step)
    }
    
    /**
     * Update the layout matchers table from a step
     */
    fun updateLayoutMatchersTable(step: Step) {
        layoutMatchersModel.data.clear()
        (step.layoutMatchers ?: emptyList()).forEach { matcher ->
            layoutMatchersModel.data.add(arrayOf(
                matcher.matchTarget,
                matcher.matchOperand,
                matcher.matchCriteria,
                matcher.highlightType,
                matcher.transitionCondition
            ))
        }
        layoutMatchersModel.fireTableDataChanged()
    }
    
    /**
     * Handle navigation to the previous step
     */
    private fun onPreviousStep() {
        val previousSteps = editorLogic.findPreviousSteps()
        
        if (previousSteps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No previous step found.", "Navigation", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        
        if (previousSteps.size == 1) {
            // If there's only one previous step, navigate to it
            setStep(previousSteps[0])
        } else {
            // If there are multiple previous steps, show selection dialog
            val options = previousSteps.map { it.id }.toTypedArray()
            val selected = JOptionPane.showInputDialog(
                this,
                "Select previous step:",
                "Previous Step",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            )
            
            if (selected != null) {
                val selectedStep = previousSteps.find { it.id == selected }
                if (selectedStep != null) {
                    setStep(selectedStep)
                }
            }
        }
    }
    
    /**
     * Handle navigation to the next step
     */
    private fun onNextStep() {
        val nextSteps = editorLogic.findNextSteps()
        
        if (nextSteps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No next step found.", "Navigation", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        
        if (nextSteps.size == 1) {
            // If there's only one next step, navigate to it
            setStep(nextSteps[0])
        } else {
            // If there are multiple next steps, show selection dialog
            val options = nextSteps.map { it.id }.toTypedArray()
            val selected = JOptionPane.showInputDialog(
                this,
                "Select next step:",
                "Next Step",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            )
            
            if (selected != null) {
                val selectedStep = nextSteps.find { it.id == selected }
                if (selectedStep != null) {
                    setStep(selectedStep)
                }
            }
        }
    }
    
    /**
     * Save changes to the current step
     */
    private fun saveChanges() {
        // Get values from UI
        val newId = idField.text
        val newScreenId = screenIdField.text
        val newGuideContent = guideContentArea.text
        val newNextStepIds = nextStepsField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val isSubStep = isSubStepCheckbox.isSelected
        
        // Convert table data to LayoutMatcher objects
        val layoutMatchers = mutableListOf<LayoutMatcher>()
        for (row in 0 until layoutMatchersModel.rowCount) {
            val matchTarget = layoutMatchersModel.getValueAt(row, 0)?.toString() ?: ""
            val matchOperand = layoutMatchersModel.getValueAt(row, 1)?.toString() ?: ""
            val matchCriteria = layoutMatchersModel.getValueAt(row, 2)?.toString()
            val highlightType = layoutMatchersModel.getValueAt(row, 3)?.toString()
            val transitionCondition = layoutMatchersModel.getValueAt(row, 4)?.toString()
            
            layoutMatchers.add(LayoutMatcher(
                matchTarget,
                matchOperand,
                matchCriteria,
                highlightType,
                transitionCondition
            ))
        }
        
        // Update step in the logic class
        val (success, message) = editorLogic.updateStepFromData(
            newId,
            newScreenId,
            newGuideContent,
            newNextStepIds,
            isSubStep,
            layoutMatchers
        )
        
        // Show result message
        JOptionPane.showMessageDialog(
            this,
            message,
            if (success) "Success" else "Error",
            if (success) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE
        )
    }
    
    /**
     * Set a step for editing
     */
    fun setStep(step: Step) {
        editorLogic.setStep(step)
        updateUIFromStep(step)
    }
    
    /**
     * Reset the UI
     */
    fun reset() {
        idField.text = ""
        screenIdField.text = ""
        guideContentArea.text = ""
        nextStepsField.text = ""
        isSubStepCheckbox.isSelected = false
        
        layoutMatchersModel.data.clear()
        layoutMatchersModel.fireTableDataChanged()
        
        editorLogic.reset()
    }
} 