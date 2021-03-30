import sun.reflect.generics.reflectiveObjects.NotImplementedException
import kotlin.math.abs

object TypeConverter {
    fun toPrimitive(value: EcmaData): EcmaData {
        return value
    }

    fun toNumber(value: EcmaData): NumberData {
        return when(value) {
            is UndefinedData -> NumberData(NumberData.NumberKind.NaN, 0)
            is NullData -> NumberData(NumberData.NumberKind.ZeroP, 0)
            is BooleanData -> {
                if(value.value) NumberData(NumberData.NumberKind.Real, 1)
                else NumberData(NumberData.NumberKind.ZeroP, 0)
            }
            is NumberData -> value
            is StringData -> throw NotImplementedError()
            is ObjectData -> throw NotImplementedError()
            else -> throw Exception()
        }
    }

    fun toInt32(value: EcmaData): Int {
        val num = toNumber(value)
        if(num.kind == NumberData.NumberKind.NaN
            || num.kind == NumberData.NumberKind.ZeroP
            || num.kind == NumberData.NumberKind.ZeroN
            || num.kind == NumberData.NumberKind.InfinityP
            || num.kind == NumberData.NumberKind.InfinityN) {
            return 0
        }
        return num.value //TODO: 正確な
        //val posInt = sign(num) * floor(abs(num))
        //val int32bit = posInt % 2^32
        //return if(int32bit >= 2^31) int32bit - 2^32 else int32bit
    }

    fun toUInt32(value: EcmaData): Int {
        val num = toNumber(value)
        if(num.kind == NumberData.NumberKind.NaN
            || num.kind == NumberData.NumberKind.ZeroP
            || num.kind == NumberData.NumberKind.ZeroN
            || num.kind == NumberData.NumberKind.InfinityP
            || num.kind == NumberData.NumberKind.InfinityN) {
            return 0
        }
        return abs(num.value) //TODO: 正確な
        //val posInt = sign(num) * floor(abs(num))
        //val int32bit = posInt % 2^32
        //return int32bit
    }

    fun toBoolean(value: EcmaData): Boolean {
        if(value is UndefinedData) return false
        if(value is NullData) return false
        if(value is BooleanData) return value.value
        if(value is NumberData) {
            if(value.isZero() || value.kind == NumberData.NumberKind.NaN) return false
            return true
        }
        return true //TODO: StringとObject
    }

    fun toString(value: EcmaData): String {
        return when(value) {
            is UndefinedData -> "undefined"
            is NullData -> "null"
            is BooleanData -> if(value.value) "true" else "false"
            is NumberData -> {
                if(value.kind == NumberData.NumberKind.NaN) "NaN"
                else if(value.isZero()) "0"
                else if(!value.sign()) "-" + toString(-value)
                else if(value.isInfinite()) "Infinity"
                else value.value.toString() //TODO 正確な
            }
            is StringData -> value.value //TODO ??
            is ObjectData -> {
                val primValue = toPrimitive(value) //TODO ??
                toString(primValue)
            }
            else -> throw Exception()
        }
    }

    fun toObject(value: EcmaData): ObjectData {
        when(value) {
            is UndefinedData -> throw Exception("TypeError")
            is NullData -> throw Exception("TypeError")
            is BooleanData -> throw NotImplementedException() //BooleanObject
            is NumberData -> throw NotImplementedException() //NumberObject
            is StringData -> throw NotImplementedException() //StringObject
            is ObjectData -> return value
            else -> throw Exception()
        }
    }

    fun isCallable(value: EcmaData): Boolean {
        if(value !is ObjectData) return false
        return value.call != null
    }

    fun sameValue(x: EcmaData, y: EcmaData): Boolean {
        if(x::class != y::class) return false
        if(x is UndefinedData) return true
        if(x is NullData) return true
        if(x is NumberData) {
            y as NumberData
            if(x.kind != y.kind) return false
            if(x.kind == NumberData.NumberKind.NaN
                && y.kind == NumberData.NumberKind.NaN) return true
            if(x.value == y.value) return true
            return false
        }
        if(x is StringData) {
            y as StringData
            return x.value == y.value
        }
        if(x is BooleanData) {
            y as BooleanData
            return x.value == y.value
        }
        if(x is ObjectData) {
            y as ObjectData
            return x == y //TODO: これでインスタンス比較できるかな？
        }
        throw Exception()
    }
}
