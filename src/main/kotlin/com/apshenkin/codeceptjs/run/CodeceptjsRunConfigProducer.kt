package com.apshenkin.codeceptjs.run

import com.apshenkin.codeceptjs.structure.CodeceptjsFileStructureBuilder
import com.apshenkin.codeceptjs.utils.CodeceptjsSpecStructure
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.javascript.testFramework.JsTestElementPath
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testing.JsTestRunConfigurationProducer
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.text.nullize
import kotlin.reflect.KProperty1

val codeceptjsDescriptorFile = arrayOf("codecept.json", "codecept.conf.js", "codecept.conf.ts")

class CodeceptjsRunConfigProducer : JsTestRunConfigurationProducer<CodeceptjsRunConfig>(listOf("codeceptjs")) {
    override fun isConfigurationFromCompatibleContext(configuration: CodeceptjsRunConfig, context: ConfigurationContext): Boolean {
        val psiElement = context.psiLocation ?: return false
        val codeceptjsBase = findFileUpwards((psiElement as? PsiDirectory)?.virtualFile ?: psiElement.containingFile?.virtualFile
        ?: return false, codeceptjsDescriptorFile) ?: return false
        val thatData = configuration.getPersistentData()
        val thisData = createTestElementRunInfo(psiElement, CodeceptjsRunConfig.CodeceptjsRunSettings(), codeceptjsBase.path)?.mySettings
                ?: return false
        if (thatData.kind != thisData.kind) return false
        val compare: (KProperty1<CodeceptjsRunConfig.CodeceptjsRunSettings, String?>) -> Boolean = { it.get(thatData).nullize(true) == it.get(thisData).nullize(true) }
        return when (thatData.kind) {
            CodeceptjsRunConfig.TestKind.DIRECTORY -> compare(CodeceptjsRunConfig.CodeceptjsRunSettings::specsDir)
            CodeceptjsRunConfig.TestKind.SPEC -> compare(CodeceptjsRunConfig.CodeceptjsRunSettings::specFile)
            CodeceptjsRunConfig.TestKind.TEST -> compare(CodeceptjsRunConfig.CodeceptjsRunSettings::specFile) && compare(CodeceptjsRunConfig.CodeceptjsRunSettings::testName)
        }
    }

    private fun createTestElementRunInfo(element: PsiElement, templateRunSettings: CodeceptjsRunConfig.CodeceptjsRunSettings, codeceptjsBase: String): CodeceptjsTestElementInfo? {
        val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return null
        templateRunSettings.setWorkingDirectory(codeceptjsBase)
        val containingFile = element.containingFile as? JSFile ?: return if (virtualFile.isDirectory) {
            templateRunSettings.kind = CodeceptjsRunConfig.TestKind.DIRECTORY
            templateRunSettings.specsDir = virtualFile.canonicalPath
            return CodeceptjsTestElementInfo(templateRunSettings, null)
        } else null

        val textRange = element.textRange ?: return null

        val test = findTestByRange(containingFile, textRange)
        val path = test.first
        val dataDrivenTest = test.second
        if (path == null) {
            templateRunSettings.kind = CodeceptjsRunConfig.TestKind.SPEC
            templateRunSettings.specFile = containingFile.virtualFile.canonicalPath
            return CodeceptjsTestElementInfo(templateRunSettings, containingFile)
        }
        templateRunSettings.specFile = virtualFile.path
        templateRunSettings.kind = if (path.testName != null || path.suiteNames.isNotEmpty()) CodeceptjsRunConfig.TestKind.TEST else CodeceptjsRunConfig.TestKind.SPEC
        templateRunSettings.allNames = path.allNames
        if (templateRunSettings.kind == CodeceptjsRunConfig.TestKind.TEST) {
            templateRunSettings.testName = path.testName ?: path.suiteNames.last()
            templateRunSettings.isDataBasedTest = dataDrivenTest
        }
        return CodeceptjsTestElementInfo(templateRunSettings, path.testElement)
    }

    class CodeceptjsTestElementInfo(val mySettings: CodeceptjsRunConfig.CodeceptjsRunSettings, val myEnclosingElement: PsiElement?)

    override fun setupConfigurationFromCompatibleContext(configuration: CodeceptjsRunConfig, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val psiElement = context.psiLocation ?: return false
        val codeceptjsBase = findFileUpwards((psiElement as? PsiDirectory)?.virtualFile ?: psiElement.containingFile?.virtualFile
        ?: return false, codeceptjsDescriptorFile) ?: return false
        val runInfo = createTestElementRunInfo(psiElement, configuration.getPersistentData(), codeceptjsBase.path) ?: return false
        configuration.setGeneratedName()
        runInfo.myEnclosingElement?.let { sourceElement.set(it) }
        return true
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return type.configurationFactory
    }
}

fun findFileUpwards(specName: VirtualFile, fileName: Array<String>): VirtualFile? {
    var cur = specName.parent
    while (cur != null) {
        if (cur.children.find { name -> fileName.any { it == name.name } } != null) {
            return cur
        }
        cur = cur.parent
    }
    return null
}

fun findTestByRange(containingFile: JSFile, textRange: TextRange): Pair<JsTestElementPath?, Boolean> {
    val codeceptjsElement = CodeceptjsFileStructureBuilder.getInstance().fetchCachedTestFileStructure(containingFile).findJasmineElement(textRange)

    if (codeceptjsElement != null) {
        return Pair(
                CodeceptjsFileStructureBuilder.getInstance().fetchCachedTestFileStructure(containingFile).findTestElementPath(textRange),
                // for Features, we should not set end line regexp so true by default
                if (codeceptjsElement is CodeceptjsSpecStructure) codeceptjsElement.isDataDrivenTest else true
        )
    }

    return Pair(
            MochaTddFileStructureBuilder.getInstance().fetchCachedTestFileStructure(containingFile).findTestElementPath(textRange),
            false
    )
}
