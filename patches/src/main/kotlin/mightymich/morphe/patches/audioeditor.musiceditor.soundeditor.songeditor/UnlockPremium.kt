package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    // Fingerprint based on the string visible in the video/screenshot
    val purchaseCheckFingerprint = Fingerprint(
        returnType = "Z", // "Z" means boolean
        strings = listOf("purchase_buy__")
    )

    execute {
        // Find the method using the fingerprint
        val method = purchaseCheckFingerprint.result?.mutableMethod
            ?: throw PatchException(
                "Could not find the purchase check method containing 'purchase_buy__'."
            )

        // Insert at the very beginning of the method:
        //   const/4 v0, 0x1  -> load 1 (true) into register v0
        //   return v0        -> return true immediately
        method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
