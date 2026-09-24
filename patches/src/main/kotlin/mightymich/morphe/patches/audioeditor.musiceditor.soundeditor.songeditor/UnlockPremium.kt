package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    // 1. Define the fingerprint that locates the target method.
    //    It matches a boolean-returning method ("Z") containing the string "purchase_buy__".
    val purchaseCheckFingerprint = Fingerprint(
        returnType = "Z", // "Z" means boolean
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Property delegation gives us direct access to the matched method.
        //    If the fingerprint fails to match, the library throws an exception automatically.
        val method by purchaseCheckFingerprint

        // 3. Insert instructions at the very beginning of the method:
        //      const/4 v0, 0x1  -> load 1 (true) into register v0
        //      return v0        -> return true immediately
        //    This makes the app always believe the purchase was completed.
        method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
