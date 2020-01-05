package io.quarkus.allopen.cli

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.lexer.KtTokens.FINAL_KEYWORD
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

class AllOpenDeclarationAttributeAltererExtension(private val annotations: MutableList<String>)
    : DeclarationAttributeAltererExtension, AnnotationBasedExtension {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> {
        return annotations
    }

    override fun refineDeclarationModality(
            modifierListOwner: KtModifierListOwner,
            declaration: DeclarationDescriptor?,
            containingDeclaration: DeclarationDescriptor?,
            currentModality: Modality,
            bindingContext: BindingContext,
            isImplicitModality: Boolean): Modality? {

        return if (currentModality != Modality.FINAL ||
                (modifierListOwner !is KtClass && modifierListOwner !is KtNamedFunction) ||
                containingDeclaration.toString() == "Companion") {
            null
        } else if (!isImplicitModality && modifierListOwner.hasModifier(FINAL_KEYWORD)) {
            Modality.FINAL // Explicit final
        } else {
            Modality.OPEN
        }
    }
}
