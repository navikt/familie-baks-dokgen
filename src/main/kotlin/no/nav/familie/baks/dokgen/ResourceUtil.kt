package no.nav.familie.baks.dokgen

import java.io.FileNotFoundException
import java.net.URL

internal object ResourceUtil {
    fun getTemplateContent(templateName: String): String {
        return getResource("/templates/$templateName/template.hbs").readText()
    }

    fun getSchemaJsonAsString(templateName: String): String {
        return getResource("/templates/$templateName/schema.json").readText()
    }

    fun getSchemaJsonAsString(format: DocFormat): String {
        return getResource("/formats/$format/schema.json").readText()
    }

    fun getHeader(format: DocFormat): String {
        return getResource("/formats/$format/header.html").readText()
    }

    fun getFooter(format: DocFormat): String {
        return getResource("/formats/$format/footer.html").readText()
    }

    fun getStylesheet(format: DocFormat): String {
        return getResource("/formats/$format/style.css").readText()
    }

    private fun getResource(location: String): URL {
        return this::class.java.getResource(location) ?: throw FileNotFoundException(location)
    }
}
