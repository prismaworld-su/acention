package su.prismaworld.acention.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Запрещает забирать предметы из меню /end и обрабатывает нажатия на кнопки. */
class EndPortalGuiListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val gui = top.getHolder(false) as? EndPortalGui ?: return
        // Отменяем любые клики, включая shift-клик и сбор предметов из своего инвентаря.
        event.isCancelled = true
        if (event.clickedInventory != top) return
        val player = event.whoClicked as? Player ?: return
        gui.handleClick(player, event.slot)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.getHolder(false) is EndPortalGui) {
            event.isCancelled = true
        }
    }
}
