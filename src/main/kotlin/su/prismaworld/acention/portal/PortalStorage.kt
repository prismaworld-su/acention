package su.prismaworld.acention.portal

import org.bukkit.Material
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/** Состояние портала, которое переживает перезапуск сервера. */
class PortalData(
    var lastOpening: PortalOpening? = null,
    /** Сколько предметов каждого типа уже брошено в закрытый портал. */
    val progress: MutableMap<Material, Int> = LinkedHashMap(),
)

/** Чтение и запись состояния портала в data.yml. */
class PortalStorage(private val file: File, private val logger: Logger) {

    fun load(): PortalData {
        if (!file.exists()) return PortalData()

        val yaml = YamlConfiguration()
        try {
            yaml.load(file)
        } catch (e: IOException) {
            return recoverFromBrokenFile(e)
        } catch (e: InvalidConfigurationException) {
            return recoverFromBrokenFile(e)
        }

        val data = PortalData()
        yaml.getConfigurationSection("last-opening")?.let { section ->
            val openedAt = section.getLong("opened-at", -1)
            val expiresAt = section.getLong("expires-at", -1)
            if (openedAt >= 0 && expiresAt >= 0) {
                data.lastOpening = PortalOpening(
                    openerName = section.getString("player-name"),
                    openerId = section.getString("player-uuid")?.let(::parseUuid),
                    openedAt = Instant.ofEpochMilli(openedAt),
                    expiresAt = Instant.ofEpochMilli(expiresAt),
                )
            }
        }
        yaml.getConfigurationSection("progress")?.let { section ->
            for (name in section.getKeys(false)) {
                val material = Material.matchMaterial(name) ?: continue
                val amount = section.getInt(name)
                if (amount > 0) data.progress[material] = amount
            }
        }
        return data
    }

    fun save(data: PortalData) {
        val yaml = YamlConfiguration()
        data.lastOpening?.let { opening ->
            yaml.set("last-opening.player-name", opening.openerName)
            yaml.set("last-opening.player-uuid", opening.openerId?.toString())
            yaml.set("last-opening.opened-at", opening.openedAt.toEpochMilli())
            yaml.set("last-opening.expires-at", opening.expiresAt.toEpochMilli())
        }
        for ((material, amount) in data.progress) {
            yaml.set("progress.${material.name}", amount)
        }

        try {
            file.parentFile.mkdirs()
            // Запись через временный файл, чтобы сбой во время сохранения не испортил data.yml.
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(yaml.saveToString(), Charsets.UTF_8)
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Не удалось сохранить ${file.name}", e)
        }
    }

    private fun recoverFromBrokenFile(cause: Exception): PortalData {
        val backup = File(file.parentFile, "${file.name}.broken")
        logger.log(Level.SEVERE, "Не удалось прочитать ${file.name}, он переименован в ${backup.name}. Портал закрыт.", cause)
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Не удалось переименовать ${file.name}", e)
        }
        return PortalData()
    }

    private fun parseUuid(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
