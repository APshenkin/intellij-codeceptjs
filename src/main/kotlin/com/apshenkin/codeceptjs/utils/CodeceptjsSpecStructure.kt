package com.apshenkin.codeceptjs.utils

import com.intellij.javascript.testFramework.jasmine.JasmineSpecStructure
import com.intellij.javascript.testFramework.jasmine.JasmineSuiteStructure

class CodeceptjsSpecStructure(
        scenarioParseResult: ScenarioParseResult, parent: JasmineSuiteStructure?
) : JasmineSpecStructure(scenarioParseResult.callExpression, scenarioParseResult.expression, scenarioParseResult.name, parent) {

    val isDataDrivenTest = scenarioParseResult.isDataDrivenTest

}
