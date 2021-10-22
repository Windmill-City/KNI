package kni.jobject.jclass

import kni.JEnv
import kni.jobject.JObject
import kni.jobject.JRefLocal
import kni.toBoolean
import kni.toJValues
import kni.tojboolean
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import native.jni.jmethodID

/**
 * Wrapper of [jmethodID]
 *
 * @param id [jmethodID] to wrap
 * @param name method name
 * @param dsc method descriptor
 * @param isStatic is method static?
 */
data class JMethod(
    val id: jmethodID, val name: String, val dsc: JDescriptor,
    override val isStatic: Boolean
) : IJClassMember {
    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified R> invoke(env: JEnv, obj: JObject, vararg args: Any?): R? {
        memScoped {
            return when (R::class) {
                Unit::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticVoidMethodA!! else env.nativeInf.CallVoidMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                JObject::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticObjectMethodA!! else env.nativeInf.CallObjectMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                        ?.let { JObject(JRefLocal(it)) }
                }
                Boolean::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticBooleanMethodA!! else env.nativeInf.CallBooleanMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this)).toBoolean()
                }
                Byte::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticByteMethodA!! else env.nativeInf.CallByteMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Char::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticCharMethodA!! else env.nativeInf.CallCharMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this)).toInt().toChar()
                }
                UShort::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticCharMethodA!! else env.nativeInf.CallCharMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Short::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticShortMethodA!! else env.nativeInf.CallShortMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Int::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticIntMethodA!! else env.nativeInf.CallIntMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Long::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticLongMethodA!! else env.nativeInf.CallLongMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Float::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticFloatMethodA!! else env.nativeInf.CallFloatMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                Double::class -> {
                    val fCall =
                        if (isStatic) env.nativeInf.CallStaticDoubleMethodA!! else env.nativeInf.CallDoubleMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, id, args.toJValues(this))
                }
                else -> throw IllegalArgumentException("Unsupported return type:${R::class.qualifiedName}")
            } as R?
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified R> invokeNonVirtual(env: JEnv, obj: JObject, clz: JClass, vararg args: Any?): R? {
        memScoped {
            return when (R::class) {
                Unit::class -> {
                    val fCall = env.nativeInf.CallNonvirtualVoidMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                JObject::class -> {
                    val fCall = env.nativeInf.CallNonvirtualObjectMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                        ?.let { JObject(JRefLocal(it)) }
                }
                Boolean::class -> {
                    val fCall = env.nativeInf.CallNonvirtualBooleanMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this)).toBoolean()
                }
                Byte::class -> {
                    val fCall = env.nativeInf.CallNonvirtualByteMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Byte::class -> {
                    val fCall = env.nativeInf.CallNonvirtualByteMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Char::class -> {
                    val fCall = env.nativeInf.CallNonvirtualCharMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this)).toInt().toChar()
                }
                UShort::class -> {
                    val fCall = env.nativeInf.CallNonvirtualCharMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Short::class -> {
                    val fCall = env.nativeInf.CallNonvirtualShortMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Int::class -> {
                    val fCall = env.nativeInf.CallNonvirtualIntMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Long::class -> {
                    val fCall = env.nativeInf.CallNonvirtualLongMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Float::class -> {
                    val fCall = env.nativeInf.CallNonvirtualFloatMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                Double::class -> {
                    val fCall = env.nativeInf.CallNonvirtualDoubleMethodA!!
                    fCall.invoke(env.internalEnv, obj.ref.obj, clz.ref.obj, id, args.toJValues(this))
                }
                else -> throw IllegalArgumentException("Unsupported return type:${R::class.qualifiedName}")
            } as R?
        }
    }

    /**
     * Convert a [IJClassMember] to a reflection object
     */
    override fun toReflected(env: JEnv, clz: JClass): JObject {
        return JObject(
            JRefLocal(
                env.nativeInf.ToReflectedMethod!!.invoke(env.internalEnv, clz.ref.obj, id, isStatic.tojboolean())
                    ?: throw OutOfMemoryError("Converting JMethod to reflection object")
            )
        )
    }

    companion object {
        /**
         * Convert a reflection object to a [IJClassMember]
         */
        fun fromReflected(env: JEnv, obj: JObject, name: String, dsc: JDescriptor, isStatic: Boolean): IJClassMember {
            return JMethod(
                env.nativeInf.FromReflectedMethod!!.invoke(env.internalEnv, obj.ref.obj)!!,
                name,
                dsc,
                isStatic
            )
        }
    }
}