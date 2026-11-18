package su.prismaworld.acention

/** Права плагина (описаны в plugin.yml). */
object Permissions {
    /** Открыть меню /end. */
    const val END_GUI = "acention.end"
    /** Открывать и закрывать портал кнопками в меню. */
    const val END_MANAGE = "acention.end.manage"
    /** /end reload */
    const val RELOAD = "acention.reload"
    /** Проходить через закрытый портал. */
    const val PORTAL_BYPASS = "acention.portal.bypass"
}
