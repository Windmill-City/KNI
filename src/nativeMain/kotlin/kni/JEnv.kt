@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package kni

import kni.jobject.*
import kni.jobject.jclass.*
import kotlinx.cinterop.*
import native.jni.JNIEnvVar
import native.jni.JNINativeInterface_
import native.jni.JNINativeMethod
import native.jni.JavaVMVar

typealias InternalEnv = CPointer<JNIEnvVar>

/**
 * Java Native Environment
 *
 * The JNI interface pointer (JNIEnv) is valid only in the current thread.
 *
 * Should another thread need to access the Java VM, it must first call [JavaVM.attachCurrentThread]
 * to attach itself to the VM and obtain a JNI interface pointer.
 */
class JEnv(val internalEnv: InternalEnv) {
    val nativeInf: JNINativeInterface_ = internalEnv.pointed.pointed!!

    /**
     * Get the [JVersion]
     */
    fun getVersion(): JVersion {
        return nativeInf.GetVersion!!.invoke(internalEnv)
    }

    /**
     * Get [JavaVM]
     */
    fun getJavaVM(): JavaVM {
        memScoped {
            val vm: CPointerVar<JavaVMVar> = this.alloc()
            nativeInf.GetJavaVM!!.invoke(internalEnv, vm.ptr).succeedOrThr("Getting JavaVM")
            return JavaVM(vm.value!!, getVersion())
        }
    }

    /**
     * Push in a local frame, local ref created in the frame will be freed when [popLocalFrame]
     *
     * @param capacity ensure at least given number of local refs can be created
     *
     * @throws VMOutOfMemoryException
     */
    fun pushLocalFrame(capacity: Int = 0) {
        nativeInf.PushLocalFrame!!.invoke(internalEnv, capacity)
            .succeedOrThr("Pushing local frame:$capacity")
    }

    /**
     * Release current frame and free the local refs
     */
    fun popLocalFrame() {
        nativeInf.PopLocalFrame!!.invoke(internalEnv, null)
    }

    /**
     * Ensure at least given number of local refs can be created
     *
     * @throws VMOutOfMemoryException
     */
    fun ensureLocalCapacity(capacity: Int) {
        nativeInf.EnsureLocalCapacity!!.invoke(internalEnv, capacity)
            .succeedOrThr("Ensure Capacity:$capacity")
    }

    /**
     * Register native methods
     *
     * @param clz class to register method
     * @param methods array of [JNINativeMethod]
     * @param len array length of the [methods]
     *
     * @throws JNIError on failure
     */
    fun registerNatives(clz: JClass, methods: CPointer<JNINativeMethod>, len: Int) {
        nativeInf.RegisterNatives!!.invoke(internalEnv, clz.ref.obj, methods, len)
            .succeedOrThr("Registering native methods")
    }

    /**
     * Unregister native method
     *
     * @param clz class to unregister method
     *
     * @throws JNIError on failure
     */
    fun unregisterNatives(clz: JClass) {
        nativeInf.UnregisterNatives!!.invoke(internalEnv, clz.ref.obj).succeedOrThr("Unregistering native methods")
    }

    /**
     * Raises a fatal error and does not expect the VM to recover.
     * This function does not return.
     */
    fun fatalError(message: String) {
        memScoped {
            nativeInf.FatalError!!.invoke(internalEnv, message.cstr.getPointer(this))
        }
    }

    /**
     * Local frame scope
     *
     * @see JEnv.pushLocalFrame
     */
    inline fun <R> localFrame(capacity: Int = 0, action: () -> R): R {
        pushLocalFrame(capacity)
        return try {
            action()
        } finally {
            popLocalFrame()
        }
    }

    /**
     * Throw an exception to the VM
     *
     * @throws Error on failure
     */
    fun JThrowable.thr() {
        this.thr(this@JEnv)
    }


    /**
     * Get the length in bytes of the String, in Modified UTF8 format
     */
    fun JString.getByteLenMU8(): Int {
        return this.getByteLenMU8(this@JEnv)
    }

    /**
     * Get the length of the String
     */
    fun JString.getLen(): Int {
        return this.getLen(this@JEnv)
    }

