package kni.jobject

import kni.TestVM
import kotlinx.cinterop.memScoped
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestJString {
    private val testStrings = listOf(
        "",
        "Test:\u0001 \u0002 \u007E \u007F",
        "Test:\u0000 \u0080 \u0081 \u07FE \u07FF",
        "Test:\u0800 \u0801 \uFFFF"
    )

    @Test
    fun testNewRefTo() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    with("Test".toJString(this)) {
                        newRefTo(this.obj)
                    }
                }
            }
        }
    }

    @Test
    fun testGetLen() {
        with(TestVM.vm) {
            useEnv {
                fun checkByteLenMU8(string: String) {
                    localFrame {
                        string.toJString(this).getByteLenMU8(this).apply {
                            assertEquals(string.mutf8.size - 1, this)
                        }
                    }
                }

                fun checkLenU16(string: String) {
                    localFrame {
                        string.toJString(this).getLen(this).apply {
                            assertEquals(string.length, this)
                        }
                    }
                }

                testStrings.apply {
                    onEach(::checkByteLenMU8)
                    onEach(::checkLenU16)
                }
            }
        }
    }

    @Test
    fun testGetRegion() {
        with(TestVM.vm) {
            useEnv {
                fun checkGetRegion(string: String, range: IntRange) {
                    memScoped {
                        localFrame {
                            val len = range.last - range.first + 1
                            string.toJString(this@useEnv)
                                .getRegion(this@useEnv, this, range.first, len)
                                .toKStringFromUtf16ByLen(len)
                                .apply { assertEquals(string.substring(range), this) }
                        }
                    }
                }

                fun checkGetRegionMU8(string: String, range: IntRange) {
                    memScoped {
                        localFrame {
                            val len = range.last - range.first + 1
                            string.toJString(this@useEnv)
                                .getRegionMU8(this@useEnv, this, range.first, len)
                                .toKStringFromMUtf8()
                                .apply { assertEquals(string.substring(range), this) }
                        }
                    }
                }

                @Suppress("EmptyRange")
                listOf(
                    "Test" to 0..3,
                    "Test" to 2..3,
                    "Test" to 1..2,
                    "Test" to 3..3,
                    "" to 0..-1
                ).onEach { (s, r) ->
                    checkGetRegion(s, r)
                    checkGetRegionMU8(s, r)
                }

                assertFails { checkGetRegion("Test", 5..5) }
                assertFails { checkGetRegionMU8("Test", 5..5) }
            }
        }
    }

    @Test
    fun testUseStr() {
        with(TestVM.vm) {
            useEnv {
                fun checkUseStr(string: String) {
                    localFrame {
                        string.toJString(this).useStr(this) { len, _ ->
                            assertEquals(string, toKStringFromUtf16ByLen(len))
                        }
                    }
                }

                fun checkUseStrMU8(string: String) {
                    localFrame {
                        string.toJString(this).useStrMU8(this) { _, _ ->
                            assertEquals(string, toKStringFromMUtf8())
                        }
                    }
                }

                testStrings.apply {
                    onEach(::checkUseStr)
                    onEach(::checkUseStrMU8)
                }
            }
        }
    }

    @Test
    fun testMUTF8String() {
        fun checkMUTF8(string: String, size: Int) {
            memScoped {
                with(string.mutf8) {
                    assertEquals(size, this.size)
                    getPointer(this@memScoped).toKStringFromMUtf8()
                        .apply { assertEquals(string, this) }
                }
            }
        }

        testStrings.onEachIndexed { i, s -> checkMUTF8(s, listOf(1, 13, 20, 17)[i]) }
    }

    @Test
    fun testJStringConvert() {
        with(TestVM.vm) {
            useEnv {
                fun checkJString(string: String) {
                    localFrame {
                        string.toJString(this).toKString(this)
                            .apply { assertEquals(string, this) }
                    }
                }

                fun checkJStringMU8(string: String) {
                    localFrame {
                        memScoped {
                            string.mutf8.getPointer(this).toJString(this@useEnv).toKString(this@useEnv)
                                .apply { assertEquals(string, this) }
                        }
                    }
                }

                testStrings.apply {
                    onEach(::checkJString)
                    onEach(::checkJStringMU8)
                }
            }
        }
    }
}