import Parser.*

class ByteCompiler {
    val byteLines: MutableList<ByteOperation> = mutableListOf()
    private val labelTable: MutableMap<String, Int> = mutableMapOf()
    private var uniqueLabelIndex = 0

    fun run(node: Node) {
        byteLines.clear()
        labelTable.clear()
        compile(node)
        replaceLabel()
    }

    private fun compile(node: Node) {
        when(node.type) {
            NodeType.Program -> {
                node.body?.forEach { compile(it) }
                return
            }
            //Statementは評価後一つの値を残す。値がない場合はemptyを残す
            NodeType.ExpressionStatement -> {
                node.expression?.let {
                    compile(it)
                }
                return
            }
            NodeType.BlockStatement -> {
                if(node.body.isNullOrEmpty()) {
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                }
                else {
                    //Stackには最新の結果のみを残すようにする
                    //ただしemptyの場合は前の結果を残す
                    node.body.forEachIndexed { i, element ->
                        compile(element)
                        if(i != 0) {
                            val label = "L${uniqueLabelIndex++}"
                            byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                            byteLines.add(ByteOperation(OpCode.Swap, null))
                            labelTable[label] = byteLines.size
                            byteLines.add(ByteOperation(OpCode.Pop, null))
                        }
                    }
                }
                return
            }
            NodeType.EmptyStatement -> {
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
            }
            NodeType.IfStatement -> {
                if(node.alternate == null) { //elseなし
                    node.test?.let { compile(it) }
                    val label = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.IfFalse, label))
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                    node.consequent?.let { compile(it) }
                    val label2 = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.Goto, label2))
                    labelTable[label] = byteLines.size
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    labelTable[label2] = byteLines.size
                }
                else { //elseあり
                    node.test?.let { compile(it) }
                    val label = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.IfFalse, label))
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                    node.consequent?.let { compile(it) }
                    val label2 = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.Goto, label2))
                    labelTable[label] = byteLines.size
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                    compile(node.alternate)
                    labelTable[label2] = byteLines.size
                }
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
                    "&" -> OpCode.And
                    "|" -> OpCode.Or
                    "^" -> OpCode.Xor
                    "<<" -> OpCode.ShiftL
                    ">>" -> OpCode.ShiftR
                    ">>>" -> OpCode.ShiftUR
                    "<" -> OpCode.LT
                    ">" -> OpCode.GT
                    "<=" -> OpCode.LTE
                    ">=" -> OpCode.GTE
                    "instanceof" -> OpCode.InstanceOf
                    "in" -> OpCode.In
                    "==" -> OpCode.Eq
                    "!=" -> OpCode.Neq
                    "===" -> OpCode.EqS
                    "!==" -> OpCode.NeqS
                    else -> {
                        throw NotImplementedError()
                    }
                }
                byteLines.add(ByteOperation(code, null))
                return
            }
            NodeType.LogicalExpression -> {
                when(node.operator) {
                    "&&" -> {
                        node.left?.let { compile(it) }
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfFalse, label))
                        byteLines.add(ByteOperation(OpCode.Pop, null))
                        node.right?.let { compile(it) }
                        labelTable[label] = byteLines.size
                    }
                    "||" -> {
                        node.left?.let { compile(it) }
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfTrue, label))
                        byteLines.add(ByteOperation(OpCode.Pop, null))
                        node.right?.let { compile(it) }
                        labelTable[label] = byteLines.size
                    }
                    else -> throw Exception()
                }
            }
            NodeType.UnaryExpression -> {
                node.argument?.let { compile(it) }
                if(node.operator == "void") {
                    byteLines.add(ByteOperation(OpCode.GetValue, null))
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                    byteLines.add(ByteOperation(OpCode.Push, "undefined"))
                    return
                }
                val code = when(node.operator) {
                    "delete" -> OpCode.Delete
                    "typeof" -> OpCode.TypeOf
                    "+" -> OpCode.ToNum
                    "-" -> OpCode.Neg
                    "~" -> OpCode.Not
                    "!" -> OpCode.LogicalNot
                    else -> throw Exception()
                }
                byteLines.add(ByteOperation(code, null))
            }
            NodeType.ConditionalExpression -> {
                node.test?.let { compile(it) }
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfFalse, label))
                byteLines.add(ByteOperation(OpCode.Pop, null))
                node.consequent?.let { compile(it) }
                val label2 = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.Goto, label2))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
                node.alternate?.let { compile(it) }
                labelTable[label2] = byteLines.size
            }
            NodeType.SequenceExpression -> {
                node.expressions?.forEachIndexed { i, expr ->
                    compile(expr)
                    byteLines.add(ByteOperation(OpCode.GetValue, null))
                    if(i+1 != node.expressions.size) {
                        byteLines.add(ByteOperation(OpCode.Pop, null))
                    }
                }
            }
            NodeType.Literal -> {
                if(node.raw == "null"
                    || node.raw == "undefined"
                    || node.raw == "true"
                    || node.raw == "false") {
                    byteLines.add(ByteOperation(OpCode.Push, node.raw))
                    return
                }
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

    private fun replaceLabel() {
        for(i in byteLines.indices) {
            val operand = byteLines[i].operand ?: continue
            labelTable[operand]?.let {
                byteLines[i] = byteLines[i].copy(operand = "$it")
            }
        }
    }

    data class ByteOperation(
        val opCode: OpCode,
        val operand: String?
    )
    enum class OpCode {
        Push, Pop, Add, Sub, Mul, Div, Rem,
        ShiftL, ShiftR, ShiftUR, And, Or, Xor,
        GT, GTE, LT, LTE, InstanceOf, In,
        Eq, Neq, EqS, NeqS, IfTrue, IfFalse,
        Delete, TypeOf, ToNum, Neg, Not, LogicalNot, Goto,
        GetValue, IfEmpty, Swap
    }
}
