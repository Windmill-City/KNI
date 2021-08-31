package kni

import kotlinx.cinterop.cValue
import native.jni.JNI_VERSION_10
import native.jni.JavaVMInitArgs

object TestVM {
    val vm: JavaVM

    /**
     * Set the working path of the test task to where the jvm library locates, or cause failure in JavaVM.create.
     * The working path is configured in build.gradle.kts
     */
    init {
        lateinit var vm: JavaVM
        cValue<JavaVMInitArgs> {
            version = JNI_VERSION_10
            vm = JavaVM.create(this)
        }
        this.vm = vm
        println("JNI version of JavaVM:${vm.version.toJVerStr()}")
    }
}