package mightymich.morphe.patches.videoguru

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Unlocks Pro features in Video Guru by forcing the premium check to return true."
) {
    compatibleWith(VideoGuruCompatibility.VIDEO_GURU)

    // 1. Fingerprint: locate the method that contains the string "SubscribePro".
    val subscribeProFingerprint = Fingerprint(
        filters = listOf(
            string("SubscribePro")
        )
    )

    execute {
        subscribeProFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val instructions = method.implementation?.instructions?.toList()
                ?: throw PatchException("Method has no implementation.")

            // 2. Find the invoke instruction that calls a()Z.
            //    We look for any invoke whose reference is a MethodReference
            //    with name "a", returnType "Z", and no parameters.
            var invokeIndex = -1
            var invokeRegister = -1
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is MethodReference &&
                        ref.name == "a" &&
                        ref.returnType == "Z" &&
                        ref.parameterTypes.isEmpty()
                    ) {
                        invokeIndex = i
                        // Get the register that will receive the result.
                        // The move-result instruction follows immediately after invoke.
                        if (i + 1 < instructions.size) {
                            val next = instructions[i + 1]
                            if (next.opcode.name == "MOVE_RESULT" &&
                                next is OneRegisterInstruction
                            ) {
                                invokeRegister = next.registerA
                            }
                        }
                        break
                    }
                }
            }

            if (invokeIndex == -1 || invokeRegister == -1) {
                throw PatchException("Could not find a call to a()Z followed by move-result.")
            }

            // 3. Replace the move-result instruction with const/4 vX, 0x1.
            //    This forces the result of a()Z to be true.
            method.replaceInstruction(
                invokeIndex + 1,
                "const/4 v$invokeRegister, 0x1"
            )
        }
    }
}
