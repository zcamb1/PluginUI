package com.example.rulemaker.ui

import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.util.mxPoint
import com.mxgraph.view.mxGraph
import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step

object GraphPanelEdgeUtils {
    fun createSpecialEdges(
        graph: mxGraph,
        currentRule: Rule?,
        cellToStepMap: Map<Any, Step>,
        stepToCellMap: Map<String, Any>,
        findNodesOnPathBetween: (mxGeometry, mxGeometry, String, String, Boolean) -> List<String>
    ) {
        val edges = graph.getChildEdges(graph.defaultParent)
        if (edges == null || edges.isEmpty()) {
            return
        }
        val rule = currentRule ?: return
        for (edge in edges) {
            if (edge !is mxCell || !edge.isEdge) continue
            val source = edge.source as? mxCell
            val target = edge.target as? mxCell
            if (source == null || target == null) continue
            val sourceStep = cellToStepMap[source]
            val targetStep = cellToStepMap[target]
            if (sourceStep == null || targetStep == null) continue
            if (!sourceStep.isSubStep && targetStep.isSubStep) {
                configureMainToSubEdge(graph, edge, source, target, cellToStepMap)
                continue
            }
            if (sourceStep.isSubStep && !targetStep.isSubStep) {
                configureSubToMainEdge(graph, edge, source, target, cellToStepMap, findNodesOnPathBetween, stepToCellMap)
                continue
            }
            if (!sourceStep.isSubStep && !targetStep.isSubStep) {
                configureMainToMainEdge(graph, edge)
                continue
            }
            if (sourceStep.isSubStep && targetStep.isSubStep) {
                configureSubToSubEdge(graph, edge)
                continue
            }
        }
    }

    fun configureSubToSubEdge(graph: mxGraph, edge: mxCell) {
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }

    fun configureMainToMainEdge(graph: mxGraph, edge: mxCell) {
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }

    fun configureMainToSubEdge(
        graph: mxGraph,
        edge: mxCell,
        source: mxCell,
        target: mxCell,
        cellToStepMap: Map<Any, Step>
    ) {
        val sourceGeo = graph.getCellGeometry(source)
        val targetGeo = graph.getCellGeometry(target)
        if (sourceGeo == null || targetGeo == null) return
        val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
        val targetCenterX = targetGeo.x + targetGeo.width / 2
        val xDiff = Math.abs(sourceCenterX - targetCenterX)
        val edgeGeo = edge.geometry.clone() as mxGeometry
        edgeGeo.points = ArrayList<mxPoint>()
        if (xDiff < 20) {
            edgeGeo.points = ArrayList()
            graph.model.setGeometry(edge, edgeGeo)
            graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
            return
        }
        val isToTheRight = targetGeo.x > sourceGeo.x + sourceGeo.width
        if (isToTheRight) {
            val x1 = sourceGeo.x + sourceGeo.width
            val y1 = sourceGeo.y + sourceGeo.height / 2
            val x3 = targetGeo.x
            val y3 = targetGeo.y + targetGeo.height / 2
            val midX = (x1 + x3) / 2
            if (Math.abs(y1 - y3) < 30) {
                // No extra control points needed
            } else {
                edgeGeo.points.add(mxPoint(midX, y1))
                edgeGeo.points.add(mxPoint(midX, y3))
            }
        } else {
            val x1 = sourceGeo.x + sourceGeo.width + 30
            val y1 = sourceGeo.y + sourceGeo.height / 2
            val x3 = targetGeo.x
            val y3 = targetGeo.y + targetGeo.height / 2
            val x2 = x1
            val y2 = y3
            edgeGeo.points.add(mxPoint(x1, y1))
            edgeGeo.points.add(mxPoint(x2, y2))
        }
        graph.model.setGeometry(edge, edgeGeo)
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }

