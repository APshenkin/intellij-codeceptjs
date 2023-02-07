package com.apshenkin.codeceptjs.structure

import com.apshenkin.codeceptjs.utils.CodeceptjsSpecStructure
import com.apshenkin.codeceptjs.utils.Utils
import com.intellij.javascript.testFramework.AbstractTestFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructure
import com.intellij.javascript.testFramework.jasmine.JasmineSuiteStructure
import com.intellij.javascript.testFramework.util.JsPsiUtils
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSStatement

class CodeceptjsFileStructureBuilder : AbstractTestFileStructureBuilder<JasmineFileStructure>() {

    companion object {
        private val INSTANCE = CodeceptjsFileStructureBuilder()
        fun getInstance(): CodeceptjsFileStructureBuilder {
            return INSTANCE
        }
    }


    override fun buildTestFileStructure(jsFile: JSFile): JasmineFileStructure {
        return Builder(jsFile).build()
    }

    private class Builder constructor(jsFile: JSFile?) {
        private val myFileStructure: JasmineFileStructure

        init {
            myFileStructure = JasmineFileStructure(jsFile!!)
        }

        fun build(): JasmineFileStructure {
            if (Utils.isCodeceptjsFile(myFileStructure.jsFile)) {
                val statements = JsPsiUtils.listStatementsInExecutionOrder(myFileStructure.jsFile).iterator()
                var suiteStructure: JasmineSuiteStructure? = null;
                while (statements.hasNext()) {
                    val statement = statements.next() as JSStatement
                    val jsCallExpression = JsPsiUtils.toCallExpressionFromStatement(statement)
                    if (jsCallExpression != null) {
                        val suite = handleFeatureExpr(jsCallExpression)
                        if (suite != null) {
                            suiteStructure = suite
                        } else {
                            handleScenarioExpr(suiteStructure, jsCallExpression)
                        }
                    }
                }
            }
            // Because post process is not accessible, we will invoke it via reflection
            val postProcess = myFileStructure.javaClass.getDeclaredMethod("postProcess")
            postProcess.isAccessible = true
            postProcess.invoke(myFileStructure)
            return myFileStructure
        }

        private fun handleFeatureExpr(callExpression: JSCallExpression): JasmineSuiteStructure? {
            val feature = Utils.parseFeatureExpr(callExpression, myFileStructure.jsFile)

            if (feature != null) {
                val suiteStructure = JasmineSuiteStructure(feature.first, null, feature.second, feature.third, null)

                myFileStructure.addSuiteStructure(suiteStructure)

                return suiteStructure
            }

            return null;
        }

        private fun handleScenarioExpr(parentSuiteStructure: JasmineSuiteStructure?, callExpression: JSCallExpression) {
            val scenario = Utils.parseScenarioExpr(callExpression)
            if (scenario != null) {
                val specStructure = CodeceptjsSpecStructure(scenario, parentSuiteStructure)
                if (parentSuiteStructure != null) {
                    parentSuiteStructure.addChild(specStructure)
                }
                myFileStructure.addSpecStructure(specStructure)
            }
        }
    }
}
