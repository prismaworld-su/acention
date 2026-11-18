package su.prismaworld.acention.config

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.configuration.ConfigurationSection

/**
 * Доступ к настраиваемым текстам (MiniMessage) из config.yml.
 * Во всех текстах доступен плейсхолдер `<prefix>`.
 */
class Texts(private val config: ConfigurationSection) {

    private val miniMessage = MiniMessage.miniMessage()
    private val prefix: TagResolver =
        Placeholder.component("prefix", miniMessage.deserialize(config.getString("messages.prefix").orEmpty()))

    /** Текст по пути [path] или `null`, если он пуст (сообщение отключено). */
    fun text(path: String, vararg placeholders: TagResolver): Component? {
        val raw = config.getString(path)
        if (raw.isNullOrEmpty()) return null
        return parse(raw, placeholders)
    }

    /** Список строк по пути [path] (для описаний предметов). */
    fun lines(path: String, vararg placeholders: TagResolver): List<Component> =
        config.getStringList(path).map { parse(it, placeholders) }

    /** Текст, который должен существовать всегда (заголовки, названия предметов). */
    fun required(path: String, vararg placeholders: TagResolver): Component =
        text(path, *placeholders) ?: Component.empty()

    /** Отправляет сообщение [path], если оно не отключено. */
    fun send(audience: Audience, path: String, vararg placeholders: TagResolver) {
        text(path, *placeholders)?.let(audience::sendMessage)
    }

    private fun parse(raw: String, placeholders: Array<out TagResolver>): Component =
        miniMessage.deserialize(raw, TagResolver.resolver(prefix, *placeholders))

    companion object {
        /** Убирает курсив, который клиент по умолчанию добавляет к названиям и описаниям предметов. */
        fun Component.withoutItalic(): Component = decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }
}
