package com.apshenkin.codeceptjs.structureView

import com.apshenkin.codeceptjs.utils.JSCallWithTestName
import com.apshenkin.codeceptjs.utils.Utils
import com.intellij.ide.structureView.StructureViewExtension
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.javascript.testFramework.util.JsPsiUtils
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSStatement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

/**
 * Extends the structure view, so we can include all
 * the codeceptjs tests in it.
 */
class CodeceptjsStructureViewExtension : StructureViewExtension {
    override fun getType(): Class<out PsiElement> {
        return JSFile::class.java
    }

    override fun getChildren(parent: PsiElement?): Array<StructureViewTreeElement> {
        if (parent !is JSFile) {
            return arrayOf()
        }

        if (!Utils.isCodeceptjsFile(parent)) {
            return arrayOf()
        }

        return parent.getCodeceptjsTests()
                .map { CodeceptjsStructureViewElement(it) }
                .toTypedArray()
    }

    override fun filterChildren(baseChildren: MutableCollection<StructureViewTreeElement>, extensionChildren: MutableList<StructureViewTreeElement>) {
        if (extensionChildren.size > 0) {
            baseChildren.removeIf {
                it !is CodeceptjsStructureViewElement
            }
        }
    }

    override fun getCurrentEditorElement(editor: Editor?, parent: PsiElement?): Any? {
        return null
    }
}

private fun JSFile.getCodeceptjsTests(): ArrayList<JSCallWithTestName> {
    val statements = JsPsiUtils.listStatementsInExecutionOrder(this).iterator()

    val tests = arrayListOf<JSCallWithTestName>()
    var suiteStructure: JSCallWithTestName? = null
    while (statements.hasNext()) {
        val statement = statements.next() as JSStatement
        val jsCallExpression = JsPsiUtils.toCallExpressionFromStatement(statement)
        if (jsCallExpression != null) {
            val suite = Utils.parseFeatureExpr(jsCallExpression, this)
            if (suite != null) {
                suiteStructure = JSCallWithTestName(suite.third, suite.first)
                tests.add(suiteStructure)
            } else {
                val scenario = Utils.parseScenarioExpr(jsCallExpression)

                if (scenario != null && suiteStructure != null) {
                    suiteStructure.addChild(CodeceptjsStructureViewElement(JSCallWithTestName(scenario.name, scenario.callExpression)))
                }
            }
        }
    }

    return tests
}
