package com.example.rulemaker.ui

import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
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
    
    private val LOG = Logger.getInstance(GraphPanel::class.java)
    
    private val graph = mxGraph()
    private val graphComponent = mxGraphComponent(graph)
    
    private var currentRule: Rule? = null
    private val cellToStepMap = mutableMapOf<Any, Step>()
    private val stepToCellMap = mutableMapOf<String, Any>()
    
    // Biến lưu zoom level hiện tại
    private var userZoomLevel: Double = 0.9  // Mặc định là 0.9 - scale nhỏ hơn một chút

    // Constants for styling
    private val MAIN_STEP_STYLE = "mainStep"
    private val SUB_STEP_STYLE = "subStep"
    private val EDGE_STYLE = "edge"
    private val ACTIVE_STEP_STYLE = "activeStep"
    private val START_STEP_STYLE = "startStep"
    private val END_STEP_STYLE = "endStep"
    
    // Default edge color - can be overridden when rule is loaded
    private var defaultEdgeColor = "#b1b1b1" // Default dark gray color
    
    // Store main flow path for layout
    private var mainFlowPath = listOf<String>()
    
    
    init {
        // Configure graph settings
        graph.isAllowDanglingEdges = false
        graph.isAllowLoops = true
        graph.isCellsEditable = false
        graph.isCellsResizable = false
        graph.isCellsMovable = true
        graph.isDisconnectOnMove = false

        // Set default edge style to orthogonal
        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_ROUNDED] = true
        graph.stylesheet.defaultEdgeStyle[mxConstants.STYLE_ARCSIZE] = 15
        
        // Configure graph component
        graphComponent.connectionHandler.isEnabled = false
        graphComponent.setToolTips(true)

        
        // Set anti-aliasing for better rendering
        graphComponent.setAntiAlias(true)
        graphComponent.setTextAntiAlias(true)
        graphComponent.verticalScrollBar.unitIncrement = 7
        graphComponent.horizontalScrollBar.unitIncrement = 7
        // Set background color
        graphComponent.setBackground(JBColor(Color(250, 250, 250), Color(60, 63, 65)))
        
        // Increase grid size for more spacing
        graph.gridSize = 50
        
        // Configure viewport to show negative coordinates
        graphComponent.viewport.setViewPosition(java.awt.Point(0, 200))
        
        setupStylesheet()
        
        // Create panel for controls
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
   
        
        // Add components to panel
        add(controlPanel, BorderLayout.NORTH)
        add(graphComponent, BorderLayout.CENTER)
        
        // Add listener to update coordinates when cells are moved
        graph.addListener("cellsMoved") { sender, evt ->
            // updateCoordinateDisplay() call removed
        }
        
        // Add mouse listener for selecting cells
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    if (cell != null && cell is mxCell && !cell.isEdge) {
                        val step = cellToStepMap[cell]
                        if (step != null) {
                            onStepSelected(step)
                            
                            // Remove coordinate display dialog when clicking on a node
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
    
        // Helper function để tạo style node
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
    
    /**
     * Helper method to safely get the current zoom level
     */
    private fun getCurrentZoom(): Double {
        return try {
            graphComponent.graph.view.scale
        } catch (e: Exception) {
            LOG.warn("Could not determine current zoom level: ${e.message}")
            1.0 // Default zoom level
        }
    }
    
    /**
     * Helper method to safely set the zoom level with a minimum bound
     * to ensure nodes remain visible and readable
     */
    private fun setZoomLevel(zoomLevel: Double) {
        try {
            // Tăng zoom mặc định lên 1.2 để các node trông lớn hơn
            val adjustedZoom = 1.0
            graphComponent.zoom(adjustedZoom)
            LOG.info("Set zoom level to $adjustedZoom (requested: $zoomLevel)")
        } catch (e: Exception) {
            LOG.error("Failed to set zoom level: ${e.message}")
        }
    }
    
    /**
     * Make sure all nodes are visible in the viewport
     * by translating the entire graph if needed
     */
    private fun ensureNodesVisible() {
        LOG.info("Ensuring all nodes are visible in the viewport")
        
        // Find boundaries of all cells
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
        
        // Log bounds for debugging
        LOG.info("Graph bounds: minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY")
        
        // If we have any nodes with negative coordinates, translate the entire graph
        var needsTranslation = false
        var translateX = 0.0
        var translateY = 0.0
        
        if (minX < 0) {
            translateX = Math.abs(minX) + 50
            needsTranslation = true
            LOG.info("Need to translate X by $translateX")
        }
        
        if (minY < 0) {
            translateY = Math.abs(minY) + 50
            needsTranslation = true
            LOG.info("Need to translate Y by $translateY")
        }
        
        // Apply translation if needed
        if (needsTranslation) {
            LOG.info("Translating graph by X=$translateX, Y=$translateY")
            
            graph.model.beginUpdate()
            try {
                for (cell in stepToCellMap.values) {
                    val geo = graph.getCellGeometry(cell) ?: continue
                    val newGeo = geo.clone() as mxGeometry
                    
                    newGeo.x += translateX
                    newGeo.y += translateY
                    
                    graph.model.setGeometry(cell, newGeo)
                }
                
                // Update control points for edges after moving nodes
                for (edge in graph.getChildEdges(graph.defaultParent)) {
                    val geo = graph.getCellGeometry(edge) ?: continue
                    if (geo.points != null && geo.points.isNotEmpty()) {
                        val newGeo = geo.clone() as mxGeometry
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
            
            // Log new bounds
            LOG.info("Translation complete")
        }
        
        // Fit the graph to the viewport with some padding
        graphComponent.zoomAndCenter()
        
        // Removed explicit zoom setting here to avoid conflicts
    }

    /**
     * Display a rule in the graph panel.
     */
    fun displayRule(rule: Rule) {
        // Lưu ý: Tất cả việc điều chỉnh zoom KHÔNG được thực hiện trực tiếp ở đây
        // mà được tập trung ở phương thức applyLayout() để tránh việc reset zoom nhiều lần
        
        // Lưu lại zoom level hiện tại trước khi load file mới
        val currentZoom = getCurrentZoom()
        
        currentRule = rule
        cellToStepMap.clear()
        stepToCellMap.clear()
        
        // Check if rule has custom edge color defined
        if (rule.edgeColor != null && rule.edgeColor.isNotEmpty()) {
            defaultEdgeColor = rule.edgeColor
            LOG.info("Using edge color from rule: $defaultEdgeColor")
            // Re-setup stylesheet with new color
            setupStylesheet()
        }
        
        // Create the graph in a single transaction with improved positioning
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
        LOG.info("After layout, graph has ${rule.steps.size} nodes and $edgeCount edges")
        
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
        
        // Khôi phục zoom level sau khi đã load xong file
        setZoomLevel(currentZoom)
    }
    
    /**
     * Calculate an appropriate width for a node based on text length.
     */
    private fun calculateWidthForText(text: String): Double {
        // Phân loại theo độ dài của nội dung
        val textLength = text.length
        
        // Giảm kích thước tất cả node (giảm 20-25% chiều rộng)
        return when {
            textLength < 12 -> 140.0  // Giảm từ 180 xuống 140
            textLength < 18 -> 170.0  // Giảm từ 220 xuống 170
            else -> 220.0             // Giảm từ 280 xuống 220
        }
    }
    
    /**
     * Format a nice label for the step.
     */
    private fun formatStepLabel(step: Step): String {
        // Keep original ID, no text replacement
        return step.id
    }
    
    /**
     * Create a cell for a step with appropriate styling.
     */
    private fun createStepCell(step: Step): Any {
        val parent = graph.defaultParent
        
        // Determine the appropriate style based on step characteristics
        val style = determineStepStyle(step)
        
        // Create a better label for the step
        val label = formatStepLabel(step)
        
        // Calculate required width based on text length
        val width = calculateWidthForText(label)
        
        // Giảm chiều cao cho tất cả các node
        val height = 45.0  // Giảm từ 60.0 xuống 45.0
        
        // Disable word wrapping to ensure single line text
        val finalStyle = style + ";wordWrap=false;whiteSpace=nowrap;overflow=hidden;fontSize=12;"  // Giảm font size từ 14 xuống 12
        
        val cell = graph.insertVertex(
            parent, step.id, label, 
            0.0, 0.0, width, height, finalStyle
        )
        
        cellToStepMap[cell] = step
        stepToCellMap[step.id] = cell
        
        return cell
    }
    
    /**
     * Check if a step is a start step (entry point).
     * A start step is a step that is not referenced by any other step.
     */
    private fun isStartStep(step: Step): Boolean {
        // A step is considered a start step if it's not referenced by any other step
        val rule = currentRule ?: return false
        return !rule.steps.any { otherStep -> otherStep.nextStepIds.contains(step.id) }
    }
    
    /**
     * Determine the appropriate style for a step.
     * Now directly uses the isSubStep field from JSON instead of calculating it.
     * Also, prioritizes start and end styles over main/sub styles.
     */
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
        // A step is considered an end step if it has no outgoing connections
        return step.nextStepIds.isEmpty()
    }
    
    /**
     * Manually create ALL edges in the rule at once with appropriate styling
     * based on the relationship between nodes.
     */
    private fun createAllEdges(rule: Rule) {
        var edgesCreated = 0
        
        // Clear any existing edges first to prevent duplicates
        val existingEdges = graph.getChildEdges(graph.defaultParent)
        if (existingEdges != null && existingEdges.isNotEmpty()) {
            graph.removeCells(existingEdges)
        }
        
        // Find all bidirectional pairs (nodes that connect to each other)
        val bidirectionalPairs = findAllBidirectionalPairs()
        LOG.info("Found ${bidirectionalPairs.size} bidirectional pairs: ${bidirectionalPairs.joinToString()}")
        
        // Identify start nodes for special edge styling
        val startNodes = rule.steps.filter { step -> isStartStep(step) }.map { it.id }
        LOG.info("Found start nodes: ${startNodes.joinToString()}")
        
        // Create all edges based on nextStepIds
        for (step in rule.steps) {
            val sourceCell = stepToCellMap[step.id]
            if (sourceCell == null) {
                LOG.error("Source cell not found for step ${step.id}")
                continue
            }
            
            // Check if source is a main step (not a sub-step)
            val isMainStep = !step.isSubStep
            val isSourceStartNode = startNodes.contains(step.id)
            
            for (nextStepId in step.nextStepIds) {
                val targetCell = stepToCellMap[nextStepId]
                if (targetCell == null) {
                    LOG.error("Target cell not found for step $nextStepId")
                    continue
                }
                
                // Get target step info
                val targetStep = rule.steps.find { it.id == nextStepId }
                if (targetStep == null) {
                    LOG.error("Target step not found for ID $nextStepId")
                    continue
                }
                
                // Check if target is a main step
                val isTargetMainStep = !targetStep.isSubStep
                
                // Use consistent EDGE_STYLE for all connections
                // to follow the rule: "Avoid making the connectors too colorful"
                val edgeStyle = EDGE_STYLE
                
                // Create the edge with appropriate style
                    val edge = graph.insertEdge(
                        graph.defaultParent,
                        "edge_${step.id}_to_$nextStepId",
                        "",
                        sourceCell,
                        targetCell,
                    edgeStyle
                    )
                    edgesCreated++
                
                // Log connection information - but don't change color
                if (isSourceStartNode) {
                    LOG.info("Created edge from START node: ${step.id} → $nextStepId")
                } else if (isMainStep && isTargetMainStep) {
                    LOG.info("Created main-to-main edge: ${step.id} → $nextStepId")
                } else {
                    LOG.info("Created edge: ${step.id} → $nextStepId")
                }
            }
        }
        
        LOG.info("Created $edgesCreated edges based on rule data")
    }
    
    /**
     * Custom layout algorithm optimized for main flow with start and end nodes.
     */
    private fun applyCustomMainFlowLayout(rule: Rule) {
        if (rule.steps.isEmpty()) return
        
        // Store main flow path as class variable for later use
        mainFlowPath = identifyMainFlowPath(rule)
        
        graph.model.beginUpdate()
        try {
            LOG.info("Applying custom column-based layout")
            LOG.info("Main flow path: ${mainFlowPath.joinToString(" -> ")}")
            
            // Step 1: Position the main flow horizontally
            positionMainFlow(mainFlowPath)
            
            // Step 2: Ensure all connections have proper edge routing
            ensureVerticalEdgesAligned()
            
            
        } finally {
            graph.model.endUpdate()
        }
    }

    fun highlightStep(stepId: String) {
        // Bỏ highlight cũ (nếu có)
        for ((cell, _) in cellToStepMap) {
            graph.setCellStyle(determineStepStyle(cellToStepMap[cell]!!), arrayOf(cell))
        }
        // Highlight node mới
        val cell = stepToCellMap[stepId]
        if (cell != null) {
            graph.setCellStyle(ACTIVE_STEP_STYLE, arrayOf(cell))
        }
        graphComponent.refresh()
    }
    
    /**
     * Position the main flow nodes horizontally from left to right.
     */
    private fun positionMainFlow(mainFlowPath: List<String>) {
        val xSpacing = 500.0  // Giảm khoảng cách cơ bản xuống để tối ưu hơn (từ 450 xuống 350)
        val mainFlowY = 200.0  // Vị trí Y của dòng chính
        
        LOG.info("Positioning main flow nodes horizontally")
        
        // First, verify all nodes in main flow path exist
        val missingNodes = mainFlowPath.filter { !stepToCellMap.containsKey(it) }
        if (missingNodes.isNotEmpty()) {
            LOG.error("Missing nodes in main flow: ${missingNodes.joinToString(", ")}")
        }

        
        

        
        // Position all main flow nodes horizontally with dynamic spacing
        var currentX = 350.0  // Vị trí X bắt đầu
        
        for ((index, nodeId) in mainFlowPath.withIndex()) {
            val cell = stepToCellMap[nodeId] ?: continue
            
            val geo = graph.getCellGeometry(cell)
            if (geo != null) {
                val newGeo = geo.clone() as mxGeometry
                
                newGeo.x = index * xSpacing + 100
                newGeo.y = mainFlowY
                LOG.info("Positioned main node $nodeId at x=${newGeo.x}, y=${newGeo.y}")
                
                graph.model.setGeometry(cell, newGeo)
                
            }
        }

        positionSubNodes()

    } 
    /**
     * Position sub-nodes in vertical columns relative to their connections
     */
    private fun positionSubNodes() {
        val rule = currentRule ?: return
        val mainFlowY = 200.0
        val ySpacingAbove = 100.0
        val ySpacingBelow = 100.0
    
        // Xác định tất cả các node là sub-node (isSubStep = true)
        val subNodes = rule.steps.filter { it.isSubStep }
        if (subNodes.isEmpty()) return
    
        // Map để theo dõi sub-nodes đã được xử lý
        val processedSubNodes = mutableSetOf<String>()
    
        // Map parent -> list sub-nodes
        val parentToSubNodesMap = mutableMapOf<String, List<String>>()
        for (step in rule.steps) {
            val subNodeIds = step.nextStepIds.filter { id ->
                rule.steps.find { it.id == id }?.isSubStep == true
            }
            if (subNodeIds.isNotEmpty()) {
                parentToSubNodesMap[step.id] = subNodeIds
            }
        }
    
        // Gộp logic: duyệt qua tất cả các parent có sub-node (bao gồm cả start node)
        for ((parentId, subNodeIds) in parentToSubNodesMap) {
            val parentCell = stepToCellMap[parentId] ?: continue
            val parentGeo = graph.getCellGeometry(parentCell) ?: continue
    
            for ((index, subNodeId) in subNodeIds.withIndex()) {
                if (processedSubNodes.contains(subNodeId)) continue
                val subCell = stepToCellMap[subNodeId] ?: continue
                val subGeo = graph.getCellGeometry(subCell)
                if (subGeo != null) {
                    val newGeo = subGeo.clone() as mxGeometry
                    // Đặt bên phải parent
                    newGeo.x = parentGeo.x + parentGeo.width + 120

                    val isBidirectional = rule.steps.find { it.id == subNodeId }?.nextStepIds?.contains(parentId) == true
                    if (isBidirectional) {
                        newGeo.x = parentGeo.x
                        // Nếu là kết nối 2 chiều, luôn đặt dưới main node
                        newGeo.y = parentGeo.y + ySpacingBelow
                    // Chia đều trên/dưới
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
    
        // Xử lý các sub-node chưa được xử lý (nếu có)
    
        // updateCoordinateDisplay() call removed
    }
    
    /**
     * Public method to get the main steps of the current rule.
     */
    fun getMainSteps(): List<Step> {
        val rule = currentRule ?: return emptyList()
        return rule.steps.filter { !it.isSubStep }
    }
    
    /**
     * Public method to get the sub-steps of the current rule.
     */
    fun getSubSteps(): List<Step> {
        val rule = currentRule ?: return emptyList()
        return rule.steps.filter { it.isSubStep }
    }
    
    /**
     * Show context menu for a step.
     */
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
        
        // Only allow removing if the step has no children
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
        null, // <-- truyền null để center on screen
        "Enter ID of node to swap with:",
        "Swap Node",
        JOptionPane.QUESTION_MESSAGE
        )
            if (swapId != null && swapId.isNotBlank()) {
                onSwapNode(step, swapId.trim())
            }
        }
        popup.add(swapItem)
        
        
        // Add layout menu item
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
    
    /**
     * Get the cell object for a given step ID
     */
    fun getCellForStep(stepId: String): mxCell? {
        return stepToCellMap[stepId] as? mxCell
    }
    
    /**
     * Get the geometry for a given cell
     */
    fun getCellGeometry(cell: mxCell?): mxGeometry? {
        return cell?.let { graph.getCellGeometry(it) }
    }

    /**
     * Show context menu for empty space.
     */
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
    
    
    /**
     * Refresh the graph display based on the current rule.
     */
    fun refreshGraph() {
        currentRule?.let { displayRule(it) }
        
        // Extra check to ensure all nodes are visible after refresh
        ensureNodesVisible()
    }
    
    
    /**
     * Identify the main flow path from start nodes to end nodes.
     * Trong layout mới, chỉ là các node chính, không bao gồm các sub-node.
     */
    private fun identifyMainFlowPath(rule: Rule): List<String> {
        LOG.info("Identifying main flow path for column-based layout")
        
        // Lấy tất cả các node chính (không phải sub node)
        val mainNodes = rule.steps.filter { !it.isSubStep }.map { it.id }
        
        // Tìm start node
        val startNodes = findStartNodes(rule)
        if (startNodes.isEmpty()) {
            LOG.info("No start nodes found, using all main nodes")
            return mainNodes
        }
        
        val startNodeId = startNodes[0]
        LOG.info("Using start node: $startNodeId")
        
        // Xây dựng đường đi từ start node
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()
        
        // Thêm start node vào đầu tiên
        result.add(startNodeId)
        visited.add(startNodeId)
        
        // BFS để tìm đường đi từ start node
        val queue = ArrayDeque<String>()
        queue.add(startNodeId)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentStep = rule.steps.find { it.id == current } ?: continue
            
            // Chỉ xem xét các kết nối đến các node chính (không phải sub node)
            for (nextId in currentStep.nextStepIds) {
                val nextStep = rule.steps.find { it.id == nextId } ?: continue
                
                // Chỉ quan tâm đến các node chính
                if (!nextStep.isSubStep && nextId !in visited) {
                    visited.add(nextId)
                    result.add(nextId)
                    queue.add(nextId)
                }
            }
        }
        
        // Kiểm tra xem có mainNode nào chưa được thăm
        val missingMainNodes = mainNodes.filter { it !in visited }
        if (missingMainNodes.isNotEmpty()) {
            LOG.info("Some main nodes not reachable from start node: ${missingMainNodes.joinToString()}")
            // Thêm các node chưa thăm vào cuối
            result.addAll(missingMainNodes)
        }
        
        LOG.info("Main flow path: ${result.joinToString(" -> ")}")
        return result
    }
    
    /**
     * Count the number of edges in the graph
     */
    private fun countEdges(): Int {
        val edges = graph.getChildEdges(graph.defaultParent)
        return edges?.size ?: 0
    }
    
    /**
     * Check if the rule has any expected connections
     */
    private fun hasExpectedConnections(rule: Rule): Boolean {
        return rule.steps.any { it.nextStepIds.isNotEmpty() }
    }
    
    /**
     * Ensure vertical edges are properly aligned by adjusting node positions
     */
    private fun ensureVerticalEdgesAligned() {
        // Process all connections
        currentRule?.let { rule ->
            for (step in rule.steps) {
                val sourceCell = stepToCellMap[step.id] ?: continue
                val sourceGeo = graph.getCellGeometry(sourceCell) ?: continue
                val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
                
                for (nextStepId in step.nextStepIds) {
                    val targetCell = stepToCellMap[nextStepId] ?: continue
                    val targetGeo = graph.getCellGeometry(targetCell) ?: continue
                    
                    // Check if this is a vertical connection (Y differs a lot, X is close)
                    val yDiff = Math.abs(sourceGeo.y - targetGeo.y)
                    val xDiff = Math.abs(sourceGeo.x - targetGeo.x)
                    
                    if (yDiff > 70 && xDiff < 100) {
                        LOG.info("Found vertical connection: ${step.id} -> $nextStepId")
                        
                        // Calculate X position for the target node to perfectly align with the source node
                        val targetCenterX = targetGeo.x + targetGeo.width / 2
                        val newTargetX = sourceCenterX - targetGeo.width / 2
                        
                        // Only adjust if the center coordinates differ significantly
                        if (Math.abs(sourceCenterX - targetCenterX) > 2) {
                            LOG.info("Adjusting ${nextStepId} X position from ${targetGeo.x} to $newTargetX to align with ${step.id}")
                            
                            val newGeo = targetGeo.clone() as mxGeometry
                            newGeo.x = newTargetX
                            graph.model.setGeometry(targetCell, newGeo)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Public method to apply layout when needed.
     */
    fun applyLayout() {
        graph.model.beginUpdate()
        try {
            // Get the current rule and apply the layout
            currentRule?.let { 
                applyCustomMainFlowLayout(it)
            } ?: {
                // Apply a simple layout if no rule is available
                val layout = mxHierarchicalLayout(graph, SwingConstants.WEST)
                layout.execute(graph.defaultParent)
            }()
            
            // Center the graph in the view
            graphComponent.zoomAndCenter()
            
            // Khôi phục zoom level hiện tại của người dùng thay vì reset
            setZoomLevel(userZoomLevel)
            
        } finally {
            graph.model.endUpdate()
        }
    }

    /**
     * Find all start nodes in the rule (nodes that are not referenced by any other node).
     */
    private fun findStartNodes(rule: Rule): List<String> {
        val allReferencedIds = rule.steps.flatMap { it.nextStepIds }.toSet()
        val startNodes = rule.steps.filter { it.id !in allReferencedIds }.map { it.id }
        
        LOG.info("Found ${startNodes.size} start nodes: ${startNodes.joinToString()}")
        return startNodes
    }
    
    /**
     * Find true end nodes (nodes with no next steps/children)
     */
    private fun findEndNodes(rule: Rule): List<String> {
        val endNodes = rule.steps.filter { it.nextStepIds.isEmpty() }.map { it.id }
        LOG.info("Found ${endNodes.size} end nodes: ${endNodes.joinToString()}")
        return endNodes
    }
    
    
    /**
     * Reconstruct the path from start to end using the parent map.
     */
    private fun reconstructPath(parentMap: Map<String, String>, startNodeId: String, endNodeId: String): List<String> {
        val path = mutableListOf<String>()
        var currentId = endNodeId
        
        // Work backwards from end to start
        while (currentId != startNodeId) {
            path.add(0, currentId)
            currentId = parentMap[currentId] ?: break
        }
        
        // Add the start node
        path.add(0, startNodeId)
        
        return path
    }


    private fun findNodesOnPathBetween(
    sourceGeo: mxGeometry,
    targetGeo: mxGeometry,
    sourceId: String,
    targetId: String,
    isSourceSubStep: Boolean
    ): List<String> {
    val rule = currentRule ?: return emptyList()
    val nodesOnPath = mutableListOf<String>()

    // Xác định phía của source node (trên/dưới main flow)
    val isSourceAbove = sourceGeo.y < 150

    // Tìm target step và source step
    val targetStep = rule.steps.find { it.id == targetId }
    val sourceStep = rule.steps.find { it.id == sourceId }

    // Tính tọa độ tâm của source và target
    val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
    val targetCenterX = targetGeo.x + targetGeo.width / 2

    // Nếu kết nối dọc (gần như cùng X), bỏ qua
    val xDiff = Math.abs(sourceCenterX - targetCenterX)
    if (xDiff < 30) return emptyList()

    // Nếu là kết nối sub-to-main
    if (isSourceSubStep && targetStep != null && !targetStep.isSubStep) {
        val subNodesOfTarget = rule.steps.filter {
            it.isSubStep && targetStep.nextStepIds.contains(it.id)
        }
        // Chỉ coi là node chắn nếu sub node cùng phía và X <= X của main node
        val sameDirectionSubNodes = subNodesOfTarget.filter { subNode ->
            val subCell = stepToCellMap[subNode.id] ?: return@filter false
            val subGeo = graph.getCellGeometry(subCell) ?: return@filter false
            val isSubAbove = subGeo.y < 150
            isSourceAbove == isSubAbove && subGeo.x <= targetGeo.x
        }
        if (sameDirectionSubNodes.isNotEmpty()) {
            sameDirectionSubNodes.forEach { subNode -> nodesOnPath.add(subNode.id) }
            return nodesOnPath
        }
    }

    // Kiểm tra các node nằm giữa source và target theo X
    val minX = minOf(sourceGeo.x, targetGeo.x)
    val maxX = maxOf(sourceGeo.x, targetGeo.x)
    for (step in rule.steps) {
        if (step.id == sourceId || step.id == targetId) continue
        val cell = stepToCellMap[step.id] ?: continue
        val cellGeo = graph.getCellGeometry(cell) ?: continue
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


    /**
     * Apply special routing to edges to avoid crossing over nodes.
     */
    private fun createSpecialEdges() {
        LOG.info("Creating special orthogonal edges with route planning")
        
        // Lấy tất cả cạnh
        val edges = graph.getChildEdges(graph.defaultParent)
        if (edges == null || edges.isEmpty()) {
            LOG.warn("No edges found to process")
            return
        }
        
        val rule = currentRule ?: return
        
        // Xử lý từng cạnh
        for (edge in edges) {
            if (edge !is mxCell || !edge.isEdge) continue
            
            val source = edge.source as? mxCell
            val target = edge.target as? mxCell
            
            if (source == null || target == null) continue
            
            val sourceStep = cellToStepMap[source]
            val targetStep = cellToStepMap[target]
            
            if (sourceStep == null || targetStep == null) continue
            
            // Xử lý kết nối đặc biệt từ main node đến sub node
            if (!sourceStep.isSubStep && targetStep.isSubStep) {
                // Đây là kết nối từ main đến sub
                configureMainToSubEdge(edge, source, target)
                continue
            }
            
            // Xử lý kết nối đặc biệt từ sub node đến main node
            if (sourceStep.isSubStep && !targetStep.isSubStep) {
                // Đây là kết nối từ sub đến main
                configureSubToMainEdge(edge, source, target)
                continue
            }
            
           // Xử lý kết nối giữa các main node
           if (!sourceStep.isSubStep && !targetStep.isSubStep) {
               // Kết nối giữa các main node
               configureMainToMainEdge(edge, source, target)
               continue
           }

            if (sourceStep.isSubStep && targetStep.isSubStep) {
                configureSubToSubEdge(edge, source, target)
                continue
            }
        }
    }
    
    private fun configureSubToSubEdge(edge: mxCell, source: mxCell, target: mxCell) {
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }

    private fun configureMainToMainEdge(edge: mxCell, source: mxCell, target: mxCell) {
        // Kết nối từ main node đến main node - đơn giản là đường thẳng ngang
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
        LOG.info("Configured Main -> Main edge routing")
    }
    /**
     * Configure edge routing from main node to sub node
     */
    private fun configureMainToSubEdge(edge: mxCell, source: mxCell, target: mxCell) {
        val sourceGeo = graph.getCellGeometry(source)
        val targetGeo = graph.getCellGeometry(target)
        
        if (sourceGeo == null || targetGeo == null) return

        
        // Lấy ID của sub-step để log
        val sourceId = cellToStepMap[source]?.id ?: "unknown"
        val targetId = cellToStepMap[target]?.id ?: "unknown"
        LOG.info("Configuring edge from main node $sourceId to sub node $targetId")
        
        // Kiểm tra vị trí của target so với source
        val isAbove = targetGeo.y < sourceGeo.y

        val sourceCenterX = sourceGeo.x + sourceGeo.width / 2
        val targetCenterX = targetGeo.x + targetGeo.width / 2
        val xDiff = Math.abs(sourceCenterX - targetCenterX)
        // Tạo control points để tạo đường đi
        val edgeGeo = edge.geometry.clone() as mxGeometry
        edgeGeo.points = ArrayList<mxPoint>()
        if (xDiff < 20) {
            // Hai node cùng X, vẽ cạnh thẳng dọc từ giữa node này sang node kia
            // Không cần thêm control point, chỉ cần đảm bảo style là orthogonal
            edgeGeo.points = ArrayList() // Không thêm điểm điều khiển
            graph.model.setGeometry(edge, edgeGeo)
            graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
            LOG.info("Configured vertical edge for ${sourceId} <-> ${targetId}")
            return
        }
        // Check if target is to the right of source (new layout)
        val isToTheRight = targetGeo.x > sourceGeo.x + sourceGeo.width
        
        if (isToTheRight) {
            // Target is to the right of source (new layout)
            // Điểm kết nối từ phía bên phải của source
            val x1 = sourceGeo.x + sourceGeo.width
            val y1 = sourceGeo.y + sourceGeo.height / 2
            
            // Điểm kết nối đến phía bên trái của target
            val x3 = targetGeo.x
            val y3 = targetGeo.y + targetGeo.height / 2
            
            // Middle point for a smooth curve
            val midX = (x1 + x3) / 2
            
            if (Math.abs(y1 - y3) < 30) {
                // If they're at similar Y levels, just a simple orthogonal route
                LOG.info("Using simple orthogonal route for main->sub")
                        } else {
                // Need to add control points for better routing
                edgeGeo.points.add(mxPoint(midX, y1))
                edgeGeo.points.add(mxPoint(midX, y3))
                LOG.info("Added control points for main->sub vertical difference")
            }
        } else {
            // Legacy support for old layout (target on the left of source)
            // Điểm kết nối từ phía bên phải của source
            val x1 = sourceGeo.x + sourceGeo.width + 30
            val y1 = sourceGeo.y + sourceGeo.height / 2
            
            // Điểm kết nối đến phía bên trái của target
            val x3 = targetGeo.x
            val y3 = targetGeo.y + targetGeo.height / 2
            
            // Điểm trung gian
            val x2 = x1
            val y2 = y3
            
            // Thêm các điểm
            edgeGeo.points.add(mxPoint(x1, y1))
            edgeGeo.points.add(mxPoint(x2, y2))
            LOG.info("Using legacy routing for main->sub")
        }
        
        // Áp dụng geometry mới
        graph.model.setGeometry(edge, edgeGeo)
        
        // Áp dụng style
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
        
        LOG.info("Configured Main -> Sub edge routing")
    }
    
    /**
     * Configure edge routing from sub node to main node
     */
    private fun configureSubToMainEdge(edge: mxCell, source: mxCell, target: mxCell) {
        val sourceGeo = graph.getCellGeometry(source)
        val targetGeo = graph.getCellGeometry(target)
        if (sourceGeo == null || targetGeo == null) return
    
        val sourceId = cellToStepMap[source]?.id ?: "unknown"
        val targetId = cellToStepMap[target]?.id ?: "unknown"
        LOG.info("Configuring edge from sub node $sourceId to main node $targetId")
    
        val edgeGeo = edge.geometry.clone() as mxGeometry
        edgeGeo.points = ArrayList<mxPoint>()
    
        val sourceRightX = sourceGeo.x + sourceGeo.width
        val sourceMidY = sourceGeo.y + sourceGeo.height / 2
        val targetMidX = targetGeo.x + targetGeo.width / 2
        val targetTopY = targetGeo.y
        val targetBottomY = targetGeo.y + targetGeo.height
    
        // Nếu cùng X (gần thẳng đứng), vẽ cạnh thẳng dọc
        val xDiff = Math.abs((sourceGeo.x + sourceGeo.width / 2) - (targetGeo.x + targetGeo.width / 2))
        if (xDiff < 20) {
            edgeGeo.points = ArrayList()
            graph.model.setGeometry(edge, edgeGeo)
            graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
            LOG.info("Configured vertical edge for $sourceId <-> $targetId")
            return
        }
    
        // Kiểm tra có sub node cùng phía nằm giữa đường đi không
        val nodesOnPath = findNodesOnPathBetween(sourceGeo, targetGeo, sourceId, targetId, true)
        if (nodesOnPath.isNotEmpty()) {
            // Có node chắn trên đường đi, vẽ đường đi vòng
            val obstacleId = nodesOnPath.first()
            val obstacleCell = stepToCellMap[obstacleId] ?: return
            val obstacleGeo = graph.getCellGeometry(obstacleCell) ?: return
    
            val obstacleRightX = obstacleGeo.x + obstacleGeo.width + 40
            val isBelow = sourceGeo.y > targetGeo.y
    
            // Đi thẳng ra phải sub node
            edgeGeo.points.add(mxPoint(sourceRightX + 20, sourceMidY))
            // Đi lên/xuống đến ngoài node chắn
            val yObstacle = if (isBelow)
                obstacleGeo.y + obstacleGeo.height + 30
            else
                obstacleGeo.y - 30
            edgeGeo.points.add(mxPoint(sourceRightX + 20, yObstacle))
            // Đi ngang sang phải ngoài node chắn
            edgeGeo.points.add(mxPoint(obstacleRightX, yObstacle))
            // Đi lên/xuống đến ngang với main node
            val yTarget = if (isBelow) targetTopY else targetBottomY
            edgeGeo.points.add(mxPoint(obstacleRightX, yTarget))
            // Đi sang trái vào trung điểm main node
            edgeGeo.points.add(mxPoint(targetMidX+ 30, yTarget))
            LOG.info("Routed around sub node $obstacleId for $sourceId -> $targetId")
        } else {
            // Không có node chắn, vẽ thẳng ra phải, lên/xuống đến ngang main node, rồi rẽ vào
            val isAbove = sourceGeo.y + sourceGeo.height < targetGeo.y
            val isBelow = sourceGeo.y > targetGeo.y + targetGeo.height
            val yTarget = when {
                isAbove -> targetTopY - 10
                isBelow -> targetBottomY + 10
                else -> targetMidX // Nếu nằm ngang, rẽ thẳng vào giữa
            }
            val outX = maxOf(sourceRightX + 40, targetMidX)
            edgeGeo.points.add(mxPoint(outX, sourceMidY))
            if (isAbove || isBelow) {
                edgeGeo.points.add(mxPoint(outX, if (isAbove) targetTopY - 10 else targetBottomY + 10))
                edgeGeo.points.add(mxPoint(targetMidX, if (isAbove) targetTopY - 10 else targetBottomY + 10))
            } else {
                // Nếu nằm ngang, chỉ cần 1 control point
                edgeGeo.points.add(mxPoint(targetMidX, sourceMidY))
            }
            LOG.info("Simple right then vertical then left routing for $sourceId -> $targetId")
        }
    
        // Áp dụng geometry mới và style
        graph.model.setGeometry(edge, edgeGeo)
        graph.model.setStyle(edge, "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=2;")
    }
    
    /**
     * Find all bidirectional connection pairs (A→B and B→A).
     */
    private fun findAllBidirectionalPairs(): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        
        // Check all nodes
        currentRule?.steps?.forEach { stepA ->
            stepA.nextStepIds.forEach { stepBId ->
                // Check if B points back to A
                val stepB = currentRule?.steps?.find { it.id == stepBId }
                if (stepB?.nextStepIds?.contains(stepA.id) == true) {
                    // Add to list if not already there (avoid duplicates)
                    val orderedPair = if (stepA.id < stepBId) 
                                        Pair(stepA.id, stepBId) 
                                     else 
                                        Pair(stepBId, stepA.id)
                                        
                    if (!pairs.contains(orderedPair)) {
                        pairs.add(orderedPair)
                        LOG.info("Found bidirectional connection: ${stepA.id} <-> $stepBId")
                    }
                }
            }
        }
        
        return pairs
    }

} 
