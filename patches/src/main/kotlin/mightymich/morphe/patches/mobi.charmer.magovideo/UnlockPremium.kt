package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium check method to return true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Class fingerprint: locate the class that contains the string "onetime_purchase".
    //    This is more stable because strings change less often than method names.
    val classFingerprint = Fingerprint(
        filters = listOf(
            string("onetime_purchase")
        )
    )

    // 2. Method fingerprint: within the found class, search for a method that returns boolean (Z).
    //    This is most likely the method that checks the premium status.
    val methodFingerprint = Fingerprint(
        returnType = "Z",
        classFingerprint = classFingerprint // Restrict the search to the class found above.
    )

    execute {
        methodFingerprint.let { fingerprint ->
            val method = fingerprint.method

            // 3. Insert instructions at the very beginning of the method:
            //      const/4 v0, 0x1  -> load 1 (true) into register v0
            //      return v0        -> return true immediately
            //    This forces the method to always return true, regardless of the original logic.
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}
