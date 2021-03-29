import TypeConverter.isCallable
import TypeConverter.sameValue
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import kotlin.math.abs
import kotlin.math.sign

open class EcmaData {
    fun toStack(address: Int? = null): StackData {
        return when(this) {
            is NumberData -> NumberStackData(this.kind, this.value)
            is UndefinedData -> UndefinedStackData()
            is NullData -> NullStackData()
            is BooleanData -> BooleanStackData(this.value)
            is ObjectData -> ObjectStackData(address!!)
            is StringData -> StringStackData(address!!)
            else -> throw Exception()
        }
    }
}

data class NumberData(
    val kind: NumberKind,
    val value: Int
): EcmaData() {
    enum class NumberKind {
        Real, ZeroP, ZeroN, InfinityP, InfinityN, NaN
    }

    companion object {
        fun naN() = NumberData(NumberKind.NaN, 0)
        fun zeroP() = NumberData(NumberKind.ZeroP, 0)
        fun zeroN() = NumberData(NumberKind.ZeroN, 0)
        fun infiniteP() = NumberData(NumberKind.InfinityP, 0)
        fun infiniteN() = NumberData(NumberKind.InfinityN, 0)
        fun real(v: Int): NumberData {
            if(v == 0) return zeroP()
            return NumberData(NumberKind.Real, v)
        }
    }

    fun isZero() = kind == NumberKind.ZeroP || kind == NumberKind.ZeroN
    fun isInfinite() = kind == NumberKind.InfinityP || kind == NumberKind.InfinityN
    fun sign() = when(kind) {
        NumberKind.NaN,
        NumberKind.ZeroP,
        NumberKind.InfinityP -> true
        NumberKind.ZeroN,
        NumberKind.InfinityN -> false
        NumberKind.Real -> value >= 0 //TODO: 厳密にはどうなの？
    }

    operator fun plus(other: NumberData): NumberData {
        if(this.kind == NumberKind.NaN
            || other.kind == NumberKind.NaN) return naN()
        if((this.kind == NumberKind.InfinityP && other.kind == NumberKind.InfinityN)
            || (this.kind == NumberKind.InfinityN && other.kind == NumberKind.InfinityP)) {
            return naN()
        }
        if(this.kind == NumberKind.InfinityP || other.kind == NumberKind.InfinityP) {
            return this
        }
        if(this.kind == NumberKind.InfinityN || other.kind == NumberKind.InfinityN) {
            return this
        }
        if(this.kind == NumberKind.ZeroN && other.kind == NumberKind.ZeroN) {
            return this
        }
        if(this.kind == NumberKind.ZeroP
            && (other.kind == NumberKind.ZeroP || other.kind == NumberKind.ZeroN)) {
            return this
        }
        if(other.kind == NumberKind.ZeroP
            && (this.kind == NumberKind.ZeroP || this.kind == NumberKind.ZeroN)) {
            return other
        }
        if(this.kind == NumberKind.ZeroN || this.kind == NumberKind.ZeroP) {
            return other
        }
        if(other.kind == NumberKind.ZeroN || other.kind == NumberKind.ZeroP) {
            return this
        }
        if(this.value.sign + other.value.sign == 0
            && abs(this.value) == abs(other.value)
        ) {
            return zeroP()
        }
        return real(this.value + other.value) //TODO: 正確な処理
    }

    operator fun minus(other: NumberData): NumberData {
        return this + (-other)
    }

    operator fun unaryMinus(): NumberData {
        return when(this.kind) {
            NumberKind.Real -> real(-this.value)
            NumberKind.ZeroP -> zeroN()
            NumberKind.ZeroN -> zeroP()
            NumberKind.InfinityP -> infiniteN()
            NumberKind.InfinityN -> infiniteP()
            NumberKind.NaN -> this
        }
    }

    operator fun times(other: NumberData): NumberData {
        if(this.kind == NumberKind.NaN
            || other.kind == NumberKind.NaN) return naN()
        if((this.isZero() && other.isInfinite())
            || (this.isInfinite() && this.isZero())) {
            return naN()
        }
        val sign = this.sign() == other.sign()
        if(this.isInfinite() && other.isInfinite()) {
            return if(sign) infiniteP() else infiniteN()
        }
        if(this.isZero() || other.isZero()) {
            return if(sign) zeroP() else zeroN()
        }
        if(this.isInfinite() || other.isInfinite()) {
            return if(sign) infiniteP() else infiniteN()
        }
        return real(this.value * other.value) //TODO: 正確な処理
    }

    operator fun div(other: NumberData): NumberData {
        if(this.kind == NumberKind.NaN
            || other.kind == NumberKind.NaN) return naN()
        if(this.isZero() && other.isZero()) {
            return naN()
        }
        if(this.isInfinite() && other.isInfinite()) {
            return naN()
        }
        val sign = this.sign() == other.sign()
        if(this.isInfinite() && this.isZero()) {
            return if(sign) infiniteP() else infiniteN()
        }
        if(this.isInfinite()) {
            return if(sign) infiniteP() else infiniteN()
        }
        if(other.isInfinite()) {
            return if(sign) zeroP() else zeroN()
        }
        if(other.isZero()) {
            return if(sign) infiniteP() else infiniteN()
        }
        return real(this.value / other.value) //TODO: 正確な処理
    }

    operator fun rem(other: NumberData): NumberData {
        if(this.kind == NumberKind.NaN
            || other.kind == NumberKind.NaN) return naN()
        if(this.isInfinite() || other.isZero()) return naN()
        if(this.isInfinite() && other.isInfinite()) return this
        if(other.isInfinite()) return this
        if(this.isZero()) return this
        return real(this.value % other.value) //TODO: 正確な処理
    }
}
class NullData: EcmaData()
class UndefinedData: EcmaData()
data class BooleanData(
    val value: Boolean
): EcmaData()
data class StringData(
    val value: String
): EcmaData()

