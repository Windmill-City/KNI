package kni

import kotlinx.cinterop.*
import native.jni.*
import platform.posix.snprintf

typealias InternalVM = CPointer<JavaVMVar>
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
 * Do action with Default VM Init Args
 *
 * @param version the version expect the VM to support, VM will return the actual supported
 * version in [JavaVMInitArgs]
 *
 * @throws Error on failure
 */
fun withVMInitArgs(version: JVersion = JNI_VERSION_10, action: (args: CValue<JavaVMInitArgs>) -> Unit) {
    val args = cValue<JavaVMInitArgs> {
        this.version = version
        JNI_GetDefaultJavaVMInitArgs(this.ptr).succeedOrThr("Getting Default VM Init Args")
    }
    action(args)
}

/**
 * Java VM - Share between threads within a process
 *
 * @param internalVM pointer to [JavaVMVar]
 * @param version JNI Version
 */
@Suppress("MemberVisibilityCanBePrivate")
class JavaVM(val internalVM: InternalVM, val version: JVersion) {
    val invokeInf: JNIInvokeInterface_ = internalVM.pointed.pointed!!

    companion object {
        /**
         * Create a [JavaVM] instance in the calling thread, which is considered the main thread
         *
         * @param args init arguments of the VM
         *
         * @throws Error on failure
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
     * The [destroy] function unloads a Java VM.
     *
     * The VM waits until the current thread is the only non-daemon user thread before it actually unloads.
     *
     * Even if you have destroyed the previous Java VM, you cannot recreate the Java VM on the same process,
     * this is by design!
     *
     * @throws Error on failure
     */
    fun destroy() {
        invokeInf.DestroyJavaVM!!.invoke(internalVM).succeedOrThr("Destroying VM")
    }

    /**
     * Attach current thread to VM
     *
     * The native thread remains attached VM until it calls [detachCurrentThread]
     *
     * @param args Attach argument, the name of it is in the format of Modified UTF-8
     *
     * @throws Error on failure
     */
    fun attachCurrentThread(args: JavaVMAttachArgs? = null, asDaemon: Boolean = false): JEnv {
        memScoped {
            val internalEnv: CPointerVar<JNIEnvVar> = this.alloc()
            (if (asDaemon) invokeInf.AttachCurrentThreadAsDaemon else invokeInf.AttachCurrentThread)!!.invoke(
                internalVM,
                internalEnv.ptr.reinterpret(),
                args?.ptr
            )
                .succeedOrThr("Attaching thread with version:${args?.version?.toJVerStr()}, VM version:${version.toJVerStr()}")
            return JEnv(internalEnv.value!!)
        }
    }

    /**
     * Detach current thread from VM
     *
     * A native thread attached to the VM must call [detachCurrentThread] to detach itself before exiting.
     * A thread cannot detach itself if there are Java methods on the call stack.
     *
     * @throws Error on failure
     */
    fun detachCurrentThread() {
        invokeInf.DetachCurrentThread!!.invoke(internalVM)
            .succeedOrThr("Detaching thread")
    }

    /**
     * Get [JEnv] for the calling thread
     *
     * @param version JNI version
     *
     * @throws Error on unsupported JNI version
     */
    fun getEnv(version: JVersion = this.version): JEnv? {
        memScoped {
            val internalEnv: CPointerVar<JNIEnvVar> = this.alloc()
            invokeInf.GetEnv!!.invoke(internalVM, internalEnv.ptr.reinterpret(), version)
                .ifFailed {
                    if (it == JNI_EDETACHED) return null
                    it.succeedOrThr("Getting JEnv with version:${version.toJVerStr()}, VM version:${this@JavaVM.version.toJVerStr()}")
                }
            return JEnv(internalEnv.value!!)
        }
    }

    /**
     * [JEnv] scope, get [JEnv] of current thread, if null, then attach to VM to get [JEnv],
     * and detach after returning
     *
     * @throws Error on failure
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