package com.example.rulemaker.service.parser

import com.example.rulemaker.model.*
import com.google.gson.JsonObject

/**
 * Service for parsing JSON into Rule objects using auto-mapping
 */
object RuleJsonParser {
    /**
     * Parse a JSON string into a Rule object
     */
    fun parseRuleFromJson(jsonString: String): Rule {
        val rule = JsonHelper.fromJson<Rule>(jsonString)
        rule.steps.forEach { step ->
            if (step.nextStepIds == null) step.nextStepIds = mutableListOf()
        }
        return rule
    }
    /**
     * Parse a JSON object into a Rule object
     */
    fun parseRuleFromJson(jsonObject: JsonObject): Rule {
        val rule = JsonHelper.fromJson<Rule>(jsonObject.toString())
        rule.steps.forEach { step ->
            if (step.nextStepIds == null) step.nextStepIds = mutableListOf()
        }
        return rule
    }
}
