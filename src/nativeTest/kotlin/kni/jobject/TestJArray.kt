package kni.jobject

import kni.TestVM
import kni.jobject.jclass.JClass
import kotlinx.cinterop.*
import native.jni.JNIInvalidRefType
import native.jni.JNI_OK
import native.jni.jbyte
import native.jni.jbyteVar
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestJArray {
    @Test
    fun testNewArr() {
        with(TestVM.vm) {
            useEnv {
                JArray.arrayOf<jbyte>(this, 10).ref.free(this)
            }
        }
    }

    @Test
    fun testGetLen() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val arr = JArray.arrayOf<jbyte>(this, 10)
                    assertEquals(10, arr.getLen(this))
                }
            }
        }
    }

    @Test
    fun testSetAndGetAsObj() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    val initialElement = "ObjArrayElement"
                    val eleToSet = "ObjArrayElementToSet"
                    strClz.arrayOf(this, 10, initialElement.toJString(this))
                        .apply {
                            fun checkStr(string: String, index: Int) {
                                assertEquals(string, JString(getRefAt(this@useEnv, index)!!).toKString(this@useEnv))
                            }
                            for (i in 0..9) checkStr(initialElement, i)

                            setRefAt(this@useEnv, 0, eleToSet.toJString(this@useEnv).ref)
                            checkStr(eleToSet, 0)

                            setRefAt(this@useEnv, 1, null)
                            assertEquals(null, getRefAt(this@useEnv, 1))
                        }
                }
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    strClz.arrayOf(this, 10, null)
                        .apply {
                            for (i in 0..9) assertEquals(null, getRefAt(this@useEnv, i))
                        }
                }
            }
        }
    }

    @Test
    fun testUseRefAt() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    val initialElement = "InitElement"
                    strClz.arrayOf(this, 10, initialElement.toJString(this))
                        .apply {
                            lateinit var ref: JRefLocal
                            useRefAt(this@useEnv, 0) { it, _ ->
                                ref = it!!
                                assertEquals(initialElement, JString(it).toKString(this@useEnv))
                            }
                            assertEquals(JNIInvalidRefType, ref.getObjRefType(this@useEnv))
                        }
                }
            }
        }
    }

    @Test
    fun testOnEachRef() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    val initialElement = "InitElement"
                    strClz.arrayOf(this, 10, initialElement.toJString(this))
                        .apply {
                            onEachRef(this@useEnv, 0..9) { it, _ ->
                                assertEquals(initialElement, JString(it!!).toKString(this@useEnv))
                            }
                        }
                }
            }
        }
    }

    @Test
    fun testSetAndGetRegion() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    memScoped {
                        JArray.arrayOf<jbyte>(this@useEnv, 10).apply {
                            val arrToSet: CPointer<jbyteVar> = this@memScoped.allocArray(10)
                            for (i in 0..9) arrToSet[i] = i.toByte()

                            setRegionOf<jbyte, jbyteVar>(this@useEnv, 0..9, arrToSet)

                            getRegionAs<jbyte, jbyteVar>(this@useEnv, this@memScoped, 0..9)
                                .apply {
                                    for (i in 0..9) assertEquals(i.toByte(), this[i])
                                }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testUseAs() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    JArray.arrayOf<jbyte>(this@useEnv, 10).apply {
                        useAs<jbyte, jbyteVar>(this@useEnv) { len, copied ->
                            assertEquals(10, len)
                            println("Copied:$copied")
                            this[0] = 1.toByte()
                            JNI_OK
                        }
                        assertFails {
                            useAs<jbyte, jbyteVar>(this@useEnv) { _, _ ->
                                this[1] = 2.toByte()
                                throw Error("Test")
                                @Suppress("UNREACHABLE_CODE") JNI_OK
                            }
                        }
                        useCriticalAs<jbyteVar>(this@useEnv) { len, copied ->
                            assertEquals(10, len)
                            println("Copied:$copied")
                            this[2] = 3.toByte()
                            JNI_OK
                        }
                        var expectVal by Delegates.notNull<Byte>()
                        assertFails {
                            useCriticalAs<jbyteVar>(this@useEnv) { _, copied ->
                                this[3] = 4.toByte()
                                expectVal = if (copied) 0 else 4
                                throw Error("Test")
                                @Suppress("UNREACHABLE_CODE") JNI_OK
                            }
                        }
                        memScoped {
                            getRegionAs<jbyte, jbyteVar>(this@useEnv, this, 0..9)
                                .apply {
                                    assertEquals(1, this[0], "Success UseAs")
                                    assertEquals(0, this[1], "Fail UseAs")
                                    assertEquals(3, this[2], "Success UseCriticalAs")
                                    assertEquals(expectVal, this[3], "Fail UseCriticalAs")
                                }
                        }
                    }
                }
            }
        }
    }
}