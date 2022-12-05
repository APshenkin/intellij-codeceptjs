package com.apshenkin.codeceptjs.utils

import com.apshenkin.codeceptjs.structureView.CodeceptjsStructureViewElement
import com.intellij.javascript.testFramework.util.JsPsiUtils
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.ecma6.ES6TaggedTemplateExpression
import com.intellij.lang.javascript.psi.resolve.JSReferenceUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import java.util.*

class Utils {
    companion object {
        val FEATURE_NAME = "Feature"
        val XFEATURE_NAME = "xFeature"
        val SCENARIO_NAME = "Scenario"
        val XSCENARIO_NAME = "xScenario"
        val DATA_NAME = "Data"
        private val SUITE_MODIFIERS = Arrays.asList("skip", "only")

        fun isCodeceptjsFile(jsFile: JSFile): Boolean {
            val contents = jsFile.viewProvider.contents
            return JsPsiUtils.mightContainGlobalCall(contents, FEATURE_NAME, true) ||
                    JsPsiUtils.mightContainGlobalCall(contents, XFEATURE_NAME, true)
        }

        private fun getMethodReferenceExpression(callExpression: JSCallExpression): JSReferenceExpression? {
            val methodExpr = callExpression.methodExpression
            var referenceExpr = ObjectUtils.tryCast(methodExpr, JSReferenceExpression::class.java)
            return if (referenceExpr != null) {
                referenceExpr
            } else {
                val innerCallExpr = ObjectUtils.tryCast(methodExpr, JSCallExpression::class.java)
                if (innerCallExpr != null) {
                    referenceExpr = ObjectUtils.tryCast(innerCallExpr.methodExpression, JSReferenceExpression::class.java)
                    if (referenceExpr != null && "each" == referenceExpr.referenceName) {
                        return referenceExpr
                    }
                }
                val templateExpr = ObjectUtils.tryCast(methodExpr, ES6TaggedTemplateExpression::class.java)
                if (templateExpr != null) ObjectUtils.tryCast(templateExpr.tag, JSReferenceExpression::class.java) else null
            }
        }

        private fun getProperCallExpr(callExpression: JSCallExpression): JSCallExpression {
            val methodExpr = ObjectUtils.tryCast(callExpression.methodExpression, JSReferenceExpression::class.java)
            if (methodExpr != null) {
                val qualifier = methodExpr.qualifier
                if (qualifier is JSCallExpression && "timeout" == methodExpr.referenceName) {
                    return qualifier
                }
            }
            return callExpression
        }

        fun parseFeatureExpr(callExpression: JSCallExpression, jsFile: JSFile): Triple<JSCallExpression, JSExpression, String>? {
            val properCallExpression = getProperCallExpr(callExpression)
            val expr = getMethodReferenceExpression(properCallExpression)
            val components = if (expr != null) JSReferenceUtil.getReferenceComponents(expr, 3) else mutableListOf<String>()
            if (components.size != 0) {
                val arguments = properCallExpression.arguments
                val argc = arguments.size
                val methodName = components[0]
                val testNameExpression: JSExpression
                val name: String?
                if (argc == 1 && isSuiteMethodName(methodName, jsFile)) {
                    if (isSuiteComponents(components)) {
                        testNameExpression = Objects.requireNonNull(arguments[0]) as JSExpression
                        name = JsPsiUtils.extractStringValue(testNameExpression)
                        if (name != null) {

                            return Triple(properCallExpression, testNameExpression, name)
                        }
                    }
                }
            }

            return null
        }

        fun parseScenarioExpr(callExpression: JSCallExpression): Triple<JSCallExpression, JSExpression, String>? {
            val properCallExpression = getProperCallExpr(callExpression)
            val expr = getMethodReferenceExpression(properCallExpression)
            val components = if (expr != null) JSReferenceUtil.getReferenceComponents(expr, 3) else mutableListOf<String>()
            val each = components.size > 1 && "each" == ContainerUtil.getLastItem(components)
            if (each) {
                components.removeAt(components.size - 1)
            }
            if (components.size != 0) {
                val arguments = properCallExpression.arguments
                val argc = arguments.size
                val methodName = components[0]
                val testNameExpression: JSExpression
                val name: String?
                if (argc == 2 || argc == 3 || argc == 1 && hasTodo(components) && isSpecMethodName(methodName)) {
                    testNameExpression = Objects.requireNonNull(arguments[0]) as JSExpression
                    name = JsPsiUtils.extractStringValue(testNameExpression)
                    if (name != null) {
                        return Triple(properCallExpression, testNameExpression, name)
                    }
                }
            }

            return null
        }

        private fun isSpecMethodName(methodName: String): Boolean {
            return methodName == SCENARIO_NAME || methodName == XSCENARIO_NAME
        }

        private fun isSuiteMethodName(methodName: String, psiFile: PsiFile): Boolean {
            return if ("parallel" == methodName) {
                val contents = psiFile.viewProvider.contents
                StringUtil.contains(contents, (StringUtil.QUOTER.apply("mocha.parallel") as CharSequence)) || StringUtil.contains(contents, (StringUtil.SINGLE_QUOTER.apply("mocha.parallel") as CharSequence))
            } else {
                methodName == FEATURE_NAME || methodName == XFEATURE_NAME
            }
        }

        private fun isSuiteComponents(components: List<String>): Boolean {
            return if (components.size == 1) {
                true
            } else {
                val describePropName = components[1]
                if (components.size != 2) {
                    if (components.size == 3) SUITE_MODIFIERS.contains(describePropName) else false
                } else {
                    SUITE_MODIFIERS.contains(describePropName) || "posix" == describePropName
                }
            }
        }

        private fun hasTodo(components: List<String>): Boolean {
            return components.contains("todo")
        }
    }
}

class JSCallWithTestName(val name: String, val jsCallExpression: JSCallExpression) {
    val children: ArrayList<CodeceptjsStructureViewElement> = arrayListOf()

    fun addChild(element: CodeceptjsStructureViewElement) {
        children.add(element)
    }
}
