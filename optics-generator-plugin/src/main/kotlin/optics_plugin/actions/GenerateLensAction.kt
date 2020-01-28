package optics_plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import optics_plugin.ui.GenerateLensDialog
import org.jetbrains.kotlin.idea.scratch.output.executeCommand
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName


class GenerateLensAction : AnAction() {

    companion object {
        private const val LENS_CLASS_NAME = "Lens"
        private const val LENSES_FQCN = "com.horseunnamed.optics.$LENS_CLASS_NAME"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val ktClass = getKtClassFromContext(e)
        ktClass?.let { dataClass ->
            val dialog = GenerateLensDialog(dataClass)
            dialog.show()
            if (dialog.isOK) {
                generateLenses(dataClass, dialog.getSelectedParameters())
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = getKtClassFromContext(e) != null
    }


    private fun getKtClassFromContext(e: AnActionEvent): KtClass? {
        val currentPsiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)

        if (currentPsiFile == null || editor == null) {
            e.presentation.isEnabled = false
            return null
        }

        val offset = editor.caretModel.offset
        val psiElement = currentPsiFile.findElementAt(offset)

        return PsiTreeUtil.getParentOfType(psiElement, KtClass::class.java)?.takeIf { it.isData() }
    }

    private fun generateLenses(dataClass: KtClass, selectedParameters: List<KtParameter>) {
        executeCommand {
            runWriteAction {
                val ktPsiFactory = KtPsiFactory(dataClass.project)

                dataClass.addToImports(LENSES_FQCN)

                val hasCompanionObject = dataClass.companionObjects.firstOrNull() != null
                val companionObject = dataClass.companionObjects.firstOrNull() ?: ktPsiFactory.createCompanionObject()
                val objectBody = companionObject.getOrCreateBody()

                selectedParameters.forEach { parameter ->
                    val lensProperty = parameter.toLensPropertyDeclaration(dataClass, ktPsiFactory)
                    objectBody.addBefore(lensProperty, objectBody.rBrace)
                    objectBody.addBefore(ktPsiFactory.createLineBreak(), objectBody.rBrace)
                }

                if (!hasCompanionObject) {
                    dataClass.getOrCreateBody().apply {
                        addBefore(companionObject, this.rBrace)
                    }
                }

                CodeStyleManager.getInstance(dataClass.project).reformat(dataClass)
            }
        }
    }


    private fun KtClass.addToImports(vararg imports: String) {
        val ktPsiFactory = KtPsiFactory(project)
        for (path in imports) {
            containingKtFile.importList?.add(ktPsiFactory.createLineBreak())
            containingKtFile.importList?.add(ktPsiFactory.createImportDirective(ImportPath.fromString(path)))
            containingKtFile.importList?.add(ktPsiFactory.createLineBreak())
        }
    }

    private fun KtParameter.toLensPropertyDeclaration(ktClass: KtClass, ktPsiFactory: KtPsiFactory): KtProperty {
        val lensText = """val $name = $LENS_CLASS_NAME<${ktClass.name}, ${type()?.serialName()}>(
                    get = { it.${name} },
                    set = { ${ktClass.name?.decapitalize()}, $name -> ${ktClass.name?.decapitalize()}.copy(${name} = ${name}) }
                )
                """

        return ktPsiFactory.createProperty(lensText)
    }

    private fun KtPsiFactory.createLineBreak() = createWhiteSpace("\n")

}