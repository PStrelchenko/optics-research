package optics_plugin.extensions

import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClass


fun KtParameter.isNullableType() = this.typeReference?.typeElement is KtNullableType

fun KtParameter.isGenericType(): Boolean {
    return containingClass()?.let { ktClass ->
        val typeParameters = ktClass.getTypeParametersNames()
        val currentTypeName = (this.typeReference?.typeElement as? KtUserType)?.referencedName

        typeParameters.contains(currentTypeName)
    } ?: false
}