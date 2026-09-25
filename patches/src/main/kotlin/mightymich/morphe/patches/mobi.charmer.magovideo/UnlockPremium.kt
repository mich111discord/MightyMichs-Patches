package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium check method to return true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate any method that reads the static boolean field Z in class Lf2/l;.
    //    This method is responsible for checking the premium status.
    val premiumCheckFingerprint = Fingerprint(
        returnType = "Z",
        filters = listOf(
            fieldAccess(
                opcode = Opcode.SGET_BOOLEAN,
                definingClass = "Lf2/l;",
                name = "Z",
                type = "Z"
            )
        )
    )

    execute {
        premiumCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method

            // 2. Insert instructions at the very beginning of the method:
            //      const/4 v0, 0x1  -> load 1 (true) into register v0
            //      return v0        -> return true immediately
            //    This forces the premium check to always succeed.
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
