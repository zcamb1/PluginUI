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
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import java.util.ArrayDeque

/**
 * Main window for the Rule Maker plugin.
 */
class RuleMakerWindow(private val project: Project) : JPanel(BorderLayout()) {
    
    private val LOG = Logger.getInstance(RuleMakerWindow::class.java)
    
    private val ruleParser = RuleParser()
    private var currentRule: Rule? = null
    
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
    }
    
    private val mirrorScreenPanel = JPanel(BorderLayout()).apply {
        add(JBLabel("Mirrored Screen Panel", SwingConstants.CENTER), BorderLayout.CENTER)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Mirrored Screen Panel",
            TitledBorder.LEFT,
            TitledBorder.TOP
        )
    }
    
    init {
        // Create toolbar for the Step Editor panel
        val actionGroup = createActionGroup()
        val toolbar = ActionManager.getInstance().createActionToolbar("RuleMakerToolbar", actionGroup, true)
        toolbar.targetComponent = this
        
        // Create Step Editor panel with toolbar
        val stepEditorPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(editorPanel, BorderLayout.CENTER)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Rule and Step Input Panel",
                TitledBorder.LEFT,
                TitledBorder.TOP
            )
            preferredSize = Dimension(400, 150)
        }
        
        // Create top panel with Step Editor and Mirror Panel side by side using JSplitPane for resizable panels
        val topPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = stepEditorPanel
            rightComponent = mirrorScreenPanel
            resizeWeight = 0.4
            setDividerLocation(500)
            setContinuousLayout(true)
        }
        
        // Create graph panel with border
        val graphPanelWithBorder = JPanel(BorderLayout()).apply {
            add(JBScrollPane(graphPanel), BorderLayout.CENTER)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Graph Panel",
                TitledBorder.LEFT,
                TitledBorder.TOP
            )
            preferredSize = Dimension(800, 400)
        }
        
        // Create log panel with border
        val logPanelWithBorder = JPanel(BorderLayout()).apply {
            add(JBScrollPane(logMessagePanel), BorderLayout.CENTER)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Log Message Panel",
                TitledBorder.LEFT,
                TitledBorder.TOP
            )
            preferredSize = Dimension(800, 100)
        }
        
        // Create main vertical splitter between top and bottom sections
        val mainVerticalSplitter = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = topPanel
            
            // Create second splitter for graph and log panels
            val graphLogSplitter = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
                topComponent = graphPanelWithBorder
                bottomComponent = logPanelWithBorder
                resizeWeight = 0.8
                setDividerLocation(0.8)
                setContinuousLayout(true)

            }
            
            bottomComponent = graphLogSplitter
            resizeWeight = 0.3
            setDividerLocation(0.3)
            setContinuousLayout(true)

        }
        
        // Add main content to layout
        add(mainVerticalSplitter, BorderLayout.CENTER)
        
        // Set default size
        preferredSize = Dimension(1000, 700)
    }
    
    /**
     * Create action group for toolbar.
     */
    private fun createActionGroup(): DefaultActionGroup {
        val group = DefaultActionGroup()
        
        // Open Rule action
        val openRuleAction = object : AnAction("Open Rule", "Open a rule JSON file", AllIcons.Actions.MenuOpen) {
            override fun actionPerformed(e: AnActionEvent) {
                openRuleFile()
            }
        }
        group.add(openRuleAction)
        
        // Validate Rule action
        val validateRuleAction = object : AnAction("Validate Rule", "Validate the current rule", AllIcons.General.InspectionsOK) {
            override fun actionPerformed(e: AnActionEvent) {
                validateRule()
            }
        }
        group.add(validateRuleAction)
        
        // Add Step action
        val addStepAction = object : AnAction("Add Step", "Add a new step", AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                onAddStep(null,null,null)
            }
        }
        group.add(addStepAction)
        
        return group
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
     * This method is no longer needed since isSubStep is explicitly set in JSON
     * Kept for backward compatibility with older rule files that might not have isSubStep
     */
    private fun identifyMainPathAndSetSubSteps(rule: Rule) {
        // Just log the current isSubStep settings without modifying them
        LOG.info("Main steps: ${rule.steps.filter { !it.isSubStep }.map { it.id }}")
        LOG.info("Sub-steps: ${rule.steps.filter { it.isSubStep }.map { it.id }}")
    }
    
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