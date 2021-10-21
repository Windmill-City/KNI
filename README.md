# KNI

Kotlin Native wrapper for Java Native Interface(JNI)

# Usage

```kotlin
fun method() {
    @JClass(ObjectClazz)
    val clz: JClass

    @JField(Clazz.field)
    val field: JField
}

@JNative(Clazz.nativeMethod)
fun nativeImpl(env:JEnv, arg1: Any){
    //Impl
}
```