    /**
     * Get String by region, in Modified UTF-8 format
     *
     * @param start start index of the region, inclusive
     * @param len the length of string to get start from the start index
     */
    fun JString.getRegionMU8(placement: NativePlacement, start: Int, len: Int): CArrayPointer<ByteVar> {
        return this.getRegionMU8(this@JEnv, placement, start, len)
    }

    /**
     * Get String by region, in UTF-16 format
     *
     * @param start start index of the region, inclusive
     * @param len the length of string to get start from the start index
     */
    fun JString.getRegion(placement: NativePlacement, start: Int, len: Int): CArrayPointer<UShortVar> {
        return getRegion(this@JEnv, placement, start, len)
    }

    /**
     * Use String in Modified UTF-8 format
     *
     * @param action CPointer<ByteVar>.(len: Int, copied: Boolean), pointer points to the string, len is the length
     * in bytes of the Modified UTF-8 representation of the string, copied indicates if the string has copied
     *
     * @throws VMOutOfMemoryException
     */
    inline fun <R> JString.useStrMU8(action: CPointer<ByteVar>.(len: Int, copied: Boolean) -> R): R {
        return this.useStrMU8(this@JEnv, action)
    }

    /**
     * Use String in UTF-16 format
     *
     * @param critical if GetStringCritical
     * @param action CPointer<ByteVar>.(len: Int, copied: Boolean), pointer points to the string, len is the length
     * of the string, copied indicates if the string has copied
     *
     * @throws VMOutOfMemoryException
     */
    inline fun <R> JString.useStr(
        critical: Boolean = false,
        action: CPointer<UShortVar>.(len: Int, copied: Boolean) -> R
    ): R {
        return this.useStr(this@JEnv, critical, action)
    }

    /**
     * Convert [JString] to [String]
     */
    fun JString.toKString(): String {
        return this.toKString(this@JEnv)
    }

    /**
     * Convert Modified UTF-8 C string to [JString]
     *
     * @throws VMOutOfMemoryException
     */
    fun CPointer<ByteVar>.toJString(): JString {
        return this.toJString(this@JEnv)
    }

    /**
     * Convert UTF-16 C string to [JString]
     *
     * @param len UTF-16 String length
     *
     * @throws VMOutOfMemoryException
     */
    fun CPointer<UShortVar>.toJString(len: Int): JString {
        return this.toJString(this@JEnv, len)
    }

    /**
     * Convert [String] to [JString]
     */
    fun String.toJString(): JString {
        return this.toJString(this@JEnv)
    }

    /**
     * Free the reference
     */
    fun JRef.free() {
        this.free(this@JEnv)
    }

    /**
     * Test if two reference refers to the same java Object
     */
    fun JRef.isSameObj(ref: JRef?): Boolean {
        return this.isSameObj(this@JEnv, ref)
    }

    /**
     * Get [JObjectRefType] of the reference
     */
    fun JRef.getObjRefType(): JObjectRefType {
        return this.getObjRefType(this@JEnv)
    }

    /**
     * Get [JRefGlobal] of the Object
     */
    fun JRef.getGlobalRef(): JRefGlobal? {
        return this.getGlobalRef(this@JEnv)
    }

    /**
     * Get [JRefWeak] of the Object
     */
    fun JRef.getWeakRef(): JRefWeak? {
        return this.getWeakRef(this@JEnv)
    }

    /**
     * Get Object class
     */
    fun JObject.getClass(): JClass {
        return this.getClass(this@JEnv)
    }

    /**
     * Test if the Object is an instance of specific class
     */
    fun JObject.isInstanceOf(clazz: JClass): Boolean {
        return this.isInstanceOf(this@JEnv, clazz)
    }

    /**
     * Lock the Object for the calling thread
     *
     * If the Object is owned by another thread, the calling thread will wait until the monitor is released
     *
     * If the calling thread already owns the Object it increases the counter in the monitor
     */
    fun JObject.lock(): Boolean {
        return this.lock(this@JEnv)
    }

    /**
     * Unlock the Object for the calling thread
     *
     * @throws kni.JNIError if current thread does not own the monitor
     */
    fun JObject.unlock() {
        return this.unlock(this@JEnv)
    }

