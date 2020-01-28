package optics_plugin.generator

import com.intellij.psi.PsiElement
import optics_plugin.Constants
import optics_plugin.extensions.decapitalizedName
import optics_plugin.extensions.getTypeParametersDeclaration
import optics_plugin.extensions.isGenericClass
import optics_plugin.extensions.isNullableType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass


fun KtParameter.getLensDeclaration(): PsiElement? {
    return this.containingClass()?.let { ktClass ->
        when {
            ktClass.isGenericClass() -> createFunctionFromText(
                ktClass, """
                fun ${ktClass.getTypeParametersDeclaration()} $name() = ${getLensClassForParameter(this)}(
                    get = ${getClassNameForGetter(this)}::${name},
                    set = { ${ktClass.decapitalizedName}, $name -> ${ktClass.decapitalizedName}.copy(${name} = ${name}) }
                )
                """
            )

            else -> createPropertyFromText(
                ktClass, """
                val $name = ${getLensClassForParameter(this)}(
                    get = ${getClassNameForGetter(this)}::${name},
                    set = { ${ktClass.decapitalizedName}, $name -> ${ktClass.decapitalizedName}.copy(${name} = ${name}) }
                )
                """
            )
        }
    }
}

private fun createPropertyFromText(ktClass: KtClass, text: String): KtProperty {
    return KtPsiFactory(ktClass.project).createProperty(text)
}

private fun createFunctionFromText(ktClass: KtClass, text: String): KtFunction {
    return KtPsiFactory(ktClass.project).createFunction(text)
}

private fun getLensClassForParameter(ktParameter: KtParameter): String {
    return if (ktParameter.isNullableType()) Constants.OPT_LENS_CLASS_NAME else Constants.LENS_CLASS_NAME
}

private fun getClassNameForGetter(ktParameter: KtParameter): String {
    return ktParameter.containingClass()?.let { ktClass ->
        when {
            ktClass.isGenericClass() -> "${ktClass.name}${ktClass.getTypeParametersDeclaration()}"
            else -> "${ktClass.name}"
        }
    } ?: ""
}