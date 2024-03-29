package com.apshenkin.codeceptjs.run

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.javascript.testFramework.util.EscapeUtils
import com.intellij.openapi.vfs.VirtualFileManager

class CodeceptjsRerunTestFileAction(consoleView: SMTRunnerConsoleView, consoleProperties: CodeceptjsConsoleProperties) : AbstractRerunFailedTestsAction(consoleView) {
    init {
        this.init(consoleProperties)
        this.model = consoleView.resultsViewer
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile {
//        val configuration = this.myConsoleProperties.configuration as CodeceptjsRunConfig
//        val state = CodeceptjsRunState(environment, configuration)
        TODO("codeceptjs currently can't define test pattern to run")
//        state.setFailedTests(convertToTestFqns(this.getFailedTests(configuration.project)))
//        return object : MyRunProfile(configuration) {
//            override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
//                return state
//            }
//        }
    }

    private fun convertToTestFqns(tests: List<AbstractTestProxy>): List<List<String>> {
        return tests.mapNotNull { convertToTestFqn(it) }.toList()
    }

    private fun convertToTestFqn(test: AbstractTestProxy): List<String>? {
        val url = test.locationUrl
        if (test.isLeaf && url != null) {
            val testFqn = EscapeUtils.split(VirtualFileManager.extractPath(url), '.')
            if (testFqn.isNotEmpty()) {
                return testFqn
            }
        }
        return null
    }
}
