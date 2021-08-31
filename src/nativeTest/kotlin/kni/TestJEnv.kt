package kni

import kni.jobject.jclass.JClass
import native.jni.JNIInvalidRefType
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class TestJEnv {
    @Test
    fun testGetVersion() {
        with(TestVM.vm) {
            useEnv {
                assertEquals(this@with.version, getVersion())
            }
        }
    }

    @Test
    fun testGetJavaVM() {
        with(TestVM.vm) {
            useEnv {
                assertEquals(this@with.internalVM, this.getJavaVM().internalVM)
            }
        }
    }

    @Test
    fun testLocalFrame() {
        with(TestVM.vm) {
            useEnv {
                pushLocalFrame(10)
                ensureLocalCapacity(10)
                popLocalFrame()

                localFrame {
                    JClass.findClass(this, "java/lang/String")
                }.apply {
                    assertEquals(JNIInvalidRefType, getObjRefType(this@useEnv))
                }
            }
        }
    }

    @Ignore
    @Test
    fun testFatalError() {
        with(TestVM.vm) {
            useEnv {
                fatalError("Test")
            }
        }
    }
}