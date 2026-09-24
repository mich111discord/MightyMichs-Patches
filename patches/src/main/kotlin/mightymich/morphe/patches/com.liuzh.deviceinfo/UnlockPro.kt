package mightymich.morphe.patches.devinfo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Forces the 'is_pro_user' check to always return true, unlocking Pro features in Device Info."
) {
    compatibleWith(DevInfoCompatibility.DEVINFO)

    // 1. Fingerprint: locate the method that contains the string "is_pro_user". 
    val isProUserFingerprint = Fingerprint(
        filters = listOf(
            string("is_pro_user")
        )
    )

    execute {
        isProUserFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val implementation = method.implementation
                ?: throw PatchException("Method has no implementation.")

            val instructions = implementation.instructions.toList()

            // 2. Find the CONST_STRING that loads "is_pro_user".
            var stringIndex = -1
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is StringReference && ref.string == "is_pro_user") {
                        stringIndex = i
                        break
                    }
                }
            }

            if (stringIndex == -1) {
                throw PatchException("Could not find the string 'is_pro_user' in the target method.")
            }

            // 3. Find the next CONST_4 instruction with value 0 (the default value for getBoolean).
            var const4Index = -1
            for (i in (stringIndex + 1) until instructions.size) {
                val instruction = instructions[i]
                if (instruction.opcode.name == "CONST_4" &&
                    instruction is NarrowLiteralInstruction &&
                    instruction.narrowLiteral == 0
                ) {
                    const4Index = i
                    break
                }
            }

            if (const4Index == -1) {
                throw PatchException("Could not find the CONST_4 with value 0 after 'is_pro_user'.")
            }

            // 4. Get the register of that CONST_4 instruction.
            val const4Instruction = instructions[const4Index] as OneRegisterInstruction
            val register = const4Instruction.registerA

            // 5. Replace it with CONST_4 register, 1 (true).
            method.replaceInstruction(
                const4Index,
                "const/4 v$register, 0x1"
            )
        }
    }
}
