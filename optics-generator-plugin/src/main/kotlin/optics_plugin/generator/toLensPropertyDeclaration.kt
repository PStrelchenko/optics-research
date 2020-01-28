package optics_plugin.generator

import optics_plugin.Constants
import optics_plugin.extensions.isNullableType
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName


fun KtParameter.toLensPropertyDeclaration(ktClass: KtClass): KtProperty {
    return createPropertyFromText(
        ktClass, """
        val $name = ${getLensClassForParameter(this)}<${ktClass.name}, ${type()?.serialName()}>(
            get = { it.${name} },
            set = { ${ktClass.name?.decapitalize()}, $name -> ${ktClass.name?.decapitalize()}.copy(${name} = ${name}) }
        )
        """
    )
}

// Variant without generic
fun KtParameter.toLensReferencePropertyDeclaration(ktClass: KtClass): KtProperty {
    return createPropertyFromText(
        ktClass, """
        val $name = ${getLensClassForParameter(this)}(
            get = ${ktClass.name}::${name},
            set = { ${ktClass.name?.decapitalize()}, $name -> ${ktClass.name?.decapitalize()}.copy(${name} = ${name}) }
        )
        """
    )
}

private fun createPropertyFromText(ktClass: KtClass, text: String): KtProperty {
    return KtPsiFactory(ktClass.project).createProperty(text)
}

private fun getLensClassForParameter(ktParameter: KtParameter): String {
    return if (ktParameter.isNullableType()) Constants.OPT_LENS_CLASS_NAME else Constants.LENS_CLASS_NAME
}