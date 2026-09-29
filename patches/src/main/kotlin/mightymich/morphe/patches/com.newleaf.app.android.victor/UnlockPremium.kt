package mightymich.morphe.patches.reelshort

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features (Experimental)",
    description = "Unlocks ReelShort premium by forcing getVip, getExecVipFeatures and getVipStatus to return true. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(ReelShortCompatibility.REELSHORT)

    // 1. Fingerprint for getVip() – returns boolean or int.
    val getVipFingerprint = Fingerprint(
        name = "getVip",
        returnType = "Z" // boolean; if it fails, try "I".
    )


    val getExecVipFeaturesFingerprint = Fingerprint(
        name = "getExecVipFeatures",
        returnType = "I"
    )

    val getVipStatusFingerprint = Fingerprint(
        name = "getVipStatus",
        returnType = "I"
    )

    execute {

        getVipFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find getVip method.")
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }


        getExecVipFeaturesFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find getExecVipFeatures method.")
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x2
                    return v0
                """
            )
        }

       
        getVipStatusFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find getVipStatus method.")
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
