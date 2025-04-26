package com.example.rulemaker.ui

import java.awt.*
import javax.swing.*
import com.intellij.ui.components.JBLabel
import javax.swing.border.TitledBorder

/**
 * Panel hiển thị mirror screen (giả lập hoặc preview màn hình app).
 * Nếu chưa có kết nối, hiển thị thông báo mặc định.
 * Nếu muốn mở rộng, có thể nhúng screenshot, video stream, hoặc remote view.
 */
class MirrorPanel : JPanel(BorderLayout()) {

    private val statusLabel = JBLabel("No screen connected", SwingConstants.CENTER).apply {
        foreground = Color.LIGHT_GRAY
        font = font.deriveFont(Font.ITALIC, font.size2D + 2)
    }

    init {
        background = Color(45, 45, 45)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Mirror screen",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        preferredSize = Dimension(290, 300)
        add(statusLabel, BorderLayout.CENTER)
    }

    /**
     * Hiển thị trạng thái kết nối hoặc nội dung preview màn hình.
     * Có thể mở rộng để show hình ảnh, video, v.v.
     */
    fun setStatus(text: String) {
        statusLabel.text = text
    }

    // Nếu muốn mở rộng, có thể thêm hàm showImage, showVideo, ...
}
