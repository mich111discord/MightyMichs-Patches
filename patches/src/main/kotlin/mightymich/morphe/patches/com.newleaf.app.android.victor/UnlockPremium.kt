package mightymich.morphe.patches.reelshort

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks ReelShort premium by forcing the vipStatus method to return true. WARNING: May cause crashes.",
    default = true
    compatibleWith(ReelShortCompatibility.REELSHORT)
    val vipStatusFingerprint = Fingerprint(
        name = "vipStatus",
        returnType = "I" // It returns an integer status code.
    )

    execute {
        vipStatusFingerprint.let { fingerprint ->
            val method = fingerprint.method

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