    /**
     * Lock scope
     * @see lock
     * @see unlock
     */
    fun <R> JObject.lock(action: () -> R): R {
        return this.lock(this@JEnv, action)
    }

    /**
     * Get the starting address of the buffer
     *
     * @return null
     * - the memory region is undefined
     * - the [JDirectBuffer.ref] is not a direct java.nio.Buffer
     * - JNI access to direct buffers is not supported by this VM
     */
    fun JDirectBuffer.getAddress(): COpaquePointer? {
        return this.getAddress(this@JEnv)
    }

    /**
     * Get the capacity of the buffer
     *
     * @return -1
     *  - the [JDirectBuffer.ref] is an unaligned view buffer and the processor architecture does not support unaligned access
     *  - the [JDirectBuffer.ref] is not a direct java.nio.Buffer
     *  - JNI access to direct buffers is not supported by this VM
     */
    fun JDirectBuffer.getCapacity(): Long {
        return this.getCapacity(this@JEnv)
    }

    /**
     * Create [JDirectBuffer]
     *
     * @throws VMOutOfMemoryException
     * @throws UnsupportedOperationException if VM does not support
     */
    fun newDirectBuffer(address: COpaquePointer, capacity: Long): JDirectBuffer {
        return JDirectBuffer.create(this@JEnv, address, capacity)
    }

    /**
     * Get the length of the array
     */
    fun JArray.getLen(): Int {
        return getLen(this@JEnv)
    }

    /**
     * Get Object reference at [index]
     *
     * @param index index of the Object
     * @return Object reference
     */
    fun JArray.getRefAt(index: Int): JRefLocal? {
        return this.getRefAt(this@JEnv, index)
    }

    /**
     * Set Object reference at [index]
     *
     * @param index index of the Object
     * @param ref Object reference
     */
    fun JArray.setRefAt(index: Int, ref: JRef?) {
        this.setRefAt(this@JEnv, index, ref)
    }

    /**
     * Use Object reference at specific index and free it after use
     *
     * @param index object index
     */
    inline fun <R> JArray.useRefAt(index: Int, action: (JRefLocal?, Int) -> R): R {
        return this.useRefAt(this@JEnv, index, action)
    }

    /**
     * Operate on each Object reference in specific range
     *
     * @param range operating range
     */
    inline fun JArray.onEachRef(range: IntRange, action: (JRefLocal?, Int) -> Unit) {
        return this.onEachRef(this@JEnv, range, action)
    }

    /**
     * Get primitive array in specific range
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> JArray.getRegionAs(
        placement: NativePlacement,
        range: IntRange
    ): CPointer<TVar> {
        return this.getRegionAs<T, TVar>(this@JEnv, placement, range)
    }

    /**
     * Set primitive array of specific range
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> JArray.setRegionOf(range: IntRange, buf: CPointer<TVar>) {
        return this.setRegionOf<T, TVar>(this@JEnv, range, buf)
    }

    /**
     * Use [JArray] as a primitive array
     *
     * @param T can be one of primitive types of VM
     *
     * @throws VMOutOfMemoryException
     * @throws IllegalArgumentException if not primitive types
     */
    inline fun <reified T, reified TVar : CPrimitiveVar> JArray.useAs(
        action: CArrayPointer<TVar>.(Int, Boolean) -> JArrReleaseMode
    ) {
        return this.useAs<T, TVar>(this@JEnv, action)
    }

    /**
     * Use primitive array in critical mode, and free it after use
     *
     * @throws VMOutOfMemoryException
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified TVar : CPrimitiveVar> JArray.useCriticalAs(
        action: CPointer<TVar>.(Int, Boolean) -> JArrReleaseMode
    ) {
        return this.useCriticalAs(this@JEnv, action)
    }

    /**
     * Create a new array of primitive types
     *
     * @param T can be one of primitive types of VM
     *
     * @throws VMOutOfMemoryException
     * @throws IllegalArgumentException if not primitive types
     */
    inline fun <reified T> arrayOf(len: Int): JArray {
        return JArray.arrayOf<T>(this@JEnv, len)
    }

    /**
     * Get super class of this class
     *
     * @return
     *  - JClass - Super Class of this class
     *  - null - if this class is Object or an interface
     */
    fun JClass.getSuper(): JClass? {
        return this.getSuper(this@JEnv)
    }

