package su.prismaworld.acention.portal

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

/** Поиск безопасного места рядом с порталом, куда можно вытолкнуть сущность. */
object PortalGeometry {

    private const val FRAME_SEARCH_RADIUS = 4

    /**
     * Ближайшая к [near] точка над рамкой портала, окружающей блок [portal],
     * или `null`, если рядом нет рамки со свободным местом над ней.
     */
    fun nearestFrameTop(portal: Block, near: Location): Location? {
        var best: Location? = null
        var bestDistance = Double.MAX_VALUE
        for (dx in -FRAME_SEARCH_RADIUS..FRAME_SEARCH_RADIUS) {
            for (dz in -FRAME_SEARCH_RADIUS..FRAME_SEARCH_RADIUS) {
                val frame = portal.getRelative(dx, 0, dz)
                if (frame.type != Material.END_PORTAL_FRAME) continue
                val above = frame.getRelative(BlockFace.UP)
                if (!above.isPassable || !above.getRelative(BlockFace.UP).isPassable) continue
                val top = frame.location.add(0.5, 1.0, 0.5)
                val distance = top.distanceSquared(near)
                if (distance < bestDistance) {
                    best = top
                    bestDistance = distance
                }
            }
        }
        return best
    }
}
