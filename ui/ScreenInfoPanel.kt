package com.example.rulemaker.ui

import java.awt.*
import javax.swing.*
import com.intellij.ui.components.JBTextField
import javax.swing.border.TitledBorder

/**
 * Panel hiển thị và chỉnh sửa thông tin Screen/package.
 */
class ScreenInfoPanel : JPanel(BorderLayout()) {

    private val screenIdField = JBTextField().apply {
        isEditable = true
        preferredSize = Dimension(160, 32)
    }
    private val packageNameField = JBTextField().apply {
        isEditable = true
        preferredSize = Dimension(160, 32)
    }

    private val applyButton = JButton("Apply").apply {
        preferredSize = Dimension(100, 32)
    }
    private val captureButton = JButton("Capture")
    private val recordButton = JButton("Record")

    init {
        background = Color(60, 63, 65)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Screen information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )
        preferredSize = Dimension(290, 300)

        // Form group
        val formPanel = JPanel()
        formPanel.layout = GroupLayout(formPanel)
        formPanel.background = background

        val screenIdLabel = JLabel("screen id:").apply { foreground = Color.WHITE }
        val packageNameLabel = JLabel("package name:").apply { foreground = Color.WHITE }

        val layout = formPanel.layout as GroupLayout
        layout.autoCreateGaps = true
        layout.autoCreateContainerGaps = true

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(
                    layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addComponent(screenIdLabel)
                            .addComponent(packageNameLabel)
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(screenIdField)
                            .addComponent(packageNameField)
                        )
                )
                .addComponent(applyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        )
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(screenIdLabel)
                    .addComponent(screenIdField)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(packageNameLabel)
                    .addComponent(packageNameField)
                )
                .addGap(12)
                .addComponent(applyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        )

        // Control buttons (Capture, Record) căn giữa dưới cùng
        val controlPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10)).apply {
            background = Color(60, 63, 65)
            add(captureButton)
            add(recordButton)
        }

        // Dùng GridBagLayout để căn giữa form và controlPanel
        val columnPanel = JPanel(GridBagLayout()).apply {
            background = Color(60, 63, 65)
            val gbc = GridBagConstraints()
            gbc.gridx = 0; gbc.gridy = 0
            gbc.weightx = 1.0; gbc.weighty = 1.0
            gbc.anchor = GridBagConstraints.CENTER
            gbc.fill = GridBagConstraints.NONE
            add(formPanel, gbc)
            val gbc2 = GridBagConstraints()
            gbc2.gridx = 0; gbc2.gridy = 1
            gbc2.weightx = 1.0; gbc2.weighty = 0.0
            gbc2.anchor = GridBagConstraints.SOUTH
            gbc2.fill = GridBagConstraints.NONE
            add(controlPanel, gbc2)
            val gbc3 = GridBagConstraints()
            gbc3.gridx = 0; gbc3.gridy = 2
            gbc3.weightx = 1.0; gbc3.weighty = 0.0
            gbc3.anchor = GridBagConstraints.SOUTH
            gbc3.fill = GridBagConstraints.NONE
            add(Box.createVerticalStrut(30), gbc3)
            preferredSize = Dimension(290, 300)
        }

        add(columnPanel, BorderLayout.CENTER)
    }

    // Getter/setter cho các trường nếu cần kết nối với logic ngoài
    fun getScreenId(): String = screenIdField.text
    fun setScreenId(value: String) { screenIdField.text = value }
    fun getPackageName(): String = packageNameField.text
    fun setPackageName(value: String) { packageNameField.text = value }

    fun setOnApply(action: () -> Unit) {
        applyButton.addActionListener { action() }
    }
    fun setOnCapture(action: () -> Unit) {
        captureButton.addActionListener { action() }
    }
    fun setOnRecord(action: () -> Unit) {
        recordButton.addActionListener { action() }
    }
}
