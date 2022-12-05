package com.apshenkin.codeceptjs.run

import com.apshenkin.codeceptjs.run.ui.*
import com.intellij.execution.*
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.interpreter.NodeInterpreterUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageDescriptor
import com.intellij.javascript.testFramework.PreferableRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.io.LocalFileFinder
import java.io.File


class CodeceptjsRunConfig(
        project: Project,
        factory: ConfigurationFactory,
        @get:JvmName("getUsername") override val interpreter: NodeJsInterpreter?
) : AbstractNodeTargetRunProfile(project, factory, ""),
        JSRunProfileWithCompileBeforeLaunchOption,
        NodeDebugRunConfiguration,
        PreferableRunConfiguration,
        CommonProgramRunConfigurationParameters {

    private var myCodeceptjsRunSettings: CodeceptjsRunSettings = CodeceptjsRunSettings()

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val state = CodeceptjsRunState(env, this)
        return state
    }

    override fun getConfigurationEditor(): SettingsEditor<out AbstractNodeTargetRunProfile> {
        val group = SettingsEditorGroup<CodeceptjsRunConfig>()
        group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), CodeceptjsConfigurableEditorPanel(this.project))
        return group
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        XmlSerializer.deserializeInto(this, element)
        XmlSerializer.deserializeInto(myCodeceptjsRunSettings, element)

        EnvironmentVariablesComponent.readExternal(element, myCodeceptjsRunSettings.envs)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        XmlSerializer.serializeInto(this, element)
        XmlSerializer.serializeInto(myCodeceptjsRunSettings, element)

        EnvironmentVariablesComponent.writeExternal(element, myCodeceptjsRunSettings.envs)
    }

    override fun suggestedName(): String? {
        return when (myCodeceptjsRunSettings.kind) {
            TestKind.DIRECTORY -> "All Tests in ${getRelativePath(project, myCodeceptjsRunSettings.specsDir ?: return null)}"
            TestKind.SPEC -> getRelativePath(project, myCodeceptjsRunSettings.specFile ?: return null)
            TestKind.TEST -> myCodeceptjsRunSettings.allNames?.joinToString(" -> ") ?: return null
        }
    }

    override fun getActionName(): String? {
        return when (myCodeceptjsRunSettings.kind) {
            TestKind.DIRECTORY -> "All Tests in ${getLastPathComponent(myCodeceptjsRunSettings.specsDir ?: return null)}"
            TestKind.SPEC -> getLastPathComponent(myCodeceptjsRunSettings.specFile ?: return null)
            TestKind.TEST -> myCodeceptjsRunSettings.allNames?.joinToString(" -> ") ?: return null
        }
    }

    private fun getRelativePath(project: Project, path: String): String {
        val file = LocalFileFinder.findFile(path)
        if (file != null && file.isValid) {
            val root = ProjectFileIndex.getInstance(project).getContentRootForFile(file)
            if (root != null && root.isValid) {
                val relativePath = VfsUtilCore.getRelativePath(file, root, File.separatorChar)
                relativePath?.let { return relativePath }
            }
        }
        return getLastPathComponent(path)
    }

    private fun getLastPathComponent(path: String): String {
        val lastIndex = path.lastIndexOf('/')
        return if (lastIndex >= 0) path.substring(lastIndex + 1) else path
    }

    fun getPersistentData(): CodeceptjsRunSettings {
        return myCodeceptjsRunSettings
    }


    fun getContextFile(): VirtualFile? {
        val data = getPersistentData()
        return findFile(data.specFile ?: "")
                ?: findFile(data.specsDir ?: "")
                ?: findFile(data.workingDirectory ?: "")
    }


    private fun findFile(path: String): VirtualFile? =
            if (FileUtil.isAbsolute(path)) LocalFileSystem.getInstance().findFileByPath(path) else null


    class CypTextRange(@JvmField var startOffset: Int = 0, @JvmField var endOffset: Int = 0)

    interface TestKindViewProducer {
        fun createView(project: Project): CodeceptjsTestKindView
    }

    enum class TestKind(val myName: String) : TestKindViewProducer {
        DIRECTORY("All in &directory") {
            override fun createView(project: Project) = CodeceptjsDirectoryKindView(project)
        },
        SPEC("Spec &file") {
            override fun createView(project: Project) = CodeceptjsSpecKindView(project)
        },
        TEST("Test") {
            override fun createView(project: Project) = CodeceptjsTestView(project)
        },
//        SUITE("Suite") {
//            override fun createView(project: Project) = CodeceptjsSpecKindView(project)
//        }
    }

    override fun clone(): RunConfiguration {
        val clone = super.clone() as CodeceptjsRunConfig
        clone.myCodeceptjsRunSettings = myCodeceptjsRunSettings.clone()
        return clone
    }

    override fun createConfigurationEditor(): SettingsEditor<out AbstractNodeTargetRunProfile> {
        TODO("Not yet implemented")
    }

    fun getCodeceptjsPackage(): NodePackage {
        return if (RunManager.getInstance(this.project).isTemplate(this)) {
            createCodeceptjsPckg() ?: NodePackage("")
        } else {
            createCodeceptjsPckg() ?: run {
                val interpreter = NodeJsInterpreterRef.create(myCodeceptjsRunSettings.nodeJsRef).resolve(project)
                val pkg = codeceptjsPackageDescriptor.findFirstDirectDependencyPackage(project, interpreter, getContextFile())
                myCodeceptjsRunSettings.codeceptjsPackageRef = pkg.systemIndependentPath
                pkg
            }
        }
    }

    private fun createCodeceptjsPckg(): NodePackage? {
        return myCodeceptjsRunSettings.codeceptjsPackageRef?.let { codeceptjsPackageDescriptor.createPackage(it) }
    }

    companion object {
        val codeceptjsPackageName = "codeceptjs"
        val codeceptjsPackageDescriptor = NodePackageDescriptor(listOf(codeceptjsPackageName), emptyMap(), null)
    }

    data class CodeceptjsRunSettings(val u: Unit? = null) : Cloneable {
        @JvmField
        @Deprecated("use allNames", ReplaceWith("allNames"))
        var textRange: CypTextRange? = null

        @JvmField
        var allNames: List<String>? = null

        @JvmField
        var specsDir: String? = null

        @JvmField
        var specFile: String? = null

        @JvmField
        var testName: String? = null

        @JvmField
        var workingDirectory: String? = null

        @JvmField
        var envs: MutableMap<String, String> = LinkedHashMap()

        @JvmField
        var additionalParams: String = ""

        @JvmField
        var passParentEnvs: Boolean = true

        @JvmField
        var nodeJsRef: String = NodeJsInterpreterRef.createProjectRef().referenceName

        @JvmField
        var npmRef: String? = NpmUtil.createProjectPackageManagerPackageRef().referenceName

        @JvmField
        var codeceptjsPackageRef: String? = null

        @JvmField
        var kind: TestKind = TestKind.SPEC

        @JvmField
        var mochaMultiReporter: Boolean = true


        public override fun clone(): CodeceptjsRunSettings {
            try {
                val data = super.clone() as CodeceptjsRunSettings
                data.envs = LinkedHashMap(envs)
                data.allNames = allNames?.toList()
                return data
            } catch (e: CloneNotSupportedException) {
                throw RuntimeException(e)
            }
        }

        fun getWorkingDirectory(): String = ExternalizablePath.localPathValue(workingDirectory)

        fun setWorkingDirectory(value: String?) {
            workingDirectory = ExternalizablePath.urlValue(value)
        }

        fun getSpecName(): String = specFile?.let { File(it).name } ?: ""

        fun setEnvs(envs: Map<String, String>) {
            this.envs.clear()
            this.envs.putAll(envs)
        }
    }

    override fun checkConfiguration() {
        val data = getPersistentData()
        val workingDir = data.getWorkingDirectory()
        val interpreter: NodeJsInterpreter? = NodeJsInterpreterRef.create(data.nodeJsRef).resolve(project)
        NodeInterpreterUtil.checkForRunConfiguration(interpreter)
        if ((data.kind == TestKind.SPEC || data.kind == TestKind.TEST) && data.getSpecName().isBlank()) {
            throw RuntimeConfigurationError("Codeceptjs spec must be defined")
        }
        if (data.kind == TestKind.DIRECTORY && data.specsDir.isNullOrBlank()) {
            throw RuntimeConfigurationError("Spec directory must be defined")
        }
        if (!File(workingDir).exists()) {
            throw RuntimeConfigurationWarning("Working directory '$workingDir' doesn't exist")
        }

        if (data.npmRef?.isBlank() == true) {
            getCodeceptjsPackage().validateAndGetErrorMessage(codeceptjsPackageName, project, interpreter)?.let {
                throw RuntimeConfigurationWarning(it)
            }
        }
    }

    override fun getWorkingDirectory(): String? {
        return myCodeceptjsRunSettings.getWorkingDirectory()
    }

    override fun getEnvs(): MutableMap<String, String> {
        return myCodeceptjsRunSettings.envs
    }


    override fun setWorkingDirectory(value: String?) {
        myCodeceptjsRunSettings.setWorkingDirectory(value)
    }

    override fun setEnvs(envs: MutableMap<String, String>) {
        myCodeceptjsRunSettings.setEnvs(envs)
    }

    override fun isPassParentEnvs(): Boolean {
        return myCodeceptjsRunSettings.passParentEnvs
    }

    override fun setPassParentEnvs(passParentEnvs: Boolean) {
        myCodeceptjsRunSettings.passParentEnvs = passParentEnvs
    }

    override fun isPreferredOver(p0: RunConfiguration, p1: PsiElement): Boolean {
        return true
    }

    override fun setProgramParameters(value: String?) {
        myCodeceptjsRunSettings.additionalParams = value ?: ""
    }

    override fun getProgramParameters(): String? {
        return myCodeceptjsRunSettings.additionalParams
    }
}
