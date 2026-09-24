// ========================================================
// Author: MightyMich
// Project: MightyMich's Patches
// Description: Custom bytecode patches for Android applications
// ========================================================

package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    execute {
        // 1. Locate the target method safely by searching through application instructions
        val method = findMethod { m ->
            m.returnType == "Z" && 
            m.implementation?.instructions?.any { instruction -> 
                instruction.toString().contains("purchase_buy__") 
            } == true
        } ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

        // 2. Dynamically determine a safe register to avoid overwriting 
        //    method arguments or the 'this' reference (preventing Verifier Errors).
        val localsCount = method.implementation?.registersCount ?: 1
        val targetRegister = if (localsCount > 0) localsCount - 1 else 0

        // 3. Insert instructions at the very beginning of the method:
        //    const/4 vX, 0x1  -> load 1 (true) into the target register
        //    return vX        -> return true immediately
        method.addInstructions(
            0,
            """
                const/4 v$targetRegister, 0x1
                return v$targetRegister
            """
        )
    }
}
