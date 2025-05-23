package no.nav.familie.baks.dokgen.handlebars

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.io.IOException

internal interface CustomHelpers {
    /**
     * Allows using switch/case in hbs templates
     *
     * Syntax:
     *  {{#switch variable}}
     *      {{#case "SOME_VALUE"}}
     *          <p>Content that should display if variable=="SOME_VALUE"</p>
     *      {{/case}}
     *      {{#case "SOME_OTHER_VALUE"}}
     *          <p>Content that should display if variable=="SOME_OTHER_VALUE"</p>
     *      {{/case}}
     *  {{/switch}}
     */
    class SwitchHelper : Helper<Any> {
        @Throws(IOException::class)
        override fun apply(
            variable: Any,
            options: Options,
        ): Any? {
            val variabelNavn: MutableList<String> = ArrayList()
            val variabelVerdier: MutableList<Any> = ArrayList()
            variabelNavn.add("__condition_fulfilled")
            variabelVerdier.add(0)
            variabelNavn.add("__condition_variable")
            variabelVerdier.add(if (options.hash.isEmpty()) variable else options.hash)
            val ctx = Context.newBlockParamContext(options.context, variabelNavn, variabelVerdier)
            val resultat: String = options.fn.apply(ctx)

            val antall = ctx["__condition_fulfilled"] as Int
            if (Integer.valueOf(1) == antall) {
                return resultat
            }
            return null
        }
    }

    /**
     * @see SwitchHelper
     */
    class CaseHelper : Helper<Any> {
        @Throws(IOException::class)
        override fun apply(
            caseKonstant: Any,
            options: Options,
        ): Any? {
            val konstant = if (options.hash.isEmpty()) caseKonstant else options.hash
            val model = options.context.model() as MutableMap<String, Any>
            val conditionVariable = model["__condition_variable"]
            if (caseKonstant is Iterable<*>) {
                if ((caseKonstant as List<*>).contains(conditionVariable)) {
                    incrementConditionFulfilledCounter(model)
                    return options.fn()
                }
            } else if (konstant == conditionVariable) {
                incrementConditionFulfilledCounter(model)
                return options.fn()
            }
            return options.inverse()
        }

        private fun incrementConditionFulfilledCounter(model: MutableMap<String, Any>) {
            var antall = model["__condition_fulfilled"] as Int
            model["__condition_fulfilled"] = ++antall
        }
    }

    /**
     * Allows to create a table with a set number of columns from only td cells
     * Useful if you have to render table cells but you don't know how many cells you will have.
     *
     * Example:
     * {{#table [columns=2] [class=""]]}}
     *      <td>This is one cell</td>
     *      <td>This is another cell</td>
     *      <td>This is a third cell</td>
     * {{/table}}
     *
     * will render a table with two tr rows with two cells in each
     *
     * +----------------------+----------------------+
     * | This is one cell     | This is another cell |
     * +----------------------+----------------------+
     * | This is a third cell |                      |
     * +----------------------+----------------------+
     *
     * Only supports td elements, if you want th you can give the header cells their own css classes.
     * You can also supply an optional class parameter to the helper which will be added to the table.
     *
     */
    class TableHelper : Helper<Any> {
        override fun apply(
            context: Any,
            options: Options,
        ): Any {
            val columnCount = options.hash<Int>("columns", 2)
            val tableContents = options.fn(context)
            val cells =
                tableContents
                    .trim()
                    .split("</td>")
                    .filter { it.isNotEmpty() }
                    .map { "$it</td>" }

            val wrappedInRows = mutableListOf("<tr>")
            cells.forEachIndexed { index, cell ->
                run {
                    if (index > 0 && index % columnCount == 0) {
                        wrappedInRows += "</tr><tr>"
                    }
                    wrappedInRows += cell
                }
            }

            if (cells.count() % columnCount > 0) {
                // If there are fewer cells than columns, the cells will stretch unless we do this
                val missingCellsInLastRow = columnCount - (cells.count() % columnCount)
                wrappedInRows += "<td></td>".repeat(missingCellsInLastRow)
            }

            wrappedInRows += "</tr>"

            val classParam = options.hash<String>("class", "")
            val classString = if (classParam.isNotEmpty()) "class=$classParam" else ""
            return "<table $classString>${wrappedInRows.joinToString("")}</table>"
        }
    }

    /**
     * Allows simple addition inside a template
     *
     * {{add 3 4}} prints 7 for example. Mostly useful when printing index
     * in loops. For example:
     * {{#each context.questions as | question |}}
     *     {{add @index 1}}. {{question.prompt}}
     * {{/each}}
     *
     * Will print every question prompt in an array of unknown size along with its index incremented by 1
     */
    class AdditionHelper : Helper<Int> {
        override fun apply(
            leftOperand: Int,
            options: Options,
        ): Any = leftOperand + options.param<Int>(0)
    }

    /**
     * Allows converting ISO8601 to be formatted as a norwegian dd.mm.yyyy string
     *
     * {{norwegian-date 2020-02-01}} prints 01.02.2020
     */
    class NorwegianDateHelper : Helper<String> {
        override fun apply(
            isoFormattedDate: String,
            options: Options,
        ): Any = isoFormattedDate.split('-').reversed().joinToString(separator = ".")
    }
}
