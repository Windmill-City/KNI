package kni.jobject.jclass

import kni.JEnv
import kni.jobject.JObject
import kni.jobject.JRefLocal
import kni.toBoolean
import kni.tojboolean
import kotlinx.cinterop.invoke
import native.jni.jfieldID

/**
 * Wrapper of [jfieldID]
 *
 * @param id [jfieldID] to wrap
 * @param name field name
 * @param dsc field descriptor
 * @param isStatic is field static?
 */
data class JField(
    val id: jfieldID, val name: String, val dsc: JDescriptor,
    override val isStatic: Boolean
) : IJClassMember {
    /**
     * Get field value for specific object
     *
     * @param T can be one of primitive types or [JObject]
     *
     * @throws IllegalArgumentException if requesting unsupported types
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified T> getValue(env: JEnv, obj: JObject): T? {
        return when (T::class) {
            Boolean::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticBooleanField!! else env.nativeInf.GetBooleanField!!
                fGetValue(env.internalEnv, obj.ref.obj, id).toBoolean()
            }
            Byte::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticByteField!! else env.nativeInf.GetByteField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            UShort::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticCharField!! else env.nativeInf.GetCharField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            Char::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticCharField!! else env.nativeInf.GetCharField!!
                fGetValue(env.internalEnv, obj.ref.obj, id).toInt().toChar()
            }
            Short::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticShortField!! else env.nativeInf.GetShortField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            Int::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticIntField!! else env.nativeInf.GetIntField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            Long::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticLongField!! else env.nativeInf.GetLongField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            Float::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticFloatField!! else env.nativeInf.GetFloatField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            Double::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticDoubleField!! else env.nativeInf.GetDoubleField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)
            }
            JObject::class -> {
                val fGetValue = if (isStatic) env.nativeInf.GetStaticObjectField!! else env.nativeInf.GetObjectField!!
                fGetValue(env.internalEnv, obj.ref.obj, id)?.let { JObject(JRefLocal(it)) }
            }
            else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
        } as T?
    }

    /**
     * Set field value for specific object
     *
     * @param T can be one of primitive types or [JObject]
     *
     * @throws IllegalArgumentException if value type is not supported
     */
    inline fun <reified T> setValue(env: JEnv, obj: JObject, value: T?) {
        when (T::class) {
            Boolean::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticBooleanField!! else env.nativeInf.SetBooleanField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, (value as Boolean).tojboolean())
            }
            Byte::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticByteField!! else env.nativeInf.SetByteField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Byte)
            }
            UShort::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticCharField!! else env.nativeInf.SetCharField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as UShort)
            }
            Char::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticCharField!! else env.nativeInf.SetCharField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, (value as Char).code.toUShort())
            }
            Short::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticShortField!! else env.nativeInf.SetShortField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Short)
            }
            Int::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticIntField!! else env.nativeInf.SetIntField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Int)
            }
            Long::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticLongField!! else env.nativeInf.SetLongField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Long)
            }
            Float::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticFloatField!! else env.nativeInf.SetFloatField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Float)
            }
            Double::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticDoubleField!! else env.nativeInf.SetDoubleField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value as Double)
            }
            JObject::class -> {
                val fSetValue = if (isStatic) env.nativeInf.SetStaticObjectField!! else env.nativeInf.SetObjectField!!
                fSetValue(env.internalEnv, obj.ref.obj, id, value?.let { (value as JObject).ref.obj })
            }
            else -> throw IllegalArgumentException("Unsupported type:${T::class.qualifiedName}")
        }
    }

    /**
     * Convert a [IJClassMember] to a reflection object
     */
    override fun toReflected(env: JEnv, clz: JClass): JObject {
        return JObject(
            JRefLocal(
                env.nativeInf.ToReflectedField!!.invoke(env.internalEnv, clz.ref.obj, id, isStatic.tojboolean())
                    ?: throw OutOfMemoryError("Converting JField to reflection object")
            )
        )
    }

    companion object {
        /**
         * Convert a reflection object to a [IJClassMember]
         */
        fun fromReflected(env: JEnv, obj: JObject, name: String, dsc: JDescriptor, isStatic: Boolean): IJClassMember {
            return JField(
                env.nativeInf.FromReflectedField!!.invoke(env.internalEnv, obj.ref.obj)!!,
                name,
                dsc,
                isStatic
            )
        }
    }
}