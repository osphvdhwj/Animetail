package eu.kanade.domain.base

import android.content.Context
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller
import eu.kanade.tachiyomi.util.system.hasMiuiPackageInstaller
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class ExtensionInstallerPreference(
    private val context: Context,
    preferenceStore: PreferenceStore,
) : Preference<ExtensionInstaller> {

    private val basePref = preferenceStore.getEnum(key(), defaultValue())

    override fun key() = "extension_installer"

    val entries get() = ExtensionInstaller.entries.run {
        if (context.hasMiuiPackageInstaller) {
            filter { it != ExtensionInstaller.PACKAGEINSTALLER }
        } else {
            toList()
        }
    }

    override fun defaultValue(): ExtensionInstaller {
        return when {
            isRootAvailable -> ExtensionInstaller.ROOT
            context.hasMiuiPackageInstaller -> ExtensionInstaller.LEGACY
            else -> ExtensionInstaller.PACKAGEINSTALLER
        }
    }

    private val isRootAvailable: Boolean
        get() {
            val paths = System.getenv("PATH")?.split(":") ?: emptyList()
            for (path in paths) {
                if (java.io.File(path, "su").exists()) {
                    return true
                }
            }
            return java.io.File("/sbin/su").exists() ||
                   java.io.File("/system/bin/su").exists() ||
                   java.io.File("/system/xbin/su").exists() ||
                   java.io.File("/data/local/xbin/su").exists() ||
                   java.io.File("/data/local/bin/su").exists()
        }

    private fun check(value: ExtensionInstaller): ExtensionInstaller {
        when (value) {
            ExtensionInstaller.PACKAGEINSTALLER -> {
                if (context.hasMiuiPackageInstaller) return ExtensionInstaller.LEGACY
            }

            ExtensionInstaller.SHIZUKU -> {
                if (!context.isShizukuInstalled) return defaultValue()
            }

            else -> {}
        }
        return value
    }

    override fun get(): ExtensionInstaller {
        val value = basePref.get()
        val checkedValue = check(value)
        if (value != checkedValue) {
            basePref.set(checkedValue)
        }
        return checkedValue
    }

    override fun set(value: ExtensionInstaller) {
        basePref.set(check(value))
    }

    override fun isSet() = basePref.isSet()

    override fun delete() = basePref.delete()

    override fun changes() = basePref.changes()

    override fun stateIn(scope: CoroutineScope) = basePref.stateIn(scope)
}
