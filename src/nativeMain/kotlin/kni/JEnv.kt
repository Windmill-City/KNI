package kni

import kni.jobject.jclass.JClass
import kotlinx.cinterop.*
import native.jni.JNIEnvVar
import native.jni.JNINativeInterface_
import native.jni.JNINativeMethod
import native.jni.JavaVMVar

typealias InternalEnv = CPointer<JNIEnvVar>

/**
 * Java Native Environment
 *
 * The JNI interface pointer (JNIEnv) is valid only in the current thread.
 *
 * Should another thread need to access the Java VM, it must first call [JavaVM.attachCurrentThread]
 * to attach itself to the VM and obtain a JNI interface pointer.
 */
class JEnv(val internalEnv: InternalEnv) {
    val nativeInf: JNINativeInterface_ = internalEnv.pointed.pointed!!

    /**
     * Get the [JVersion]
     */
    fun getVersion(): JVersion {
        return nativeInf.GetVersion!!.invoke(internalEnv)
    }

    /**
     * Get [JavaVM]
     */
    fun getJavaVM(): JavaVM {
        memScoped {
            val vm: CPointerVar<JavaVMVar> = this.alloc()
            nativeInf.GetJavaVM!!.invoke(internalEnv, vm.ptr).succeedOrThr("Getting JavaVM")
            return JavaVM(vm.value!!, getVersion())
        }
    }

    /**
     * Push in a local frame, local ref created in the frame will be freed when [popLocalFrame]
     *
     * @param capacity ensure at least given number of local refs can be created
     *
     * @throws VMOutOfMemoryException if system out of memory, also a pending exception in VM
     */
    fun pushLocalFrame(capacity: Int = 0) {
        nativeInf.PushLocalFrame!!.invoke(internalEnv, capacity)
            .succeedOrThr("Pushing local frame:$capacity")
    }

    /**
     * Release current frame and free the local refs
     */
    fun popLocalFrame() {
        nativeInf.PopLocalFrame!!.invoke(internalEnv, null)
    }

    /**
     * Ensure at least given number of local refs can be created
     *
     * @throws VMOutOfMemoryException if system out of memory, also a pending exception in VM
     */
    fun ensureLocalCapacity(capacity: Int) {
        nativeInf.EnsureLocalCapacity!!.invoke(internalEnv, capacity)
            .succeedOrThr("Ensure Capacity:$capacity")
    }

    /**
     * Register native methods
     *
     * @param clz class to register method
     * @param methods array of [JNINativeMethod]
     * @param len array length of the [methods]
     */
    fun registerNatives(clz: JClass, methods: CPointer<JNINativeMethod>, len: Int) {
        nativeInf.RegisterNatives!!.invoke(internalEnv, clz.ref.obj, methods, len)
            .succeedOrThr("Registering native methods")
    }

    /**
     * Unregister native method
     *
     * @param clz class to unregister method
     */
    fun unregisterNatives(clz: JClass) {
        nativeInf.UnregisterNatives!!.invoke(internalEnv, clz.ref.obj).succeedOrThr("Unregistering native methods")
    }

    /**
     * Raises a fatal error and does not expect the VM to recover.
     * This function does not return.
     */
    fun fatalError(message: String) {
        memScoped {
            nativeInf.FatalError!!.invoke(internalEnv, message.cstr.getPointer(this))
        }
    }

    /**
     * Local frame scope
     *
     * @see JEnv.pushLocalFrame
     */
    inline fun <R> localFrame(capacity: Int = 0, action: () -> R): R {
        pushLocalFrame(capacity)
        return try {
            action()
        } finally {
            popLocalFrame()
        }
    }
}