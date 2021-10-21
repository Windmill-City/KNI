package kni

import kotlinx.cinterop.*
import native.jni.*
import platform.posix.snprintf

typealias InternalVM = CPointer<JavaVMVar>
/**
 * JNI Version
 */
typealias JVersion = jint

/**
 * Convert [JVersion] to hex String
 */
fun JVersion.toJVerStr(): String {
    memScoped {
        val jVerStr = this.allocArray<ByteVar>(11)
        snprintf(
            jVerStr,
            11,
            "0x%08x",
            this@toJVerStr
        )
        return jVerStr.toKStringFromUtf8()
    }
}

/**
 * Get Default VM Init Args
 *
 * @param version the version expect the VM to support, VM will return the actual supported
 * version in [JavaVMInitArgs]
 *
 * @throws JNIError on failure
 */
fun getVMInitArgs(version: JVersion): CValue<JavaVMInitArgs> {
    val args = cValue<JavaVMInitArgs> {
        this.version = version
        JNI_GetDefaultJavaVMInitArgs(this.ptr).succeedOrThr("Getting Default VM Init Args")
    }
    return args
}

/**
 * Java VM
 *
 * Share between threads within a process
 *
 * @param internalVM pointer to [native.jni.JavaVM]
 * @param envVer Default [JVersion] to retrieve [JEnv]
 */
@Suppress("MemberVisibilityCanBePrivate")
class JavaVM(val internalVM: InternalVM, val envVer: JVersion) {
    val invokeInf: JNIInvokeInterface_ = internalVM.pointed.pointed!!

    companion object {
        /**
         * Create a [JavaVM] instance in the calling thread, which is considered the main thread
         *
         * @param args init arguments of the VM
         *
         * @throws JNIError on failure
         */
        @Suppress("NAME_SHADOWING")
        fun create(args: JavaVMInitArgs): JavaVM {
            memScoped {
                val internalVM: CPointerVar<JavaVMVar> = this.alloc()
                val internalEnv: CPointerVar<JNIEnvVar> = this.alloc()
                JNI_CreateJavaVM(internalVM.ptr, internalEnv.ptr.reinterpret(), args.ptr).succeedOrThr("Creating VM")
                return JavaVM(internalVM.value!!, args.version)
            }
        }
    }

    /**
     * Unloads the Java VM.
     *
     * The VM waits until the current thread is the only non-daemon user thread before it actually unloads.
     *
     * Even if you have destroyed the previous Java VM, you cannot recreate the Java VM on the same process,
     * this is by design!
     *
     * @throws JNIError on failure
     */
    fun destroy() {
        invokeInf.DestroyJavaVM!!.invoke(internalVM).succeedOrThr("Destroying VM")
    }

    /**
     * Attach current thread to VM
     *
     * The native thread remains attached VM until it calls [detachCurrentThread]
     *
     * @param args Attach argument, the name of which is in the format of Modified UTF-8
     *
     * @throws JNIError on failure
     */
    fun attachCurrentThread(args: JavaVMAttachArgs? = null, asDaemon: Boolean = false): JEnv {
        memScoped {
            val internalEnv: CPointerVar<JNIEnvVar> = this.alloc()
            (if (asDaemon) invokeInf.AttachCurrentThreadAsDaemon else invokeInf.AttachCurrentThread)!!.invoke(
                internalVM,
                internalEnv.ptr.reinterpret(),
                args?.ptr
            )
                .succeedOrThr("Attaching thread with version:${args?.version?.toJVerStr()}, VM version:${envVer.toJVerStr()}")
            return JEnv(internalEnv.value!!)
        }
    }

    /**
     * Detach current thread from VM
     *
     * A native thread attached to the VM must call [detachCurrentThread] to detach itself before exiting.
     * A thread cannot detach itself if there are Java methods on the call stack.
     *
     * @throws JNIError on failure
     */
    fun detachCurrentThread() {
        invokeInf.DetachCurrentThread!!.invoke(internalVM).succeedOrThr("Detaching thread")
    }

    /**
     * Get [JEnv] for the calling thread
     *
     * @param version JNI version
     *
     * @throws JNIError on unsupported JNI version
     */
    fun getEnv(version: JVersion = this.envVer): JEnv? {
        memScoped {
            val internalEnv: CPointerVar<JNIEnvVar> = this.alloc()
            invokeInf.GetEnv!!.invoke(internalVM, internalEnv.ptr.reinterpret(), version)
                .ifFailed {
                    if (it == JNI_EDETACHED) return null
                    it.succeedOrThr("Getting JEnv with version:${version.toJVerStr()}, VM version:${this@JavaVM.envVer.toJVerStr()}")
                }
            return JEnv(internalEnv.value!!)
        }
    }

    /**
     * [JEnv] scope
     */
    inline fun <R> useEnv(
        args: JavaVMAttachArgs? = null,
        asDaemon: Boolean = false,
        action: JEnv.() -> R
    ): R {
        getEnv()?.apply { return action(this) }
        attachCurrentThread(args, asDaemon).apply {
            try {
                return action(this)
            } finally {
                detachCurrentThread()
            }
        }
    }
}