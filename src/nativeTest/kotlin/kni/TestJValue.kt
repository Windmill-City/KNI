package kni

import kni.jobject.jclass.JClass
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import native.jni.JNI_FALSE
import native.jni.JNI_TRUE
import native.jni.jboolean
import native.jni.jvalue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TestJValue {
    @Test
    @Suppress("SpellCheckingInspection")
    fun testJBoolean() {
        val jtrue: jboolean = JNI_TRUE.toUByte()
        val jfalse: jboolean = JNI_FALSE.toUByte()

        assertTrue { jtrue.toBoolean() }
        assertTrue { !jfalse.toBoolean() }

        assertEquals(JNI_TRUE.toUByte(), true.tojboolean())
        assertEquals(JNI_FALSE.toUByte(), false.tojboolean())

        assertFails { 0xFF.toUByte().toBoolean() }
    }

    @Test
    fun testJValueConvert() {
        inline fun <reified T> checkConvert(value: T, cvt: (jvalue) -> T) {
            assertEquals(value, value.asJValue().useContents { cvt(this) })
        }

        checkConvert(true, jvalue::asBoolean)
        checkConvert(false, jvalue::asBoolean)
        checkConvert(0xEC.toByte(), jvalue::asByte)
        checkConvert('c', jvalue::asChar)
        checkConvert(0xE3E3.toUShort(), jvalue::asUShort)
        checkConvert(0xE3E3.toShort(), jvalue::asShort)
        checkConvert(0x0303_E3E3, jvalue::asInt)
        checkConvert(0x0303_E3E3_0303_E3E3L, jvalue::asLong)
        checkConvert(0.030303f, jvalue::asFloat)
        checkConvert(0.030303, jvalue::asDouble)

        TestVM.vm.useEnv {
            localFrame {
                val strClz = JClass.findClass(this, "java/lang/String")
                assertTrue { strClz.isSameObj(this, strClz.asJValue().useContents { asJObject<JClass>() }) }
            }
        }
    }

    @Test
    fun testAsJValuesOfNullArray() {
        arrayOfNulls<Any?>(16).apply {
            memScoped {
                val array = this@apply.toJValues(this)
                this@apply.onEachIndexed { i, it -> assertEquals(it, array[i].asJObject<JClass>()) }
            }
        }
    }

    @Test
    fun testAsJValuesOfNonNullArray() {
        arrayOf(0x0E0E, 0x0B0B).apply {
            memScoped {
                val array = this@apply.toJValues(this)
                this@apply.onEachIndexed { i, it -> assertEquals(it, array[i].asInt()) }
            }
        }
    }
}