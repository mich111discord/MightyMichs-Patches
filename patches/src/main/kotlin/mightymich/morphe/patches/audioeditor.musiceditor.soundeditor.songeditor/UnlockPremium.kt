package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.methodCall

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    compatibleWith(AudioEditorCompatibility.AUDIO_EDITOR)

    // 1. Fingerprint: match a method that contains the string "purchase_buy__"
    //    and calls getBoolean.
    val purchaseCheckFingerprint = Fingerprint(
        filters = listOf(
            string("purchase_buy__"),
            methodCall("getBoolean")
        )
    )

    execute {
        purchaseCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val implementation = method.implementation
                ?: throw PatchException("Method has no implementation.")

            val instructions = implementation.instructions.toList()

            // 2. Find the "move-result" instruction that follows getBoolean.
            var moveResultIndex = -1

            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction.opcode.name == "MOVE_RESULT" || instruction.opcode.name == "MOVE_RESULT_OBJECT") {
                    // Check if the previous instruction is a call to getBoolean.
                    if (i > 0) {
                        val prev = instructions[i - 1]
                        if (prev.toString().contains("getBoolean")) {
                            moveResultIndex = i
                            break
                        }
                    }
                }
            }

            if (moveResultIndex == -1) {
                throw PatchException("Could not find the getBoolean/move-result sequence in the method.")
            }

            // 3. Insert "const/4 p0, 0x1" right after move-result p0.
            //    This overwrites the result of getBoolean with true (1).
            method.addInstructions(
                moveResultIndex + 1,
                """
                    const/4 p0, 0x1
                """
            )
        }
    }
}
