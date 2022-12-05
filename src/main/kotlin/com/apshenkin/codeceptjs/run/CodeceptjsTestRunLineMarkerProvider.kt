package com.apshenkin.codeceptjs.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.intellij.util.ObjectUtils


class CodeceptjsTestRunLineMarkerProvider : RunLineMarkerContributor() {
    private val SCENARIO_WORD = "Scenario"
    private val FEATURE_WORD = "Feature"

    private val TOOLTIP_PROVIDER = Function<PsiElement, String> { "Run spec" }

    override fun getInfo(element: PsiElement): Info? {
        if (element is JSCallExpression) {
            val methodExpr = ObjectUtils.tryCast(element.methodExpression, JSReferenceExpression::class.java)

            if (methodExpr?.text == SCENARIO_WORD) {
                return Info(AllIcons.RunConfigurations.TestState.Run, TOOLTIP_PROVIDER, *ExecutorAction.getActions(0))
            }

            if (methodExpr?.text == FEATURE_WORD) {
                return Info(AllIcons.RunConfigurations.TestState.Run_run, TOOLTIP_PROVIDER, *ExecutorAction.getActions(0))
            }
        }

        return null
    }
}
