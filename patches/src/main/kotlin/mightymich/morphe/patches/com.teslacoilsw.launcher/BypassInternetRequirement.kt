package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private val removeInternetPermissionResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { dom ->
            val root = dom.documentElement

            val toRemove = mutableListOf<Element>()
            for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
                val nodes = root.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val e = nodes.item(i) as Element
                    if (e.getAttribute("android:name") == "android.permission.INTERNET") {
                        toRemove += e
                    }
                }
            }
            toRemove.forEach { it.parentNode.removeChild(it) }
        }
    }
}

@Suppress("unused")
val bypassInternetRequirementPatch = bytecodePatch(
    name = "Safe Remove Internet Permission (Experimental)",
    description = "Safely removes the INTERNET permission from Nova Launcher and stubs network checks to prevent search crashes. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)
    dependsOn(removeInternetPermissionResourcePatch)

    val isNetworkAvailableFingerprint = Fingerprint(
        name = "isNetworkAvailable",
        returnType = "Z"
    )

    val isConnectedFingerprint = Fingerprint(
        name = "isConnected",
        returnType = "Z"
    )

    execute {
        runCatching {
            isNetworkAvailableFingerprint.let { fingerprint ->
                val method = fingerprint.method
                    ?: throw PatchException("Could not find isNetworkAvailable.")
                method.addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """)
            }
        }

        runCatching {
            isConnectedFingerprint.let { fingerprint ->
                val method = fingerprint.method
                    ?: throw PatchException("Could not find isConnected.")
                method.addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """)
            }
        }
    }
}
