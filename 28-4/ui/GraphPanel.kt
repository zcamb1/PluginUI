package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.example.rulemaker.ui.util.GraphUtils
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.util.mxPoint
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import com.mxgraph.view.mxStylesheet
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.util.*
import javax.swing.*

/**
 * Panel for displaying and interacting with the rule steps graph.
 */
class GraphPanel(
    private val onStepSelected: (Step) -> Unit,
    private val onAddStep: (Step?, mxCell?, mxGeometry?) -> Unit,
    private val onAddSubStep: (Step) -> Unit,
    private val onRemoveStep: (Step) -> Boolean,
    private val onSwapNode: (Step, String) -> Unit
) : JPanel(BorderLayout()) {
    
    private val graph = mxGraph()
    private val graphComponent = mxGraphComponent(graph)
    
    private var currentRule: Rule? = null
    private val cellToStepMap = mutableMapOf<Any, Step>()
    private val stepToCellMap = mutableMapOf<String, Any>()
    
    private var userZoomLevel: Double = 0.9

    private val MAIN_STEP_STYLE = "mainStep"
    private val SUB_STEP_STYLE = "subStep"
    private val EDGE_STYLE = "edge"
    private val ACTIVE_STEP_STYLE = "activeStep"
    private val START_STEP_STYLE = "startStep"
    private val END_STEP_STYLE = "endStep"
    
    private var defaultEdgeColor = "#b1b1b1"
    
    private var mainFlowPath = listOf<String>()
    
    
    init {
        graph.isAllowDanglingEdges = false
        graph.isAllowLoops = true
        graph.isCellsEditable = false
        graph.isCellsResizable = false
        graph.isCellsMovable = true
        graph.isDisconnectOnMove = false

        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_ROUNDED] = true
        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_ARCSIZE] = 15
        
        graphComponent.connectionHandler.isEnabled = false
        graphComponent.setToolTips(true)

        graphComponent.setAntiAlias(true)
        graphComponent.setTextAntiAlias(true)
        graphComponent.verticalScrollBar.unitIncrement = 7
        graphComponent.horizontalScrollBar.unitIncrement = 7
        graphComponent.setBackground(JBColor(Color(250, 250, 250), Color(60, 63, 65)))
        
        graph.gridSize = 50
        
        graphComponent.viewport.setViewPosition(java.awt.Point(0, 200))
        
        setupStylesheet()
        
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
   
        
        add(controlPanel, BorderLayout.NORTH)
        add(graphComponent, BorderLayout.CENTER)
        
        graph.addListener("cellsMoved") { sender, evt ->
        }
        
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    if (cell != null && cell is mxCell && !cell.isEdge) {
                        val step = cellToStepMap[cell]
                        if (step != null) {
                            onStepSelected(step)
                            
                        }
                    }
                }
            }
            
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    if (cell != null && cell is mxCell && !cell.isEdge) {
                        val step = cellToStepMap[cell]
                        if (step != null) {
                            showContextMenu(e.x, e.y, step)
                        }
                    } else {
                        // Show context menu for empty space
                        showEmptySpaceContextMenu(e.x, e.y)
                    }
                }
            }
        })
    }
 
    private fun setupStylesheet() {
        val stylesheet = graph.stylesheet
    
        fun nodeStyle(
            fill: String,
            stroke: String,
            font: Int = 12,
            width: Double = 1.5
        ): java.util.HashMap<String, Any> {
            val map = java.util.HashMap<String, Any>()
            map[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_RECTANGLE
            map[mxConstants.STYLE_PERIMETER] = mxConstants.PERIMETER_RECTANGLE
            map[mxConstants.STYLE_ROUNDED] = true
            map[mxConstants.STYLE_ARCSIZE] = 20
            map[mxConstants.STYLE_FONTSIZE] = font
            map[mxConstants.STYLE_FONTCOLOR] = "#000000"
            map[mxConstants.STYLE_FILLCOLOR] = fill
            map[mxConstants.STYLE_STROKECOLOR] = stroke
            map[mxConstants.STYLE_STROKEWIDTH] = width
            map[mxConstants.STYLE_SHADOW] = true
            map[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
            map[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
            map["wordWrap"] = "true"
            return map
        }
    
        // Đăng ký các style cho node
        stylesheet.putCellStyle(MAIN_STEP_STYLE, nodeStyle("#D4E7FF", "#7EA6E0"))
        stylesheet.putCellStyle(SUB_STEP_STYLE, nodeStyle("#F0F0F0", "#B0B0B0", font = 11))
        stylesheet.putCellStyle(START_STEP_STYLE, nodeStyle("#A5D6A7", "#2E7D32", width = 2.0))
        stylesheet.putCellStyle(END_STEP_STYLE, nodeStyle("#FFD2D2", "#FF9999"))
        stylesheet.putCellStyle(ACTIVE_STEP_STYLE, nodeStyle("#FFE2B8", "#FFA940", width = 2.0))
    
        // Edge style
        val edgeStyle = java.util.HashMap<String, Any>().apply {
            put(mxConstants.STYLE_STROKECOLOR, defaultEdgeColor)
            put(mxConstants.STYLE_STROKEWIDTH, 2.0)
            put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC)
            put(mxConstants.STYLE_FONTSIZE, 11)
            put(mxConstants.STYLE_OPACITY, 100)
            put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL)
            put(mxConstants.STYLE_ROUNDED, true)
            put(mxConstants.STYLE_ARCSIZE, 15)
            put(mxConstants.STYLE_ENDSIZE, 12.0)
        }
        stylesheet.putCellStyle(EDGE_STYLE, edgeStyle)
    }    
    
    private fun getCurrentZoom(): Double {
        return try {
            graphComponent.graph.view.scale
        } catch (e: Exception) {
            1.0 // Default zoom level
        }
    }
    
    private fun setZoomLevel(zoomLevel: Double) {
        try {
            val adjustedZoom = 1.0
            graphComponent.zoom(adjustedZoom)
        } catch (e: Exception) {
        }
    }
    
    private fun ensureNodesVisible() {
        GraphPanelLayoutUtils.ensureNodesVisible(graph, stepToCellMap, graphComponent)
    }

    fun displayRule(rule: Rule) {
        val currentZoom = getCurrentZoom()
        
        currentRule = rule
        cellToStepMap.clear()
        stepToCellMap.clear()
        
        // Check if rule has custom edge color defined
        if (rule.edgeColor != null && rule.edgeColor.isNotEmpty()) {
            defaultEdgeColor = rule.edgeColor
            // Re-setup stylesheet with new color
            setupStylesheet()
        }
        
        graph.model.beginUpdate()
        try {
            graph.removeCells(graph.getChildCells(graph.defaultParent))
            
            // First create all nodes with appropriate styles
            for (step in rule.steps) {
                createStepCell(step)
            }
            
            // Create all edges
            createAllEdges(rule)
            
        } finally {
            graph.model.endUpdate()
        }
        
        // Apply layout in a separate transaction
        graph.model.beginUpdate()
        try {
            // Apply our custom layout logic
            applyCustomMainFlowLayout(rule)
            
            // Apply special treatments for edges to avoid crossing nodes
            createSpecialEdges()
            
            // Translate graph and ensure all nodes are visible
            ensureNodesVisible()
            
        } finally {
            graph.model.endUpdate()
        }
        
        // Check if edges were created
        val edgeCount = countEdges()
        
        // If no edges, try a direct approach
        if (edgeCount == 0 && hasExpectedConnections(rule)) {
            JOptionPane.showMessageDialog(
                this,
                "Warning: Could not create connections automatically. " +
                "Connections exist in the data but are not visible in the graph.",
                "Connection Issue",
                JOptionPane.WARNING_MESSAGE
            )
        }
        
        setZoomLevel(currentZoom)
    }
    
    private fun calculateWidthForText(text: String): Double {
        return GraphUtils.calculateWidthForText(text)
    }
    
    private fun formatStepLabel(step: Step): String {
        return GraphUtils.formatStepLabel(step)
    }
    
    private fun createStepCell(step: Step): Any {
        val parent = graph.defaultParent
        
        val style = determineStepStyle(step)
        val label = formatStepLabel(step)
        val width = calculateWidthForText(label)
        val height = 45.0 
        
        val finalStyle = style + ";wordWrap=false;whiteSpace=nowrap;overflow=hidden;fontSize=12;" 
        
        val cell = graph.insertVertex(
            parent, step.id, label, 
            0.0, 0.0, width, height, finalStyle
        )
        
        cellToStepMap[cell] = step
        stepToCellMap[step.id] = cell
        
        return cell
    }
    

    private fun isStartStep(step: Step): Boolean {
        // A step is considered a start step if it's not referenced by any other step
        val rule = currentRule ?: return false
        return GraphUtils.isStartStep(step, rule)
    }
    

    private fun determineStepStyle(step: Step): String {
        // Check if this is the first step (entry point)
        val isFirstStep = isStartStep(step)
        
        // Check if this is an end step (no outgoing connections)
        val isEndStep = isEndStep(step)
        
        return when {
            isFirstStep -> START_STEP_STYLE
            isEndStep -> END_STEP_STYLE
            step.isSubStep -> SUB_STEP_STYLE
            else -> MAIN_STEP_STYLE
        }
    }
    
    /**
     * Check if a step is an end step (terminal node).
     */
    private fun isEndStep(step: Step): Boolean {
        return GraphUtils.isEndStep(step)
    }
    
    private fun createAllEdges(rule: Rule) {
        var edgesCreated = 0
        
        // Clear any existing edges first to prevent duplicates
        val existingEdges = graph.getChildEdges(graph.defaultParent)
        if (existingEdges != null && existingEdges.isNotEmpty()) {
            graph.removeCells(existingEdges)
        }
        
        // Find all bidirectional pairs (nodes that connect to each other)
        val bidirectionalPairs = findAllBidirectionalPairs()
        
        // Identify start nodes for special edge styling
        val startNodes = rule.steps.filter { step -> isStartStep(step) }.map { it.id }
        
        // Create all edges based on nextStepIds
        for (step in rule.steps) {
            val sourceCell = stepToCellMap[step.id]
            if (sourceCell == null) {
                continue
            }
            
            val isMainStep = !step.isSubStep
            val isSourceStartNode = startNodes.contains(step.id)
            
            for (nextStepId in step.nextStepIds) {
                val targetCell = stepToCellMap[nextStepId]
                if (targetCell == null) {
                    continue
                }
                
                val targetStep = rule.steps.find { it.id == nextStepId }
                if (targetStep == null) {
                    continue
                }
                
                val isTargetMainStep = !targetStep.isSubStep
                
                val edgeStyle = EDGE_STYLE
                
                    val edge = graph.insertEdge(
                        graph.defaultParent,
                        "edge_${step.id}_to_$nextStepId",
                        "",
                        sourceCell,
                        targetCell,
                    edgeStyle
                    )
                    edgesCreated++
                
                if (isSourceStartNode) {
                } else if (isMainStep && isTargetMainStep) {
                } else {
                }
            }
        }
    }
    
    private fun applyCustomMainFlowLayout(rule: Rule) {
        if (rule.steps.isEmpty()) return
        
        mainFlowPath = identifyMainFlowPath(rule)
        
        graph.model.beginUpdate()
        try {
            positionMainFlow(mainFlowPath)
            
            ensureVerticalEdgesAligned()
            
        } finally {
            graph.model.endUpdate()
        }
    }
    
    private fun positionMainFlow(mainFlowPath: List<String>) {
        GraphPanelLayoutUtils.positionMainFlow(graph, stepToCellMap, mainFlowPath, currentRule)
    }
    
    private fun positionSubNodes() {
        GraphPanelLayoutUtils.positionSubNodes(graph, stepToCellMap, currentRule)
    }
    

    fun getMainSteps(): List<Step> {
        val rule = currentRule ?: return emptyList()
        return rule.steps.filter { !it.isSubStep }
    }
    

    fun getSubSteps(): List<Step> {
        val rule = currentRule ?: return emptyList()
        return rule.steps.filter { it.isSubStep }
    }

    private fun showContextMenu(x: Int, y: Int, step: Step) {
        val popup = JPopupMenu()
        
        val editItem = JMenuItem("Edit Step")
        editItem.addActionListener { onStepSelected(step) }
        popup.add(editItem)
        
        if (!step.isSubStep) {
            val addSubStepItem = JMenuItem("Add Sub-Step")
            addSubStepItem.addActionListener { onAddSubStep(step) }
            popup.add(addSubStepItem)
    
            val addNextStepItem = JMenuItem("Add Next Step")
            addNextStepItem.addActionListener {
                val parentCell = stepToCellMap[step.id] as? com.mxgraph.model.mxCell
                val parentGeo = parentCell?.let { graph.getCellGeometry(it) }
                onAddStep(step, parentCell, parentGeo)
            }
            popup.add(addNextStepItem)
        }
        
        val removeItem = JMenuItem("Remove Step")
        removeItem.isEnabled = !step.hasChildren()
        removeItem.addActionListener {
            if (onRemoveStep(step)) {
                refreshGraph()
            }
        }
        popup.add(removeItem)

        val swapItem = JMenuItem("Swap Node")
        swapItem.addActionListener {
            val swapId = JOptionPane.showInputDialog(
        null,
        "Enter ID of node to swap with:",
        "Swap Node",
        JOptionPane.QUESTION_MESSAGE
        )
            if (swapId != null && swapId.isNotBlank()) {
                onSwapNode(step, swapId.trim())
            }
        }
        popup.add(swapItem)
        
        popup.addSeparator()
        val layoutItem = JMenuItem("Rearrange Layout")
        layoutItem.addActionListener {
            applyLayout()
        }
        popup.add(layoutItem)
        
        popup.show(graphComponent.graphControl, x, y)
    }
    fun setCellGeometry(cell: mxCell?, geo: mxGeometry) {
        graph.model.setGeometry(cell, geo)
    }
    
    fun getCellForStep(stepId: String): mxCell? {
        return stepToCellMap[stepId] as? mxCell
    }
    

    fun getCellGeometry(cell: mxCell?): mxGeometry? {
        return cell?.let { graph.getCellGeometry(it) }
    }


    private fun showEmptySpaceContextMenu(x: Int, y: Int) {
        val popup = JPopupMenu()
        
        val addStepItem = JMenuItem("Add New Step")
        addStepItem.addActionListener { onAddStep(null,null,null) }
        popup.add(addStepItem)
        
        // Add layout menu item
        popup.addSeparator()
        val layoutItem = JMenuItem("Rearrange Layout")
        layoutItem.addActionListener {
            applyLayout()
        }
        popup.add(layoutItem)
        
        popup.show(graphComponent.graphControl, x, y)
    }
    
    

    fun refreshGraph() {
        currentRule?.let { displayRule(it) }
        
        ensureNodesVisible()
    }

    private fun findStartNodes(rule: Rule): List<String> {
        return GraphUtils.findStartNodes(rule)
    }
    
    private fun findEndNodes(rule: Rule): List<String> {
        return GraphUtils.findEndNodes(rule)
    }
    
    private fun reconstructPath(parentMap: Map<String, String>, startNodeId: String, endNodeId: String): List<String> {
        return GraphUtils.reconstructPath(parentMap, startNodeId, endNodeId)
    }

    private fun identifyMainFlowPath(rule: Rule): List<String> {
        return GraphUtils.identifyMainFlowPath(rule)
    }
    
    private fun countEdges(): Int {
        val edges = graph.getChildEdges(graph.defaultParent)
        return edges?.size ?: 0
    }
    
    private fun hasExpectedConnections(rule: Rule): Boolean {
        return GraphUtils.hasExpectedConnections(rule)
    }
    

    private fun ensureVerticalEdgesAligned() {
        // Process allconnections
        currentRule?.let { rule ->
            for (step in rule.steps) {
                val sourceCell = stepToCellMap[step.id] ?: continue
                val sourceGeo = graph.getCellGeometry(sourceCell) ?: continue
                val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
                
                for (nextStepId in step.nextStepIds) {
                    val targetCell = stepToCellMap[nextStepId] ?: continue
                    val targetGeo = graph.getCellGeometry(targetCell) ?: continue
                    
                    val yDiff = Math.abs(sourceGeo.y - targetGeo.y)
                    val xDiff = Math.abs(sourceGeo.x - targetGeo.x)
                    
                    if (yDiff > 70 && xDiff < 100) {
                        val targetCenterX = targetGeo.x + targetGeo.width / 2
                        val newTargetX = sourceCenterX - targetGeo.width / 2
                        
                        if (Math.abs(sourceCenterX - targetCenterX) > 2) {
                            val newGeo = targetGeo.clone() as mxGeometry
                            newGeo.x = newTargetX
                            graph.model.setGeometry(targetCell, newGeo)
                        }
                    }
                }
            }
        }
    }
    
    fun applyLayout() {
        graph.model.beginUpdate()
        try {
            currentRule?.let { 
                applyCustomMainFlowLayout(it)
            } ?: {
                val layout = mxHierarchicalLayout(graph, SwingConstants.WEST)
                layout.execute(graph.defaultParent)
            }()
            
            graphComponent.zoomAndCenter()
            setZoomLevel(userZoomLevel)
            
        } finally {
            graph.model.endUpdate()
        }
    }

    private fun findAllBidirectionalPairs(): List<Pair<String, String>> {
        val rule = currentRule ?: return emptyList()
        return GraphUtils.findAllBidirectionalPairs(rule)
    }

    private fun findNodesOnPathBetween(
        sourceGeo: mxGeometry,
        targetGeo: mxGeometry,
        sourceId: String,
        targetId: String,
        isSourceSubStep: Boolean
    ): List<String> {
        return GraphPanelEdgeUtils.findNodesOnPathBetween(
            currentRule,
            stepToCellMap,
            graph,
            sourceGeo,
            targetGeo,
            sourceId,
            targetId,
            isSourceSubStep
        )
    }

    private fun createSpecialEdges() {
        GraphPanelEdgeUtils.createSpecialEdges(
            graph,
            currentRule,
            cellToStepMap,
            stepToCellMap,
            ::findNodesOnPathBetween
        )
    }

    private fun configureSubToSubEdge(edge: mxCell, source: mxCell, target: mxCell) {
        GraphPanelEdgeUtils.configureSubToSubEdge(graph, edge)
    }

    private fun configureMainToMainEdge(edge: mxCell, source: mxCell, target: mxCell) {
        GraphPanelEdgeUtils.configureMainToMainEdge(graph, edge)
    }

    private fun configureMainToSubEdge(edge: mxCell, source: mxCell, target: mxCell) {
        GraphPanelEdgeUtils.configureMainToSubEdge(graph, edge, source, target, cellToStepMap)
    }

    private fun configureSubToMainEdge(edge: mxCell, source: mxCell, target: mxCell) {
        GraphPanelEdgeUtils.configureSubToMainEdge(graph, edge, source, target, cellToStepMap, ::findNodesOnPathBetween, stepToCellMap)
    }

} 