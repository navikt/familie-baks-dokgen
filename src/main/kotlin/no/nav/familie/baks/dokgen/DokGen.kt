package no.nav.familie.baks.dokgen

import com.github.erosb.jsonsKema.JsonArray
import com.github.erosb.jsonsKema.JsonBoolean
import com.github.erosb.jsonsKema.JsonNull
import com.github.erosb.jsonsKema.JsonNumber
import com.github.erosb.jsonsKema.JsonObject
import com.github.erosb.jsonsKema.JsonString
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import no.nav.familie.baks.dokgen.DocFormat.PDF
import no.nav.familie.baks.dokgen.ResourceUtil.getFooter
import no.nav.familie.baks.dokgen.ResourceUtil.getHeader
import no.nav.familie.baks.dokgen.ResourceUtil.getSchemaJsonAsString
import no.nav.familie.baks.dokgen.ResourceUtil.getStylesheet
import no.nav.familie.baks.dokgen.ResourceUtil.getTemplateContent
import no.nav.familie.baks.dokgen.handlebars.CustomHelpers
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import kotlin.text.Charsets.UTF_8

class DokGen {
    private val handlebars: Handlebars = opprettHandlebarsKonfigurasjon()
    private val htmlRenderer: HtmlRenderer
    private val parser: Parser

    init {
        listOf(TablesExtension.create()).let {
            htmlRenderer = HtmlRenderer.builder().extensions(it).build()
            parser = Parser.builder().extensions(it).build()
        }
    }

    fun lagHtmlTilPdf(
        templateNavn: String,
        inputData: Map<String, Any>,
    ): String {
        val (template, data) = validerOgKompiler(templateNavn, inputData)

        val styledDocument =
            with(htmlRenderer) {
                val content = template.apply(data)
                Jsoup.parse("<div id=\"content\">${render(content.let(parser::parse))}</div>")
            }.apply {
                head().append("<meta charset=\"UTF-8\">")
                head().append("<style>" + getStylesheet(PDF) + "</style>")
            }

        val header = handlebars.compileInline(getHeader(PDF)).apply(data)
        val body = styledDocument.body()
        val footer = getFooter(PDF)

        body.prepend(header)
        body.append(footer)

        return styledDocument.html()
    }

    private fun validerOgKompiler(
        templateName: String,
        input: Map<String, Any>,
    ): Pair<Template, Context> {
        val validationSchemes =
            listOf(getSchemaJsonAsString(templateName), getSchemaJsonAsString(PDF)).map {
                SchemaLoader(it)
            }

        // Convert input Map to JSON using json-sKema types
        val inputJson = mapToJsonValue(input)

        for (schema in validationSchemes) {
            val validator: Validator = Validator.forSchema(schema.load())
            val validationResult = validator.validate(inputJson)

            if (validationResult is ValidationFailure) {
                throw IllegalArgumentException("Input validation failed: $validationResult")
            }
        }

        val template = handlebars.compileInline(getTemplateContent(templateName))
        val context = mapTilContext(input + Pair("templateName", templateName))

        return Pair(template, context)
    }

    private fun mapToJsonValue(value: Any?): JsonValue =
        when (value) {
            null -> {
                JsonNull()
            }

            is Boolean -> {
                JsonBoolean(value)
            }

            is Number -> {
                JsonNumber(value)
            }

            is String -> {
                JsonString(value)
            }

            is Map<*, *> -> {
                val properties = mutableMapOf<JsonString, JsonValue>()
                @Suppress("UNCHECKED_CAST")
                (value as Map<String, Any?>).forEach { (k, v) ->
                    properties[JsonString(k)] = mapToJsonValue(v)
                }
                JsonObject(properties)
            }

            is List<*> -> {
                val items = value.map { mapToJsonValue(it) }
                JsonArray(items)
            }

            else -> {
                JsonString(value.toString())
            }
        }

    private fun mapTilContext(data: Map<String, Any>): Context =
        Context
            .newBuilder(data)
            .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
            .build()

    private fun opprettHandlebarsKonfigurasjon(): Handlebars {
        val loader =
            ClassPathTemplateLoader().apply {
                charset = UTF_8
                prefix = "/templates/"
            }
        return Handlebars(loader).apply {
            registerHelper("eq", ConditionalHelpers.eq)
            registerHelper("neq", ConditionalHelpers.neq)
            registerHelper("gt", ConditionalHelpers.gt)
            registerHelper("gte", ConditionalHelpers.gte)
            registerHelper("lt", ConditionalHelpers.lt)
            registerHelper("lte", ConditionalHelpers.lte)
            registerHelper("and", ConditionalHelpers.and)
            registerHelper("or", ConditionalHelpers.or)
            registerHelper("not", ConditionalHelpers.not)
            registerHelper("switch", CustomHelpers.SwitchHelper())
            registerHelper("case", CustomHelpers.CaseHelper())
            registerHelper("table", CustomHelpers.TableHelper())
            registerHelper("add", CustomHelpers.AdditionHelper())
            registerHelper("norwegian-date", CustomHelpers.NorwegianDateHelper())
            registerHelpers(StringHelpers::class.java)
        }
    }
}
