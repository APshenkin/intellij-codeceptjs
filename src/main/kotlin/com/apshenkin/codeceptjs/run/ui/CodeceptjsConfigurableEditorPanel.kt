package com.apshenkin.codeceptjs.run.ui

import com.apshenkin.codeceptjs.run.CodeceptjsRunConfig
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.execution.ui.MacroComboBoxWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.PanelWithAnchor
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent


class CodeceptjsConfigurableEditorPanel(private val myProject: Project) : SettingsEditor<CodeceptjsRunConfig>(), PanelWithAnchor {

    private lateinit var myTabs: JTabbedPane

    private lateinit var myCommonParams: CommonProgramParametersPanel
    private lateinit var myWholePanel: JPanel
    private var anchor: JComponent? = null
    private lateinit var myNodePackageField: NodePackageField
    private lateinit var myNodeJsInterpreterField: NodeJsInterpreterField
    private lateinit var myCodeceptjsPackageField: NodePackageField
    private val kindButtons: Array<JRadioButton> = Array(CodeceptjsRunConfig.TestKind.values().size) { JRadioButton(CodeceptjsRunConfig.TestKind.values()[it].myName) }
    private lateinit var kindPanel: JPanel
    private lateinit var kindSettingsPanel: JPanel

//    private lateinit var debugCheckbox: JCheckBox
    private lateinit var mochaMultiCheckbox: JCheckBox

    private lateinit var myCodeceptjsPckgLabel: JLabel
    private lateinit var myNodeIntLabel: JLabel

    private val directory: LabeledComponent<MacroComboBoxWithBrowseButton>
    private val myRadioButtonMap: MutableMap<CodeceptjsRunConfig.TestKind, JRadioButton> = EnumMap(CodeceptjsRunConfig.TestKind::class.java)

    private val myTestKindViewMap: MutableMap<CodeceptjsRunConfig.TestKind, CodeceptjsTestKindView> = EnumMap(CodeceptjsRunConfig.TestKind::class.java)

    private val myLongestLabelWidth: Int

    private val debugArg = "--debug"
    private val debugReg = "(?:^|\\s+)${debugArg}(?:$|\\s+)".toRegex()


