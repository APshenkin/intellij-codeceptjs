package com.apshenkin.codeceptjs.structureView

import com.apshenkin.codeceptjs.CodeceptjsIcons
import com.apshenkin.codeceptjs.utils.JSCallWithTestName
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.navigation.ItemPresentation

/**
 * Defines how the elements in the structure view
 * should be rendered.
 */
class CodeceptjsStructureViewElement(val element: JSCallWithTestName) : StructureViewTreeElement {
    override fun getPresentation(): ItemPresentation {

        return PresentationData(
                element.name,
                null,
                CodeceptjsIcons.CODECEPTJS_TEST,
                null,
        )
    }

    override fun getChildren(): Array<CodeceptjsStructureViewElement> {
        return element.children.toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {
        return element.jsCallExpression.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return element.jsCallExpression.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element.jsCallExpression.canNavigateToSource()
    }

    override fun getValue(): Any {
        return element.jsCallExpression
    }
}
