package android.util

import android.volley.toPair
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect2() {
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).toPair<kotlin.Pair<String, Any>>()
    }
}
