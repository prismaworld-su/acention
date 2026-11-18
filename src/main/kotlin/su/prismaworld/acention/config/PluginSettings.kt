package su.prismaworld.acention.config

import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.file.FileConfiguration
import java.time.DateTimeException
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

/**
 * Снимок настроек из config.yml. Создаётся при запуске и при /end reload.
 * Некорректные значения не ломают плагин: в лог пишется предупреждение и берётся значение по умолчанию.
 */
class PluginSettings private constructor(
    /** На сколько открывается портал. */
    val openDuration: Duration,
    /** Предметы, необходимые для открытия портала (порядок как в конфиге). */
    val requirements: Map<Material, Int>,
    /** Звук открытия портала или `null`, если отключён. */
    val openSound: Sound?,
    val time: TimeFormat,
    val texts: Texts,
) {

    companion object {
        private const val DEFAULT_DURATION_SECONDS = 600L
        private const val DEFAULT_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss"

        fun load(config: FileConfiguration, logger: Logger): PluginSettings = PluginSettings(
            openDuration = loadDuration(config, logger),
            requirements = loadRequirements(config, logger),
            openSound = loadSound(config, logger),
            time = TimeFormat(
                formatter = loadDateFormatter(config, logger),
                hoursUnit = config.getString("time.units.hours").orEmpty(),
                minutesUnit = config.getString("time.units.minutes").orEmpty(),
                secondsUnit = config.getString("time.units.seconds").orEmpty(),
            ),
            texts = Texts(config),
        )

        private fun loadDuration(config: FileConfiguration, logger: Logger): Duration {
            val seconds = config.getLong("portal.open-duration", DEFAULT_DURATION_SECONDS)
            if (seconds <= 0) {
                logger.warning("portal.open-duration должен быть больше 0, используется $DEFAULT_DURATION_SECONDS.")
                return Duration.ofSeconds(DEFAULT_DURATION_SECONDS)
            }
            return Duration.ofSeconds(seconds)
        }

        private fun loadRequirements(config: FileConfiguration, logger: Logger): Map<Material, Int> {
            val section = config.getConfigurationSection("portal.required-items")
            val result = LinkedHashMap<Material, Int>()
            for (name in section?.getKeys(false).orEmpty()) {
                val material = Material.matchMaterial(name)
                if (material == null || material.isLegacy || material.isAir || !material.isItem) {
                    logger.warning("portal.required-items: неизвестный предмет '$name', пропущен.")
                    continue
                }
                val amount = section!!.getInt(name)
                if (amount <= 0) {
                    logger.warning("portal.required-items: количество для '$name' должно быть больше 0, пропущен.")
                    continue
                }
                result.merge(material, amount, Int::plus)
            }
            if (result.isEmpty()) {
                logger.warning("portal.required-items пуст: портал можно открыть только через меню /end.")
            }
            return result
        }

        private fun loadSound(config: FileConfiguration, logger: Logger): Sound? {
            if (!config.getBoolean("portal.open-sound.enabled", true)) return null
            val rawKey = config.getString("portal.open-sound.key").orEmpty()
            val key = try {
                Key.key(rawKey)
            } catch (_: InvalidKeyException) {
                logger.warning("portal.open-sound.key: некорректный ключ звука '$rawKey', звук отключён.")
                return null
            }
            // Звук может прийти из ресурспака, поэтому неизвестный ключ — только предупреждение.
            if (Registry.SOUND_EVENT.get(NamespacedKey(key.namespace(), key.value())) == null) {
                logger.warning("portal.open-sound.key: звук '$rawKey' не найден среди стандартных звуков игры.")
            }
            val volume = config.getDouble("portal.open-sound.volume", 1.0).toFloat().coerceAtLeast(0f)
            val pitch = config.getDouble("portal.open-sound.pitch", 1.0).toFloat().coerceIn(0.5f, 2f)
            return Sound.sound(key, Sound.Source.MASTER, volume, pitch)
        }

        private fun loadDateFormatter(config: FileConfiguration, logger: Logger): DateTimeFormatter {
            val zoneName = config.getString("time.zone").orEmpty().trim()
            val zone = if (zoneName.isEmpty()) {
                ZoneId.systemDefault()
            } else {
                try {
                    ZoneId.of(zoneName)
                } catch (_: DateTimeException) {
                    logger.warning("time.zone: неизвестный часовой пояс '$zoneName', используется часовой пояс сервера.")
                    ZoneId.systemDefault()
                }
            }
            val pattern = config.getString("time.format", DEFAULT_DATE_FORMAT)!!
            val formatter = try {
                DateTimeFormatter.ofPattern(pattern)
            } catch (_: IllegalArgumentException) {
                logger.warning("time.format: некорректный шаблон '$pattern', используется '$DEFAULT_DATE_FORMAT'.")
                DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)
            }
            return formatter.withZone(zone)
        }
    }
}
