package optics_plugin.ui

import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import javax.swing.JComponent


class GenerateLensDialog(ktClass: KtClass) : DialogWrapper(ktClass.project) {

    private val parametersListModel: CollectionListModel<KtParameter>
    private val myComponent: JComponent


    init {
        title = "Choose parameters for generating lenses"

        val targetParameters = fetchTargetParameters(ktClass)
        parametersListModel = CollectionListModel(targetParameters)

        val jList = JBList(parametersListModel).apply {
            cellRenderer = DefaultPsiElementCellRenderer()
        }
        val toolbarDecorator = ToolbarDecorator.createDecorator(jList).also { it.disableAddAction() }
        val jPanel = toolbarDecorator.createPanel()

        myComponent = LabeledComponent.create(jPanel, "Parameters to include in lenses generator")

        init()
    }


    override fun createCenterPanel(): JComponent? {
        return myComponent
    }


    fun getSelectedParameters(): List<KtParameter> = parametersListModel.items


    private fun fetchTargetParameters(ktClass: KtClass): List<KtParameter> {
        val primaryConstructorParameters = ktClass.primaryConstructorParameters
        val companionObject = ktClass.companionObjects.firstOrNull()

        return if (companionObject == null) {
            primaryConstructorParameters
        } else {
            val properties = companionObject.body?.properties ?: listOf()

            primaryConstructorParameters.filter { parameter ->
                properties.find { it.name == parameter.name } == null
            }
        }
    }

}