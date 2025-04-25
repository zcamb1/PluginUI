package com.example.rulemaker.ui

import com.example.rulemaker.model.LayoutMatcher
import com.example.rulemaker.model.Step
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionListModel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Color
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

/**
 * Panel for editing step details with enhanced features.
 */
class EditorPanel(
    private val onStepUpdated: (Step) -> Unit,
    private var currentRule: com.example.rulemaker.model.Rule? = null
) : JPanel(BorderLayout()) {
    
    private val idField = JBTextField()
    private val screenIdField = JBTextField()
    private val guideContentArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val nextStepsField = JBTextField()
    private val isSubStepCheckbox = JCheckBox("Is Sub-Step")
    
    // New UI components for enhanced editing
    private val actionTypeComboBox = ComboBox<String>(arrayOf("tap", "swipe_left", "swipe_right", "swipe_up", "swipe_down", "long_press", "back"))
    private val layoutMatchersTable = createLayoutMatchersTable()
    private val transitionConditionField = JBTextField()
    
    // Step navigation buttons
    private val prevStepButton = JButton("Previous Step").apply {
        icon = AllIcons.Actions.Back
        isEnabled = false
    }
    private val nextStepButton = JButton("Next Step").apply {
        icon = AllIcons.Actions.Forward
        isEnabled = false
    }
    
    // Step history for navigation
    private val stepHistory = mutableListOf<String>()
    private var currentHistoryIndex = -1
    
    private var currentStep: Step? = null
    
    init {
        // Simple layout with just the basic form and navigation
        val mainPanel = JPanel(BorderLayout())
        
        // Basic form for step editing
        val formPanel = createBasicInfoPanel()
        mainPanel.add(formPanel, BorderLayout.CENTER)
        
        // Navigation panel at bottom
        val navigationPanel = createNavigationPanel()
        mainPanel.add(navigationPanel, BorderLayout.SOUTH)
        
        // Add to main layout
        add(mainPanel, BorderLayout.CENTER)
        
        // Simple border
        border = JBUI.Borders.empty(5)
    }
    
    // Getters for fields
    fun getIdField(): JBTextField = idField
    fun getScreenIdField(): JBTextField = screenIdField
    fun getGuideContentArea(): JTextArea = guideContentArea
    fun getNextStepsField(): JBTextField = nextStepsField
    fun getIsSubStepCheckbox(): JCheckBox = isSubStepCheckbox
    
    /**
     * Create the basic information panel.
     */
    private fun createBasicInfoPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // Create form
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Step ID:", idField)
            .addLabeledComponent("Screen ID:", screenIdField)
            .addLabeledComponent("Guide Content:", JBScrollPane(guideContentArea).apply {
                preferredSize = Dimension(300, 100)
                minimumSize = Dimension(200, 50)
            })
            .addLabeledComponent("Next Step IDs:", nextStepsField)
            .addComponent(isSubStepCheckbox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            
        // Add padding
        formPanel.border = JBUI.Borders.empty(10)
        
        panel.add(formPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * Create the layout matchers table.
     */
    private fun createLayoutMatchersTable(): JBTable {
        val columnNames = arrayOf("Target Type", "Match Value", "Match Criteria", "Highlight")
        val tableModel = DefaultTableModel(columnNames, 0)
        
        val table = JBTable(tableModel)
        table.preferredScrollableViewportSize = Dimension(300, 150)
        
        return table
    }
    
    /**
     * Create the navigation panel.
     */
    private fun createNavigationPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER))
    
        // Create Previous Step button
        val prevStepButton = JButton("Previous Step").apply {
            icon = AllIcons.Actions.Back
            isEnabled = true
            addActionListener { onPreviousStep() }
        }
    
        // Create Next Step button
        val nextStepButton = JButton("Next Step").apply {
            icon = AllIcons.Actions.Forward
            isEnabled = true
            addActionListener { onNextStep() }
        }
    
        // Save Changes button
        val saveButton = JButton("Save Changes").apply {
            addActionListener { saveChanges() }
        }
    
        // Add buttons to panel
        panel.add(prevStepButton)
        panel.add(saveButton)
        panel.add(nextStepButton)
    
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            JBUI.Borders.empty(8)
        )
    
        return panel
    }

    /**
     * Navigate to the previous step
     */
    fun onPreviousStep() {
        val currentStep = this.currentStep ?: return
        val rule = currentRule ?: return
        // Find all parent nodes
        val parentIds = rule.steps.filter { it.nextStepIds.contains(currentStep.id) }.map { it.id }
        if (parentIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No previous step.", "Navigation", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        if (parentIds.size == 1) {
            val prevStep = rule.steps.find { it.id == parentIds[0] }
            if (prevStep != null) setStep(prevStep)
        } else {
            val options = parentIds.toTypedArray()
            val selected = JOptionPane.showInputDialog(this, "Select previous step:", "Previous Step", JOptionPane.QUESTION_MESSAGE, null, options, options[0])
            if (selected != null) {
                val prevStep = rule.steps.find { it.id == selected }
                if (prevStep != null) setStep(prevStep)
            }
        }
    }
    
    /**
     * Navigate to the next step
     */
    fun onNextStep() {
        val currentStep = this.currentStep ?: return
        val rule = currentRule ?: return
        val nextIds = currentStep.nextStepIds
        if (nextIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No next step.", "Navigation", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        if (nextIds.size == 1) {
            val nextStep = rule.steps.find { it.id == nextIds[0] }
            if (nextStep != null) setStep(nextStep)
        } else {
            val options = nextIds.toTypedArray()
            val selected = JOptionPane.showInputDialog(this, "Select next step:", "Next Step", JOptionPane.QUESTION_MESSAGE, null, options, options[0])
            if (selected != null) {
                val nextStep = rule.steps.find { it.id == selected }
                if (nextStep != null) setStep(nextStep)
            }
        }
    }
    
    /**
     * Update the layout matchers table with data from the current step.
     */
    private fun updateLayoutMatchersTable() {
        val tableModel = layoutMatchersTable.model as DefaultTableModel
        tableModel.rowCount = 0
        
        val step = currentStep ?: return
        
        for (matcher in step.layoutMatchers) {
            tableModel.addRow(arrayOf(
                matcher.matchTarget,
                matcher.matchOperand,
                matcher.matchCriteria ?: "",
                matcher.highlightType ?: ""
            ))
        }
    }
    
    /**
     * Set the step to edit.
     */
    fun setStep(step: Step) {
        currentStep = step
        
        // Add to step history
        stepHistory.add(step.id)
        currentHistoryIndex = stepHistory.size - 1
        updateNavigationButtons()
        
        // Update UI with step details
        idField.text = step.id
        screenIdField.text = step.screenId
        guideContentArea.text = step.guideContent
        nextStepsField.text = step.nextStepIds.joinToString(", ")
        isSubStepCheckbox.isSelected = step.isSubStep
        
        // Update transition condition
        transitionConditionField.text = step.transitionCondition ?: ""
        
        // Update layout matchers table
        updateLayoutMatchersTable()
        
        // Allow editing of both ID fields
        idField.isEditable = true
        screenIdField.isEditable = true
    }
    
    /**
     * Update the enabled state of navigation buttons.
     */
    private fun updateNavigationButtons() {
        prevStepButton.isEnabled = currentHistoryIndex > 0
        nextStepButton.isEnabled = currentHistoryIndex < stepHistory.size - 1
    }
    
    /**
     * Set the current rule for reference.
     */
    fun setRule(rule: com.example.rulemaker.model.Rule) {
        currentRule = rule
    }
    
    /**
     * Save changes to the current step.
     */
    fun saveChanges() {
        if (currentStep == null) return
        
        val oldId = currentStep!!.id
        val oldScreenId = currentStep!!.screenId
        
        val newId = idField.text
        val newScreenId = screenIdField.text
        
        // Track if ID was changed for message
        var idChanged = false
        
        // Check if ID has changed and update all references in the rule
        if (oldId != newId && currentRule != null) {
            val success = currentRule!!.updateStepId(oldId, newId)
            
            if (success) {
                idChanged = true
            } else {
                // ID change failed, revert to old ID
                JOptionPane.showMessageDialog(
                    this,
                    "Could not update step ID. ID reverted.",
                    "ID Change Failed",
                    JOptionPane.ERROR_MESSAGE
                )
                idField.text = oldId
                currentStep!!.id = oldId
                // Notify listener of the revert
                onStepUpdated(currentStep!!)
                return
            }
        }
        
        // Update step with values from UI
        currentStep!!.id = newId
        currentStep!!.screenId = newScreenId
        currentStep!!.guideContent = guideContentArea.text
        currentStep!!.isSubStep = isSubStepCheckbox.isSelected
        
        // Parse next step IDs
        val nextStepIds = nextStepsField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        currentStep!!.nextStepIds.clear()
        currentStep!!.nextStepIds.addAll(nextStepIds)
        
        // Notify listener
        onStepUpdated(currentStep!!)
        
        // Show confirmation message with ID change info if applicable
        val message = if (idChanged) {
            "Changes saved successfully!\nStep ID changed from $oldId to $newId. All references updated."
        } else {
            "Changes saved successfully!"
        }
        
        JOptionPane.showMessageDialog(
            this,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * Create a new step with initial values.
     * isSubStep is now managed internally via RuleMakerWindow.identifyMainPathAndSetSubSteps,
     * but we still keep the UI control to allow manual override.
     */
    fun createNewStep(isSubStep: Boolean = false): Step {
        val newStepId = "step_${System.currentTimeMillis()}"
        val step = Step(
            id = newStepId,
            screenId = "com.example.activity",
            guideContent = "New step",
            isSubStep = isSubStep
        )
        
        // Set for editing
        setStep(step)
        
        // ID should be editable for new steps
        idField.isEditable = true
        
        return step
    }
    
    /**
     * Reset the form.
     */
    fun reset() {
        currentStep = null
        idField.text = ""
        screenIdField.text = ""
        guideContentArea.text = ""
        nextStepsField.text = ""
        isSubStepCheckbox.isSelected = false
        
        // Reset action type
        actionTypeComboBox.selectedItem = "tap"
        
        // Reset transition condition
        transitionConditionField.text = ""
        
        // Clear layout matchers table
        val tableModel = layoutMatchersTable.model as DefaultTableModel
        tableModel.rowCount = 0
    }
} 
