package com.apshenkin.codeceptjs.utils

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression

data class ScenarioParseResult(
        val callExpression: JSCallExpression,
        val expression: JSExpression,
        val name: String,
        val isDataDrivenTest: Boolean,
)
