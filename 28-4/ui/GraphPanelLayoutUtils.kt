package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.mxgraph.model.mxGeometry
import com.mxgraph.view.mxGraph
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxPoint

object GraphPanelLayoutUtils {
    fun positionMainFlow(
        graph: mxGraph,
        stepToCellMap: Map<String, Any>,
        mainFlowPath: List<String>,
        currentRule: Rule?
    ) {
        val xSpacing = 550.0
        val mainFlowY = 200.0
        val missingNodes = mainFlowPath.filter { !stepToCellMap.containsKey(it) }
        if (missingNodes.isNotEmpty()) {
            // Optionally handle missing nodes
        }
        var currentX = 350.0
        for ((index, nodeId) in mainFlowPath.withIndex()) {
            val cell = stepToCellMap[nodeId] ?: continue
            val geo = graph.getCellGeometry(cell)
            if (geo != null) {
                val newGeo = geo.clone() as mxGeometry
                newGeo.x = index * xSpacing + 100
                newGeo.y = mainFlowY
                graph.model.setGeometry(cell, newGeo)
            }
        }
        positionSubNodes(graph, stepToCellMap, currentRule)
    }

    fun positionSubNodes(
        graph: mxGraph,
        stepToCellMap: Map<String, Any>,
        currentRule: Rule?
    ) {
        val rule = currentRule ?: return
        val mainFlowY = 200.0
        val ySpacingAbove = 100.0
        val ySpacingBelow = 100.0
        val subNodes = rule.steps.filter { it.isSubStep }
        if (subNodes.isEmpty()) return
        val processedSubNodes = mutableSetOf<String>()
        val parentToSubNodesMap = mutableMapOf<String, List<String>>()
        for (step in rule.steps) {
            val subNodeIds = step.nextStepIds.filter { id ->
                rule.steps.find { it.id == id }?.isSubStep == true
            }
            if (subNodeIds.isNotEmpty()) {
                parentToSubNodesMap[step.id] = subNodeIds
            }
        }
        for ((parentId, subNodeIds) in parentToSubNodesMap) {
            val parentCell = stepToCellMap[parentId] ?: continue
            val parentGeo = graph.getCellGeometry(parentCell) ?: continue
            val bidirectionalSubNodes = subNodeIds.filter { subNodeId ->
                rule.steps.find { it.id == subNodeId }?.nextStepIds?.contains(parentId) == true
            }
            val normalSubNodes = subNodeIds.filterNot { bidirectionalSubNodes.contains(it) }
            for (subNodeId in bidirectionalSubNodes) {
                if (processedSubNodes.contains(subNodeId)) continue
                val subCell = stepToCellMap[subNodeId] ?: continue
                val subGeo = graph.getCellGeometry(subCell)
                if (subGeo != null) {
                    val newGeo = subGeo.clone() as mxGeometry
                    newGeo.x = parentGeo.x
                    newGeo.y = parentGeo.y + ySpacingBelow
                    graph.model.setGeometry(subCell, newGeo)
                    processedSubNodes.add(subNodeId)
                }
            }
            for ((index, subNodeId) in normalSubNodes.withIndex()) {
                if (processedSubNodes.contains(subNodeId)) continue
                val subCell = stepToCellMap[subNodeId] ?: continue
                val subGeo = graph.getCellGeometry(subCell)
                if (subGeo != null) {
                    val newGeo = subGeo.clone() as mxGeometry
                    newGeo.x = parentGeo.x + parentGeo.width + 120
                    if (normalSubNodes.size == 1) {
                        newGeo.y = parentGeo.y - ySpacingAbove
                    } else if (index % 2 == 0) {
                        val slotIndex = index / 2
                        val yOffset = (slotIndex + 1) * ySpacingAbove
                        newGeo.y = parentGeo.y - yOffset
                    } else {
                        val slotIndex = index / 2
                        val yOffset = (slotIndex + 1) * ySpacingBelow
                        newGeo.y = parentGeo.y + yOffset
                    }
                    graph.model.setGeometry(subCell, newGeo)
                    processedSubNodes.add(subNodeId)
                }
            }
        }
    }

    fun ensureNodesVisible(
        graph: mxGraph,
        stepToCellMap: Map<String, Any>,
        graphComponent: mxGraphComponent
    ) {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE
        for (cell in stepToCellMap.values) {
            val geo = graph.getCellGeometry(cell) ?: continue
            minX = Math.min(minX, geo.x)
            minY = Math.min(minY, geo.y)
            maxX = Math.max(maxX, geo.x + geo.width)
            maxY = Math.max(maxY, geo.y + geo.height)
        }
        var needsTranslation = false
        var translateX = 0.0
        var translateY = 0.0
        if (minX < 0) {
            translateX = Math.abs(minX) + 50
            needsTranslation = true
        }
        if (minY < 0) {
            translateY = Math.abs(minY) + 50
            needsTranslation = true
        }
        if (needsTranslation) {
            graph.model.beginUpdate()
            try {
                for (cell in stepToCellMap.values) {
                    val geo = graph.getCellGeometry(cell) ?: continue
                    val newGeo = geo.clone() as com.mxgraph.model.mxGeometry
                    newGeo.x += translateX
                    newGeo.y += translateY
                    graph.model.setGeometry(cell, newGeo)
                }
                for (edge in graph.getChildEdges(graph.defaultParent)) {
                    val geo = graph.getCellGeometry(edge) ?: continue
                    if (geo.points != null && geo.points.isNotEmpty()) {
                        val newGeo = geo.clone() as com.mxgraph.model.mxGeometry
                        val newPoints = ArrayList<mxPoint>()
                        for (point in geo.points) {
                            newPoints.add(mxPoint(point.x + translateX, point.y + translateY))
                        }
                        newGeo.points = newPoints
                        graph.model.setGeometry(edge, newGeo)
                    }
                }
            } finally {
                graph.model.endUpdate()
            }
        }
        graphComponent.zoomAndCenter()
    }
} 