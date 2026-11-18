package su.prismaworld.acention.command

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import su.prismaworld.acention.Acention
import su.prismaworld.acention.Permissions
import su.prismaworld.acention.gui.EndPortalGui
import su.prismaworld.acention.portal.EndPortalManager

/**
 * `/end` — открыть меню портала в Энд.
 * `/end reload` — перезагрузить конфигурацию.
 */
class EndCommand(private val plugin: Acention, private val manager: EndPortalManager) : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val sender = source.sender
        val texts = plugin.settings.texts

        if (args.firstOrNull().equals(RELOAD, ignoreCase = true)) {
            if (!sender.hasPermission(Permissions.RELOAD)) {
                texts.send(sender, "messages.no-permission")
                return
            }
            plugin.loadSettings()
            EndPortalGui.refreshAll()
            plugin.settings.texts.send(sender, "messages.reloaded")
            return
        }

        if (!sender.hasPermission(Permissions.END_GUI)) {
            texts.send(sender, "messages.no-permission")
            return
        }
        val player = source.executor as? Player ?: sender as? Player
        if (player == null) {
            texts.send(sender, "messages.players-only")
            return
        }
        EndPortalGui(plugin, manager).open(player)
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        if (args.size > 1 || !source.sender.hasPermission(Permissions.RELOAD)) return emptyList()
        val typed = args.firstOrNull().orEmpty()
        return listOf(RELOAD).filter { it.startsWith(typed, ignoreCase = true) }
    }

    override fun canUse(sender: CommandSender): Boolean =
        sender.hasPermission(Permissions.END_GUI) || sender.hasPermission(Permissions.RELOAD)

    override fun permission(): String = Permissions.END_GUI

    private companion object {
        const val RELOAD = "reload"
    }
}
