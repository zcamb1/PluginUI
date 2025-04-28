package com.example.rulemaker.ui.util

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step

/**
 * Utility functions for working with graph and rules.
 * Contains helper functions extracted from GraphPanel for better code organization.
 */
object GraphUtils {

    /**
     * Calculate an appropriate width for a node based on text length.
     */
    fun calculateWidthForText(text: String): Double {
        // Phân loại theo độ dài của nội dung
        val textLength = text.length
        
        // Giảm kích thước tất cả node (giảm 20-25% chiều rộng)
        return when {
            textLength < 12 -> 140.0  // Giảm từ 180 xuống 140
            textLength < 18 -> 170.0  // Giảm từ 220 xuống 170
            else -> 220.0             // Giảm từ 280 xuống 220
        }
    }
    
    /**
     * Format a nice label for the step.
     */
    fun formatStepLabel(step: Step): String {
        // Keep original ID, no text replacement
        return step.id
    }
    
    /**
     * Check if a step is a start step (entry point).
     * A start step is a step that is not referenced by any other step.
     */
    fun isStartStep(step: Step, rule: Rule): Boolean {
        // A step is considered a start step if it's not referenced by any other step
        return !rule.steps.any { otherStep -> otherStep.nextStepIds.contains(step.id) }
    }
    
    /**
     * Check if a step is an end step (terminal node).
     */
    fun isEndStep(step: Step): Boolean {
        // A step is considered an end step if it has no outgoing connections
        return step.nextStepIds.isEmpty()
    }
    
    /**
     * Find all start nodes in the rule (nodes that are not referenced by any other node).
     */
    fun findStartNodes(rule: Rule): List<String> {
        val allReferencedIds = rule.steps.flatMap { it.nextStepIds }.toSet()
        val startNodes = rule.steps.filter { it.id !in allReferencedIds }.map { it.id }
        
        return startNodes
    }
    
    /**
     * Find true end nodes (nodes with no next steps/children)
     */
    fun findEndNodes(rule: Rule): List<String> {
        val endNodes = rule.steps.filter { it.nextStepIds.isEmpty() }.map { it.id }
        return endNodes
    }
    
    /**
     * Find all bidirectional connection pairs (A→B and B→A).
     */
    fun findAllBidirectionalPairs(rule: Rule): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        
        // Check all nodes
        rule.steps.forEach { stepA ->
            stepA.nextStepIds.forEach { stepBId ->
                // Check if B points back to A
                val stepB = rule.steps.find { it.id == stepBId }
                if (stepB?.nextStepIds?.contains(stepA.id) == true) {
                    // Add to list if not already there (avoid duplicates)
                    val orderedPair = if (stepA.id < stepBId) 
                                        Pair(stepA.id, stepBId) 
                                     else 
                                        Pair(stepBId, stepA.id)
                                        
                    if (!pairs.contains(orderedPair)) {
                        pairs.add(orderedPair)
                    }
                }
            }
        }
        
        return pairs
    }
    
    /**
     * Identify the main flow path from start nodes to end nodes.
     * Trong layout mới, chỉ là các node chính, không bao gồm các sub-node.
     */
    fun identifyMainFlowPath(rule: Rule): List<String> {
        // Lấy tất cả các node chính (không phải sub node)
        val mainNodes = rule.steps.filter { !it.isSubStep }.map { it.id }
        
        // Tìm start node
        val startNodes = findStartNodes(rule)
        if (startNodes.isEmpty()) {
            return mainNodes
        }
        
        val startNodeId = startNodes[0]
        
        // Xây dựng đường đi từ start node
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()
        
        // Thêm start node vào đầu tiên
        result.add(startNodeId)
        visited.add(startNodeId)
        
        // BFS để tìm đường đi từ start node
        val queue = ArrayDeque<String>()
        queue.add(startNodeId)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentStep = rule.steps.find { it.id == current } ?: continue
            
            // Chỉ xem xét các kết nối đến các node chính (không phải sub node)
            for (nextId in currentStep.nextStepIds) {
                val nextStep = rule.steps.find { it.id == nextId } ?: continue
                
                // Chỉ quan tâm đến các node chính
                if (!nextStep.isSubStep && nextId !in visited) {
                    visited.add(nextId)
                    result.add(nextId)
                    queue.add(nextId)
                }
            }
        }
        
        // Kiểm tra xem có mainNode nào chưa được thăm
        val missingMainNodes = mainNodes.filter { it !in visited }
        if (missingMainNodes.isNotEmpty()) {
            result.addAll(missingMainNodes)
        }
        
        return result
    }
    
    /**
     * Reconstruct the path from start to end using the parent map.
     */
    fun reconstructPath(parentMap: Map<String, String>, startNodeId: String, endNodeId: String): List<String> {
        val path = mutableListOf<String>()
        var currentId = endNodeId
        
        // Work backwards from end to start
        while (currentId != startNodeId) {
            path.add(0, currentId)
            currentId = parentMap[currentId] ?: break
        }
        
        // Add the start node
        path.add(0, startNodeId)
        
        return path
    }
    
    /**
     * Check if the rule has any expected connections
     */
    fun hasExpectedConnections(rule: Rule): Boolean {
        return rule.steps.any { it.nextStepIds.isNotEmpty() }
    }
} 