package mightymich.morphe.patches.videoguru

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Unlocks Pro features in Video Guru by forcing the premium check to return true."
) {
    compatibleWith(VideoGuruCompatibility.VIDEO_GURU)

    // 1. Fingerprint: locate the class that contains the string "SubscribePro".
    //    We don't search for a method here, because the string is used in the class,
    //    but the actual premium check method (a()Z) might be called from other methods.
    val classFingerprint = Fingerprint(
        filters = listOf(
            string("SubscribePro")
        )
    )

    execute {
        classFingerprint.let { fingerprint ->
            // Get the class definition from the match.
            val classDef = fingerprint.match?.classDef
                ?: throw PatchException("Could not find class with 'SubscribePro' string.")

            // 2. Iterate over all methods in the class.
            classDef.methods.forEach { method ->
                val impl = method.implementation ?: return@forEach
                val instructions = impl.instructions.toList()

                // 3. Find all invoke instructions that call a()Z.
                for (i in instructions.indices) {
                    val instruction = instructions[i]
                    if (instruction is ReferenceInstruction) {
                        val ref = instruction.reference
                        if (ref is MethodReference &&
                            ref.name == "a" &&
                            ref.returnType == "Z" &&
                            ref.parameterTypes.isEmpty()
                        ) {
                            // 4. Check if the next instruction is move-result.
                            if (i + 1 < instructions.size) {
                                val next = instructions[i + 1]
                                if (next.opcode.name == "MOVE_RESULT" &&
                                    next is OneRegisterInstruction
                                ) {
                                    // 5. Replace move-result with const/4 vX, 0x1.
                                    method.replaceInstruction(
                                        i + 1,
                                        "const/4 v${next.registerA}, 0x1"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
