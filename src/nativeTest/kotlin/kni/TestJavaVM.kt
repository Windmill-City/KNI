package kni

import kotlin.test.Test
import kotlin.test.assertTrue

class TestJavaVM {
    @Test
    private fun testAttachAndDetach() {
        with(TestVM.vm) {
            attachCurrentThread()
            getEnv()!!.apply {
                println("JNI version of JEnv:${this.getVersion().toJVerStr()}")
            }
            detachCurrentThread()
            assertTrue { getEnv() == null }
        }
    }

    @Test
    private fun testUseEnv() {
        with(TestVM.vm) {
            useEnv {
                println("JNI version of JEnv:${getVersion().toJVerStr()}")
            }
            assertTrue { getEnv() == null }
        }
    }
}