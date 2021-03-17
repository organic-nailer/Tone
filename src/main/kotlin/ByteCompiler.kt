import Parser.*

class ByteCompiler {
    val byteLines: MutableList<ByteOperation> = mutableListOf()

    fun compile(node: Node) {
        when(node.type) {
            NodeType.Program -> {
                node.body?.forEach { compile(it) }
                return
            }
            NodeType.ExpressionStatement -> {
                node.expression?.let {
                    compile(it)
                }
                return
            }
            NodeType.BinaryExpression -> {
                node.left?.let { compile(it) }
                node.right?.let { compile(it) }
                val code = when(node.operator) {
                    "+" -> OpCode.Add
                    "-" -> OpCode.Sub
                    "*" -> OpCode.Mul
                    "/" -> OpCode.Div
                    "%" -> OpCode.Rem
                    else -> {
                        throw NotImplementedError()
                    }
                }
                byteLines.add(ByteOperation(code, null))
                return
            }
            NodeType.Literal -> {
                node.value?.toIntOrNull()?.let {
                    byteLines.add(ByteOperation(OpCode.Push, it.toString()))
                    return
                }
                throw NotImplementedError()
            }
            else -> {
                throw NotImplementedError()
            }
        }
    }

    data class ByteOperation(
        val opCode: OpCode,
        val operand: String?
    )
    enum class OpCode {
        Push, Pop, Add, Sub, Mul, Div, Rem,
        ShiftL, ShiftR, ShiftUR, And, Or, Xor
    }
}
