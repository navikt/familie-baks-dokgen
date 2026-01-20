package no.nav.familie.baks.dokgen

import java.io.FileNotFoundException
import java.net.URL

internal object ResourceUtil {
    fun getTemplateContent(templateName: String): String = getResource("/templates/$templateName/template.hbs").readText()

    fun getSchemaJsonAsString(templateName: String): String = getResource("/templates/$templateName/schema.json").readText()

    fun getSchemaJsonAsString(format: DocFormat): String = getResource("/formats/$format/schema.json").readText()

    fun getHeader(format: DocFormat): String = getResource("/formats/$format/header.html").readText()

    fun getFooter(format: DocFormat): String = getResource("/formats/$format/footer.html").readText()

    fun getStylesheet(format: DocFormat): String = getResource("/formats/$format/style.css").readText()

    private fun getResource(location: String): URL = this::class.java.getResource(location) ?: throw FileNotFoundException(location)

    fun getDraft07SchemaAsString(): String = getResource("/formats/draft-schema7.json").readText()
}
