package kni.jobject

import kni.JEnv
import kni.toBoolean
import kotlinx.cinterop.*
import native.jni.jbooleanVar
import native.jni.jobject
import native.jni.jstring

/**
 * Wrapper of [jstring]
 */
class JString(override val obj: jstring) : JObject(obj) {
    /**
     * Gets the same type of wrapper for the new [jobject]
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : JObject> T.newRefTo(obj: jobject): T {
        return JString(obj) as T
    }

    /**
     * Get the length in bytes of the Modified UTF-8 representation of the [JString]
     */
    fun getByteLenMU8(env: JEnv): Int {
        return env.nativeInf.GetStringUTFLength!!.invoke(env.internalEnv, obj)
    }

    /**
     * Get the length of [JString]
     */
    fun getLen(env: JEnv): Int {
        return env.nativeInf.GetStringLength!!.invoke(env.internalEnv, obj)
    }

    /**
     * Get [JString] region, in Modified UTF-8 format
     *
     * @param start start index of the region, inclusive
     * @param len the length of string to get start from the start index
     */
    fun getRegionMU8(env: JEnv, placement: NativePlacement, start: Int, len: Int): CArrayPointer<ByteVar> {
        val buf = placement.allocArray<ByteVar>(len)
        env.nativeInf.GetStringUTFRegion!!.invoke(env.internalEnv, obj, start, len, buf)
        return buf
    }

    /**
     * Get [JString] region, in UTF-16 format
     *
     * @param start start index of the region, inclusive
     * @param len the length of string to get start from the start index
     */
    fun getRegion(env: JEnv, placement: NativePlacement, start: Int, len: Int): CArrayPointer<UShortVar> {
        val buf = placement.allocArray<UShortVar>(len)
        env.nativeInf.GetStringRegion!!.invoke(env.internalEnv, obj, start, len, buf)
        return buf
    }

    /**
     * Use [JString] in Modified UTF-8 format
     *
     * @param action CPointer<ByteVar>.(len: Int, copied: Boolean), pointer points to the string, len is the length
     * in bytes of the Modified UTF-8 representation of the string, copied indicates if the string has copied
     *
     * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
     */
    inline fun <R> useStrMU8(env: JEnv, action: CPointer<ByteVar>.(len: Int, copied: Boolean) -> R): R {
        val fGetStr = env.nativeInf.GetStringUTFChars!!
        val fFreeStr = env.nativeInf.ReleaseStringUTFChars!!

        memScoped {
            val copied: jbooleanVar = this.alloc()
            val jStr = fGetStr.invoke(env.internalEnv, obj, copied.ptr)
                ?: throw OutOfMemoryError("Using MUTF-8 String")
            try {
                return action(jStr, getByteLenMU8(env), copied.value.toBoolean())
            } finally {
                fFreeStr.invoke(env.internalEnv, obj, jStr)
            }
        }
    }

    /**
     * Use [JString] in UTF-16 format
     *
     * @param critical if GetStringCritical
     * @param action CPointer<ByteVar>.(len: Int, copied: Boolean), pointer points to the string, len is the length
     * of the string, copied indicates if the string has copied
     *
     * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
     */
    inline fun <R> useStr(
        env: JEnv,
        critical: Boolean = false,
        action: CPointer<UShortVar>.(len: Int, copied: Boolean) -> R
    ): R {
        val fGetStr = if (critical) env.nativeInf.GetStringCritical!! else env.nativeInf.GetStringChars!!
        val fFreeStr = if (critical) env.nativeInf.ReleaseStringCritical!! else env.nativeInf.ReleaseStringChars!!

        memScoped {
            val copied: jbooleanVar = this.alloc()
            val jStr = fGetStr.invoke(env.internalEnv, obj, copied.ptr)
                ?: throw OutOfMemoryError("Using UTF-16 String")
            try {
                return action(jStr, getLen(env), copied.value.toBoolean())
            } finally {
                fFreeStr.invoke(env.internalEnv, obj, jStr)
            }
        }
    }

    /**
     * Convert [JString] to [String]
     *
     * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
     */
    fun toKString(env: JEnv): String {
        return useStr(env, true) { len, _ -> toKStringFromUtf16ByLen(len) }
    }
}

/**
 * Convert [JObject] to [JString]
 */
fun JObject.asJString(): JString {
    return JString(obj)
}

/**
 * Convert Modified UTF-8 C string to [JString]
 *
 * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
 */
