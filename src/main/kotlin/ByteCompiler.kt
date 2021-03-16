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
                when(node.operator) {
                    "+" -> {
                        node.left?.let { compile(it) }
                        node.right?.let { compile(it) }
                        byteLines.add(ByteOperation(OpCode.Add, null))
                        return
                    }
                    "*" -> {
                        node.left?.let { compile(it) }
                        node.right?.let { compile(it) }
                        byteLines.add(ByteOperation(OpCode.Mul, null))
                        return
                    }
                    else -> {
                        throw NotImplementedError()
                    }
                }
            }
            NodeType.Literal -> {
                node.value?.toIntOrNull()?.let {
                    byteLines.add(ByteOperation(OpCode.Push, it))
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
        val operand: Int?
    )
    enum class OpCode {
        Push, Pop, Add, Sub, Mul, Div
    }
}
