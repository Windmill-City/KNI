package kni

import kotlinx.cinterop.*
import native.jni.JNIEnvVar
import native.jni.JNINativeInterface_
import native.jni.JavaVMVar

typealias InternalEnv = CPointer<JNIEnvVar>

/**
 * Java Native Environment of specific thread
 *
 * The JNI interface pointer (JNIEnv) is valid only in the current thread.
 *
 * Should another thread need to access the Java VM, it must first call [JavaVM.attachCurrentThread]
 * to attach itself to the VM and obtain a JNI interface pointer.
 */
class JEnv(val internalEnv: InternalEnv) {
    val nativeInf: JNINativeInterface_ = internalEnv.pointed.pointed!!

    /**
     * Get the JNI Version of the interface
     */
    fun getVersion(): JVersion {
        return nativeInf.GetVersion!!.invoke(internalEnv)
    }

    /**
     * Get [JavaVM] of this [JEnv]
     */
    fun getJavaVM(): JavaVM {
        memScoped {
            val vm: CPointerVar<JavaVMVar> = this.alloc()
            nativeInf.GetJavaVM!!.invoke(internalEnv, vm.ptr).succeedOrThr("Getting JavaVM")
            return JavaVM(vm.value!!, getVersion())
        }
    }

    /**
     * Push in a local frame, local ref created in the frame will be freed after [popLocalFrame]
     *
     * @param capacity ensure at least given number of local refs can be created,
     * you can create more than it
     *
     * @throws OutOfMemoryError if system out of memory, also a pending exception in VM
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
     * @throws OutOfMemoryError if system out of memory, also a pending exception in VM
     */
    fun ensureLocalCapacity(capacity: Int) {
        nativeInf.EnsureLocalCapacity!!.invoke(internalEnv, capacity)
            .succeedOrThr("Ensure Capacity:$capacity")
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
     * VM doesn't aware the return of sub-calls, so if you get the object ref from VM in sub-calls,
     * the ref won't be freed until the root method returns to VM.
     *
     * To solve this problem, we push in a local frame before calling any sub-calls,
     * and then pop it up after returning, so that unused refs can be freed.
     *
     * @see JEnv.pushLocalFrame
     * @throws OutOfMemoryError when system runs out of memory
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