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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    // 1. Fingerprint: locate the method that contains the string "purchase_buy__".
    val purchaseCheckFingerprint = Fingerprint(
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Use the official Morphe pattern to access the match and its instructions.
        purchaseCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

            // 3. Find the instruction that loads the "purchase_buy__" string.
            //    In the smali you showed, this is the CONST_STRING instruction.
            val stringInstructionMatch = fingerprint.instructionMatches
                .firstOrNull { it.instruction.opcode.name == "CONST_STRING" }
                ?: throw PatchException("Could not find the CONST_STRING instruction in the target method.")

            // 4. Get the register that holds the string.
            val register = stringInstructionMatch.getInstruction<OneRegisterInstruction>().registerA

            // 5. Insert "const/4 vX, 0x1" right after the string is loaded.
            //    This forces the boolean check to always use 'true'.
            method.addInstructions(
                stringInstructionMatch.index + 1,
                """
                    const/4 v$register, 0x1
                """
            )
        }
    }
}
