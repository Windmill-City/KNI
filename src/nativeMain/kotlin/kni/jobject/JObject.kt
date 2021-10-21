package kni.jobject

import kni.JEnv
import kni.jobject.jclass.JClass
import kni.succeedOrThr
import kni.toBoolean
import kotlinx.cinterop.invoke
import native.jni.*

/**
 * Instance of [JavaVM] Object
 *
 * @param ref reference of the Object
 */
open class JObject(val ref: JRef) {
    /**
     * Get Object class
     */
    fun getClass(env: JEnv): JClass {
        return JClass(JRefLocal(env.nativeInf.GetObjectClass!!.invoke(env.internalEnv, ref.obj)!!))
    }

    /**
     * Test if the Object is an instance of specific class
     */
    fun isInstanceOf(env: JEnv, clazz: JClass): Boolean {
        return env.nativeInf.IsInstanceOf!!.invoke(env.internalEnv, ref.obj, clazz.ref.obj).toBoolean()
    }

    /**
     * Lock the Object for the calling thread
     *
     * If the Object is owned by another thread, the calling thread will wait until the monitor is released
     *
     * If the calling thread already owns the Object it increases the counter in the monitor
     */
    fun lock(env: JEnv): Boolean {
        return env.nativeInf.MonitorEnter!!.invoke(env.internalEnv, ref.obj) == JNI_OK
    }

    /**
     * Unlock the Object for the calling thread
     *
     * @throws kni.JNIError if current thread does not own the monitor
     */
    fun unlock(env: JEnv) {
        return env.nativeInf.MonitorExit!!.invoke(env.internalEnv, ref.obj).succeedOrThr("Unlocking obj")
    }

    /**
     * Lock scope
     * @see lock
     * @see unlock
     */
    fun <R> lock(env: JEnv, action: () -> R): R {
        lock(env)
        try {
            return action()
        } finally {
            unlock(env)
        }
    }
}