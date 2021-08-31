package kni

import native.jni.JNI_ERR
import native.jni.JNI_OK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TestJResult {
    @Test
    fun testSucceedAndFailed() {
        assertTrue { JNI_OK.succeed }
        assertTrue { JNI_ERR.succeed.not() }
    }

    @Test
    fun testIfSucceed() {
        var calls = 0
        JNI_OK.ifSucceed { calls++ }
        assertEquals(1, calls)
        JNI_ERR.ifSucceed { calls++ }
        assertEquals(1, calls)
    }

    @Test
    fun testIfFailed() {
        var calls = 0
        JNI_OK.ifFailed { calls++ }
        assertEquals(0, calls)
        JNI_ERR.ifFailed { calls++ }
        assertEquals(1, calls)
    }

    @Test
    fun testSucceedOrThr() {
        JNI_OK.succeedOrThr()
        assertFails { JNI_ERR.succeedOrThr("Test") }.message!!.contains("Test")
    }
}