open class ObjectData: EcmaData() {
    protected val namedProperties
        : MutableMap<String, PropertyDescriptor> = mutableMapOf()

    open val prototype: ObjectData? = null
    open val className: String = ""
    open val extensible: Boolean = true
    fun get(propertyName: String): EcmaData? {
        val desc = getProperty(propertyName) ?: return null
        if(desc.type == PropertyDescriptor.DescriptorType.Data) return desc.value
        val getter = desc.get ?: return null
        //return getter.[[Call]](this)
        throw NotImplementedError()
    }
    fun getOwnProperty(propertyName: String): PropertyDescriptor? {
        if(!namedProperties.containsKey(propertyName)) {
            return null
        }
        if(namedProperties.containsKey(propertyName)) {
            return namedProperties[propertyName]!!.copy()
        }
        //TODO: String Objectは追記事項あり
        throw Exception()
    }
    fun getProperty(propertyName: String): PropertyDescriptor? {
        val prop = getOwnProperty(propertyName)
        if(prop != null) return prop
        if(prototype == null) return null
        return prototype!!.getOwnProperty(propertyName)
    }
    fun put(propertyName: String, value: EcmaData, throwFlag: Boolean) {
        if(!canPut(propertyName)) {
            if(throwFlag) throw Exception("TypeError") //TODO: TypeError
            else return
        }
        val ownDesc = getOwnProperty(propertyName)
        if(ownDesc?.type == PropertyDescriptor.DescriptorType.Data) {
            val valueDesc = PropertyDescriptor.data(value = value, address = ownDesc.address!!)
            defineOwnProperty(propertyName, valueDesc, throwFlag)
            return
        }
        val desc = getProperty(propertyName)
        if(desc?.type == PropertyDescriptor.DescriptorType.Accessor) {
            val setter = desc.set!!
            //return setter.[[Call]](this,value)
            throw NotImplementedException()
        }
        else {
            val newDesc = PropertyDescriptor.data(
                value = value, writable = true, enumerable = true, configurable = true,
                address = 0
            )
            defineOwnProperty(propertyName, newDesc, throwFlag)
        }
        return
    }
    fun canPut(propertyName: String): Boolean {
        val desc = getOwnProperty(propertyName)
        if(desc != null) {
            if(desc.type == PropertyDescriptor.DescriptorType.Accessor) {
                return desc.set != null
            }
            return desc.writable!!
        }
        if(prototype == null) return extensible
        val inherited = prototype!!.getProperty(propertyName) ?: return extensible
        if(inherited.type == PropertyDescriptor.DescriptorType.Accessor) {
            return inherited.set != null
        }
        return inherited.writable!!
    }
    fun hasProperty(propertyName: String): Boolean {
        val desc = getProperty(propertyName)
        return desc != null
    }
    fun delete(propertyName: String, throwFlag: Boolean): Boolean {
        val desc = getOwnProperty(propertyName) ?: return true
        if(desc.configurable == true) {
            namedProperties.remove(propertyName)
            return true
        }
        if(throwFlag) throw Exception("TypeError") //TODO: TypeError
        return false
    }
    fun defaultValue(hint: String) /*: Primitive */ {
        val toString = get("toString")
        if(isCallable(toString ?: UndefinedData())) {
            //val str = toString.[[Call]]()
            //if(str is Primitive) return str
            throw NotImplementedException()
        }
        val valueOf = get("valueOf")
        if(isCallable(valueOf ?: UndefinedData())) {
            //val value = valueOf.[[Call]]()
            //if(value is Primitive) return value
            throw NotImplementedException()
        }
        throw Exception("TypeError") //TODO: TypeError
    }
    fun defaultValue(hint: Int) /*: Primitive */ {
        val valueOf = get("valueOf")
        if(isCallable(valueOf ?: UndefinedData())) {
            //val value = valueOf.[[Call]]()
            //if(value is Primitive) return value
            throw NotImplementedException()
        }
        val toString = get("toString")
        if(isCallable(toString ?: UndefinedData())) {
            //val str = toString.[[Call]]()
            //if(str is Primitive) return str
            throw NotImplementedException()
        }
        throw Exception("TypeError") //TODO: TypeError
    }
    fun defineOwnProperty(propertyName: String, descriptor: PropertyDescriptor, throwFlag: Boolean): Boolean {
        val current = getOwnProperty(propertyName)
        if(current == null && !extensible) {
            if(throwFlag) throw Exception("TypeError") //TODO: TypeError
            return false
        }
        if(current == null && extensible) {
            if(descriptor.type == PropertyDescriptor.DescriptorType.Data
                || descriptor.type == PropertyDescriptor.DescriptorType.Generic) {
                namedProperties[propertyName] = descriptor
            }
            else {
                namedProperties[propertyName] = descriptor
            }
            return true
        }
        if(descriptor.everyFieldIsAbsent()) return true
        if(current == descriptor) return true //TODO: 正しい比較
        if(current?.configurable == true) {
            if(descriptor.configurable == true) {
                if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                return false
            }
            if(current.enumerable != descriptor.enumerable) {
                if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                return false
            }
        }
        if(descriptor.type == PropertyDescriptor.DescriptorType.Generic) {
        }
        else if(current!!.type != descriptor.type) {
            if(current.configurable == false) {
                if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                return false
            }
            if(current.type == PropertyDescriptor.DescriptorType.Data) {
                namedProperties[propertyName] = PropertyDescriptor.accessor(
                    address = 0,
                    configurable = current.configurable!!,
                    enumerable = current.enumerable!!
                ) //TODO: これdescriptor反映させなくていいの？
            }
            else {
                namedProperties[propertyName] = PropertyDescriptor.data(
                    descriptor.value!!,
                    address = 0,
                    configurable = current.configurable!!,
                    enumerable = current.enumerable!!
                ) //TODO: これdescriptor反映させなくていいの？
            }
        }
        else if(current.type == PropertyDescriptor.DescriptorType.Data
            && descriptor.type == PropertyDescriptor.DescriptorType.Data) {
            if(current.configurable == false) {
                if(current.writable == false && descriptor.writable == true) {
                    if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                    return false
                }
                if(current.writable == false) {
                    if(!sameValue(current.value!!, descriptor.value!!)) {
                        if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                        return false
                    }
                }
            }
        }
        else {
            if(current.configurable == false) {
                if(descriptor.set != null && !sameValue(descriptor.set, current.set!!)) {
                    if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                    return false
                }
                if(descriptor.get != null && !sameValue(descriptor.get, current.get!!)) {
                    if(throwFlag) throw Exception("TypeError") //TODO: TypeError
                    return false
                }
            }
        }
        namedProperties[propertyName] = PropertyDescriptor(
            type = current!!.type,
            address = 0,
            value = descriptor.value ?: current.value,
            writable = descriptor.writable ?: current.writable,
            get = descriptor.get ?: current.get,
            set = descriptor.set ?: current.set,
            enumerable = descriptor.enumerable ?: current.enumerable,
            configurable = descriptor.configurable ?: current.configurable
        )
        return true
    }

