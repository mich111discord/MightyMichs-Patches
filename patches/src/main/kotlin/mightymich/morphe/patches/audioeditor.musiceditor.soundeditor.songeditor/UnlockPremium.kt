// ========================================================
// Author: MightyMich
// Project: MightyMich's Patches
// Description: Custom bytecode patches for Android applications
// ========================================================

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
    // 1. Define the fingerprint using the official Morphe DSL.
    //    It matches a boolean-returning method containing the string "purchase_buy__".
    val purchaseCheckFingerprint = Fingerprint(
        returnType = "Z", // "Z" means boolean
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Access the matched method via '.result?.method'.
        //    Throws a clear exception if the fingerprint fails to match.
        val method = purchaseCheckFingerprint.result?.method
            ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

        // 3. Insert instructions at the very beginning of the method:
        //      const/4 v0, 0x1  -> load 1 (true) into register v0
        //      return v0        -> return true immediately
        method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
