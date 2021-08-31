package kni.jobject

import kni.JEnv
import kotlinx.cinterop.invoke

/**
 * Global/Weak Reference of [JObject]
 */
abstract class RefObj<T : JObject>(val obj: T) {
    /**
     * Free the reference
     */
    abstract fun free(env: JEnv)

    /**
     * Get the local ref of the object, or null if it has been freed
     *
     * @param autoFree should [free] the ref after [get]?
     */
    fun get(env: JEnv, autoFree: Boolean = false): T? {
        return env.nativeInf.NewLocalRef!!.invoke(env.internalEnv, obj.obj).apply { if (autoFree) free(env) }
            ?.let { with(obj) { newRefTo(it) } }
    }

    /**
     * Get the local ref of the object, if null, then free the ref
     *
     * @see get
     */
    fun getOrFree(env: JEnv, autoFree: Boolean = false): T? {
        return get(env, autoFree).apply { if (!autoFree && this == null) free(env) }
    }
}

/**
 * Global Reference of [JObject]
 */
class GlobalRefObj<T : JObject>(obj: T) : RefObj<T>(obj) {
    override fun free(env: JEnv) {
        env.nativeInf.DeleteGlobalRef!!.invoke(env.internalEnv, obj.obj)
    }
}

/**
 * Weak Reference of [JObject]
 */
class WeakRefObj<T : JObject>(obj: T) : RefObj<T>(obj) {
    override fun free(env: JEnv) {
        env.nativeInf.DeleteWeakGlobalRef!!.invoke(env.internalEnv, obj.obj)
    }
}

/**
 * Creates a new global ref to the [JObject], the [JObject] can be a global or local ref
 *
 * @throws OutOfMemoryError if system runs out of memory, also a pending exception in vm
 */
fun <T : JObject> T.getGlobalRef(env: JEnv): GlobalRefObj<T> {
    val jObj = env.nativeInf.NewGlobalRef!!.invoke(env.internalEnv, obj)
        ?: throw OutOfMemoryError("Creating Global Ref")
    return GlobalRefObj(newRefTo(jObj))
}

/**
 * Creates a weak ref to the [JObject], the [JObject] can be a global or local ref
 *
 * @throws OutOfMemoryError if system runs out of memory, also a pending exception in vm
 */
fun <T : JObject> T.getWeakRef(env: JEnv): WeakRefObj<T> {
    val jObj = env.nativeInf.NewWeakGlobalRef!!.invoke(env.internalEnv, obj)
        ?: throw OutOfMemoryError("Creating Weak Ref")
    return WeakRefObj(newRefTo(jObj))
}