    /**
     * Is this class assignable form [clz]
     */
    fun JClass.isAssignableFrom(clz: JClass): Boolean {
        return this.isAssignableFrom(this@JEnv, clz)
    }

    /**
     * Alloc a new Java object, without invoking any of the constructors for the object
     *
     * This class must not be an array class
     *
     * @throws VMOutOfMemoryException if system runs out of memory, also a pending exception in VM
     */
    fun JClass.allocObj(): JObject {
        return this.allocObj(this@JEnv)
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
    fun JClass.getField(static: Boolean, name: String, dsc: JDescriptor): JField {
        return this.getField(this@JEnv, static, name, dsc)
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
    fun JClass.getMethod(static: Boolean, name: String, dsc: JDescriptor): JMethod {
        return this.getMethod(this@JEnv, static, name, dsc)
    }

    /**
     * Create a new obj by specific constructor of this class
     *
     * @param ctor constructor method of this class
     *
     * @throws VMException if construction failed
     */
    fun JClass.newObj(ctor: JMethod, vararg args: Any?): JObject {
        return this.newObj(this@JEnv, ctor, args)
    }

    /**
     * Throw a new exception to VM of this class with specific message
     *
     * @param message message of the exception
     *
     * @throws JNIError on failure
     */
    fun JClass.thrNew(message: String?) {
        this.thrNew(this@JEnv, message)
    }

    /**
     * Create a new object array of this type
     *
     * @param len array length
     * @param obj initial value, all the element are initially set to this value
     *
     * @throws VMException on failure
     */
    fun JClass.arrayOf(len: Int, obj: JObject?): JArray {
        return this.arrayOf(this@JEnv, len, obj)
    }

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
        clzName: String,
        loader: JObject,
        rawClzBuf: CPointer<ByteVar>,
        bufLen: Int
    ): JClass {
        return JClass.loadClass(this@JEnv, clzName, loader, rawClzBuf, bufLen)
    }

    /**
     * Find class by the accosted classloader
     *
     * @param name full-qualified name of class or type signature of array
     *
     * @throws VMException on failure
     */
    fun findClass(name: String): JClass {
        return JClass.findClass(this@JEnv, name)
    }

    /**
     * Get field value for specific object
     *
     * @param T can be one of primitive types or [JObject]
     *
     * @throws IllegalArgumentException if requesting unsupported types
     */
    inline fun <reified T> JField.getValue(obj: JObject): T? {
        return this.getValue<T>(this@JEnv, obj)
    }

    /**
     * Set field value for specific object
     *
     * @param T can be one of primitive types or [JObject]
     *
     * @throws IllegalArgumentException if value type is not supported
     */
    inline fun <reified T> JField.setValue(obj: JObject, value: T?) {
        this.setValue<T>(this@JEnv, obj, value)
    }

    /**
     * Convert a [IJClassMember] to a reflection object
     */
    fun JField.toReflected(clz: JClass): JObject {
        return this.toReflected(this@JEnv, clz)
    }

    /**
     * Convert a reflection object to a [IJClassMember]
     */
    fun fieldFromReflected(obj: JObject, name: String, dsc: JDescriptor, isStatic: Boolean): IJClassMember {
        return JField.fromReflected(this@JEnv, obj, name, dsc, isStatic)
    }

    inline fun <reified R> JMethod.invoke(obj: JObject, vararg args: Any?): R? {
        return this.invoke<R>(this@JEnv, obj, args)
    }

    inline fun <reified R> JMethod.invokeNonVirtual(obj: JObject, clz: JClass, vararg args: Any?): R? {
        return this.invokeNonVirtual(this@JEnv, obj, clz, args)
    }

    /**
     * Convert a [IJClassMember] to a reflection object
     */
    fun JMethod.toReflected(clz: JClass): JObject {
        return this.toReflected(this@JEnv, clz)
    }

    /**
     * Convert a reflection object to a [IJClassMember]
     */
    fun methodFromReflected(obj: JObject, name: String, dsc: JDescriptor, isStatic: Boolean): IJClassMember {
        return JMethod.fromReflected(this@JEnv, obj, name, dsc, isStatic)
    }
}