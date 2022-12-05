package com.apshenkin.codeceptjs.run

import com.apshenkin.codeceptjs.CodeceptjsIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.project.Project
import javax.swing.Icon

val type = ConfigurationTypeUtil.findConfigurationType(CodeceptjsConfigurationType::class.java)

class CodeceptjsConfigurationType : ConfigurationTypeBase("CodeceptjsConfigurationType", "Codeceptjs", "Run Codeceptjs Test", CodeceptjsIcons.CODECEPTJS) {
    val configurationFactory: ConfigurationFactory

    init {
        configurationFactory = object : ConfigurationFactory(this) {
            override fun getId(): String {
                return name
            }

            override fun createTemplateConfiguration(p0: Project): RunConfiguration {
                return CodeceptjsRunConfig(p0, this, NodeJsInterpreterRef.createProjectRef().resolve(p0))
            }

            override fun getIcon(): Icon {
                return CodeceptjsIcons.CODECEPTJS
            }

            override fun isApplicable(project: Project): Boolean {
                return true
            }
        }
        addFactory(configurationFactory)
    }
}
