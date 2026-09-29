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

    // 1. Fingerprint for getVip() – returns int (I), not boolean.
    val getVipFingerprint = Fingerprint(
        name = "getVip",
        returnType = "I"
    )

    // 2. Fingerprint for getExecVipFeatures() – returns int.
    val getExecVipFeaturesFingerprint = Fingerprint(
        name = "getExecVipFeatures",
        returnType = "I"
    )

    // 3. Fingerprint for getVipStatus() – returns int.
    val getVipStatusFingerprint = Fingerprint(
        name = "getVipStatus",
        returnType = "I"
    )

    execute {
        // Patch getVip -> return 0x1.
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

        // Patch getExecVipFeatures -> return 0x2.
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

        // Patch getVipStatus -> return 0x1 (or 0x8 if 0x1 does not work).
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
