package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    compatibleWith(AudioEditorCompatibility.AUDIO_EDITOR)

    // 1. Fingerprint: locate the method containing the string "purchase_buy__".
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

            // 2. Find the CONST_STRING that loads "purchase_buy__".
            var stringIndex = -1
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is StringReference && ref.string == "purchase_buy__") {
                        stringIndex = i
                        break
                    }
                }
            }

            if (stringIndex == -1) {
                throw PatchException("Could not find the string 'purchase_buy__' in the target method.")
            }

            // 3. Starting from the string, find the FIRST invoke that returns Z (boolean).
            var invokeIndex = -1
            for (i in (stringIndex + 1) until instructions.size) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is MethodReference &&
                        ref.returnType == "Z" &&
                        ref.parameterTypes.size == 1
                    ) {
                        invokeIndex = i
                        break
                    }
                }
            }

            if (invokeIndex == -1) {
                throw PatchException("Could not find a boolean-returning invoke after 'purchase_buy__'.")
            }

            // 4. Find the next MOVE_RESULT after the invoke.
            var moveResultIndex = -1
            for (i in (invokeIndex + 1) until instructions.size) {
                val op = instructions[i].opcode.name
                if (op == "MOVE_RESULT" ||
                    op == "MOVE_RESULT_OBJECT" ||
                    op == "MOVE_RESULT_WIDE"
                ) {
                    moveResultIndex = i
                    break
                }
            }

            if (moveResultIndex == -1) {
                throw PatchException("Could not find the move-result after the boolean invoke.")
            }

            // 5. Get the register that receives the boolean result.
            val resultInstruction = instructions[moveResultIndex] as OneRegisterInstruction
            val resultRegister = resultInstruction.registerA

            // 6. Insert "const/4 vX, 0x1" after move-result.
            //    This overwrites the boolean result with true (1).
            method.addInstructions(
                moveResultIndex + 1,
                """
                    const/4 v$resultRegister, 0x1
                """
            )
        }
    }
}
