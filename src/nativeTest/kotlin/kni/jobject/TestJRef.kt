package kni.jobject

import kni.TestVM
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
                    val strClz = findClass("java/lang/String")
                    strClz.ref.getGlobalRef()
                }!!.apply {
                    assertEquals(JNIGlobalRefType, getObjRefType())
                }
            }
        }
    }

    @Test
    fun testWeakRef() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = findClass("java/lang/String")
                    strClz.ref.getWeakRef()
                }!!.apply {
                    assertEquals(JNIWeakGlobalRefType, getObjRefType())

                    localFrame {
                        get(false)!!
                        getOrFree(true)!!
                    }
                }
            }
        }
    }
}