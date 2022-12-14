package com.apshenkin.codeceptjs.run

import com.intellij.execution.Executor
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.testing.JsTestConsoleProperties
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.util.config.BooleanProperty
import com.intellij.util.config.DumbAwareToggleBooleanProperty
import javax.swing.JComponent

class CodeceptjsConsoleProperties(config: CodeceptjsRunConfig, executor: Executor, private val myLocator: SMTestLocator, val withTerminalConsole: Boolean) : JsTestConsoleProperties(config, "CodeceptjsTestRunner", executor) {
    companion object {
        val SHOW_LATEST_SCREENSHOT = BooleanProperty("showLatestScreenshot", false)
    }

    init {
        isUsePredefinedMessageFilter = false
        setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false)
        setIfUndefined(TestConsoleProperties.HIDE_IGNORED_TEST, true)
        setIfUndefined(TestConsoleProperties.SCROLL_TO_SOURCE, true)
        setIfUndefined(TestConsoleProperties.SELECT_FIRST_DEFECT, true)
        isIdBasedTestTree = true
        isPrintTestingStartedTime = false
    }

    override fun getTestLocator(): SMTestLocator? {
        return myLocator
    }

    override fun createRerunFailedTestsAction(consoleView: ConsoleView?): AbstractRerunFailedTestsAction? {
        return CodeceptjsRerunTestFileAction(consoleView as SMTRunnerConsoleView, this)
    }

    override fun appendAdditionalActions(actionGroup: DefaultActionGroup, parent: JComponent, target: TestConsoleProperties?) {
        actionGroup.add(DumbAwareToggleBooleanProperty("Show Latest Screenshot",
                "Show the latest screenshot if multiple files found for the test",
                null, target, SHOW_LATEST_SCREENSHOT))
    }
}
