package su.prismaworld.acention.portal

import java.time.Instant
import java.util.UUID

/**
 * Информация о последнем открытии портала.
 *
 * @property openerName имя открывшего или `null`, если его нельзя определить (например, предметы выбросил раздатчик).
 * @property openerId UUID открывшего, если это игрок.
 * @property openedAt когда портал был открыт.
 * @property expiresAt когда портал закрывается (или был закрыт досрочно).
 */
data class PortalOpening(
    val openerName: String?,
    val openerId: UUID?,
    val openedAt: Instant,
    val expiresAt: Instant,
) {
    fun isActive(now: Instant): Boolean = now.isBefore(expiresAt)
}
