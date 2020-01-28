package optics_plugin.extensions

import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter


fun KtParameter.isNullableType() = this.typeReference?.typeElement is KtNullableType