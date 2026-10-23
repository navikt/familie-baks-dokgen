package no.nav.familie.baks.dokgen

import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DokGenRegresjonTest {
    private val dokGen = DokGen()

    @Test
    fun `avviser manglende påkrevd dokumentfelt`() {
        val feil = assertFailsWith<IllegalArgumentException> { dokGen.lagHtmlTilPdf("regresjon", gyldigInput() - "navn") }

        assertContains(feil.message.orEmpty(), "Input validation failed")
        assertContains(feil.message.orEmpty(), "navn")
    }

    @Test
    fun `avviser fødselsnummer som ikke er elleve sifre`() {
        val feil =
            assertFailsWith<IllegalArgumentException> {
                dokGen.lagHtmlTilPdf("regresjon", gyldigInput() + ("fodselsnummer" to "1234567890X"))
            }

        assertContains(feil.message.orEmpty(), "Input validation failed")
        assertContains(feil.message.orEmpty(), "fodselsnummer")
    }

    @Test
    fun `handlebars velger riktig gren og formaterer dato og sum`() {
        val ja = Jsoup.parse(dokGen.lagHtmlTilPdf("regresjon", gyldigInput()))
        val nei = Jsoup.parse(dokGen.lagHtmlTilPdf("regresjon", gyldigInput() + ("valg" to "NEI")))

        assertContains(ja.select("#content").text(), "Valgt ja")
        assertFalse(ja.select("#content").text().contains("Valgt nei"))
        assertContains(nei.select("#content").text(), "Valgt nei")
        assertFalse(nei.select("#content").text().contains("Valgt ja"))
        assertContains(ja.select("#content").text(), "Dato: 03.09.2021")
        assertContains(ja.select("#content").text(), "Sum: 5")
    }

    @Test
    fun `table helper deler tre celler på to kolonner og fyller siste rad`() {
        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("regresjon", gyldigInput()))
        val rader = dokument.select("#content table:not(:has(th)) tr")

        assertEquals(2, rader.size)
        assertEquals(listOf("En", "To"), rader[0].select("td").map { it.text() })
        assertEquals(listOf("Tre", ""), rader[1].select("td").map { it.text() })
    }

    @Test
    fun `markdown tabell får kolonneoverskrifter og dataceller i HTML`() {
        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("regresjon", gyldigInput()))
        val tabell = dokument.selectFirst("#content table:has(th)")

        assertEquals(listOf("Navn", "Antall"), tabell?.select("th")?.map { it.text() })
        assertEquals(listOf("Test", "3"), tabell?.select("td")?.map { it.text() })
    }

    private fun gyldigInput(): Map<String, Any> =
        mapOf(
            "fodselsnummer" to "00000000000",
            "navn" to "Testsøker",
            "dokumentDato" to "01.01.2026",
            "maalform" to "NB",
            "valg" to "JA",
            "isoDato" to "2021-09-03",
        )
}
