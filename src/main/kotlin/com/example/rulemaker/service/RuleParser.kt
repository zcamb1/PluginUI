package com.example.rulemaker.service

import com.example.rulemaker.model.*
import com.intellij.openapi.diagnostic.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Service responsible for parsing and serializing rule files.
 */
class RuleParser {
    private val LOG = Logger.getInstance(RuleParser::class.java)

    /**
     * Parse a rule from a JSON string.
     */
    fun parseRule(jsonStr: String): Rule {
        val jsonObject = JSONObject(jsonStr)
        return parseRuleFromJson(jsonObject)
    }

    /**
     * Parse multiple rules from a JSON file.
     */
    fun parseRulesFromFile(file: File): List<Rule> {
        val jsonStr = file.readText()
        val jsonObject = JSONObject(jsonStr)
        
        LOG.info("Parsing rule file: ${file.absolutePath}")
        
        // Check if root is an array or a single rule
        return when {
            // First check for "stepRules" structure (for compatibility with provided JSON)
            jsonObject.has("stepRules") -> {
                val rulesArray = jsonObject.getJSONArray("stepRules")
                val rules = mutableListOf<Rule>()
                for (i in 0 until rulesArray.length()) {
                    val rule = parseRuleFromJson(rulesArray.getJSONObject(i))
                    validateRuleConnections(rule)
                    rules.add(rule)
                }
                LOG.info("Parsed ${rules.size} rules from 'stepRules' array")
                rules
            }
            // Then check for "rules" structure (original format)
            jsonObject.has("rules") -> {
                val rulesArray = jsonObject.getJSONArray("rules")
                val rules = mutableListOf<Rule>()
                for (i in 0 until rulesArray.length()) {
                    val rule = parseRuleFromJson(rulesArray.getJSONObject(i))
                    validateRuleConnections(rule)
                    rules.add(rule)
                }
                LOG.info("Parsed ${rules.size} rules from 'rules' array")
                rules
            }
            // Finally try to parse as a single rule
            else -> {
                try {
                    val rule = parseRuleFromJson(jsonObject)
                    validateRuleConnections(rule)
                    LOG.info("Parsed a single rule")
                    listOf(rule)
                } catch (e: Exception) {
                    LOG.error("Failed to parse rule from JSON: ${e.message}")
                    throw IllegalArgumentException("Invalid rule JSON format: Could not find 'rules', 'stepRules', or a direct rule object")
                }
            }
        }
    }

    /**
     * Validate and log rule connections for debugging
     */
    private fun validateRuleConnections(rule: Rule) {
        LOG.info("Rule ${rule.id} has ${rule.steps.size} steps")
        
        var totalConnections = 0
        for (step in rule.steps) {
            LOG.info("Step ${step.id} has ${step.nextStepIds.size} connections: ${step.nextStepIds.joinToString(", ")}")
            totalConnections += step.nextStepIds.size
        }
        
        LOG.info("Total connections in rule: $totalConnections")
        
        // Check for invalid references
        val stepIds = rule.steps.map { it.id }.toSet()
        val invalidRefs = mutableListOf<String>()
        
        for (step in rule.steps) {
            for (nextId in step.nextStepIds) {
                if (!stepIds.contains(nextId)) {
                    invalidRefs.add("${step.id} -> $nextId")
                }
            }
        }
        
        if (invalidRefs.isNotEmpty()) {
            LOG.warn("Invalid step references found: ${invalidRefs.joinToString(", ")}")
        }
    }

    /**
     * Serialize a rule to a JSON string.
     * format lại json và hỗ trợ xuất ra file nếu cần
     */
    fun serializeRule(rule: Rule): String {
        val jsonObject = JSONObject()
        
        jsonObject.put("id", rule.id)
        jsonObject.put("ruleSpecVersion", rule.ruleSpecVersion)
        jsonObject.put("ruleVersion", rule.ruleVersion)
        
        // Serialize targetAppPackages
        val appsArray = JSONArray()
        for (app in rule.targetAppPackages) {
            val appObject = JSONObject()
            appObject.put("packageName", app.packageName)
            appObject.put("minAppVersion", app.minAppVersion)
            appsArray.put(appObject)
        }
        jsonObject.put("targetAppPackages", appsArray)
        
        // Add optional landing URI
        rule.landingUri?.let {
            jsonObject.put("landingUri", it)
        }
        
        // Serialize utterances
        val utterancesArray = JSONArray()
        for (utterance in rule.utterances) {
            utterancesArray.put(utterance)
        }
        jsonObject.put("utterances", utterancesArray)
        
        // Serialize preConditions
        val preConditionsArray = JSONArray()
        for (condition in rule.preConditions) {
            preConditionsArray.put(condition)
        }
        jsonObject.put("preConditions", preConditionsArray)
        
        // Serialize steps
        val stepsArray = JSONArray()
        for (step in rule.steps) {
            stepsArray.put(serializeStep(step))
        }
        jsonObject.put("steps", stepsArray)
        
        return jsonObject.toString(2)
    }

