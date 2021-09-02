package kni.jobject

import kni.JEnv
import kni.toBoolean
import kotlinx.cinterop.*
import native.jni.*

/**
 * Release mode of primitive array, can be [JNI_OK], [JNI_COMMIT], [JNI_ABORT]
 */
typealias JArrReleaseMode = jint

/**
 * Wrapper of [jarray]
 */
class JArray(override val obj: jarray) : JObject(obj) {
    /**
     * Gets the same type of wrapper for the new [jobject]
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : JObject> T.newRefTo(obj: jobject): T {
        return JArray(obj) as T
    }

    /**
     * Get the length of the array
     */
    fun getLen(env: JEnv): Int {
        return env.nativeInf.GetArrayLength!!.invoke(env.internalEnv, obj)
    }

    /**
     * Get the object at [index]
     *
     * @param index index of the object
     */
    fun getAsObj(env: JEnv, index: Int): JObject? {
        return env.nativeInf.GetObjectArrayElement!!.invoke(env.internalEnv, obj, index)?.let {
            JObject(it)
        }
    }

    /**
     * Set the object at [index]
     *
     * @param index index of the object
     */
    fun setAsObj(env: JEnv, index: Int, obj: JObject?) {
        return env.nativeInf.SetObjectArrayElement!!.invoke(env.internalEnv, this.obj, index, obj?.obj)
    }

    /**
     * Use object at specific index and free it after use
     *
     * @param index object index
     */
    inline fun <R> useAsObj(env: JEnv, index: Int, action: (JObject?, Int) -> R): R {
        onEachAsObj(env, index..index) { obj, i ->
            return action(obj, i)
        }
        throw Error("Invalid index:$index")
    }

    /**
     * Do action on objects in specific range
     *
     * @param range operating range
     */
    inline fun onEachAsObj(env: JEnv, range: IntRange, action: (JObject?, Int) -> Unit) {
        for (i in range) {
            val obj = getAsObj(env, i)
            try {
                action(obj, i)
            } finally {
                obj?.free(env)
            }
        }
    }

