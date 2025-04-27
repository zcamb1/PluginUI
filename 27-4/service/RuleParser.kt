package com.example.rulemaker.service

import com.example.rulemaker.model.Rule
import com.example.rulemaker.service.parser.JsonHelper
import com.example.rulemaker.service.parser.RuleJsonParser
import com.example.rulemaker.service.parser.RuleJsonSerializer
import com.example.rulemaker.service.validator.RuleValidator
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Service tổng: Điều phối parse, serialize, validate rule.
 */
class RuleParser {
    private val gson = JsonHelper.gson
    
    fun parseRule(jsonStr: String): Rule {
        return RuleJsonParser.parseRuleFromJson(jsonStr)
    }

    fun parseRulesFromFile(file: File): List<Rule> {
        val jsonStr = file.readText()
        val jsonElement = JsonParser.parseString(jsonStr)
        val jsonObject = jsonElement.asJsonObject
        
        return when {
            jsonObject.has("stepRules") -> {
                val rulesArray = jsonObject.getAsJsonArray("stepRules")
                List(rulesArray.size()) { i ->
                    RuleJsonParser.parseRuleFromJson(rulesArray.get(i).asJsonObject)
                }
            }
            else -> listOf(RuleJsonParser.parseRuleFromJson(jsonObject))
        }
    }

    fun serializeRule(rule: Rule): String = RuleJsonSerializer.serializeRule(rule)

    fun validateRule(rule: Rule): List<String> = RuleValidator.validateRule(rule)
    
    /**
     * Export a single rule to a JSON file
     * @param rule Rule object to export
     * @param file Destination file
     * @return Pair of success status and error message (if any)
     */
    fun exportRuleToJsonFile(rule: Rule, file: File): Pair<Boolean, String?> {
        return try {
            JsonHelper.writeJson(file.absolutePath, rule)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Error exporting rule: ${e.message}")
        }
    }
    
    /**
     * Export multiple rules to a single JSON file
     * @param rules List of Rule objects to export
     * @param file Destination file
     * @return Pair of success status and error message (if any)
     */
    fun exportRulesToJsonFile(rules: List<Rule>, file: File): Pair<Boolean, String?> {
        return try {
            val rootObject = JsonObject()
            val rulesArray = gson.toJsonTree(rules).asJsonArray
            rootObject.add("stepRules", rulesArray)
            
            file.writeText(gson.toJson(rootObject))
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Error exporting rules: ${e.message}")
        }
    }
}
