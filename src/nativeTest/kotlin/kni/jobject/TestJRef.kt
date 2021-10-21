package kni.jobject

import kni.TestVM
import kni.jobject.jclass.JClass
import native.jni.JNIGlobalRefType
import native.jni.JNIWeakGlobalRefType
import kotlin.test.Test
import kotlin.test.assertEquals

class TestJRef {
    @Test
    fun testGlobalRef() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    strClz.ref.getGlobalRef(this)
                }!!.apply {
                    assertEquals(JNIGlobalRefType, getObjRefType(this@useEnv))
                }
            }
        }
    }

    @Test
    fun testWeakRef() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    strClz.ref.getWeakRef(this)
                }!!.apply {
                    assertEquals(JNIWeakGlobalRefType, getObjRefType(this@useEnv))

                    localFrame {
                        get(this@useEnv, false)!!
                        getOrFree(this@useEnv, true)!!
                    }
                }
            }
        }
    }
}