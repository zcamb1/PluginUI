package com.example.rulemaker.ui.logic

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import javax.swing.JOptionPane

/**
 * Logic class to handle all node interactions (add, remove, swap, select).
 * This class is responsible for managing the graph nodes and their connections.
 */
class NodeInteractionLogic(
    private val getCurrentRule: () -> Rule?,
    private val setRule: (Rule) -> Unit,
    private val refreshGraph: () -> Unit,
    private val showStepInEditor: (Step) -> Unit,
    private val createNewStep: (Boolean) -> Step,
    private val getCellForStep: (String) -> mxCell?,
    private val getCellGeometry: (mxCell) -> mxGeometry?,
    private val setCellGeometry: (mxCell, mxGeometry) -> Unit,
    private val showMessage: (String, String, Int) -> Unit
) {
    /**
     * Handle step selection
     */
    fun onStepSelected(step: Step) {
        getCurrentRule()?.let { rule ->
            showStepInEditor(step)
        }
    }
    
    /**
     * Handle step update
     */
    fun onStepUpdated(step: Step) {
        refreshGraph()
    }
    
    /**
     * Add a new step
     */
    fun onAddStep(
        parentStep: Step?, 
        parentCell: mxCell?, 
        parentGeo: mxGeometry?
    ) {
        val rule = getCurrentRule() ?: return
        val newStep = createNewStep(false)
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
    
        // Refresh the graph to show the new step
        refreshGraph()
        
        // Show new step in editor
        showStepInEditor(newStep)
    }
    
    /**
     * Add a sub-step to a parent step
     */
    fun onAddSubStep(parentStep: Step) {
        val rule = getCurrentRule() ?: return
        
        // Create new sub-step
        val subStep = createNewStep(true)
        
        // Add to rule
        rule.addStep(subStep)
        
        // Connect parent to sub-step
        parentStep.addNextStep(subStep.id)
        
        // Refresh graph
        refreshGraph()
        
        // Show new step in editor
        showStepInEditor(subStep)
    }
    
    /**
     * Remove a step
     */
    fun onRemoveStep(step: Step): Boolean {
        val rule = getCurrentRule() ?: return false
        
        // Check if step has children
        if (step.hasChildren()) {
            showMessage(
                "Cannot remove step '${step.id}' because it has next steps. Remove the connections first.",
                "Cannot Remove Step",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }
        
        // Remove step from rule
        return rule.removeStep(step.id)
    }
    
    /**
     * Swap a node with another node by ID
     */
    fun onSwapNode(stepA: Step, swapId: String) {
        val rule = getCurrentRule() ?: return
        val stepB = rule.steps.find { it.id == swapId }
        if (stepB == null) {
            showMessage("Node with ID '$swapId' not found.", "Swap Node", JOptionPane.ERROR_MESSAGE)
            return
        }
        
        // Swap the nodes
        swapNodes(rule, stepA, stepB)
        
        // Refresh the graph to show the changes
        refreshGraph()
    }
    
    /**
     * Swap two nodes in the rule
     */
    fun swapNodes(rule: Rule, stepA: Step, stepB: Step) {
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
        val cellA = getCellForStep(stepA.id)
        val cellB = getCellForStep(stepB.id)
        if (cellA != null && cellB != null) {
            val geoA = getCellGeometry(cellA)
            val geoB = getCellGeometry(cellB)
            if (geoA != null && geoB != null) {
                val newGeoA = geoB.clone() as mxGeometry
                val newGeoB = geoA.clone() as mxGeometry
                setCellGeometry(cellA, newGeoA)
                setCellGeometry(cellB, newGeoB)
            }
        }
    }
    
    /**
     * Count the total number of connections in a rule
     */
    fun countConnections(rule: Rule): Int {
        return rule.steps.sumOf { it.nextStepIds.size }
    }
} 