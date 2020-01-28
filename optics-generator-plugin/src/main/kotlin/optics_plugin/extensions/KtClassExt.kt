package optics_plugin.extensions

import org.jetbrains.kotlin.psi.KtClass

fun KtClass.isGenericClass() = typeParameters.isNotEmpty()

fun KtClass.getTypeParametersNames(): MutableSet<String> = typeParameters.mapNotNullTo(mutableSetOf()) { it.name }

fun KtClass.getTypeParametersDeclaration(): String {
    val typeParametersNames = getTypeParametersNames()
    return "<${typeParametersNames.joinToString(",")}>"
}

val KtClass.decapitalizedName: String get() = name?.decapitalize() ?: ""