    /**
     * Parse a rule from a JSON object.
     */
    private fun parseRuleFromJson(jsonObject: JSONObject): Rule {
        val id = jsonObject.getString("id")
        val ruleSpecVersion = jsonObject.getInt("ruleSpecVersion")
        val ruleVersion = jsonObject.getInt("ruleVersion")
        
        // Parse target app packages
        val targetApps = mutableListOf<TargetApp>()
        if (jsonObject.has("targetAppPackages")) {
            val appsArray = jsonObject.getJSONArray("targetAppPackages")
            for (i in 0 until appsArray.length()) {
                val appObject = appsArray.getJSONObject(i)
                val packageName = appObject.getString("packageName")
                val minAppVersion = appObject.getLong("minAppVersion")
                targetApps.add(TargetApp(packageName, minAppVersion))
            }
        }
        
        // Parse optional landing URI
        val landingUri = if (jsonObject.has("landingUri")) {
            jsonObject.getString("landingUri")
        } else {
            null
        }
        
        // Parse utterances
        val utterances = mutableListOf<String>()
        if (jsonObject.has("utterances")) {
            val utterancesArray = jsonObject.getJSONArray("utterances")
            for (i in 0 until utterancesArray.length()) {
                utterances.add(utterancesArray.getString(i))
            }
        }
        
        // Parse preConditions
        val preConditions = mutableListOf<String>()
        if (jsonObject.has("preConditions")) {
            val preConditionsArray = jsonObject.getJSONArray("preConditions")
            for (i in 0 until preConditionsArray.length()) {
                preConditions.add(preConditionsArray.getString(i))
            }
        }
        
        // Parse steps
        val steps = mutableListOf<Step>()
        if (jsonObject.has("steps")) {
            val stepsArray = jsonObject.getJSONArray("steps")
            for (i in 0 until stepsArray.length()) {
                val stepObject = stepsArray.getJSONObject(i)
                steps.add(parseStep(stepObject))
            }
        }
        
        return Rule(
            id = id,
            ruleSpecVersion = ruleSpecVersion,
            ruleVersion = ruleVersion,
            targetAppPackages = targetApps,
            landingUri = landingUri,
            utterances = utterances,
            preConditions = preConditions,
            steps = steps
        )
    }

    /**
     * Parse a step from a JSON object.
     */
    private fun parseStep(jsonObject: JSONObject): Step {
        val id = jsonObject.getString("id")
        val screenId = jsonObject.getString("screenId")
        val guideContent = jsonObject.optString("guideContent", "")
        val isSubStep = jsonObject.optBoolean("isSubStep", false)
        
        // Parse layout matchers
        val layoutMatchers = mutableListOf<LayoutMatcher>()
        if (jsonObject.has("layoutMatchers")) {
            val matchersArray = jsonObject.getJSONArray("layoutMatchers")
            for (i in 0 until matchersArray.length()) {
                val matcherObject = matchersArray.getJSONObject(i)
                layoutMatchers.add(parseLayoutMatcher(matcherObject))
            }
        }
        
        // Parse next step IDs
        val nextStepIds = mutableListOf<String>()
        if (jsonObject.has("nextStepIds")) {
            val nextStepsArray = jsonObject.getJSONArray("nextStepIds")
            for (i in 0 until nextStepsArray.length()) {
                val nextId = nextStepsArray.getString(i)
                nextStepIds.add(nextId)
                LOG.info("Added next step ID: $nextId for step: $id")
            }
            LOG.info("Step $id has ${nextStepIds.size} next steps: ${nextStepIds.joinToString(", ")}")
        } else {
            LOG.info("Step $id has no next steps defined")
        }
        
        // Parse screen matcher and transition condition
        val screenMatcher = jsonObject.optString("screenMatcher", null)
        val transitionCondition = jsonObject.optString("transitionCondition", null)
        
        return Step(
            id = id,
            screenId = screenId,
            guideContent = guideContent,
            layoutMatchers = layoutMatchers,
            nextStepIds = nextStepIds,
            screenMatcher = screenMatcher,
            transitionCondition = transitionCondition,
            isSubStep = isSubStep
        )
    }

    /**
     * Parse a layout matcher from a JSON object.
     */
    private fun parseLayoutMatcher(jsonObject: JSONObject): LayoutMatcher {
        val matchTarget = jsonObject.getString("matchTarget")
        val matchOperand = jsonObject.getString("matchOperand")
        
        // Parse optional fields
        val matchCriteria = if (jsonObject.has("matchCriteria")) {
            jsonObject.getString("matchCriteria")
        } else {
            null
        }
        
        val highlightType = if (jsonObject.has("highlightType")) {
            jsonObject.getString("highlightType")
        } else {
            null
        }
        
        return LayoutMatcher(
            matchTarget = matchTarget,
            matchOperand = matchOperand,
            matchCriteria = matchCriteria,
            highlightType = highlightType
        )
    }

    /**
     * Serialize a step to a JSON object.
     */
    private fun serializeStep(step: Step): JSONObject {
        val jsonObject = JSONObject()
        
        jsonObject.put("id", step.id)
        jsonObject.put("screenId", step.screenId)
        jsonObject.put("guideContent", step.guideContent)
        jsonObject.put("isSubStep", step.isSubStep)
        
        // Serialize layout matchers
        val matchersArray = JSONArray()
        for (matcher in step.layoutMatchers) {
            val matcherObject = JSONObject()
            matcherObject.put("matchTarget", matcher.matchTarget)
            matcherObject.put("matchOperand", matcher.matchOperand)
            matcher.matchCriteria?.let { matcherObject.put("matchCriteria", it) }
            matcher.highlightType?.let { matcherObject.put("highlightType", it) }
            matchersArray.put(matcherObject)
        }
        jsonObject.put("layoutMatchers", matchersArray)
        
        // Serialize next step IDs
        val nextStepsArray = JSONArray()
        for (nextStepId in step.nextStepIds) {
            nextStepsArray.put(nextStepId)
        }
        jsonObject.put("nextStepIds", nextStepsArray)
        
        // Add optional fields
        step.screenMatcher?.let { jsonObject.put("screenMatcher", it) }
        step.transitionCondition?.let { jsonObject.put("transitionCondition", it) }
        
        return jsonObject
    }
} 
