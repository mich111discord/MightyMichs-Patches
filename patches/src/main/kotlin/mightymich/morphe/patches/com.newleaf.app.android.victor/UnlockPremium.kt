package mightymich.morphe.patches.reelshort

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features (Experimental)",
    description = "Unlocks ReelShort premium by forcing the vipStatus field to be true. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(ReelShortCompatibility.REELSHORT)


    val getVipStatusFingerprint = Fingerprint(
        definingClass = "Lcom/newleaf/app/android/victor/profile/setting/deleteaccount/v2/DeleteAccountPageStatus;",
        name = "getVipStatus",
        returnType = "I"
    )

    execute {
        getVipStatusFingerprint.let { fingerprint ->
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