    init {

        val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fileChooserDescriptor.title = ExecutionBundle.message("select.working.directory.message")
        directory = LabeledComponent.create(MacroComboBoxWithBrowseButton(fileChooserDescriptor, myProject), "Directory")

        val model = DefaultComboBoxModel<String>()
        model.addElement("All")

        val changeLists = ChangeListManager.getInstance(myProject).changeLists
        for (changeList in changeLists) {
            model.addElement(changeList.name)
        }
        kindPanel.layout = BorderLayout()
        kindPanel.add(createTestKindRadioButtonPanel())

        this.myLongestLabelWidth = JLabel("Environment variables:").preferredSize.width

//        debugCheckbox.addActionListener { applyFromCheckboxes() }
        myCommonParams.programParametersComponent.component.editorField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                resetCheckboxes()
            }
        })

        (myNodePackageField.editorComponent as? JTextComponent)?.document?.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val selected = (myNodePackageField.editorComponent as? JTextComponent)?.text?.isBlank() == false
                val tip = if (selected) "Clear 'Package manager' field to enable this one" else ""
                myNodeJsInterpreterField.isEnabled = !selected
                myCodeceptjsPackageField.isEnabled = !selected
                myNodeJsInterpreterField.childComponent.toolTipText = tip
                myCodeceptjsPackageField.editorComponent.toolTipText = tip
                myCodeceptjsPckgLabel.toolTipText = tip
                myNodeIntLabel.toolTipText = tip
            }
        })
    }

    private fun applyFromCheckboxes() {
        val params = StringBuilder(myCommonParams.programParametersComponent.component.text)
//        val debug = processCheckbox(params, debugReg, debugArg, debugCheckbox.isSelected)
//        if (debug) {
//            myCommonParams.programParametersComponent.component.text = params.toString()
//        }
    }

    private fun resetCheckboxes() {
        val text = myCommonParams.programParametersComponent.component.text
//        debugCheckbox.isSelected = debugReg.containsMatchIn(text)
    }

    private fun processCheckbox(params: StringBuilder, regex: Regex, tag: String, value: Boolean): Boolean {
        val present = regex.containsMatchIn(params)
        if (present != value) {
            val indexOf = params.indexOf(tag)
            if (present)
                params.replace(indexOf, indexOf + tag.length + 1, "")
            else
                params.insert(0, "$tag ")
            return true
        }
        return false
    }

    private fun createTestKindRadioButtonPanel(): JPanel {
        val testKindPanel = JPanel(FlowLayout(1, JBUIScale.scale(30), 0))
        testKindPanel.border = JBUI.Borders.emptyLeft(10)
        val buttonGroup = ButtonGroup()
        CodeceptjsRunConfig.TestKind.values().forEachIndexed { index, testKind ->
            val radioButton = JRadioButton(UIUtil.removeMnemonic(testKind.myName))
            val mnemonicInd = UIUtil.getDisplayMnemonicIndex(testKind.myName)
            if (mnemonicInd != -1) {
                radioButton.setMnemonic(testKind.myName[mnemonicInd + 1])
                radioButton.displayedMnemonicIndex = mnemonicInd
            }
            radioButton.addActionListener { this.setTestKind(testKind) }
            this.myRadioButtonMap[testKind] = radioButton
            testKindPanel.add(radioButton)
            buttonGroup.add(radioButton)
        }
        return testKindPanel
    }

    private fun setTestKind(testKind: CodeceptjsRunConfig.TestKind) {
        val selectedTestKind = this.getTestKind()
        if (selectedTestKind !== testKind) {
            myRadioButtonMap[testKind]?.isSelected = true
        }

        val view = getTestKindView(testKind)
        setCenterBorderLayoutComponent(this.kindSettingsPanel, view.getComponent())
    }

    private fun getTestKind() = myRadioButtonMap.entries.firstOrNull { it.value.isSelected }?.key

    private fun getTestKindView(testKind: CodeceptjsRunConfig.TestKind): CodeceptjsTestKindView {
        var view = this.myTestKindViewMap[testKind]
        if (view == null) {
            view = testKind.createView(myProject)
            this.myTestKindViewMap[testKind] = view
            val component = view.getComponent()
            if (component.layout is GridBagLayout) {
                component.add(Box.createHorizontalStrut(this.myLongestLabelWidth), GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 13, 0, JBUI.insetsRight(10), 0, 0))
            }
        }
        return view
    }


    private fun setCenterBorderLayoutComponent(panel: JPanel, child: Component) {
        val prevChild = (panel.layout as BorderLayout).getLayoutComponent("Center")
        if (prevChild != null) {
            panel.remove(prevChild)
        }
        panel.add(child, "Center")
        panel.revalidate()
        panel.repaint()
    }

    public override fun applyEditorTo(configuration: CodeceptjsRunConfig) {
        myCommonParams.applyTo(configuration)
        val data = configuration.getPersistentData()
        data.mochaMultiReporter = mochaMultiCheckbox.isSelected
        data.nodeJsRef = myNodeJsInterpreterField.interpreterRef.referenceName
        data.npmRef = myNodePackageField.selectedRef.referenceName
        data.codeceptjsPackageRef = myCodeceptjsPackageField.selected.systemIndependentPath
        data.kind = getTestKind() ?: CodeceptjsRunConfig.TestKind.SPEC
        val view = this.getTestKindView(data.kind)
        view.applyTo(data)
    }

    public override fun resetEditorFrom(configuration: CodeceptjsRunConfig) {
        myCommonParams.reset(configuration)
        val data = configuration.getPersistentData()

        myNodeJsInterpreterField.interpreterRef = NodeJsInterpreterRef.create(data.nodeJsRef)
        myCodeceptjsPackageField.selected = configuration.getCodeceptjsPackage()
        setTestKind(data.kind)
        val view = this.getTestKindView(data.kind)
        view.resetFrom(data)
        mochaMultiCheckbox.isSelected = data.mochaMultiReporter
        data.npmRef?.let { myNodePackageField.selectedRef = NodePackageRef.create(it) }
        resetCheckboxes()
    }


    private fun createUIComponents() {
        myNodeJsInterpreterField = NodeJsInterpreterField(myProject, false)
        myNodePackageField = NpmUtil.createPackageManagerPackageField(myNodeJsInterpreterField, false)
        myCodeceptjsPackageField = NodePackageField(myNodeJsInterpreterField, CodeceptjsRunConfig.codeceptjsPackageDescriptor, null)
        myCommonParams = CodeceptjsProgramParametersPanel()
        (myCommonParams as CodeceptjsProgramParametersPanel).workingDir.label.text = "Codeceptjs project base:"
    }

    override fun getAnchor(): JComponent? {
        return anchor
    }

    override fun setAnchor(anchor: JComponent?) {
        this.anchor = anchor
    }

    public override fun createEditor(): JComponent {
        return myWholePanel
    }

}

class CodeceptjsProgramParametersPanel : CommonProgramParametersPanel(true) {
    val workingDir get() = myWorkingDirectoryComponent
}

