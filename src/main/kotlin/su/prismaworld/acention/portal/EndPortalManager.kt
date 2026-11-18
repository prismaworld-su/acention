package su.prismaworld.acention.portal

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import su.prismaworld.acention.Acention
import su.prismaworld.acention.config.PluginSettings
import java.time.Instant
import java.util.UUID

/**
 * Хранит состояние портала в Энд (открыт ли, кем и когда), принимает предметы-ключи
 * и закрывает портал по истечении времени.
 */
class EndPortalManager(private val plugin: Acention, private val storage: PortalStorage) {

    private val settings: PluginSettings get() = plugin.settings

    private var data = PortalData()
    /** Было ли объявлено об открытии портала; нужно, чтобы один раз сообщить о его закрытии. */
    private var openAnnounced = false
    private var task: BukkitTask? = null

    val lastOpening: PortalOpening? get() = data.lastOpening

    val isOpen: Boolean get() = data.lastOpening?.isActive(Instant.now()) == true

    fun start() {
        data = storage.load()
        openAnnounced = isOpen
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, TICK_PERIOD, TICK_PERIOD)
    }

    fun stop() {
        task?.cancel()
        task = null
        storage.save(data)
    }

    /** Сколько предметов [material] уже брошено в портал (не больше требуемого). */
    fun progressOf(material: Material): Int =
        minOf(data.progress[material] ?: 0, settings.requirements[material] ?: 0)

    /** Открывает портал от имени [openerName] на время из конфига. */
    fun open(openerName: String?, openerId: UUID?) {
        val now = Instant.now()
        data.lastOpening = PortalOpening(openerName, openerId, now, now.plus(settings.openDuration))
        openAnnounced = true
        storage.save(data)

        val texts = settings.texts
        texts.send(
            Bukkit.getServer(), "messages.portal-opened",
            Placeholder.unparsed("player", openerName ?: unknownPlayerName()),
            Placeholder.unparsed("duration", settings.time.duration(settings.openDuration)),
        )
        settings.openSound?.let { sound ->
            Bukkit.getOnlinePlayers().forEach { it.playSound(sound, Sound.Emitter.self()) }
        }
    }

    /** Досрочно закрывает портал. Возвращает `false`, если он и так закрыт. */
    fun close(closedBy: String): Boolean {
        val opening = data.lastOpening
        val now = Instant.now()
        if (opening == null || !opening.isActive(now)) return false

        data.lastOpening = opening.copy(expiresAt = now)
        openAnnounced = false
        storage.save(data)

        settings.texts.send(Bukkit.getServer(), "messages.portal-closed-by", Placeholder.unparsed("player", closedBy))
        return true
    }

    /**
     * Обрабатывает предмет, попавший в закрытый портал [portal]: забирает нужное количество
     * в счёт открытия, а лишнее выбрасывает обратно.
     */
    fun acceptOffering(item: Item, portal: Block) {
        // Событие приходит для каждого блока портала, которого касается предмет:
        // пропускаем уже поглощённые и уже выброшенные из портала предметы.
        if (!item.isValid || !item.boundingBox.overlaps(BoundingBox.of(portal))) return

        val stack = item.itemStack
        val material = stack.type
        val thrower = item.thrower?.let(Bukkit::getPlayer)
        val missing = (settings.requirements[material] ?: 0) - (data.progress[material] ?: 0)

        if (missing <= 0) {
            if (returnItem(item, thrower, portal)) {
                thrower?.let { settings.texts.send(it, "messages.item-not-needed") }
            }
            return
        }

        val accepted = minOf(missing, stack.amount)
        val rest = stack.amount - accepted
        data.progress[material] = (data.progress[material] ?: 0) + accepted
        if (rest > 0) {
            item.itemStack = stack.clone().apply { amount = rest }
            returnItem(item, thrower, portal)
        } else {
            item.remove()
        }
        portal.world.playSound(ACCEPT_SOUND, portal.x + 0.5, portal.y + 0.5, portal.z + 0.5)

        if (settings.requirements.all { (type, amount) -> (data.progress[type] ?: 0) >= amount }) {
            data.progress.clear()
            val openerId = item.thrower
            open(thrower?.name ?: openerId?.let { Bukkit.getOfflinePlayer(it).name }, openerId)
            return
        }

        storage.save(data)
        thrower?.let {
            settings.texts.send(
                it, "messages.item-accepted",
                Placeholder.component("item", Component.translatable(material)),
                Placeholder.unparsed("amount", accepted.toString()),
                Placeholder.component("requirements", missingRequirements()),
            )
        }
    }

    /** Список ещё не брошенных предметов для плейсхолдера `<requirements>`. */
    fun missingRequirements(): Component {
        val texts = settings.texts
        val entries = settings.requirements.mapNotNull { (material, required) ->
            val current = progressOf(material)
            if (current >= required) return@mapNotNull null
            texts.text(
                "messages.requirement-entry",
                Placeholder.component("item", Component.translatable(material)),
                Placeholder.unparsed("current", current.toString()),
                Placeholder.unparsed("required", required.toString()),
                Placeholder.unparsed("missing", (required - current).toString()),
            )
        }
        val separator = texts.text("messages.requirement-separator") ?: Component.empty()
        return Component.join(JoinConfiguration.separator(separator), entries)
    }

    fun unknownPlayerName(): String = settings.texts.text("messages.unknown-player")
        ?.let(PlainTextComponentSerializer.plainText()::serialize)
        ?: "?"

    private fun tick() {
        if (openAnnounced && !isOpen) {
            openAnnounced = false
            settings.texts.send(Bukkit.getServer(), "messages.portal-closed")
        }
    }

    /**
     * Выбрасывает предмет из портала: к бросившему его игроку, если он рядом,
     * иначе на ближайшую рамку портала. Возвращает `false`, если безопасного места не нашлось.
     */
    private fun returnItem(item: Item, thrower: Player?, portal: Block): Boolean {
        val target = thrower?.location?.takeIf {
            it.world == item.world &&
                it.distanceSquared(item.location) <= RETURN_TO_PLAYER_DISTANCE_SQUARED &&
                it.block.type != Material.END_PORTAL
        } ?: PortalGeometry.nearestFrameTop(portal, item.location) ?: return false

        item.velocity = Vector()
        item.teleport(target)
        item.pickupDelay = 0
        return true
    }

    private companion object {
        const val TICK_PERIOD = 20L
        const val RETURN_TO_PLAYER_DISTANCE_SQUARED = 16.0 * 16.0
        val ACCEPT_SOUND: Sound = Sound.sound(
            Key.key("block.end_portal_frame.fill"), Sound.Source.BLOCK, 1f, 1f,
        )
    }
}
