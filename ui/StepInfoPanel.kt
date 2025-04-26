package com.example.rulemaker.ui

import com.example.rulemaker.model.LayoutMatcher
import com.example.rulemaker.model.Rule
import com.example.rulemaker.model.Step
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.table.AbstractTableModel

/**
 * Panel hiển thị và chỉnh sửa thông tin Step (Step Info), gồm form nhập liệu, bảng layoutMatchers, navigation.
 */
class StepInfoPanel(
    private val editorPanel: EditorPanel,
    private val onStepUpdated: (Step) -> Unit
) : JPanel(BorderLayout()) {

    private val layoutMatchersModel = LayoutMatchersTableModel()
    private val layoutMatchersTable = JTable(layoutMatchersModel)
    private var currentStep: Step? = null

    init {
        background = Color(60, 63, 65)
        add(createStepInfoContent(), BorderLayout.CENTER)
    }

    fun setRule(rule: Rule) {
        // Có thể dùng để reset hoặc cập nhật khi đổi rule
    }

    fun setStep(step: Step) {
        currentStep = step
        editorPanel.setStep(step)
        updateLayoutMatchersTable(step)
    }

    /**
     * Tạo UI Step Info: form nhập liệu, bảng layoutMatchers, navigation.
     */
    private fun createStepInfoContent(): JPanel {
        // 1. Form nhập liệu
        val idField = editorPanel.getIdField()
        val screenIdField = editorPanel.getScreenIdField()
        val guideContentArea = JBScrollPane(editorPanel.getGuideContentArea()).apply {
            preferredSize = Dimension(300, 60)
        }
        val nextStepsField = editorPanel.getNextStepsField().apply {
            preferredSize = Dimension(300, 32)
        }
        val isSubStepCheckbox = editorPanel.getIsSubStepCheckbox()

        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Step ID:", idField)
            .addLabeledComponent("Screen ID:", screenIdField)
            .addLabeledComponent("Guide Content:", guideContentArea)
            .addLabeledComponent("Next Step IDs:", nextStepsField)
            .panel.apply {
                border = JBUI.Borders.empty(10, 10, 0, 10)
            }

        val isSubStepPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = Color(60, 63, 65)
            add(isSubStepCheckbox)
        }

        // 2. Bảng layoutMatchers và nút cộng/trừ
        layoutMatchersTable.background = Color(60, 63, 65)
        layoutMatchersTable.foreground = Color.WHITE
        layoutMatchersTable.selectionBackground = Color(90, 90, 120)
        layoutMatchersTable.selectionForeground = Color.WHITE
        layoutMatchersTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
        layoutMatchersTable.columnModel.getColumn(0).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(1).preferredWidth = 120
        layoutMatchersTable.columnModel.getColumn(2).preferredWidth = 110
        layoutMatchersTable.columnModel.getColumn(3).preferredWidth = 120
        layoutMatchersTable.columnModel.getColumn(4).preferredWidth = 120

        // Cell editors cho các cột (tùy nhu cầu, có thể sửa lại cho phù hợp)
        layoutMatchersTable.columnModel.getColumn(0).cellEditor = DefaultCellEditor(JComboBox(arrayOf("text", "content_description", "class_name")))
        layoutMatchersTable.columnModel.getColumn(2).cellEditor = DefaultCellEditor(JComboBox(arrayOf("equals", "contains")))
        layoutMatchersTable.columnModel.getColumn(3).cellEditor = DefaultCellEditor(JComboBox(arrayOf("none", "click", "scroll_down", "scroll_right", "scroll_left_right", "invisible")))
        layoutMatchersTable.columnModel.getColumn(4).cellEditor = DefaultCellEditor(JComboBox(arrayOf("layout_match", "tts_end")))

        val matchersScrollPane = JScrollPane(layoutMatchersTable).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(750, 240)
        }

        val matcherBtnPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Color(60, 63, 65)
            val addMatcherBtn = JButton(AllIcons.General.Add).apply {
                toolTipText = "Thêm dòng Matcher"
                preferredSize = Dimension(28, 28)
                maximumSize = Dimension(28, 28)
                addActionListener { layoutMatchersModel.addEmptyRow() }
            }
            val removeMatcherBtn = JButton(AllIcons.General.Remove).apply {
                toolTipText = "Xóa dòng Matcher"
                preferredSize = Dimension(28, 28)
                maximumSize = Dimension(28, 28)
                addActionListener {
                    val row = layoutMatchersTable.selectedRow
                    if (row >= 0) layoutMatchersModel.removeRow(row)
                }
            }
            add(addMatcherBtn)
            add(removeMatcherBtn)
        }

        val layoutMatchersPanel = JPanel(BorderLayout()).apply {
            background = Color(60, 63, 65)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Layout Matchers",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )
            add(matcherBtnPanel, BorderLayout.NORTH)
            add(matchersScrollPane, BorderLayout.CENTER)
            preferredSize = Dimension(750, 300)
        }

        // 3. Gộp lại vào mainPanel
        val mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(60, 63, 65)
            maximumSize = Dimension(800, Int.MAX_VALUE)
            preferredSize = Dimension(800, 500)
            add(Box.createVerticalStrut(10))
            add(formPanel)
            add(Box.createVerticalStrut(10))
            add(layoutMatchersPanel)
            add(Box.createVerticalStrut(10))
            add(isSubStepPanel)
            add(Box.createVerticalStrut(10))
        }

        // Navigation buttons
        val navPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            background = Color(60, 63, 65)
            border = JBUI.Borders.empty(10)
            val prevButton = JButton("← Previous Step").apply {
                addActionListener { editorPanel.onPreviousStep() }
            }
            val saveButton = JButton("Save Changes").apply {
                addActionListener {
                    saveChanges()
                }
            }
            val nextButton = JButton("Next Step →").apply {
                addActionListener { editorPanel.onNextStep() }
            }
            add(prevButton)
            add(saveButton)
            add(nextButton)
        }

        val customStepPanel = JPanel(BorderLayout()).apply {
            background = Color(60, 63, 65)
            add(mainPanel, BorderLayout.CENTER)
            add(navPanel, BorderLayout.SOUTH)
        }

        val panel = JPanel(BorderLayout())
        panel.background = Color(60, 63, 65)
        panel.add(customStepPanel, BorderLayout.CENTER)
        return panel
    }

    /**
     * Cập nhật bảng layoutMatchers từ step.
     */
    fun updateLayoutMatchersTable(step: Step) {
        layoutMatchersModel.data.clear()
        step.layoutMatchers.forEach { matcher ->
            layoutMatchersModel.data.add(arrayOf(
                matcher.matchTarget,
                matcher.matchOperand,
                matcher.matchCriteria,
                matcher.highlightType,
                matcher.transitionCondition
            ))
        }
        layoutMatchersModel.fireTableDataChanged()
    }

    /**
     * Lấy dữ liệu từ bảng, cập nhật vào step hiện tại.
     */
    fun updateStepWithLayoutMatchersData(currentStep: Step) {
        val matchers = mutableListOf<LayoutMatcher>()
        for (row in 0 until layoutMatchersModel.rowCount) {
            val matchTarget = layoutMatchersModel.getValueAt(row, 0)?.toString() ?: ""
            val matchOperand = layoutMatchersModel.getValueAt(row, 1)?.toString() ?: ""
            val matchCriteria = layoutMatchersModel.getValueAt(row, 2)?.toString()
            val highlightType = layoutMatchersModel.getValueAt(row, 3)?.toString()
            val transitionCondition = layoutMatchersModel.getValueAt(row, 4)?.toString()
            matchers.add(
                LayoutMatcher(
                    matchTarget,
                    matchOperand,
                    matchCriteria,
                    highlightType,
                    transitionCondition
                )
            )
        }
        currentStep.layoutMatchers.clear()
        currentStep.layoutMatchers.addAll(matchers)
        onStepUpdated(currentStep)
    }

    /**
     * Lưu thay đổi khi bấm Save Changes.
     */
    private fun saveChanges() {
        currentStep?.let {
            updateStepWithLayoutMatchersData(it)
        }
    }

    /**
     * TableModel cho bảng layoutMatchers.
     */
    class LayoutMatchersTableModel : AbstractTableModel() {
        val columns = arrayOf("matchTarget", "matchOperand", "matchCriteria", "highlightType", "transitionCondition")
        val data = mutableListOf<Array<String?>>()

        override fun getRowCount() = data.size
        override fun getColumnCount() = columns.size
        override fun getColumnName(col: Int) = columns[col]
        override fun getValueAt(row: Int, col: Int) = data[row][col]
        override fun isCellEditable(row: Int, col: Int) = true
        override fun setValueAt(value: Any?, row: Int, col: Int) {
            data[row][col] = value as? String
            fireTableCellUpdated(row, col)
        }
        fun addEmptyRow() {
            data.add(arrayOfNulls<String>(columns.size))
            fireTableRowsInserted(data.size - 1, data.size - 1)
        }
        fun removeRow(row: Int) {
            data.removeAt(row)
            fireTableRowsDeleted(row, row)
        }
    }
}
