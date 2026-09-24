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
    // 1. Fingerprint: locate the method that contains the string "purchase_buy__".
    val purchaseCheckFingerprint = Fingerprint(
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Retrieve the matched method safely using the fingerprint result.
        val method = purchaseCheckFingerprint.result?.method
            ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

        // 3. Get the list of instructions.
        val instructions = method.implementation?.instructions?.toList()
            ?: throw PatchException("Method implementation or instructions are missing.")

        // 4. Find the index of the "move-result" instruction that follows getBoolean.
        var insertIndex = -1
        for (i in instructions.indices) {
            val instr = instructions[i]
            val opcodeName = instr.opcode.name
            if (opcodeName == "MOVE_RESULT" || opcodeName == "MOVE_RESULT_OBJECT") {
                if (i > 0) {
                    val prev = instructions[i - 1]
                    if (prev.toString().contains("getBoolean")) {
                        insertIndex = i + 1
                        break
                    }
                }
            }
        }

        if (insertIndex == -1) {
            throw PatchException("Could not find the getBoolean/move-result sequence in the method.")
        }

        // 5. Insert "const/4 p0, 0x1" right after move-result p0.
        //    This overwrites the result of getBoolean with true (1).
        method.addInstructions(
            insertIndex,
            """
                const/4 p0, 0x1
            """
        )
    }
}