fun CPointer<ByteVar>.toJString(env: JEnv): JString {
    return JString(
        env.nativeInf.NewStringUTF!!.invoke(
            env.internalEnv,
            this
        ) ?: throw OutOfMemoryError("Converting Utf8 CString to JString")
    )
}

/**
 * Convert UTF-16 C string to [JString]
 *
 * @param len UTF-16 String length
 *
 * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
 */
fun CPointer<UShortVar>.toJString(env: JEnv, len: Int): JString {
    return JString(
        env.nativeInf.NewString!!.invoke(
            env.internalEnv,
            this,
            len
        ) ?: throw OutOfMemoryError("Converting Unicode CString to JString")
    )
}

/**
 * Convert [String] to [JString]
 *
 * @throws OutOfMemoryError when system runs out of memory, also a pending exception in VM
 */
fun String.toJString(env: JEnv): JString {
    memScoped {
        return this@toJString.utf16.getPointer(this).toJString(env, length)
    }
}

/**
 * Convert UTF-16 C string to [String] by string length
 *
 * @param len C string length
 */
fun CPointer<UShortVar>.toKStringFromUtf16ByLen(len: Int): String {
    val chars = CharArray(len)

    for (i in 0 until len)
        chars[i] = this[i].toInt().toChar()

    return chars.concatToString()
}

/**
 * Convert Modified UTF-8 C string to [String]
 */
fun CPointer<ByteVar>.toKStringFromMUtf8(): String {
    fun Byte.isOneByteChar() = toInt() and 0x80 == 0
    fun Byte.isTwoByteChar() = toInt() and 0xE0 == 0xC0
    fun Byte.isThreeByteChar() = toInt() and 0xF0 == 0xE0
    fun Byte.isCharPart() = toInt() and 0xC0 == 0x80

    val len = run {
        var len = 0
        var index = 0
        var byte = this[index++]
        while (byte != 0.toByte()) {
            when {
                byte.isOneByteChar() -> len++
                byte.isTwoByteChar() && this[index++].isCharPart() -> len++
                byte.isThreeByteChar() && this[index++].isCharPart() && this[index++].isCharPart() -> len++
                else -> throw IllegalArgumentException("Invalid format of Modified UTF8 C string")
            }
            byte = this[index++]
        }
        len
    }

    val chars = CharArray(len)
    var index = 0
    for (i in 0 until len) {
        val byte = this[index++]
        when {
            byte.isOneByteChar() -> chars[i] = byte.toInt().toChar()
            byte.isTwoByteChar() -> chars[i] =
                (byte.toInt() and 0x1F
                        shl 6 or (this[index++].toInt() and 0x3F)).toChar()
            byte.isThreeByteChar() -> chars[i] =
                (byte.toInt()
                        shl 6 or (this[index++].toInt() and 0x3F)
                        shl 6 or (this[index++].toInt() and 0x3F)).toChar()
        }
    }
    return chars.concatToString()
}


/**
 * @return the value of zero-terminated Modified UTF-8-encoded C string constructed from given kotlin.String.
 */
val String.mutf8 get() = MU8CString(this.toCharArray())

/**
 * Class of Modified UTF-8 C string
 *
 * @param chars UTF-8 C string array
 */
class MU8CString(private val chars: CharArray) : CValues<ByteVar>() {
    override val size: Int by
    lazy {
        chars.sumOf {
            @Suppress("USELESS_CAST")
            when (it) {
                in '\u0001'..'\u007F' -> 1
                '\u0000', in '\u0080'..'\u07FF' -> 2
                else -> 3
            } as Int
        } + 1
    }

    override val align get() = 1

    override fun getPointer(scope: AutofreeScope): CPointer<ByteVar> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }

    /**
     * Copy the referenced values to [placement] and return placement pointer.
     */
    override fun place(placement: CPointer<ByteVar>): CPointer<ByteVar> {
        var i = 0
        for (ch in chars) {
            when (ch) {
                in '\u0001'..'\u007F' -> placement[i++] = ch.code.toByte()
                '\u0000', in '\u0080'..'\u07FF' -> {
                    placement[i++] = (ch.code shr 6 and 0x1F or 0xC0).toByte()
                    placement[i++] = (ch.code and 0x3F or 0x80).toByte()
                }
                else -> {
                    placement[i++] = (ch.code shr 12 and 0x0F or 0xE0).toByte()
                    placement[i++] = (ch.code shr 6 and 0x3F or 0x80).toByte()
                    placement[i++] = (ch.code and 0x3F or 0x80).toByte()
                }
            }
        }
        placement[i] = 0
        return placement
    }
}

