package com.example.rulemaker.model

/**
 * Represents a rule containing multiple steps.
 */
data class Rule(
    val id: String,
    val ruleSpecVersion: Int,
    val ruleVersion: Int,
    val targetAppPackages: List<TargetApp> = listOf(),
    val landingUri: String? = null,
    val utterances: List<String> = listOf(),
    val preConditions: List<String> = listOf(),
    var steps: MutableList<Step> = mutableListOf(),
    val edgeColor: String? = null
) {
    fun findStepById(id: String): Step? = steps.find { it.id == id }

    fun addStep(step: Step) {
        steps.add(step)
    }

    fun removeStep(stepId: String): Boolean {
        val step = findStepById(stepId) ?: return false
        if (step.hasChildren()) return false
        steps.forEach { s -> s.nextStepIds.remove(stepId) }
        return steps.removeIf { it.id == stepId }
    }

    fun validateNextStepReferences(): List<String> {
        val allStepIds = steps.map { it.id }.toSet()
        val invalidReferences = mutableListOf<String>()
        steps.forEach { step ->
            step.nextStepIds.forEach { nextId ->
                if (!allStepIds.contains(nextId)) {
                    invalidReferences.add("Step ${step.id} references non-existent step $nextId")
                }
            }
        }
        return invalidReferences
    }

    fun findIsolatedSteps(): List<Step> {
        val referencedStepIds = mutableSetOf<String>()
        steps.forEach { step -> referencedStepIds.addAll(step.nextStepIds) }
        return steps.filter { step ->
            !referencedStepIds.contains(step.id) && step.nextStepIds.isEmpty()
        }
    }

    fun updateStepId(oldId: String, newId: String): Boolean {
        val step = findStepById(oldId) ?: return false
        steps.forEach { s ->
            val index = s.nextStepIds.indexOf(oldId)
            if (index >= 0) s.nextStepIds[index] = newId
        }
        step.id = newId
        return true
    }
}
