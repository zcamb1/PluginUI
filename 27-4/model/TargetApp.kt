package com.example.rulemaker.model

/**
 * Represents a target app package for a rule.
 */
data class TargetApp(
    val packageName: String,
    val minAppVersion: Long
)
