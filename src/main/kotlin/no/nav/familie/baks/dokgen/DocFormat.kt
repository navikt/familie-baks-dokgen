package no.nav.familie.baks.dokgen

internal enum class DocFormat {
    PDF,
    HTML,
    EMAIL,
    ;

    override fun toString(): String = name.lowercase()
}
