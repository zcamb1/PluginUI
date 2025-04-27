package com.example.rulemaker.service.parser

import com.example.rulemaker.model.*

/**
 * Service for serializing rules to JSON format using auto-mapping
 */
object RuleJsonSerializer {
    /**
     * Serialize a Rule object to a JSON string
     */
    fun serializeRule(rule: Rule): String {
        return JsonHelper.toJson(rule)
    }
}
