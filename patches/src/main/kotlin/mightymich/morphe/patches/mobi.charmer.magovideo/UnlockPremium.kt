package mightymich.morphe.patches.mobi.charmer.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

/**
 * Unlocks MagoVideo Premium / VIP.
 *
 * In 5.7.1 the billing/VIP helper is Lm2/l; (older builds used Lf2/l;).
 * Public checks:
 *   - a()Z  → VIP active (used via La8/c and La8/b.i())
 *   - m()Z  → secondary VIP flag (via La8/b.h())
 *
 * Also patches the UI facade methods that call La8/c;->a()Z / m()Z.
 *
 * Fingerprints use unique strings ("isVip=true") so short R8 class names
 * (m2/l, a8/b) can change without breaking the patch.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks MagoVideo premium / VIP features.",
    default = true
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // Class that logs isVip=true / isVip=false (billing VIP helper)
    val vipHelperClass = Fingerprint(
        strings = listOf("isVip=true", "isVip=false")
    )

    // a()Z – main isVip (short method calling another ()Z)
    val vipAFingerprint = Fingerprint(
        returnType = "Z",
        parameters = emptyList(),
        classFingerprint = vipHelperClass,
        custom = { method, _ ->
            method.name == "a" || run {
                val insns = method.implementation?.instructions ?: return@Fingerprint false
                insns.size in 3..10 &&
                    insns.any { it.toString().contains("invoke-") } &&
                    insns.any { it.toString().contains("move-result") } &&
                    !insns.any { it.toString().contains("iget-boolean") }
            }
        }
    )

    // m()Z – secondary flag (iget-boolean + return)
    val vipMFingerprint = Fingerprint(
        returnType = "Z",
        parameters = emptyList(),
        classFingerprint = vipHelperClass,
        custom = { method, _ ->
            method.name == "m" || run {
                val insns = method.implementation?.instructions ?: return@Fingerprint false
                insns.size <= 5 &&
                    insns.any { it.toString().contains("iget-boolean") }
            }
        }
    )

    // UI facade: methods that call La8/c;->a()Z
    val facadeIsVipFingerprint = Fingerprint(
        returnType = "Z",
        parameters = emptyList(),
        custom = { method, _ ->
            val insns = method.implementation?.instructions ?: return@Fingerprint false
            insns.any { it.toString().contains("La8/c;->a()Z") }
        }
    )

    // UI facade: methods that call La8/c;->m()Z
    val facadeSecondaryFingerprint = Fingerprint(
        returnType = "Z",
        parameters = emptyList(),
        custom = { method, _ ->
            val insns = method.implementation?.instructions ?: return@Fingerprint false
            insns.any { it.toString().contains("La8/c;->m()Z") }
        }
    )

    execute {
        val trueReturn = """
            const/4 v0, 0x1
            return v0
        """

        // 1. Core a()Z
        val aMethod = vipAFingerprint.method
            ?: throw PatchException(
                "Could not find VIP a()Z (class with isVip=true/false)."
            )
        aMethod.addInstructions(0, trueReturn)

        // 2. Core m()Z
        val mMethod = vipMFingerprint.method
            ?: throw PatchException(
                "Could not find VIP m()Z (class with isVip=true/false)."
            )
        mMethod.addInstructions(0, trueReturn)

        // 3. Facade i()Z-style (optional)
        try {
            facadeIsVipFingerprint.method?.addInstructions(0, trueReturn)
        } catch (_: Exception) {
            // optional
        }

        // 4. Facade h()Z-style (optional)
        try {
            facadeSecondaryFingerprint.method?.addInstructions(0, trueReturn)
        } catch (_: Exception) {
            // optional
        }
    }
}