    /**
     * Get primitive array in specific range
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> getRegionAs(
        env: JEnv,
        placement: NativePlacement,
        range: IntRange
    ): CPointer<TVar> {
        val len = range.last - range.first + 1
        val arr = placement.allocArray<TVar>(len)
        arr.usePinned {
            when (T::class) {
                jboolean::class -> env.nativeInf.GetBooleanArrayRegion!!
                jbyte::class -> env.nativeInf.GetByteArrayRegion!!
                jchar::class -> env.nativeInf.GetCharArrayRegion!!
                jshort::class -> env.nativeInf.GetShortArrayRegion!!
                jint::class -> env.nativeInf.GetIntArrayRegion!!
                jlong::class -> env.nativeInf.GetLongArrayRegion!!
                jfloat::class -> env.nativeInf.GetFloatArrayRegion!!
                jdouble::class -> env.nativeInf.GetDoubleArrayRegion!!
                else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
            }.apply {
                @Suppress("UNCHECKED_CAST")
                (this as CPointer<CFunction<(CPointer<JNIEnvVar>, jarray, Int, Int, CPointer<CPrimitiveVar>) -> Unit>>)
                    .invoke(env.internalEnv, obj, range.first, len, arr.reinterpret())
            }
        }
        return arr
    }

    /**
     * Set primitive array of specific range
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> setRegionAs(env: JEnv, range: IntRange, buf: CPointer<TVar>) {
        val len = range.last - range.first + 1
        buf.usePinned {
            when (T::class) {
                jboolean::class -> env.nativeInf.SetBooleanArrayRegion!!
                jbyte::class -> env.nativeInf.SetByteArrayRegion!!
                jchar::class -> env.nativeInf.SetCharArrayRegion!!
                jshort::class -> env.nativeInf.SetShortArrayRegion!!
                jint::class -> env.nativeInf.SetIntArrayRegion!!
                jlong::class -> env.nativeInf.SetLongArrayRegion!!
                jfloat::class -> env.nativeInf.SetFloatArrayRegion!!
                jdouble::class -> env.nativeInf.SetDoubleArrayRegion!!
                else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
            }.apply {
                @Suppress("UNCHECKED_CAST")
                (this as CPointer<CFunction<(CPointer<JNIEnvVar>, jarray, Int, Int, CPointer<CPrimitiveVar>) -> Unit>>)
                    .invoke(env.internalEnv, obj, range.first, len, buf.reinterpret())
            }
        }
    }

    /**
     * Use [JArray] as a primitive array
     *
     * @param T can be one of primitive types of VM
     *
     * @throws OutOfMemoryError if system runs out of memory, also a pending exception in VM
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> useAs(
        env: JEnv,
        action: CArrayPointer<TVar>.(Int, Boolean) -> JArrReleaseMode
    ) {
        when (T::class) {
            jboolean::class -> {
                env.nativeInf.GetBooleanArrayElements!! to
                        env.nativeInf.ReleaseBooleanArrayElements!!
            }
            jbyte::class -> {
                env.nativeInf.GetByteArrayElements!! to
                        env.nativeInf.ReleaseByteArrayElements!!
            }
            jchar::class -> {
                env.nativeInf.GetCharArrayElements!! to
                        env.nativeInf.ReleaseCharArrayElements!!
            }
            jshort::class -> {
                env.nativeInf.GetShortArrayElements!! to
                        env.nativeInf.ReleaseShortArrayElements!!
            }
            jint::class -> {
                env.nativeInf.GetIntArrayElements!! to
                        env.nativeInf.ReleaseIntArrayElements!!
            }
            jlong::class -> {
                env.nativeInf.GetLongArrayElements!! to
                        env.nativeInf.ReleaseLongArrayElements!!
            }
            jfloat::class -> {
                env.nativeInf.GetFloatArrayElements!! to
                        env.nativeInf.ReleaseFloatArrayElements!!
            }
            jdouble::class -> {
                env.nativeInf.GetDoubleArrayElements!! to
                        env.nativeInf.ReleaseDoubleArrayElements!!
            }
            else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
        }.apply {
            memScoped {
                val isCopied: jbooleanVar = this.alloc()
                @Suppress("UNCHECKED_CAST") val arr =
                    (first as CPointer<CFunction<(CPointer<JNIEnvVar>, jarray, CPointer<jbooleanVar>) -> CPointer<CPrimitiveVar>?>>)
                        .invoke(env.internalEnv, obj, isCopied.ptr)
                        ?: throw OutOfMemoryError("Using primitive array elements of type:${T::class.qualifiedName}")
                val mode =
                    try {
                        action(arr.reinterpret(), this@JArray.getLen(env), isCopied.value.toBoolean())
                    } finally {
                        JNI_ABORT
                    }
                @Suppress("UNCHECKED_CAST")
                (second as CPointer<CFunction<(CPointer<JNIEnvVar>, jarray, CPointer<CPrimitiveVar>, JArrReleaseMode) -> Unit>>)
                    .invoke(env.internalEnv, obj, arr, mode)
            }
        }
    }

    /**
     * Use primitive array in critical mode, and free it after use
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified TVar : CPrimitiveVar> useCriticalAs(
        env: JEnv,
        action: CPointer<TVar>.(Int, Boolean) -> JArrReleaseMode
    ) {
        val fGetArr = env.nativeInf.GetPrimitiveArrayCritical!!
        val fFreeArr = env.nativeInf.ReleasePrimitiveArrayCritical!!
        memScoped {
            val isCopied: jbooleanVar = this.alloc()
            val arr = fGetArr.invoke(env.internalEnv, obj, isCopied.ptr)
                ?: throw OutOfMemoryError("Using primitive array elements of type:${TVar::class.qualifiedName}")
            val mode =
                try {
                    action(arr.reinterpret(), this@JArray.getLen(env), isCopied.value.toBoolean())
                } finally {
                    JNI_ABORT
                }
            fFreeArr(env.internalEnv, obj, arr, mode)
        }
    }

    companion object {
        /**
         * Create a new array of primitive types
         *
         * @param T can be one of primitive types of VM
         *
         * @throws Error on failure, also a pending exception in VM
         */
        inline fun <reified T> arrayOf(env: JEnv, len: Int): JArray {
            return JArray(
                when (T::class) {
                    jboolean::class -> {
                        env.nativeInf.NewBooleanArray!!.invoke(env.internalEnv, len)
                    }
                    jbyte::class -> {
                        env.nativeInf.NewByteArray!!.invoke(env.internalEnv, len)
                    }
                    jchar::class -> {
                        env.nativeInf.NewCharArray!!.invoke(env.internalEnv, len)
                    }
                    jshort::class -> {
                        env.nativeInf.NewShortArray!!.invoke(env.internalEnv, len)
                    }
                    jint::class -> {
                        env.nativeInf.NewIntArray!!.invoke(env.internalEnv, len)
                    }
                    jlong::class -> {
                        env.nativeInf.NewLongArray!!.invoke(env.internalEnv, len)
                    }
                    jfloat::class -> {
                        env.nativeInf.NewFloatArray!!.invoke(env.internalEnv, len)
                    }
                    jdouble::class -> {
                        env.nativeInf.NewDoubleArray!!.invoke(env.internalEnv, len)
                    }
                    else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
                } ?: throw Error("Creating primitive array of type:${T::class.qualifiedName}")
            )
        }
    }
}

/**
 * Convert [JObject] to [JArray]
 */
fun JObject.asJArray(): JArray {
    return JArray(obj)
}