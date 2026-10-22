package no.nav.familie.baks.dokgen

import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DokGenMalerTest {
    private val dokGen = DokGen()

    @Test
    fun `ordinær barnetrygd viser søker og barn uten valgfrie EØS-opplysninger`() {
        val data =
            testdata("soknad/testdata/minimal-bokmal.json") +
                dokumentfelter("Ordinær barnetrygd") +
                mapOf("antallEøsSteg" to 0)

        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("soknad", data))

        assertEquals("Ordinær barnetrygd", dokument.selectFirst("#overskrift")?.text())
        assertEquals("01.01.2026", dokument.selectFirst("#dato")?.text())
        assertTrue(dokument.select("#content").text().contains("Om barna dine"))
        assertTrue(dokument.select("#content").text().contains("Barn Barnessen III"))
        assertFalse(dokument.select("#content").text().contains("Barnetrygd etter EØS-reglene"))
    }

    @Test
    fun `utvidet barnetrygd viser svar og utelater samboer når opplysningene mangler`() {
        val grunnlag = testdata("soknad-utvidet/testdata/minimal-engelsk.json")
        val barn =
            (grunnlag["barn"] as List<*>).map {
                (it as Map<*, *>) + ("registrertBostedType" to felt("IKKE_FYLT_INN", "Address"))
            }
        val data =
            grunnlag +
                dokumentfelter("Extended child benefit") +
                mapOf("barn" to barn, "antallEøsSteg" to 0)

        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("soknad-utvidet", data))
        val innhold = dokument.select("#content").text()

        assertEquals("Extended child benefit", dokument.selectFirst("#overskrift")?.text())
        assertEquals("01.01.2026", dokument.selectFirst("#dato")?.text())
        assertTrue(innhold.contains("Your life situation"))
        assertTrue(innhold.contains("I am married, but we have broken up"))
        assertTrue(innhold.contains("About your children"))
        assertFalse(innhold.contains("Former cohabitant 1"))
    }

    @Test
    fun `kontantstøtte viser barn og svar uten valgfrie opplysninger`() {
        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("kontantstotte-soknad", kontantstotteData()))
        val innhold = dokument.select("#content").text()

        assertEquals("Kontantstøtte", dokument.selectFirst("#overskrift")?.text())
        assertEquals("01.01.2026", dokument.selectFirst("#dato")?.text())
        assertTrue(innhold.contains("Om deg"))
        assertTrue(innhold.contains("Hvilke barn søker du for?"))
        assertTrue(innhold.contains("Testbarn"))
        assertTrue(innhold.contains("Bor barnet fast med deg? Ja"))
        assertTrue(dokument.select("h3.soknad-seksjon-undertittel").none { it.text() == "Barnets andre forelder" })
        assertTrue(dokument.select("h2.soknad-seksjon-tittel").none { it.text().contains("EØS-reglene") })
    }

    @Test
    fun `kontantstøtte viser valgfri annen forelder og EØS-seksjoner`() {
        val grunnlag = kontantstotteData()
        val barn =
            kontantstotteBarn() +
                mapOf(
                    "harEøsSteg" to true,
                    "andreForelder" to
                        mapOf(
                            "navn" to felt("Den andre forelderen"),
                            "fødselsdato" to felt("2021-09-03", "Fødselsdato"),
                        ),
                )
        val data =
            grunnlag +
                mapOf(
                    "barn" to listOf(barn),
                    "antallEøsSteg" to 1,
                    "søker" to (grunnlag["søker"] as Map<*, *>) + ("harEøsSteg" to true),
                )

        val dokument = Jsoup.parse(dokGen.lagHtmlTilPdf("kontantstotte-soknad", data))
        val innhold = dokument.select("#content").text()
        val overskrifter = dokument.select("#content h2.soknad-seksjon-tittel, #content h3.soknad-seksjon-undertittel")

        assertTrue(overskrifter.any { it.text() == "Barnets andre forelder" })
        assertTrue(innhold.contains("Den andre forelderen"))
        assertTrue(innhold.contains("03.09.2021"))
        assertTrue(overskrifter.any { it.text().contains("Kontantstøtte etter EØS-reglene - Om deg") })
        assertTrue(overskrifter.any { it.text().contains("Kontantstøtte etter EØS-reglene - Om barnet") })
    }

    private fun kontantstotteData(): Map<String, Any> =
        dokumentfelter("Kontantstøtte") +
            mapOf(
                "søker" to mapOf("navn" to felt("Testsøker"), "sivilstand" to felt("UGIFT", "Sivilstand")),
                "barn" to listOf(kontantstotteBarn()),
                "antallEøsSteg" to 0,
                "teksterTilPdf" to
                    mapOf(
                        "soeker" to "Søker",
                        "bekreftelsesboksBroedtekst" to "Bekreftelse",
                        "bekreftelsesboksErklaering" to "Jeg bekrefter opplysningene",
                        "omDegTittel" to "Om deg",
                        "dinLivssituasjonTittel" to "Din livssituasjon",
                        "velgBarnTittel" to "Hvilke barn søker du for?",
                        "omBarnaTittel" to "Om barna",
                        "omBarnetTittel" to "Om barnet",
                        "barnetsAndreForelder" to "Barnets andre forelder",
                        "bosted" to "Bosted",
                        "vedlegg" to "Vedlegg",
                        "ja" to "Ja",
                        "nei" to "Nei",
                        "eoesForSoekerTittel" to "Kontantstøtte etter EØS-reglene - Om deg",
                        "eoesForBarnTittel" to "Kontantstøtte etter EØS-reglene - Om barnet",
                    ),
            )

    private fun kontantstotteBarn(): Map<String, Any> =
        mapOf(
            "navn" to felt("Testbarn"),
            "registrertBostedType" to felt("IKKE_FYLT_INN", "Bosted"),
            "borFastMedSøker" to felt("JA", "Bor barnet fast med deg?"),
        )

    private fun felt(
        verdi: String,
        label: String = "Navn",
    ): Map<String, String> = mapOf("label" to label, "verdi" to verdi)

    private fun dokumentfelter(label: String): Map<String, Any> =
        mapOf(
            "fodselsnummer" to "00000000000",
            "navn" to "Testsøker",
            "dokumentDato" to "01.01.2026",
            "maalform" to "NB",
            "label" to label,
        )

    private fun testdata(path: String): Map<String, Any> {
        val json = requireNotNull(javaClass.getResource("/templates/$path")) { "Mangler testdata: $path" }.readText()
        @Suppress("UNCHECKED_CAST")
        return ObjectMapper().readValue(json, Map::class.java) as Map<String, Any>
    }
}
