package optics_plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import optics_plugin.Constants
import optics_plugin.extensions.isNullableType
import optics_plugin.generator.toLensReferencePropertyDeclaration
import optics_plugin.ui.GenerateLensDialog
import org.jetbrains.kotlin.idea.scratch.output.executeCommand
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.kotlin.resolve.ImportPath

class GenerateLensAction : AnAction() {

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

                val importsList = mutableListOf(Constants.LENSES_FQCN)
                if (selectedParameters.find { it.isNullableType() } != null) {
                    importsList += Constants.OPT_LENSES_FQCN
                }
                dataClass.addToImports(*importsList.toTypedArray())

                val hasCompanionObject = dataClass.companionObjects.firstOrNull() != null
                val companionObject = dataClass.companionObjects.firstOrNull() ?: ktPsiFactory.createCompanionObject()
                val objectBody = companionObject.getOrCreateBody()

                selectedParameters.forEach { parameter ->
                    val lensProperty = parameter.toLensReferencePropertyDeclaration(dataClass)
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
        val existingImports = containingKtFile.importDirectives.mapTo(mutableSetOf()) { directive ->
            directive.importPath?.fqName?.asString().orEmpty()
        }

        val importListPsiElement = containingKtFile.importList
        for (path in imports) {
            val importDirective = ktPsiFactory.createImportDirective(ImportPath.fromString(path))
            if (existingImports.contains(path)) {
                continue
            }

            importListPsiElement?.add(ktPsiFactory.createLineBreak())
            importListPsiElement?.add(importDirective)
            importListPsiElement?.add(ktPsiFactory.createLineBreak())
        }
    }

    private fun KtPsiFactory.createLineBreak() = createWhiteSpace("\n")

}