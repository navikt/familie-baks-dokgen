package no.nav.familie.baks.dokgen

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import no.nav.familie.baks.dokgen.DocFormat.PDF
import no.nav.familie.baks.dokgen.handlebars.CustomHelpers
import no.nav.familie.baks.dokgen.ResourceUtil.getStylesheet
import no.nav.familie.baks.dokgen.ResourceUtil.getFooter
import no.nav.familie.baks.dokgen.ResourceUtil.getHeader
import no.nav.familie.baks.dokgen.ResourceUtil.getSchemaJsonAsString
import no.nav.familie.baks.dokgen.ResourceUtil.getTemplateContent
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
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

    fun lagHtmlTilPdf(templateNavn: String, inputData: Map<String, Any>): String {
        val (template, data) = validerOgKompiler(templateNavn, inputData)

        val styledDocument = with(htmlRenderer) {
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

    private fun validerOgKompiler(templateName: String, input: Map<String, Any>): Pair<Template, Context> {
        val validationSchemes = listOf(getSchemaJsonAsString(templateName), getSchemaJsonAsString(PDF)).map {
            SchemaLoader.load(JSONObject(JSONTokener(it)))
        }
        for (schema in validationSchemes) {
            schema.validate(JSONObject(input))
        }
        val template = handlebars.compileInline(getTemplateContent(templateName))
        val context = mapTilContext(input + Pair("templateName", templateName))

        return Pair(template, context)
    }

    private fun mapTilContext(data: Map<String, Any>): Context {
        return Context
            .newBuilder(data)
            .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
            .build()
    }

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

