package com.apshenkin.codeceptjs.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.*
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.execution.NodeTargetRunOptions
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.javascript.testing.JSTestRunnerUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import java.io.File
import java.nio.file.Files

private val reporterPackage = "codeceptjs-intellij-reporter"

class CodeceptjsRunState(private val myEnv: ExecutionEnvironment, private val myRunConfiguration: CodeceptjsRunConfig) : NodeBaseRunProfileState {
    private val myProject = myEnv.project

    private fun createSMTRunnerConsoleView(workingDirectory: File?, consoleProperties: CodeceptjsConsoleProperties): ConsoleView {
        val consoleView = SMTestRunnerConnectionUtil.createConsole(consoleProperties.testFrameworkName, consoleProperties) as SMTRunnerConsoleView
        consoleProperties.addStackTraceFilter(NodeStackTraceFilter(this.myProject, workingDirectory))
        consoleProperties.stackTrackFilters.forEach { consoleView.addMessageFilter(it) }
        consoleView.addMessageFilter(NodeConsoleAdditionalFilter(this.myProject, workingDirectory))
        Disposer.register(this.myProject, consoleView)
        return consoleView
    }

    private fun configureCommandLine(targetRun: NodeTargetRun, interpreter: NodeJsInterpreter, reporter: String?): PsiElement? {
        var onlyFile: PsiElement? = null
        val commandLine = targetRun.commandLineBuilder
        val clone = this.myRunConfiguration.clone() as CodeceptjsRunConfig
        val data = clone.getPersistentData()

        val workingDirectory = data.getWorkingDirectory()
        if (workingDirectory.isNotBlank()) {
            commandLine.setWorkingDirectory(workingDirectory)
        }

        val hasMochaMulti = clone.hasMochaMultiPackage()

        if (!data.mochaMultiReporter && hasMochaMulti) {
            data.mochaMultiReporter = true
        }

        var envs = data.envs.toMutableMap()
        val startCmd = "run"
        val cliFile = "bin/codecept"
        data.npmRef.takeIf {
                    it?.isNotEmpty() ?: false
                }?.let { NpmUtil.resolveRef(NodePackageRef.create(it), myProject, interpreter) }?.let { pkg ->
                    var exe = pkg.systemIndependentPath
                    if (exe.endsWith("npm") || exe.endsWith("npm.cmd")) {
                        exe = exe.reversed().replaceFirst("mpn", "xpn").reversed()
                    }
                    commandLine.setExePath(exe)
                    val yarn = NpmUtil.isYarnAlikePackage(pkg)
                    if (yarn) {
                        commandLine.addParameters("run")
                    }
                    commandLine.addParameter("codeceptjs")
                } ?: commandLine.addParameters(
                // falling back and run codeceptjs directly without package manager
                (clone.getCodeceptjsPackage().takeIf { it.systemIndependentPath.isNotBlank() }
                        ?: NodePackage.findDefaultPackage(myProject, "codeceptjs", interpreter))!!.systemDependentPath + "/$cliFile")

        commandLine.addParameter(startCmd)
        val specParams = mutableListOf<String>()
        val specParamGenerator = { _: String, ni: String -> ni }
        specParams.add(when (data.kind) {
            CodeceptjsRunConfig.TestKind.DIRECTORY -> {
                "${
                    specParamGenerator(File(data.specsDir!!).name, FileUtil.toSystemDependentName(data.specsDir!!))
                }/**/*"
            }

            CodeceptjsRunConfig.TestKind.SPEC, CodeceptjsRunConfig.TestKind.TEST -> {
                specParamGenerator(File(data.specFile!!).name, data.specFile!!)
            }
        })
        commandLine.addParameters(specParams)
        if (data.additionalParams.isNotBlank()) {
            val params = data.additionalParams.trim().split("\\s+".toRegex()).toMutableList()
            commandLine.addParameters(params)
        }
        if (data.mochaMultiReporter) {
            envs.put("IJ_CODECEPTJS_MOCHA_MULTI", "true")
        }
        targetRun.envData = EnvironmentVariablesData.create(envs, data.passParentEnvs)
        if (data.mochaMultiReporter) {
            commandLine.addParameter("--reporter")
            commandLine.addParameter("mocha-multi")
        } else {
            reporter?.let {
                commandLine.addParameter("--reporter")
                commandLine.addParameter(it)
            }
        }
        if (data.kind == CodeceptjsRunConfig.TestKind.TEST) {
            commandLine.addParameter("--grep")
            data.allNames?.let {
                commandLine.addParameter("^" + JSTestRunnerUtil.escapeJavaScriptRegexp(it.joinToString(separator = ": ")) + if (data.allNames!!.size > 1 && !data.isDataBasedTest) "$" else "")
            }
        }
        return onlyFile
    }

    private fun CodeceptjsRunConfig.getCodeceptjsReporterFile(): String {
        getContextFile()?.let {
            val info = NodeModuleSearchUtil.resolveModuleFromNodeModulesDir(it, reporterPackage, NodeModuleDirectorySearchProcessor.PROCESSOR)
            if (info != null && info.moduleSourceRoot.isDirectory) {
                return NodePackage(info.moduleSourceRoot.path).systemIndependentPath
            }
        }

        return reporter.absolutePath
    }


    override fun createExecutionResult(processHandler: ProcessHandler): ExecutionResult {
        val consoleProperties = CodeceptjsConsoleProperties(this.myRunConfiguration, this.myEnv.executor, CodeceptjsTestLocationProvider(), NodeCommandLineUtil.shouldUseTerminalConsole(processHandler))
        val workingDir: File? = if (StringUtil.isEmpty(this.myRunConfiguration.workingDirectory)) null else File(this.myRunConfiguration.workingDirectory!!)
        val consoleView = createSMTRunnerConsoleView(workingDir, consoleProperties)
        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)
        val executionResult = DefaultExecutionResult(consoleView, processHandler)
        executionResult.setRestartActions(*arrayOf<AnAction?>(consoleProperties.createRerunFailedTestsAction(consoleView)))
        return executionResult
    }

    override fun startProcess(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val interpreter: NodeJsInterpreter = NodeJsInterpreterRef.create(this.myRunConfiguration.getPersistentData().nodeJsRef).resolveNotNull(myEnv.project)
        val targetRun = NodeTargetRun(interpreter, myProject, configurator, NodeTargetRunOptions.of(false, myRunConfiguration))
        val reporter = myRunConfiguration.getCodeceptjsReporterFile()
        configureCommandLine(targetRun, interpreter, reporter)

        return targetRun.startProcess()
    }
}


private val reporter by lazy {
    Files.createTempFile("intellij-codeceptjs-reporter", ".js").toFile().apply {
        writeBytes(CodeceptjsRunState::class.java.getResourceAsStream("/bundle.js").readBytes())
        deleteOnExit()
    }
}
