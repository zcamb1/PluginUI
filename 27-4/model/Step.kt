package com.example.rulemaker.model

/**
 * Represents a step in a rule.
 */
data class Step(
    var id: String,
    var screenId: String,
    var guideContent: String = "",
    var layoutMatchers: MutableList<LayoutMatcher> = mutableListOf(),
    var nextStepIds: MutableList<String> = mutableListOf(),
    val screenMatcher: String? = null,
    val transitionCondition: String? = null,
    var isSubStep: Boolean = false
) {
    fun hasChildren(): Boolean = nextStepIds.isNotEmpty()
    fun addNextStep(stepId: String) { if (!nextStepIds.contains(stepId)) nextStepIds.add(stepId) }
    fun removeNextStep(stepId: String) { nextStepIds.remove(stepId) }
    fun setNextSteps(stepIds: List<String>) {
        nextStepIds.clear()
        nextStepIds.addAll(stepIds)
    }

    override fun toString(): String = "Step(id='$id', nextStepIds=$nextStepIds)"
}
