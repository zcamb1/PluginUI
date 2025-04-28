package com.example.rulemaker.service.validator

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step

object RuleValidator {
    fun validateNextStepReferences(rule: Rule): List<String> {
        val allStepIds = rule.steps.map { it.id }.toSet()
        val invalidReferences = mutableListOf<String>()
        rule.steps.forEach { step ->
            step.nextStepIds.forEach { nextId ->
                if (!allStepIds.contains(nextId)) {
                    invalidReferences.add("Step ${step.id} references non-existent step $nextId")
                }
            }
        }
        return invalidReferences
    }

    fun findIsolatedSteps(rule: Rule): List<Step> {
        val referencedStepIds = mutableSetOf<String>()
        rule.steps.forEach { step -> referencedStepIds.addAll(step.nextStepIds) }
        return rule.steps.filter { step ->
            !referencedStepIds.contains(step.id) && step.nextStepIds.isEmpty()
        }
    }

    fun findDuplicateStepIds(rule: Rule): List<String> {
        val seen = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()
        rule.steps.forEach { step ->
            if (!seen.add(step.id)) {
                duplicates.add(step.id)
            }
        }
        return duplicates
    }

    fun validateRule(rule: Rule): List<String> {
        val errors = mutableListOf<String>()
        errors += validateNextStepReferences(rule)
        val duplicates = findDuplicateStepIds(rule)
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate step IDs found: ${duplicates.joinToString(", ")}")
        }
        return errors
    }
}
