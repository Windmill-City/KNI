package kni.jobject

import kni.TestVM
import kni.jobject.jclass.JClass
import native.jni.JNIGlobalRefType
import native.jni.JNIWeakGlobalRefType
import kotlin.test.Test
import kotlin.test.assertEquals

class TestRefObj {
    @Test
    fun testGlobalObj() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    strClz.getGlobalRef(this)
                }.apply {
                    assertEquals(JNIGlobalRefType, this.obj.getObjRefType(this@useEnv))

                    localFrame {
                        get(this@useEnv, false)!!
                        getOrFree(this@useEnv, true)!!
                    }
                }
            }
        }
    }

    @Test
    fun testWeakObj() {
        with(TestVM.vm) {
            useEnv {
                localFrame {
                    val strClz = JClass.findClass(this, "java/lang/String")
                    strClz.getWeakRef(this)
                }.apply {
                    assertEquals(JNIWeakGlobalRefType, this.obj.getObjRefType(this@useEnv))

                    localFrame {
                        get(this@useEnv, false)!!
                        getOrFree(this@useEnv, true)!!
                    }
                }
            }
        }
    }
}