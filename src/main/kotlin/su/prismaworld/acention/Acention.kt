package su.prismaworld.acention

import org.bukkit.plugin.java.JavaPlugin
import su.prismaworld.acention.config.PluginSettings
import su.prismaworld.acention.portal.EndPortalListener
import su.prismaworld.acention.portal.EndPortalManager
import su.prismaworld.acention.portal.PortalStorage
import java.io.File

class Acention : JavaPlugin() {

    companion object {
        lateinit var instance: Acention
            private set
    }

    lateinit var settings: PluginSettings
        private set

    private var portalManager: EndPortalManager? = null

    fun loadSettings() {
        saveDefaultConfig()
        reloadConfig()
        settings = PluginSettings.load(config, logger)
    }

    override fun onEnable() {
        instance = this

        loadSettings()

        val manager = EndPortalManager(this, PortalStorage(File(dataFolder, "data.yml"), logger))
        manager.start()
        portalManager = manager

        server.pluginManager.registerEvents(EndPortalListener(this, manager), this)
    }

    override fun onDisable() {
        portalManager?.stop()
        portalManager = null
    }
}
