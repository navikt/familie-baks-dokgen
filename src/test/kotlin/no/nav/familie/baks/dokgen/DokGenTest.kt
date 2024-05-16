package no.nav.familie.baks.dokgen

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DokGenTest {

    @Test
    fun lagHtmlTilPdf() {
        val templateNavn = "soknad"
        val inputData = javaClass.classLoader.getResource("testdata1.json")!!.readText().let {
            ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
        }
        val forventetResultat = javaClass.classLoader.getResource("eksempel1.html")!!.readText()
        val faktiskResultat = DokGen().lagHtmlTilPdf(templateNavn, inputData)

        assertEquals(
            expected = forventetResultat.lines().joinToString(transform = String::trimEnd),
            actual = faktiskResultat.lines().joinToString(transform = String::trimEnd)
        )
    }
}