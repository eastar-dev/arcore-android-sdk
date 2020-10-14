package android.util

import org.junit.Test

class VersionTest {
    @Test
    fun case01() {
        assert(Version("1.0") == Version("1.0"))
    }

    @Test
    fun case02() {
        assert(Version("1.1") > Version("1.0"))
    }

    @Test
    fun case03() {
        assert(Version("1.1") >= Version("1.0"))
    }

    @Test
    fun case04() {
        assert(Version("1.1") < Version("1.2"))
    }

    @Test
    fun case05() {
        assert(Version("1.1") <= Version("1.2"))
    }

    @Test
    fun case06() {
        assert(Version("1.1.3") > Version("1.1.2"))
    }

    @Test
    fun case07() {
        assert(Version("1.1.3") >= Version("1.1.2"))
    }

    @Test
    fun case08() {
        assert(Version("1.2.0") > Version("1.1.2"))
    }

    @Test
    fun case09() {
        assert(Version("1.20.0") > Version("1.3"))
    }

    @Test
    fun case10() {
        assert(Version("1.20b.0") > Version("1.20.0"))
    }

    @Test
    fun case11() {
        assert(Version("1.20c.0") > Version("1.20b.0"))
    }

    @Test
    fun case12() {
        assert(Version("1.21c.0") > Version("1.20b.0"))
    }

    @Test
    fun case13() {
        assert(Version("1.21b.0") > Version("1.c.0"))
    }

    @Test
    fun case14() {
        assert(Version("1.c.0") > Version("1.b.0"))
    }

    @Test
    fun case15() {
        assert(Version("1.b.01") > Version("1.b.a00a"))
    }

    @Test
    fun case16() {
        assert(Version("1.b.01") == Version("1.b.01"))
    }

    @Test
    fun case17() {
        assert("1.b.01".version == "1.b.01".version)
    }
}