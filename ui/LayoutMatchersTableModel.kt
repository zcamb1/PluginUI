package com.example.rulemaker.ui

import javax.swing.table.AbstractTableModel

/**
 * TableModel cho bảng layoutMatchers trong Step Info.
 * Mỗi dòng là một LayoutMatcher, mỗi cột là một trường của matcher.
 */
class LayoutMatchersTableModel : AbstractTableModel() {
    val columns = arrayOf(
        "matchTarget",
        "matchOperand",
        "matchCriteria",
        "highlightType",
        "transitionCondition"
    )
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
