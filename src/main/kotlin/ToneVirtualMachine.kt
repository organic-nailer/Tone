import kotlin.math.abs
import kotlin.math.sign

class ToneVirtualMachine {
    fun run(code: List<ByteCompiler.ByteOperation>): Int? {
        val mainStack = ArrayDeque<StackData>()
        for(operation in code) {
            when(operation.opCode) {
                ByteCompiler.OpCode.Push -> {
                    mainStack.addFirst(operandToData(operation.operand))
                }
                ByteCompiler.OpCode.Mul,
                ByteCompiler.OpCode.Div,
                ByteCompiler.OpCode.Rem,
                ByteCompiler.OpCode.Sub -> {
                    val rRef = mainStack.removeFirst()
                    val rVal = getValue(rRef)
                    val lRef = mainStack.removeFirst()
                    val lVal = getValue(lRef)
                    val rightNum = toNumber(rVal)
                    val leftNum = toNumber(lVal)
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.Mul -> leftNum * rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Div -> leftNum / rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Rem -> leftNum % rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Sub -> leftNum - rightNum //TODO: 厳密な計算
                        else -> throw Exception()
                    }
                    mainStack.addFirst(result)
                }
                ByteCompiler.OpCode.Add -> {
                    val rRef = mainStack.removeFirst()
                    val rVal = getValue(rRef)
                    val lRef = mainStack.removeFirst()
                    val lVal = getValue(lRef)
                    val rPrim = toPrimitive(rVal)
                    val lPrim = toPrimitive(lVal)
                    if(lPrim is ReferenceData || lPrim is ReferenceData) {
                        throw NotImplementedError()
                    }
                    mainStack.addFirst(toNumber(lPrim) + toNumber(rPrim))
                }
                ByteCompiler.OpCode.ShiftL,
                ByteCompiler.OpCode.ShiftR,
                ByteCompiler.OpCode.ShiftUR -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val shiftCount = toUInt32(getValue(rRef)) and 0x1F
                    val lNum = toInt32(getValue(lRef))
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.ShiftL -> lNum shl shiftCount
                        ByteCompiler.OpCode.ShiftR -> lNum shr shiftCount
                        ByteCompiler.OpCode.ShiftUR -> lNum ushr shiftCount
                        else -> throw Exception()
                    }
                    mainStack.addFirst(NumberData(NumberData.NumberKind.Real, result))
                }
                ByteCompiler.OpCode.And,
                ByteCompiler.OpCode.Or,
                ByteCompiler.OpCode.Xor -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val rNum = toInt32(getValue(rRef))
                    val lNum = toInt32(getValue(lRef))
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.And -> lNum and rNum
                        ByteCompiler.OpCode.Or -> lNum or rNum
                        ByteCompiler.OpCode.Xor -> lNum xor rNum
                        else -> throw Exception()
                    }
                    mainStack.addFirst(NumberData(NumberData.NumberKind.Real, result))
                }
            }
        }
        if(mainStack.size != 1) {
            println("stack finished unexpected size: ${mainStack.size}")
            return null
        }
        return (mainStack.first() as? NumberData)?.value
    }

    private fun operandToData(operand: String?): StackData {
        if(operand == "null") {
            return NullData()
        }
        if(operand == "undefined") {
            return UndefinedData()
        }
        if(operand == "true") {
            return BooleanData(true)
        }
        if(operand == "false") {
            return BooleanData(false)
        }
        operand?.toIntOrNull()?.let {
            return NumberData(NumberData.NumberKind.Real, it)
        }
        throw NotImplementedError()
    }

    interface StackData
    data class NumberData(
        val kind: NumberKind,
        val value: Int
    ): StackData {
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
            if(this.value.sign + this.value.sign == 0
                && abs(this.value) == abs(this.value)) {
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
    data class NullData(
        val value: Int = 0
    ): StackData
    data class UndefinedData(
        val value: Int = 0
    ): StackData
    data class BooleanData(
        val value: Boolean
    ): StackData
    data class ReferenceData(
        val value: Int
    ): StackData

    private fun getValue(value: StackData): StackData {
        return value
    }

    private fun toPrimitive(value: StackData): StackData {
        return value
    }

    private fun toNumber(value: StackData): NumberData {
        return when(value) {
            is UndefinedData -> {
                throw NotImplementedError()
            }
            is NullData -> NumberData(NumberData.NumberKind.ZeroP, 0)
            is BooleanData -> {
                if(value.value) NumberData(NumberData.NumberKind.Real, 1)
                else NumberData(NumberData.NumberKind.ZeroP, 0)
            }
            is NumberData -> value
            is ReferenceData -> throw NotImplementedError()
            else -> throw Exception()
        }
    }

    private fun toInt32(value: StackData): Int {
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

    private fun toUInt32(value: StackData): Int {
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

    fun type(value: Int): ValueType {
        return ValueType.Number
    }
    enum class ValueType {
        Number
    }
}
