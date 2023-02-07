package com.apshenkin.codeceptjs.run

import com.apshenkin.codeceptjs.utils.Utils.Companion.DATA_NAME
import com.apshenkin.codeceptjs.utils.Utils.Companion.FEATURE_NAME
import com.apshenkin.codeceptjs.utils.Utils.Companion.SCENARIO_NAME
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.intellij.util.ObjectUtils


class CodeceptjsTestRunLineMarkerProvider : RunLineMarkerContributor() {
    private val TOOLTIP_PROVIDER = Function<PsiElement, String> { "Run spec" }

    override fun getInfo(element: PsiElement): Info? {
        if (element is JSCallExpression) {
            val methodExpr = ObjectUtils.tryCast(element.methodExpression, JSReferenceExpression::class.java)

            if (methodExpr?.text == SCENARIO_NAME || methodExpr?.text == DATA_NAME) {
                return Info(AllIcons.RunConfigurations.TestState.Run, TOOLTIP_PROVIDER, *ExecutorAction.getActions(0))
            }

            if (methodExpr?.text == FEATURE_NAME) {
                return Info(AllIcons.RunConfigurations.TestState.Run_run, TOOLTIP_PROVIDER, *ExecutorAction.getActions(0))
            }
        }

        return null
    }
}
