package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.model.LayoutMatcher
import com.example.rulemaker.service.RuleParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import java.awt.*
import java.io.File
import javax.swing.*

class RuleMakerWindow(private val project: Project) : JPanel(BorderLayout()) {

    private val LOG = Logger.getInstance(RuleMakerWindow::class.java)
    private val ruleParser = RuleParser()
    private var currentRule: Rule? = null

    // UI components (đã tách sang file riêng)
    private val editorPanel = EditorPanel(::onStepUpdated, null)
    private val graphPanel = GraphPanel(::onStepSelected, ::onAddStep, ::onAddSubStep, ::onRemoveStep, ::onSwapNode)
    private val logPanel = LogPanel()
    private val stepInfoPanel = StepInfoPanel(editorPanel, ::onStepUpdated)
    private val commonInfoPanel = JPanel(BorderLayout()).apply {
        background = Color(60, 63, 65)
        add(JLabel("zxczxczczc", SwingConstants.CENTER), BorderLayout.CENTER)
    }
    //private val commonInfoPanel = CommonInfoPanel()
    private val mirrorPanel = MirrorPanel()
    private val screenInfoPanel = ScreenInfoPanel()

    init {
        background = Color(60, 63, 65)
        preferredSize = Dimension(1200, 700)
        layout = BorderLayout()

        // Top toolbar
        add(createTopToolbar(), BorderLayout.NORTH)

        // Main content (split top/bottom)
        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.background = background

        // Top: Tabs (Common/Step Info), Mirror, Screen Info
        val topSection = JPanel(BorderLayout())
        topSection.background = background

        val tabAndMirrorContainer = JPanel()
        tabAndMirrorContainer.layout = BoxLayout(tabAndMirrorContainer, BoxLayout.X_AXIS)
        tabAndMirrorContainer.background = background
        tabAndMirrorContainer.add(createCombinedTabPanel())
        tabAndMirrorContainer.add(mirrorPanel)
        tabAndMirrorContainer.add(screenInfoPanel)
        topSection.add(tabAndMirrorContainer, BorderLayout.CENTER)

        // Bottom: Graph + Log
        val bottomSection = JPanel(BorderLayout())
        bottomSection.background = background
        val bottomPanelsLayout = JPanel()
        bottomPanelsLayout.layout = BoxLayout(bottomPanelsLayout, BoxLayout.X_AXIS)
        bottomPanelsLayout.background = background
        bottomPanelsLayout.add(graphPanel)
        bottomPanelsLayout.add(logPanel)
        bottomSection.add(bottomPanelsLayout, BorderLayout.CENTER)

        // SplitPane
        val mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, topSection, bottomSection)
        mainSplitPane.resizeWeight = 0.5
        mainSplitPane.border = null
        mainSplitPane.dividerSize = 5
        mainContentPanel.add(mainSplitPane, BorderLayout.CENTER)
        add(mainContentPanel, BorderLayout.CENTER)
    }

    private fun createTopToolbar(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = background
        val titleLabel = JLabel("IUG Rule Maker Tool")
        titleLabel.foreground = Color.WHITE
        titleLabel.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        panel.add(titleLabel, BorderLayout.WEST)

        val buttonsPanel = JPanel()
        buttonsPanel.background = background
        val exportButton = JButton("Export")
        val importButton = JButton("Import")
        val exitButton = JButton("Exit")
        importButton.addActionListener { openRuleFile() }
        buttonsPanel.add(exportButton)
        buttonsPanel.add(importButton)
        buttonsPanel.add(exitButton)
        panel.add(buttonsPanel, BorderLayout.CENTER)

        val userPanel = JPanel()
        userPanel.background = background
        val userLabel = JLabel("Hello, abcxyz ▼")
        userLabel.foreground = Color.WHITE
        userPanel.add(userLabel)
        panel.add(userPanel, BorderLayout.EAST)
        return panel
    }

    

    private fun createCombinedTabPanel(): JTabbedPane {
        val tabbedPane = JTabbedPane()
        tabbedPane.background = background
        tabbedPane.foreground = Color.WHITE
        tabbedPane.addTab("Common Info", commonInfoPanel)
        tabbedPane.addTab("Step Info", stepInfoPanel)

        tabbedPane.selectedIndex = 0
        tabbedPane.preferredSize = Dimension(580, 300)
        return tabbedPane
    }

    // ==== CALLBACKS & LOGIC ====

    private fun openRuleFile() {
        // Mở file, parse rule, gọi setRule(rules[0])
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Open Rule JSON File"
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                val rules = ruleParser.parseRulesFromFile(file)
                if (rules.isNotEmpty()) {
                    setRule(rules[0])
                    logPanel.log("Successfully loaded rule: ${rules[0].id}")
                } else {
                    logPanel.log("No rules found in the file.")
                    JOptionPane.showMessageDialog(this, "No rules found in the file.", "No Rules", JOptionPane.WARNING_MESSAGE)
                }
            } catch (e: Exception) {
                logPanel.log("Error loading rule file: ${e.message}")
                JOptionPane.showMessageDialog(this, "Error loading rule file: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private fun setRule(rule: Rule) {
        currentRule = rule
        editorPanel.setRule(rule)
        stepInfoPanel.setRule(rule)
        //commonInfoPanel.setRule(rule)
        graphPanel.displayRule(rule)
        graphPanel.applyLayout()
        logPanel.log("Loaded rule with ${rule.steps.size} steps")
    }

    private fun onStepSelected(step: Step) {
        editorPanel.setStep(step)
        stepInfoPanel.setStep(step)
        logPanel.log("Selected step: ${step.id}")
        graphPanel.highlightStep(step.id)
    }

    private fun onStepUpdated(step: Step) {
        stepInfoPanel.updateStepWithLayoutMatchersData(step)
        graphPanel.refreshGraph()
        logPanel.log("Updated step: ${step.id}")
    }

    private fun onAddStep(parentStep: Step?, cell: mxCell?, geo: mxGeometry?) {
        val rule = currentRule ?: return
        val newStep = editorPanel.createNewStep()
        rule.addStep(newStep)
        parentStep?.addNextStep(newStep.id)
        graphPanel.refreshGraph()
        logPanel.log("Added new step: ${newStep.id}")
    }

    private fun onAddSubStep(parentStep: Step) {
        val rule = currentRule ?: return
        val subStep = editorPanel.createNewStep(isSubStep = true)
        rule.addStep(subStep)
        parentStep.addNextStep(subStep.id)
        graphPanel.refreshGraph()
        logPanel.log("Added sub-step ${subStep.id} to parent ${parentStep.id}")
    }

    private fun onRemoveStep(step: Step): Boolean {
        val rule = currentRule ?: return false
        if (step.hasChildren()) {
            logPanel.log("Cannot remove step '${step.id}' because it has next steps")
            JOptionPane.showMessageDialog(this, "Cannot remove step '${step.id}' because it has next steps. Remove the connections first.", "Cannot Remove Step", JOptionPane.WARNING_MESSAGE)
            return false
        }
        if (rule.removeStep(step.id)) {
            editorPanel.reset()
            graphPanel.refreshGraph()
            logPanel.log("Removed step: ${step.id}")
            return true
        }
        return false
    }

    private fun onSwapNode(stepA: Step, swapId: String) {
        val rule = currentRule ?: return
        val stepB = rule.steps.find { it.id == swapId }
        if (stepB == null) {
            JOptionPane.showMessageDialog(this, "Node with ID '$swapId' not found.", "Swap Node", JOptionPane.ERROR_MESSAGE)
            return
        }
        // Swap isSubStep
        val tmpIsSub = stepA.isSubStep
        stepA.isSubStep = stepB.isSubStep
        stepB.isSubStep = tmpIsSub
        // Swap nextStepIds
        val tmpNext = stepA.nextStepIds.toList()
        stepA.nextStepIds.clear()
        stepA.nextStepIds.addAll(stepB.nextStepIds)
        stepB.nextStepIds.clear()
        stepB.nextStepIds.addAll(tmpNext)
        // Update references in other steps
        for (step in rule.steps) {
            for (i in step.nextStepIds.indices) {
                if (step.nextStepIds[i] == stepA.id) step.nextStepIds[i] = stepB.id
                else if (step.nextStepIds[i] == stepB.id) step.nextStepIds[i] = stepA.id
            }
        }
        graphPanel.refreshGraph()
        logPanel.log("Swapped node ${stepA.id} with ${stepB.id}")
    }

    fun getComponent(): JComponent = this
}
