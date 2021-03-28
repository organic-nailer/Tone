interface StackData

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
}

class NullStackData: StackData {
    override fun toString(): String = "sNull"
}
class UndefinedStackData: StackData {
    override fun toString(): String = "sUndefined"
}
data class BooleanStackData(val value: Boolean): StackData {
    override fun toString(): String = "s$value"
}
data class StringStackData(val address: Int): StackData
data class ObjectStackData(val address: Int): StackData
data class ReferenceStackData(val address: Int): StackData {
    override fun toString(): String = "sRef($address)"
}
class EmptyStackData: StackData {
    override fun toString(): String = "sEmpty"
}
