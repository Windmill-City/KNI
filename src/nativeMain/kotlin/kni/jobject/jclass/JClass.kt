package kni.jobject.jclass

import kni.*
import kni.jobject.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import native.jni.JNI_ERR

/**
 * Instance of [kni.JavaVM] Class
 */
class JClass(ref: JRef) : JObject(ref) {
    /**
     * Get super class of this class
     *
     * @return
     *  - JClass - Super Class of this class
     *  - null - if this class is Object or an interface
     */
    fun getSuper(env: JEnv): JClass? {
        return env.nativeInf.GetSuperclass!!.invoke(env.internalEnv, ref.obj)
            ?.let { JClass(JRefLocal(it)) }
    }

    /**
     * Is this class assignable form [clz]
     */
    fun isAssignableFrom(env: JEnv, clz: JClass): Boolean {
        return env.nativeInf.IsAssignableFrom!!.invoke(env.internalEnv, ref.obj, clz.ref.obj).toBoolean()
    }

    /**
     * Alloc a new Java object, without invoking any of the constructors for the object
     *
     * This class must not be an array class
     *
     * @throws VMOutOfMemoryException if system runs out of memory, also a pending exception in VM
     */
    fun allocObj(env: JEnv): JObject {
        return env.nativeInf.AllocObject!!.invoke(env.internalEnv, ref.obj)?.let { JObject(JRefLocal(it)) }
            ?: throw VMOutOfMemoryException("Allocating VM object")
    }

    /**
     * Get field by name and descriptor
     *
     * @param static is static?
     * @param name field name
     * @param dsc field descriptor
     *
     * @throws VMException on failure
     */
    fun getField(env: JEnv, static: Boolean, name: String, dsc: JDescriptor): JField {
        val fGetField = if (static) env.nativeInf.GetStaticFieldID!! else env.nativeInf.GetFieldID!!
        return memScoped {
            fGetField.invoke(
                env.internalEnv,
                ref.obj,
                name.mutf8.getPointer(this),
                dsc.mutf8.getPointer(this)
            )?.let { JField(it, name, dsc, static) }
                ?: throw VMException("Getting ${if (static) "static" else "instance"} field by name:$name, sig:$dsc")
        }
    }

    /**
     * Get method by name and descriptor
     *
     * @param static is static?
     * @param name method name
     * @param dsc method descriptor
     *
     * @throws VMException on failure
     */
    fun getMethod(env: JEnv, static: Boolean, name: String, dsc: JDescriptor): JMethod {
        val fGetMethod = if (static) env.nativeInf.GetStaticMethodID!! else env.nativeInf.GetMethodID!!
        return memScoped {
            fGetMethod.invoke(
                env.internalEnv,
                ref.obj,
                name.mutf8.getPointer(this),
                dsc.mutf8.getPointer(this)
            )?.let { JMethod(it, name, dsc, static) }
                ?: throw VMException("Getting ${if (static) "static" else "instance"} method by name:$name, sig:$dsc")
        }
    }

    /**
     * Create a new obj by specific constructor of this class
     *
     * @param ctor constructor method of this class
     *
     * @throws VMException if construction failed
     */
    fun newObj(env: JEnv, ctor: JMethod, vararg args: Any?): JObject {
        memScoped {
            return JObject(
                JRefLocal(
                    env.nativeInf.NewObjectA!!.invoke(
                        env.internalEnv,
                        ref.obj,
                        ctor.id,
                        args.toJValues(this)
                    ) ?: throw VMException("Creating new obj with ctor:$ctor")
                )
            )
        }
    }

    /**
     * Throw a new exception to VM of this class with specific message
     *
     * @param message message of the exception
     *
     * @throws JNIError on failure
     */
    fun thrNew(env: JEnv, message: String?) {
        memScoped {
            env.nativeInf.ThrowNew!!.invoke(
                env.internalEnv,
                ref.obj,
                message?.mutf8?.getPointer(this)
            ).succeedOrThr("Throwing new exception to VM with message:$message")
        }
    }

    /**
     * Create a new object array of this type
     *
     * @param len array length
     * @param obj initial value, all the element are initially set to this value
     *
     * @throws VMException on failure
     */
    fun arrayOf(env: JEnv, len: Int, obj: JObject?): JArray {
        return JArray(
            JRefLocal(
                env.nativeInf.NewObjectArray!!.invoke(env.internalEnv, len, ref.obj, obj?.ref?.obj)
                    ?: throw VMException("Creating new obj array")
            )
        )
    }

    companion object {
        /**
         * Load a class from buffer of raw class data
         *
         * @param clzName class name of the class be defined,
         * @param loader a class loader
         * @param rawClzBuf buffer contains raw class data
         * @param bufLen buffer len
         *
         * @throws VMException on failure
         */
        fun loadClass(
            env: JEnv,
            clzName: String,
            loader: JObject,
            rawClzBuf: CPointer<ByteVar>,
            bufLen: Int
        ): JClass {
            memScoped {
                return env.nativeInf.DefineClass!!.invoke(
                    env.internalEnv,
                    clzName.mutf8.getPointer(this),
                    loader.ref.obj,
                    rawClzBuf,
                    bufLen
                )?.let { JClass(JRefLocal(it)) } ?: throw VMException("Loading class:$clzName")
            }
        }

        /**
         * Find class by the accosted classloader
         *
         * @param name full-qualified name of class or type signature of array
         *
         * @throws VMException on failure
         */
        fun findClass(env: JEnv, name: String): JClass {
            memScoped {
                return env.nativeInf.FindClass!!.invoke(
                    env.internalEnv,
                    name.mutf8.getPointer(this)
                )?.let { JClass(JRefLocal(it)) } ?: throw VMException("Finding class:$name")
            }
        }
    }
}