package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.model.LayoutMatcher
import javax.swing.JOptionPane

/**
 * Class containing logic for step editing, separated from UI components.
 */
class EditorPanelLogic(
    private val onStepUpdated: (Step) -> Unit
) {
    private var currentRule: Rule? = null
    private var currentStep: Step? = null
    
    // Step history for navigation
    private val stepHistory = mutableListOf<String>()
    private var currentHistoryIndex = -1
    
    /**
     * Set the current rule for reference.
     */
    fun setRule(rule: Rule) {
        currentRule = rule
    }
    
    /**
     * Get the current rule.
     */
    fun getRule(): Rule? = currentRule
    
    /**
     * Get the current step being edited.
     */
    fun getCurrentStep(): Step? = currentStep
    
    /**
     * Set the step to edit.
     */
    fun setStep(step: Step) {
        currentStep = step
        
        // Add to step history
        stepHistory.add(step.id)
        currentHistoryIndex = stepHistory.size - 1
    }
    
    /**
     * Create a new step with initial values.
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
        
        return step
    }
    
    /**
     * Find the previous step(s) in the graph.
     * Returns a list because there might be multiple parent nodes.
     */
    fun findPreviousSteps(): List<Step> {
        val step = currentStep ?: return emptyList()
        val rule = currentRule ?: return emptyList()
        
        // Find all parent nodes
        return rule.steps.filter { it.nextStepIds.contains(step.id) }
    }
    
    /**
     * Find the next step(s) in the graph.
     * Returns a list because there might be multiple child nodes.
     */
    fun findNextSteps(): List<Step> {
        val step = currentStep ?: return emptyList()
        val rule = currentRule ?: return emptyList()
        
        // Find all child nodes
        return rule.steps.filter { step.nextStepIds.contains(it.id) }
    }
    
    /**
     * Update a step with the provided data.
     */
    fun updateStepFromData(
        newId: String,
        newScreenId: String,
        newGuideContent: String,
        newNextStepIds: List<String>,
        isSubStep: Boolean,
        layoutMatchers: List<LayoutMatcher>
    ): Pair<Boolean, String> {
        val step = currentStep ?: return Pair(false, "No step is currently being edited")
        val rule = currentRule ?: return Pair(false, "No rule is currently loaded")
        
        val oldId = step.id
        
        // Check if ID has changed and update all references in the rule
        if (oldId != newId) {
            val success = rule.updateStepId(oldId, newId)
            
            if (!success) {
                return Pair(false, "Could not update step ID. A step with ID '$newId' might already exist.")
            }
        }
        
        // Update step with values
        step.id = newId
        step.screenId = newScreenId
        step.guideContent = newGuideContent
        step.isSubStep = isSubStep
        
        if (step.layoutMatchers == null) {
            step.layoutMatchers = mutableListOf()
        }
        // Update layout matchers
        step.layoutMatchers.clear()
        step.layoutMatchers.addAll(layoutMatchers)
        
        // Update next step IDs
        step.nextStepIds.clear()
        step.nextStepIds.addAll(newNextStepIds)
        
        // Notify listener
        onStepUpdated(step)
        
        return Pair(true, "Changes saved successfully!")
    }
    
    /**
     * Reset the current step.
     */
    fun reset() {
        currentStep = null
    }
} 