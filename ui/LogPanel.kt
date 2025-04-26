package com.example.rulemaker.ui

import java.awt.*
import javax.swing.*
import com.intellij.ui.components.JBScrollPane
import javax.swing.border.TitledBorder

/**
 * Panel hiển thị log message cho Rule Maker.
 */
class LogPanel : JPanel(BorderLayout()) {

    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = Color(45, 45, 45)
        foreground = Color.WHITE
        font = Font("Consolas", Font.PLAIN, 13)
        text = "Log messages will appear here...\n"
    }

    init {
        background = Color(60, 63, 65)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Log",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        preferredSize = Dimension(300, 250)
        add(JBScrollPane(logArea), BorderLayout.CENTER)
    }

    /**
     * Ghi thêm một dòng log mới.
     */
    fun log(message: String) {
        logArea.append(message.trimEnd() + "\n")
        logArea.caretPosition = logArea.document.length
    }

    /**
     * Xóa toàn bộ log.
     */
    fun clear() {
        logArea.text = ""
    }
}
