package com.example.rulemaker.model

/**
 * Represents a layout matcher used in steps to match UI elements.
 */
data class LayoutMatcher(
    val matchTarget: String,
    val matchOperand: String,
    val matchCriteria: String? = null,
    val highlightType: String? = null,
    val transitionCondition: String? = null // <-- thêm dòng này
)