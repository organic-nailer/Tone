import Parser.*

class ByteCompiler {
    val byteLines: MutableList<ByteOperation> = mutableListOf()
    private val labelTable: MutableMap<String, Int> = mutableMapOf()
    private var uniqueLabelIndex = 0
    private val contextStack = ArrayDeque<ExecutionContext>()
    val refPool = mutableListOf<ReferenceData>()
    var globalObject: GlobalObject? = null

    fun run(node: Node) {
        byteLines.clear()
        labelTable.clear()
        contextStack.clear()
        compile(node)
        replaceLabel()
    }

    private fun compile(node: Node) {
        when(node.type) {
            NodeType.Program -> {
                if(node.body.isNullOrEmpty()) {
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    return
                }
                val progCxt = ExecutionContext.global()
                declarationBindingInstantiation(
                    progCxt.variableEnvironment, node.body,
                    ContextMode.Global, false //TODO: Strict
                )
                contextStack.addFirst(progCxt)
                globalObject = progCxt.thisBinding as GlobalObject
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
                contextStack.removeFirst()
                return
            }
            //Statementは評価後一つの値を残す。値がない場合はemptyを残す
            NodeType.ExpressionStatement -> {
                node.expression?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.GetValue, null))
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
            NodeType.DoWhileStatement -> {
                //TODO: continue, break
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
                compile(node.bodySingle!!)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap, null))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
                compile(node.test!!)
                byteLines.add(ByteOperation(OpCode.IfTrue, labelStart))
                byteLines.add(ByteOperation(OpCode.Pop, null))
            }
            NodeType.WhileStatement -> {
                //TODO: continue, break
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                compile(node.test!!)
                val labelEnd = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfFalse, labelEnd))
                byteLines.add(ByteOperation(OpCode.Pop, null))
                compile(node.bodySingle!!)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap, null))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
                byteLines.add(ByteOperation(OpCode.Goto, labelStart))
                labelTable[labelEnd] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
            }
            NodeType.ForStatement -> {
                //TODO: continue, break
                if(node.init?.type == NodeType.VariableDeclaration) {
                    compile(node.init)
                }
                else {
                    node.init?.let { compile(it) }
                    byteLines.add(ByteOperation(OpCode.GetValue, null))
                }
                byteLines.add(ByteOperation(OpCode.Pop, null))
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                val labelEnd = "L${uniqueLabelIndex++}"
                node.test?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.IfFalse, labelEnd))
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                }
                compile(node.bodySingle!!)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap, null))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))

                node.update?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.GetValue, null))
                    byteLines.add(ByteOperation(OpCode.Pop, null))
                }

                byteLines.add(ByteOperation(OpCode.Goto, labelStart))
                labelTable[labelEnd] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop, null))
            }
            NodeType.ForInStatement -> {
                throw NotImplementedError()
            }
            NodeType.VariableDeclaration -> {
                node.declarations!!.forEach { declaration ->
                    if(declaration.init != null) {
                        compile(declaration.id!!)
                        compile(declaration.init)
                        byteLines.add(ByteOperation(OpCode.Assign, null))
                        byteLines.add(ByteOperation(OpCode.Pop, null))
                    }
                }
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
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
            NodeType.AssignmentExpression -> {
                if(node.operator == "=") {
                    compile(node.left!!)
                    compile(node.right!!)
                    byteLines.add(ByteOperation(OpCode.Assign, null))
                }
                else {
                    //TODO: leftを2回評価する形になるので問題か？
                    compile(node.left!!)
                    compile(node.left)
                    compile(node.right!!)
                    val code = when(node.operator) {
                        "+=" -> OpCode.Add
                        "-=" -> OpCode.Sub
                        "*=" -> OpCode.Mul
                        "/=" -> OpCode.Div
                        "%=" -> OpCode.Rem
                        "&=" -> OpCode.And
                        "|=" -> OpCode.Or
                        "^=" -> OpCode.Xor
                        "<<=" -> OpCode.ShiftL
                        ">>=" -> OpCode.ShiftR
                        ">>>=" -> OpCode.ShiftUR
                        else -> throw Exception()
                    }
                    byteLines.add(ByteOperation(code, null))
                    byteLines.add(ByteOperation(OpCode.Assign, null))
                }
            }
            NodeType.Literal -> {
                if(node.raw == "null"
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
            NodeType.Identifier -> {
                val identifier = node.name!!
                val resolved = contextStack.first()
                    .resolveIdentifier(identifier, false) //TODO: strict
                refPool.add(resolved)
                byteLines.add(ByteOperation(OpCode.Push, "#${refPool.size-1}"))
            }
            else -> {
                throw NotImplementedError("${node.type} is Not Implemented")
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
        GetValue, IfEmpty, Swap, Assign
    }
}
