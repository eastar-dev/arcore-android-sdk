package android.util;

import org.junit.Test;

import dev.eastar.ktx.KtxTextKt;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class ConvertToKorean {
    @Test
    public void convertToKorean_952134() {
        assertEquals("구십오만이천백삼십사", KtxTextKt.getNumberText("952134"));
    }

    @Test
    public void convertToKorean_952104() {
        assertEquals("구십오만이천백사", KtxTextKt.getNumberText("952104"));
    }

    @Test
    public void convertToKorean_10000() {
        assertEquals("만", KtxTextKt.getNumberText("10000"));
    }

    @Test
    public void convertToKorean_10001() {
        assertEquals("만일", KtxTextKt.getNumberText("10001"));
    }

    @Test
    public void convertToKorean_1234() {
        assertEquals("천이백삼십사", KtxTextKt.getNumberText("1234"));
    }

    @Test
    public void convertToKorean_1() {
        assertEquals("일", KtxTextKt.getNumberText("1"));
    }

    @Test
    public void convertToKorean_0() {
        assertEquals("영", KtxTextKt.getNumberText("0"));
    }

    @Test
    public void convertToKorean_300000000000000000005() {
        assertEquals("해이천삼백사십오경육천칠백팔십구조백이십삼억사천오백육십칠만팔천구백일", KtxTextKt.getNumberText("123456789012345678901"));
    }

}