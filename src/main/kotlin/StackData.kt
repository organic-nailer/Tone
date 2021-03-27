interface StackData

data class NumberStackData(
    val kind: NumberData.NumberKind,
    val value: Int
): StackData

class NullStackData: StackData
class UndefinedStackData: StackData
data class BooleanStackData(val value: Boolean): StackData
data class StringStackData(val address: Int): StackData
data class ObjectStackData(val address: Int): StackData
data class ReferenceStackData(val address: Int): StackData
class EmptyStackData: StackData
