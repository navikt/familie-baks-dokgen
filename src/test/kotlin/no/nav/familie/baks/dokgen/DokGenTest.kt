package no.nav.familie.baks.dokgen

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.fail

class DokGenTest {
    @Test
    fun lagHtmlTilPdf() {
        val templateNavn = "soknad"
        val inputData =
            javaClass.classLoader.getResource("testdata1.json")!!.readText().let {
                ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
            }
        val forventetResultat = javaClass.classLoader.getResource("eksempel1.html")!!.readText()
        val faktiskResultat = DokGen().lagHtmlTilPdf(templateNavn, inputData)

        assertLinesEqual(
            expected = forventetResultat,
            actual = faktiskResultat,
        )
    }

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
