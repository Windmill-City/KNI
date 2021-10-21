package kni.jobject

import kni.JEnv
import kni.VMOutOfMemoryException
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.invoke

/**
 * Instance of Direct Buffer of [kni.JavaVM]
 */
class JDirectBuffer(ref: JRef) : JObject(ref) {
    /**
     * Get the starting address of the buffer
     *
     * @return null
     * - the memory region is undefined
     * - the [ref] is not a direct java.nio.Buffer
     * - JNI access to direct buffers is not supported by this VM
     */
    fun getAddress(env: JEnv): COpaquePointer? {
        return env.nativeInf.GetDirectBufferAddress!!.invoke(env.internalEnv, ref.obj)
    }

    /**
     * Get the capacity of the buffer
     *
     * @return -1
     *  - the [ref] is an unaligned view buffer and the processor architecture does not support unaligned access
     *  - the [ref] is not a direct java.nio.Buffer
     *  - JNI access to direct buffers is not supported by this VM
     */
    fun getCapacity(env: JEnv): Long {
        return env.nativeInf.GetDirectBufferCapacity!!.invoke(env.internalEnv, ref.obj)
    }

    companion object {
        /**
         * Create [JDirectBuffer]
         *
         * @throws VMOutOfMemoryException
         * @throws UnsupportedOperationException if VM does not support
         */
        fun create(env: JEnv, address: COpaquePointer, capacity: Long): JDirectBuffer {
            return JDirectBuffer(
                JRefLocal(
                    env.nativeInf.NewDirectByteBuffer!!.invoke(env.internalEnv, address, capacity)
                        ?: if (env.checkException()) throw VMOutOfMemoryException("Creating direct buffer")
                        else throw UnsupportedOperationException("This VM does not support JNI access to direct buffer")
                )
            )
        }
    }
}