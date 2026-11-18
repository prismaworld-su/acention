package su.prismaworld.acention

import org.bukkit.plugin.java.JavaPlugin

class Acention : JavaPlugin() {

    companion object {
        lateinit var instance: Acention
            private set
    }

    fun loadSettings() {
        saveDefaultConfig()
        reloadConfig()
    }

    override fun onEnable() {
        instance = this

        loadSettings()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