    fun configureSubToMainEdge(
        graph: mxGraph,
        edge: mxCell,
        source: mxCell,
        target: mxCell,
        cellToStepMap: Map<Any, Step>,
        findNodesOnPathBetween: (mxGeometry, mxGeometry, String, String, Boolean) -> List<String>,
        stepToCellMap: Map<String, Any>
    ) {
        val sourceGeo = graph.getCellGeometry(source)
        val targetGeo = graph.getCellGeometry(target)
        if (sourceGeo == null || targetGeo == null) return
        val sourceId = cellToStepMap[source]?.id ?: "unknown"
        val targetId = cellToStepMap[target]?.id ?: "unknown"
        val edgeGeo = edge.geometry.clone() as mxGeometry
        edgeGeo.points = ArrayList<mxPoint>()
        val sourceRightX = sourceGeo.x + sourceGeo.width
        val sourceMidY = sourceGeo.y + sourceGeo.height / 2
        val targetMidX = targetGeo.x + targetGeo.width / 2
        val targetTopY = targetGeo.y
        val targetBottomY = targetGeo.y + targetGeo.height
        val xDiff = Math.abs((sourceGeo.x + sourceGeo.width / 2) - (targetGeo.x + targetGeo.width / 2))
        if (xDiff < 20) {
            edgeGeo.points = ArrayList()
            graph.model.setGeometry(edge, edgeGeo)
            graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
            return
        }
        val nodesOnPath = findNodesOnPathBetween(sourceGeo, targetGeo, sourceId, targetId, true)
        if (nodesOnPath.isNotEmpty()) {
            val obstacleId = nodesOnPath.first()
            val obstacleCell = stepToCellMap[obstacleId] ?: return
            val obstacleGeo = graph.getCellGeometry(obstacleCell as mxCell) ?: return
            val obstacleRightX = obstacleGeo.x + obstacleGeo.width + 40
            val isBelow = sourceGeo.y > targetGeo.y
            edgeGeo.points.add(mxPoint(sourceRightX + 20, sourceMidY))
            val yObstacle = if (isBelow)
                obstacleGeo.y + obstacleGeo.height + 30
            else
                obstacleGeo.y - 30
            edgeGeo.points.add(mxPoint(sourceRightX + 20, yObstacle))
            edgeGeo.points.add(mxPoint(obstacleRightX, yObstacle))
            val yTarget = if (isBelow) targetTopY else targetBottomY
            edgeGeo.points.add(mxPoint(obstacleRightX, yTarget))
            edgeGeo.points.add(mxPoint(targetMidX + 30, yTarget))
        } else {
            val isAbove = sourceGeo.y + sourceGeo.height < targetGeo.y
            val isBelow = sourceGeo.y > targetGeo.y + targetGeo.height
            val yTarget = when {
                isAbove -> targetTopY - 10
                isBelow -> targetBottomY + 10
                else -> targetMidX
            }
            val outX = maxOf(sourceRightX + 40, targetMidX)
            edgeGeo.points.add(mxPoint(outX, sourceMidY))
            if (isAbove || isBelow) {
                edgeGeo.points.add(mxPoint(outX, if (isAbove) targetTopY - 10 else targetBottomY + 10))
                edgeGeo.points.add(mxPoint(targetMidX, if (isAbove) targetTopY - 10 else targetBottomY + 10))
            } else {
                edgeGeo.points.add(mxPoint(targetMidX, sourceMidY))
            }
        }
        graph.model.setGeometry(edge, edgeGeo)
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }

    fun findNodesOnPathBetween(
        rule: Rule?,
        stepToCellMap: Map<String, Any>,
        graph: mxGraph,
        sourceGeo: mxGeometry,
        targetGeo: mxGeometry,
        sourceId: String,
        targetId: String,
        isSourceSubStep: Boolean
    ): List<String> {
        val ruleObj = rule ?: return emptyList()
        val nodesOnPath = mutableListOf<String>()
        val isSourceAbove = sourceGeo.y < 150
        val targetStep = ruleObj.steps.find { it.id == targetId }
        val sourceStep = ruleObj.steps.find { it.id == sourceId }
        val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
        val targetCenterX = targetGeo.x + targetGeo.width / 2
        val xDiff = Math.abs(sourceCenterX - targetCenterX)
        if (xDiff < 30) return emptyList()
        if (isSourceSubStep && targetStep != null && !targetStep.isSubStep) {
            val subNodesOfTarget = ruleObj.steps.filter {
                it.isSubStep && targetStep.nextStepIds.contains(it.id)
            }
            val sameDirectionSubNodes = subNodesOfTarget.filter { subNode ->
                val subCell = stepToCellMap[subNode.id] ?: return@filter false
                val subGeo = graph.getCellGeometry(subCell as mxCell) ?: return@filter false
                val isSubAbove = subGeo.y < 150
                isSourceAbove == isSubAbove && subGeo.x <= targetGeo.x
            }
            if (sameDirectionSubNodes.isNotEmpty()) {
                sameDirectionSubNodes.forEach { subNode -> nodesOnPath.add(subNode.id) }
                return nodesOnPath
            }
        }
        val minX = minOf(sourceGeo.x, targetGeo.x)
        val maxX = maxOf(sourceGeo.x, targetGeo.x)
        for (step in ruleObj.steps) {
            if (step.id == sourceId || step.id == targetId) continue
            val cell = stepToCellMap[step.id] ?: continue
            val cellGeo = graph.getCellGeometry(cell as mxCell) ?: continue
            if (cellGeo.x > minX && cellGeo.x < maxX) {
                val isCellAbove = cellGeo.y < 150
                if (isCellAbove == isSourceAbove) {
                    if (isSourceSubStep && !step.isSubStep) continue
                    if (step.isSubStep) nodesOnPath.add(step.id)
                }
            }
        }
        return nodesOnPath
    }
} 