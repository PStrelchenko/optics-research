package optics_plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.util.PsiTreeUtil
import optics_plugin.ui.GenerateLensDialog
import org.jetbrains.kotlin.psi.KtClass

class GenerateLensAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val ktClass = getKtClassFromContext(e)
        ktClass?.let { dataClass ->
            val dialog = GenerateLensDialog(dataClass)
            dialog.show()
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

}