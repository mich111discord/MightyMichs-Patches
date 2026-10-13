package mightymich.morphe.patches.com.teslacoilsw.launcher.UnlockPremium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import mightymich.morphe.patches.com.teslacoilsw.launcher.NovaLauncherCompatibility

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Enable Prime Features",
    description = "Unlocks Nova Launcher Prime functionality by forcing prime checks to return true. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)

    val primeUtilsFingerprint = Fingerprint(
        name = "isPrimeInstalled",
        returnType = "Z"
    )

    val primeUnlockedFingerprint = Fingerprint(
        name = "isPrimeUnlocked",
        returnType = "Z"
    )

    execute {
        runCatching {
            primeUtilsFingerprint.let { fingerprint ->
                val method = fingerprint.method
                    ?: throw PatchException("Could not find isPrimeInstalled.")
                method.addInstructions(0, """
                    const/4 v0, 0x1
                    return v0
                """)
            }
        }

        runCatching {
            primeUnlockedFingerprint.let { fingerprint ->
                val method = fingerprint.method
                    ?: throw PatchException("Could not find isPrimeUnlocked.")
                method.addInstructions(0, """
                    const/4 v0, 0x1
                    return v0
                """)
            }
        }
    }
}
