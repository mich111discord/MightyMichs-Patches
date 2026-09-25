package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the purchase flag to true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the method that contains the string "onetime_purchase".
    //    This method is responsible for initializing the purchase state.
    val purchaseInitFingerprint = Fingerprint(
        filters = listOf(
            string("onetime_purchase")
        )
    )

    execute {
        purchaseInitFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val implementation = method.implementation
                ?: throw PatchException("Method has no implementation.")

            // 2. Find the end of the method to append our instructions.
            val endIndex = implementation.instructions.size

            // 3. Append instructions that force the premium flag to true.
            //    We use v0 as a scratch register (it is safe to use at the end of a void method).
            method.addInstructions(
                endIndex,
                """
                    const/4 v0, 0x1
                    sput-boolean v0, Lf2/l;->Z:Z
                """
            )
        }
    }
}
