package kni.jobject

import kni.JEnv
import kni.toBoolean
import kotlinx.cinterop.invoke
import native.jni.*

/**
 * Can be one of
 * [JNIInvalidRefType],
 * [JNILocalRefType],
 * [JNIGlobalRefType],
 * [JNIWeakGlobalRefType]
 */
typealias JObjectRefType = jobjectRefType

/**
 * Object reference of [kni.JavaVM]
 */
abstract class JRef(val obj: jobject) {
    /**
     * Free the reference
     */
    abstract fun free(env: JEnv)

    /**
     * Test if two reference refers to the same java Object
     */
    fun isSameObj(env: JEnv, ref: JRef?): Boolean {
        return env.nativeInf.IsSameObject!!.invoke(env.internalEnv, obj, ref?.obj).toBoolean()
    }

    /**
     * Get [JObjectRefType] of the reference
     */
    fun getObjRefType(env: JEnv): JObjectRefType {
        return env.nativeInf.GetObjectRefType!!.invoke(env.internalEnv, obj)
    }

    /**
     * Get [JRefGlobal] of the Object
     */
    fun getGlobalRef(env: JEnv): JRefGlobal? {
        return env.nativeInf.NewGlobalRef!!.invoke(env.internalEnv, obj)?.let { JRefGlobal(it) }
    }

    /**
     * Get [JRefWeak] of the Object
     */
    fun getWeakRef(env: JEnv): JRefWeak? {
        return env.nativeInf.NewWeakGlobalRef!!.invoke(env.internalEnv, obj)?.let { JRefWeak(it) }
    }
}

/**
 * Local reference
 *
 * Valid only in the current thread
 * Object referenced by a Local reference is considered a GC Root
 *
 * It will be freed when
 *  - the local frame of this ref has [kni.JEnv.popLocalFrame]
 *  - the JNI call has return to [kni.JavaVM]
 *  - the thread of this ref has [kni.JavaVM.detachCurrentThread]
 */
class JRefLocal(obj: jobject) : JRef(obj) {
    override fun free(env: JEnv) {
        env.nativeInf.DeleteLocalRef!!.invoke(env.internalEnv, obj)
    }
}

/**
 * Global Reference
 *
 * Can be used in different threads
 * Object referenced by a Global reference is considered a GC Root
 *
 * It won't be freed until you [free] it
 */
class JRefGlobal(obj: jobject) : JRef(obj) {
    override fun free(env: JEnv) {
        env.nativeInf.DeleteGlobalRef!!.invoke(env.internalEnv, obj)
    }
}

/**
 * Weak Reference
 *
 * Can be used in different threads
 * Object referenced by a Weak reference may be invalid in any time,
 * you should [get] a Local reference before use
 */
class JRefWeak(obj: jobject) : JRef(obj) {
    override fun free(env: JEnv) {
        env.nativeInf.DeleteWeakGlobalRef!!.invoke(env.internalEnv, obj)
    }

    /**
     * Get [JRefLocal] of the object, or null if it has been freed
     *
     * @param autoFree should [free] the ref after [get]?
     */
    fun get(env: JEnv, autoFree: Boolean = false): JRefLocal? {
        return env.nativeInf.NewLocalRef!!.invoke(env.internalEnv, obj).apply { if (autoFree) free(env) }
            ?.let { JRefLocal(it) }
    }

    /**
     * Get the [JRefLocal] of the object, or free this ref if the object has been freed
     * @see get
     */
    fun getOrFree(env: JEnv, autoFree: Boolean = false): JRefLocal? {
        return get(env, autoFree).apply { if (!autoFree && this == null) free(env) }
    }
}