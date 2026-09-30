package mightymich.morphe.patches.reelshort

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features (Experimental)",
    description = "Unlocks ReelShort premium by forcing getVip_status and isVip to return true. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(ReelShortCompatibility.REELSHORT)

    // 1. Fingerprint for getVip_status()I – returns int.
    val getVipStatusFingerprint = Fingerprint(
        name = "getVip_status",
        returnType = "I"
    )

    // 2. Fingerprint for isVip()Z – returns boolean.
    val isVipFingerprint = Fingerprint(
        name = "isVip",
        returnType = "Z"
    )

    execute {
        // Patch getVip_status -> return 0x2.
        getVipStatusFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find getVip_status method.")
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x2
                    return v0
                """
            )
        }

        // Patch isVip -> return 0x1 (true).
        isVipFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find isVip method.")
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
