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
    // 1. Fingerprint: szukamy metody, która zawiera string "purchase_buy__"
    //    NIE ograniczamy się do returnType, bo metoda e() zwraca void.
    val purchaseCheckFingerprint = Fingerprint(
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Pobieramy dopasowaną metodę
        val method = purchaseCheckFingerprint.match?.method
            ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

        // 3. Znajdujemy indeks instrukcji "move-result p0" (po getBoolean)
        //    i wstawiamy po niej "const/4 p0, 0x1", aby wymusić true.
        val instructions = method.implementation!!.instructions.toList()
        var insertIndex = -1
        
        for (i in instructions.indices) {
            val instr = instructions[i]
            // Szukamy instrukcji move-result, która następuje po getBoolean
            if (instr.opcode.name == "MOVE_RESULT" || instr.opcode.name == "MOVE_RESULT_OBJECT") {
                // Sprawdzamy, czy poprzednia instrukcja to invoke getBoolean
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

        // 4. Wstawiamy const/4 p0, 0x1 po move-result p0
        method.addInstructions(
            insertIndex,
            """
                const/4 p0, 0x1
            """
        )
    }
}// ========================================================
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
    // 1. Define the fingerprint using the standard Morphe DSL constructor.
    val purchaseCheckFingerprint = Fingerprint(
        returnType = "Z", // "Z" means boolean
        strings = listOf("purchase_buy__")
    )

    execute {
        // 2. Access the matched method via '.result?.method'.
        //    If the fingerprint fails to match, throw a clear exception.
        val method = purchaseCheckFingerprint.result?.method
            ?: throw PatchException("Could not find the purchase check method containing 'purchase_buy__'.")

        // 3. Insert instructions at the very beginning of the method:
        //      const/4 p0, 0x1  -> load 1 (true) into register p0 (this)
        //      return p0        -> return true immediately
        method.addInstructions(
            0,
            """
                const/4 p0, 0x1
                return p0
            """
        )
    }
}
