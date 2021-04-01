interface StackData {
    fun toEcmaData(): EcmaData
}

data class NumberStackData(
    val kind: NumberData.NumberKind,
    val value: Int
): StackData {
    override fun toString(): String {
        return when(kind) {
            NumberData.NumberKind.Real -> "s$value"
            NumberData.NumberKind.ZeroP -> "s+0"
            NumberData.NumberKind.ZeroN -> "s-0"
            NumberData.NumberKind.InfinityP -> "s+∞"
            NumberData.NumberKind.InfinityN -> "s-∞"
            NumberData.NumberKind.NaN -> "sNaN"
        }
    }

    override fun toEcmaData(): EcmaData = NumberData(kind, value)
}

class NullStackData: StackData {
    override fun toString(): String = "sNull"
    override fun toEcmaData(): EcmaData = NullData()
}
class UndefinedStackData: StackData {
    override fun toString(): String = "sUndefined"
    override fun toEcmaData(): EcmaData = UndefinedData()
}
data class BooleanStackData(val value: Boolean): StackData {
    override fun toString(): String = "s$value"
    override fun toEcmaData(): EcmaData = BooleanData(value)
}
data class StringStackData(val value: String): StackData {
    override fun toEcmaData(): EcmaData = StringData(value)
}
data class ObjectStackData(val value: ObjectData): StackData {
    override fun toEcmaData(): EcmaData = value
}
data class ReferenceStackData(val address: Int): StackData {
    override fun toString(): String = "sRef($address)"
    override fun toEcmaData(): EcmaData = throw Exception()
}
class EmptyStackData: StackData {
    override fun toString(): String = "sEmpty"
    override fun toEcmaData(): EcmaData = throw Exception()
}
