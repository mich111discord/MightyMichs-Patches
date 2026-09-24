package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    compatibleWith(AudioEditorCompatibility.AUDIO_EDITOR)

    // 1. Fingerprint: locate the method containing "purchase_buy__".
    val purchaseCheckFingerprint = Fingerprint(
        filters = listOf(
            string("purchase_buy__")
        )
    )

    execute {
        purchaseCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val implementation = method.implementation
                ?: throw PatchException("Method has no implementation.")

            val instructions = implementation.instructions.toList()

            // 2. Find the invoke-static call that takes a String and returns a boolean.
            var invokeIndex = -1
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is MethodReference &&
                        ref.returnType == "Z" &&
                        ref.parameterTypes.size == 1 &&
                        ref.parameterTypes[0] == "Ljava/lang/String;"
                    ) {
                        invokeIndex = i
                        break
                    }
                }
            }

            if (invokeIndex == -1) {
                throw PatchException("Could not find the (String)Z invoke call in the target method.")
            }

            // 3. Find the next MOVE_RESULT after that invoke.
            var moveResultIndex = -1
            for (i in (invokeIndex + 1) until instructions.size) {
                val op = instructions[i].opcode.name
                if (op == "MOVE_RESULT" || op == "MOVE_RESULT_OBJECT" || op == "MOVE_RESULT_WIDE") {
                    moveResultIndex = i
                    break
                }
            }

            if (moveResultIndex == -1) {
                throw PatchException("Could not find the move-result instruction after the (String)Z invoke.")
            }

            // 4. Get the register that receives the boolean result.
            val resultInstruction = instructions[moveResultIndex] as OneRegisterInstruction
            val resultRegister = resultInstruction.registerA

            // 5. Insert "const/4 vX, 0x1" after move-result.
            //    This forces the purchase check to always return true.
            method.addInstructions(
                moveResultIndex + 1,
                """
                    const/4 v$resultRegister, 0x1
                """
            )
        }
    }
}
