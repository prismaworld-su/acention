package su.prismaworld.acention.portal

import io.papermc.paper.event.entity.EntityInsideBlockEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.PortalType
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import su.prismaworld.acention.Acention
import su.prismaworld.acention.Permissions
import java.util.UUID

/**
 * Не пускает в Энд через закрытый портал и принимает брошенные в него предметы-ключи.
 * Портал на выход из Энда не ограничивается.
 */
class EndPortalListener(private val plugin: Acention, private val manager: EndPortalManager) : Listener {

    /** Когда игроку последний раз показывалось сообщение о закрытом портале. */
    private val lastLockedNotice = HashMap<UUID, Long>()
    /** Игроки, которых уже запланировано вытолкнуть из портала. */
    private val pendingPushBack = HashSet<UUID>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityInsidePortal(event: EntityInsideBlockEvent) {
        val block = event.block
        if (block.type != Material.END_PORTAL || block.world.environment == World.Environment.THE_END) return
        val entity = event.entity
        if (manager.isOpen || canBypass(entity)) return

        // Отменяем действие блока портала: никакая сущность (игрок, моб, жемчуг, предмет) не попадёт в Энд.
        event.isCancelled = true
        when (entity) {
            is Item -> manager.acceptOffering(entity, block)
            is Player -> {
                showLockedNotice(entity)
                pushBack(entity, block)
            }
        }
    }

    // Дополнительная защита на случай, если перемещение через портал запущено в обход блока портала.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityPortal(event: EntityPortalEvent) {
        if (event.portalType == PortalType.ENDER && isLocked(event.from, event.entity)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerPortal(event: PlayerPortalEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL && isLocked(event.from, event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        lastLockedNotice.remove(event.player.uniqueId)
        pendingPushBack.remove(event.player.uniqueId)
    }

    private fun isLocked(from: Location, entity: Entity): Boolean =
        from.world.environment != World.Environment.THE_END && !manager.isOpen && !canBypass(entity)

    private fun canBypass(entity: Entity): Boolean =
        entity is Player && entity.hasPermission(Permissions.PORTAL_BYPASS)

    /**
     * Выталкивает игрока из закрытого портала на ближайшую рамку: под порталом в крепости
     * находится лава, и без этого игрок провалился бы в неё сквозь блоки портала.
     * Телепорт выполняется на следующем тике, чтобы не перемещать игрока посреди обработки его движения.
     */
    private fun pushBack(player: Player, portal: Block) {
        if (!pendingPushBack.add(player.uniqueId)) return
        val scheduled = player.scheduler.run(plugin, {
            pendingPushBack.remove(player.uniqueId)
            if (manager.isOpen) return@run
            val target = PortalGeometry.nearestFrameTop(portal, player.location) ?: return@run
            val current = player.location
            target.yaw = current.yaw
            target.pitch = current.pitch
            player.fallDistance = 0f
            player.teleport(target)
        }, { pendingPushBack.remove(player.uniqueId) })
        if (scheduled == null) pendingPushBack.remove(player.uniqueId)
    }

    private fun showLockedNotice(player: Player) {
        val now = System.currentTimeMillis()
        val last = lastLockedNotice[player.uniqueId]
        if (last != null && now - last < LOCKED_NOTICE_COOLDOWN_MS) return
        lastLockedNotice[player.uniqueId] = now

        val texts = plugin.settings.texts
        val message = if (plugin.settings.requirements.isEmpty()) {
            texts.text("messages.portal-locked-no-items")
        } else {
            texts.text("messages.portal-locked", Placeholder.component("requirements", manager.missingRequirements()))
        }
        message?.let(player::sendActionBar)
    }

    private companion object {
        const val LOCKED_NOTICE_COOLDOWN_MS = 1500L
    }
}