    open val primitiveValue: EcmaPrimitive? = null
    open val construct: Function<ObjectData>? = null
    open val call: Function<ReferenceStackData?>? = null
    open val hasInstance: Function<Boolean>? = null
    open val scope: Environment? = null
    open val formalParameters: List<String>? = null
    open val code: Any? = null
    open val targetFunction: ObjectData? = null
    open val boundThis: Any? = null
    open val boundArguments: List<Any>? = null
    open val match: Function<Any>? = null
    open val parameterMap: ObjectData? = null

    data class PropertyDescriptor(
        val type: DescriptorType,
        val address: Int?,
        val value: EcmaData? = null,
        val writable: Boolean? = null,
        val get: ObjectData? = null,
        val set: ObjectData? = null,
        val enumerable: Boolean? = null,
        val configurable: Boolean? = null
    ) {
        enum class DescriptorType {
            Data, Accessor, Generic
        }

        fun everyFieldIsAbsent(): Boolean {
            return value == null
                && writable == null
                && get == null
                && set == null
                && enumerable == null
                && configurable == null
        }

        companion object {
            fun data(
                value: EcmaData,
                address: Int,
                writable: Boolean? = null,
                enumerable: Boolean? = null,
                configurable: Boolean? = null
            ) = PropertyDescriptor(
                type = DescriptorType.Data,
                address,
                value, writable,
                enumerable = enumerable,
                configurable = configurable
            )

            fun accessor(
                address: Int,
                get: ObjectData? = null,
                set: ObjectData? = null,
                enumerable: Boolean? = null,
                configurable: Boolean? = null
            ) = PropertyDescriptor(
                type = DescriptorType.Accessor,
                address = address,
                get = get, set = set,
                enumerable = enumerable,
                configurable = configurable
            )
        }
    }
}



interface EcmaPrimitive

class UndefinedPrimitive: EcmaPrimitive
class NullPrimitive: EcmaPrimitive
data class BooleanPrimitive(val value: Boolean): EcmaPrimitive
data class NumberPrimitive(val value: Int): EcmaPrimitive
data class StringPrimitive(val value: String): EcmaPrimitive
