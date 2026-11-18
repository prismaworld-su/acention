package su.prismaworld.acention.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import su.prismaworld.acention.Acention
import su.prismaworld.acention.Permissions
import su.prismaworld.acention.config.Texts.Companion.withoutItalic
import su.prismaworld.acention.portal.EndPortalManager
import java.time.Duration
import java.time.Instant

/**
 * Меню /end: состояние портала, прогресс сбора предметов и кнопка открытия/закрытия.
 * Содержимое обновляется раз в секунду, пока меню открыто.
 */
class EndPortalGui(private val plugin: Acention, private val manager: EndPortalManager) : InventoryHolder {

    private enum class ButtonAction { OPEN, CLOSE }

    private val inventory: Inventory = Bukkit.createInventory(this, SIZE, plugin.settings.texts.required("gui.title"))

    /** Действие кнопки на момент последней отрисовки — защищает от нажатия на устаревшую кнопку. */
    private var buttonAction: ButtonAction? = null

    override fun getInventory(): Inventory = inventory

    fun open(player: Player) {
        render(player)
        player.openInventory(inventory)
    }

    fun handleClick(player: Player, slot: Int) {
        if (slot != BUTTON_SLOT) return
        val action = buttonAction ?: return
        val texts = plugin.settings.texts
        if (!player.hasPermission(Permissions.END_MANAGE)) {
            texts.send(player, "messages.no-permission")
            return
        }

        when (action) {
            ButtonAction.OPEN ->
                if (manager.isOpen) texts.send(player, "messages.already-open")
                else manager.open(player.name, player.uniqueId)
            ButtonAction.CLOSE ->
                if (!manager.close(player.name)) texts.send(player, "messages.already-closed")
        }
        render(player)
    }

    private fun render(viewer: Player) {
        val filler = filler()
        for (slot in 0 until SIZE) inventory.setItem(slot, filler)
        inventory.setItem(STATUS_SLOT, statusIcon())
        inventory.setItem(REQUIREMENTS_SLOT, requirementsIcon())

        buttonAction = when {
            !viewer.hasPermission(Permissions.END_MANAGE) -> null
            manager.isOpen -> ButtonAction.CLOSE
            else -> ButtonAction.OPEN
        }
        buttonIcon()?.let { inventory.setItem(BUTTON_SLOT, it) }
    }

    private fun statusIcon(): ItemStack {
        val texts = plugin.settings.texts
        val time = plugin.settings.time
        val opening = manager.lastOpening ?: return icon(
            Material.END_PORTAL_FRAME,
            texts.required("gui.status-never-opened.name"),
            texts.lines("gui.status-never-opened.lore"),
        )

        val now = Instant.now()
        val placeholders = arrayOf(
            Placeholder.unparsed("player", opening.openerName ?: manager.unknownPlayerName()),
            Placeholder.unparsed("opened_at", time.date(opening.openedAt)),
            Placeholder.unparsed("remaining", time.duration(Duration.between(now, opening.expiresAt))),
        )
        val path = if (opening.isActive(now)) "gui.status-open" else "gui.status-closed"
        return icon(
            if (opening.isActive(now)) Material.ENDER_EYE else Material.END_PORTAL_FRAME,
            texts.required("$path.name", *placeholders),
            texts.lines("$path.lore", *placeholders),
            glint = opening.isActive(now),
        )
    }

    private fun requirementsIcon(): ItemStack {
        val texts = plugin.settings.texts
        val requirements = plugin.settings.requirements
        val lore = ArrayList<Component>()
        if (requirements.isEmpty()) {
            texts.text("gui.requirements.empty")?.let(lore::add)
        } else {
            for ((material, required) in requirements) {
                val current = manager.progressOf(material)
                val path = if (current >= required) "gui.requirements.line-done" else "gui.requirements.line-pending"
                texts.text(
                    path,
                    Placeholder.component("item", Component.translatable(material)),
                    Placeholder.unparsed("current", current.toString()),
                    Placeholder.unparsed("required", required.toString()),
                )?.let(lore::add)
            }
            lore += texts.lines("gui.requirements.footer")
        }
        return icon(requirements.keys.firstOrNull() ?: Material.NETHER_STAR, texts.required("gui.requirements.name"), lore)
    }

    private fun buttonIcon(): ItemStack? {
        val texts = plugin.settings.texts
        val duration: TagResolver = Placeholder.unparsed("duration", plugin.settings.time.duration(plugin.settings.openDuration))
        return when (buttonAction) {
            ButtonAction.OPEN -> icon(
                Material.LIME_CONCRETE,
                texts.required("gui.open-button.name", duration),
                texts.lines("gui.open-button.lore", duration),
            )
            ButtonAction.CLOSE -> icon(
                Material.RED_CONCRETE,
                texts.required("gui.close-button.name"),
                texts.lines("gui.close-button.lore"),
            )
            null -> null
        }
    }

    private fun icon(material: Material, name: Component, lore: List<Component>, glint: Boolean = false): ItemStack =
        ItemStack.of(material).apply {
            editMeta { meta ->
                meta.displayName(name.withoutItalic())
                meta.lore(lore.map { it.withoutItalic() })
                if (glint) meta.setEnchantmentGlintOverride(true)
            }
        }

    private fun filler(): ItemStack =
        ItemStack.of(Material.BLACK_STAINED_GLASS_PANE).apply { editMeta { it.isHideTooltip = true } }

    companion object {
        private const val SIZE = 27
        private const val STATUS_SLOT = 11
        private const val REQUIREMENTS_SLOT = 13
        private const val BUTTON_SLOT = 15

        /** Перерисовывает меню у всех игроков, у которых оно сейчас открыто. */
        fun refreshAll() {
            for (player in Bukkit.getOnlinePlayers()) {
                val gui = player.openInventory.topInventory.getHolder(false) as? EndPortalGui ?: continue
                gui.render(player)
            }
        }
    }
}
