package no.nav.familie.baks.dokgen

import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.fail

class DokGenTest {
    @Test
    fun lagHtmlTilPdf() {
        val inputData = lesJson("testdata1.json")
        val forventetResultat = lesTekst("eksempel1.html")

        val faktiskResultat = DokGen().lagHtmlTilPdf("soknad", inputData)

        assertLinesEqual(
            expected = forventetResultat,
            actual = faktiskResultat,
        )
    }

    @Test
    fun `utvidet barnetrygd gir HTML lik fasit`() {
        val inputData = lesJson("testdata-soknad-utvidet.json")
        val forventetResultat = lesTekst("eksempel-soknad-utvidet.html")

        val faktiskResultat = DokGen().lagHtmlTilPdf("soknad-utvidet", inputData)

        assertLinesEqual(
            expected = forventetResultat,
            actual = faktiskResultat,
        )
    }

    @Test
    fun `kontantstøtte gir HTML lik fasit`() {
        val inputData = lesJson("testdata-kontantstotte-soknad.json")
        val forventetResultat = lesTekst("eksempel-kontantstotte-soknad.html")

        val faktiskResultat = DokGen().lagHtmlTilPdf("kontantstotte-soknad", inputData)

        assertLinesEqual(
            expected = forventetResultat,
            actual = faktiskResultat,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun lesJson(sti: String): Map<String, Any> = ObjectMapper().readValue(lesTekst(sti), Map::class.java) as Map<String, Any>

    private fun lesTekst(sti: String): String =
        requireNotNull(javaClass.classLoader.getResource(sti)) {
            "Mangler testressurs: $sti"
        }.readText()

    private fun assertLinesEqual(
        expected: String,
        actual: String,
    ) {
        val expectedLines = expected.lines().map(String::trimEnd)
        val actualLines = actual.lines().map(String::trimEnd)

        val diff =
            buildString {
                val maxLines = maxOf(expectedLines.size, actualLines.size)
                var differences = 0
                for (i in 0 until maxLines) {
                    val exp = expectedLines.getOrNull(i)
                    val act = actualLines.getOrNull(i)
                    if (exp != act) {
                        differences++
                        appendLine("Line ${i + 1}:")
                        appendLine("  - ${exp ?: "<missing>"}")
                        appendLine("  + ${act ?: "<missing>"}")
                    }
                }
                if (differences > 0) {
                    insert(0, "HTML mismatch: $differences line(s) differ (- expected, + actual)\n\n")
                }
            }

        if (diff.isNotEmpty()) {
            fail(diff)
        }
    }
}
