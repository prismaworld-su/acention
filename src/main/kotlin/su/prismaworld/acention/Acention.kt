package su.prismaworld.acention

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.acention.config.PluginSettings

class Acention : JavaPlugin() {

    companion object {
        lateinit var instance: Acention
            private set
    }

    lateinit var settings: PluginSettings
        private set

    fun loadSettings() {
        saveDefaultConfig()
        reloadConfig()
        settings = PluginSettings.load(config, logger)
    }

    override fun onEnable() {
        instance = this

        loadSettings()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
