import Parser.*

class ByteCompiler {
    val byteLines: MutableList<ByteOperation> = mutableListOf()
    private val labelTable: MutableMap<String, Int> = mutableMapOf()
    private var uniqueLabelIndex = 0
    private val contextStack = ArrayDeque<ExecutionContext>()
    private val scopeStack = ArrayDeque<ScopeData>()
    val refPool = mutableListOf<ReferenceData>()
    var globalObject: GlobalObject? = null

    fun runGlobal(code: Node, global: GlobalObject) {
        byteLines.clear()
        labelTable.clear()
        contextStack.clear()

        val progCxt = ExecutionContext.global(global, code, false) //TODO: Strict
        globalObject = global
        contextStack.addFirst(progCxt)
        scopeStack.addFirst(ScopeData(NodeType.Program))

        compile(code)

        scopeStack.removeFirst()
        contextStack.removeFirst()

        replaceLabel()
    }

    fun runFunction(
        f: FunctionObject,
        thisArg: EcmaData?,
        arguments: List<EcmaData>,
        global: GlobalObject
    ) {
        byteLines.clear()
        labelTable.clear()
        contextStack.clear()

        val context = ExecutionContext.function(
            f, thisArg, arguments, global
        )
        globalObject = global
        contextStack.addFirst(context)
        scopeStack.addFirst(ScopeData(NodeType.FunctionDeclaration))

        compile(f.code)

        scopeStack.removeFirst()
        contextStack.removeFirst()
        replaceLabel()
    }

//    fun run(node: Node) {
//        byteLines.clear()
//        labelTable.clear()
//        contextStack.clear()
//        compile(node)
//        replaceLabel()
//    }

    private fun compile(node: Node, scopeLabel: String? = null, noScope: Boolean = false) {
        when(node.type) {
            NodeType.Program -> {
                if(node.body.isNullOrEmpty()) {
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                        byteLines.add(ByteOperation(OpCode.Swap))
                        labelTable[label] = byteLines.size
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                }
                return
            }
            NodeType.FunctionDeclaration -> {
                if(node.body.isNullOrEmpty()) {
                    byteLines.add(ByteOperation(OpCode.Push, "undefined"))
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                        byteLines.add(ByteOperation(OpCode.Swap))
                        labelTable[label] = byteLines.size
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                }
                return
            }
            //Statementは評価後一つの値を残す。値がない場合はemptyを残す
            NodeType.ExpressionStatement -> {
                node.expression?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.GetValue))
                }
                return
            }
            NodeType.BlockStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                if(!noScope) {
                    scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                }
                if(node.body.isNullOrEmpty()) {
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                }
                else {
                    //Stackには最新の結果のみを残すようにする
                    //ただしemptyの場合は前の結果を残す
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    node.body.forEach { element ->
                        compile(element)
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                        byteLines.add(ByteOperation(OpCode.Swap))
                        labelTable[label] = byteLines.size
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                }
                if(!noScope) {
                    labelTable[labelBreak] = byteLines.size
                    scopeStack.removeFirst()
                }
            }
            NodeType.EmptyStatement -> {
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
            }
            NodeType.IfStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                if(node.alternate == null) { //elseなし
                    compile(node.test!!)
                    val label = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.IfFalse, label))
                    byteLines.add(ByteOperation(OpCode.Pop))
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.Goto, label2))
                    labelTable[label] = byteLines.size
                    byteLines.add(ByteOperation(OpCode.Pop))
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    labelTable[label2] = byteLines.size
                }
                else { //elseあり
                    compile(node.test!!)
                    val label = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.IfFalse, label))
                    byteLines.add(ByteOperation(OpCode.Pop))
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = "L${uniqueLabelIndex++}"
                    byteLines.add(ByteOperation(OpCode.Goto, label2))
                    labelTable[label] = byteLines.size
                    byteLines.add(ByteOperation(OpCode.Pop))
                    compile(node.alternate, noScope = true)
                    labelTable[label2] = byteLines.size
                }
                labelTable[labelBreak] = byteLines.size
                scopeStack.removeFirst()
            }
            NodeType.DoWhileStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                val labelContinue = "L${uniqueLabelIndex++}"
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                compile(node.bodySingle!!, noScope = true)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                labelTable[labelContinue] = byteLines.size
                compile(node.test!!)
                byteLines.add(ByteOperation(OpCode.IfTrue, labelStart))
                labelTable[labelBreak] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                scopeStack.removeFirst()
            }
            NodeType.WhileStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                val labelContinue = "L${uniqueLabelIndex++}"
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                compile(node.test!!)
                val labelEnd = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfFalse, labelEnd))
                byteLines.add(ByteOperation(OpCode.Pop))
                compile(node.bodySingle!!, noScope = true)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                labelTable[labelContinue] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Goto, labelStart))
                labelTable[labelEnd] = byteLines.size
                labelTable[labelBreak] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                scopeStack.removeFirst()
            }
            NodeType.ForStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                val labelContinue = "L${uniqueLabelIndex++}"
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                if(node.init?.type == NodeType.VariableDeclaration) {
                    compile(node.init)
                }
                else {
                    node.init?.let { compile(it) }
                    byteLines.add(ByteOperation(OpCode.GetValue))
                }
                byteLines.add(ByteOperation(OpCode.Pop))
                byteLines.add(ByteOperation(OpCode.Push, "empty"))
                val labelStart = "L${uniqueLabelIndex++}"
                labelTable[labelStart] = byteLines.size
                val labelEnd = "L${uniqueLabelIndex++}"
                node.test?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.IfFalse, labelEnd))
                    byteLines.add(ByteOperation(OpCode.Pop))
                }
                compile(node.bodySingle!!, noScope = true)
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                byteLines.add(ByteOperation(OpCode.Swap))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                labelTable[labelContinue] = byteLines.size

                node.update?.let {
                    compile(it)
                    byteLines.add(ByteOperation(OpCode.GetValue))
                    byteLines.add(ByteOperation(OpCode.Pop))
                }

                byteLines.add(ByteOperation(OpCode.Goto, labelStart))
                labelTable[labelEnd] = byteLines.size
                labelTable[labelBreak] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))

                scopeStack.removeFirst()
            }
            NodeType.ForInStatement -> {
                //TODO: break, continue
                scopeStack.addFirst(ScopeData(node.type))
                scopeStack.removeFirst()
                throw NotImplementedError()
            }
            NodeType.SwitchStatement -> {
                val labelBreak = "L${uniqueLabelIndex++}"
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak))


                val labelEnd = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.Push, "empty"))

                compile(node.discriminant!!)


                val default = node.cases!!.indexOfFirst { it.test == null }
                val caseLabels = node.cases.map { "L${uniqueLabelIndex++}" }
                node.cases.forEachIndexed { index, case ->
                    case.test?.let {
                        byteLines.add(ByteOperation(OpCode.Copy))
                        compile(it)
                        byteLines.add(ByteOperation(OpCode.EqS))
                        byteLines.add(ByteOperation(OpCode.IfTrue, caseLabels[index]))
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                }
                if(default >= 0) {
                    byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    byteLines.add(ByteOperation(OpCode.Goto, caseLabels[default]))
                }
                else {
                    byteLines.add(ByteOperation(OpCode.Pop))
                    byteLines.add(ByteOperation(OpCode.Goto, labelEnd))
                }
                node.cases.forEachIndexed { index, case ->
                    labelTable[caseLabels[index]] = byteLines.size
                    byteLines.add(ByteOperation(OpCode.Pop))
                    byteLines.add(ByteOperation(OpCode.Pop))
                    case.consequents?.forEach { element ->
                        compile(element)
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfEmpty, label))
                        byteLines.add(ByteOperation(OpCode.Swap))
                        labelTable[label] = byteLines.size
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                    if(index+1 != node.cases.size) {
                        byteLines.add(ByteOperation(OpCode.Push, "empty"))
                        byteLines.add(ByteOperation(OpCode.Push, "empty"))
                    }
                }
                labelTable[labelEnd] = byteLines.size
                labelTable[labelBreak] = byteLines.size

                scopeStack.removeFirst()
            }
            NodeType.BreakStatement -> {
                if(node.label != null) {
                    val label = node.label.name!!
                    val scopeIndex = scopeStack.indexOfFirst {
                        it.label == label
                            || it.type == NodeType.FunctionDeclaration
                            || it.type == NodeType.FunctionExpression
                    } ?: throw Exception("SyntaxError")
                    if(scopeIndex < 0) throw Exception("SyntaxError")
                    val scope = scopeStack[scopeIndex]
                    if(scope.type == NodeType.FunctionExpression
                        || scope.type == NodeType.FunctionDeclaration) throw Exception("SyntaxError")
                    for(i in 0 until scopeIndex - (if(noScope) 1 else 0)) { //scopeがネストしている分だけPopする
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                    byteLines.add(ByteOperation(OpCode.Goto, scope.labelBreak!!))
                }
                else {
                    val scopeIndex = scopeStack.indexOfFirst {
                        it.type == NodeType.DoWhileStatement
                            || it.type == NodeType.WhileStatement
                            || it.type == NodeType.ForStatement
                            || it.type == NodeType.ForInStatement
                            || it.type == NodeType.SwitchStatement
                            || it.type == NodeType.FunctionDeclaration
                            || it.type == NodeType.FunctionExpression
                    }
                    if(scopeIndex < 0) throw Exception("SyntaxError")
                    val scope = scopeStack[scopeIndex]
                    if(scope.type == NodeType.FunctionExpression
                        || scope.type == NodeType.FunctionDeclaration) throw Exception("SyntaxError")
                    for(i in 0 until scopeIndex - (if(noScope) 1 else 0)) {
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                    byteLines.add(ByteOperation(OpCode.Goto, scope.labelBreak!!))
                }
            }
            NodeType.ContinueStatement -> {
                if(node.label != null) {
                    val label = node.label.name!!
                    val scopeIndex = scopeStack.indexOfFirst {
                        it.label == label
                            || it.type == NodeType.FunctionDeclaration
                            || it.type == NodeType.FunctionExpression
                    }
                    if(scopeIndex < 0) throw Exception("SyntaxError")
                    val scope = scopeStack[scopeIndex]
                    if(scope.type != NodeType.DoWhileStatement
                        && scope.type != NodeType.WhileStatement
                        && scope.type != NodeType.ForStatement
                        && scope.type != NodeType.ForInStatement) throw Exception("SyntaxError")
                    for(i in 0 until scopeIndex + 1 - (if(noScope) 1 else 0)) {
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                    byteLines.add(ByteOperation(OpCode.Goto, scope.labelContinue!!))
                }
                else {
                    val scopeIndex = scopeStack.indexOfFirst {
                        it.type == NodeType.DoWhileStatement
                            || it.type == NodeType.WhileStatement
                            || it.type == NodeType.ForStatement
                            || it.type == NodeType.ForInStatement
                            || it.type == NodeType.FunctionDeclaration
                            || it.type == NodeType.FunctionExpression
                    }
                    if(scopeIndex < 0) throw Exception("SyntaxError")
                    val scope = scopeStack[scopeIndex]
                    if(scope.type == NodeType.FunctionExpression
                        || scope.type == NodeType.FunctionDeclaration) throw Exception("SyntaxError")
                    for(i in 0 until scopeIndex + 1 - (if(noScope) 1 else 0)) {
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                    byteLines.add(ByteOperation(OpCode.Goto, scope.labelContinue!!))
                }
            }
            NodeType.LabeledStatement -> {
                if(scopeLabel != null) {
                    val labelBreak = "L${uniqueLabelIndex++}"
                    scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                    compile(node)
                    labelTable[labelBreak] = byteLines.size
                    scopeStack.removeFirst()
                }
                else {
                    compile(node.bodySingle!!, node.label!!.name)
                }
            }
            NodeType.ReturnStatement -> {
                scopeStack.find { it.type == NodeType.FunctionDeclaration }
                    ?: throw Exception("SyntaxError")
                node.argument?.let {
                    compile(it)
                } ?: kotlin.run {
                    byteLines.add(ByteOperation(OpCode.Push, "undefined"))
                }
                byteLines.add(ByteOperation(OpCode.Return))
            }
            NodeType.VariableDeclaration -> {
                node.declarations!!.forEach { declaration ->
                    if(declaration.init != null) {
                        compile(declaration.id!!)
                        compile(declaration.init)
                        byteLines.add(ByteOperation(OpCode.Assign))
                        byteLines.add(ByteOperation(OpCode.Pop))
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
                byteLines.add(ByteOperation(code))
                return
            }
            NodeType.LogicalExpression -> {
                when(node.operator) {
                    "&&" -> {
                        node.left?.let { compile(it) }
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfFalse, label))
                        byteLines.add(ByteOperation(OpCode.Pop))
                        node.right?.let { compile(it) }
                        labelTable[label] = byteLines.size
                    }
                    "||" -> {
                        node.left?.let { compile(it) }
                        val label = "L${uniqueLabelIndex++}"
                        byteLines.add(ByteOperation(OpCode.IfTrue, label))
                        byteLines.add(ByteOperation(OpCode.Pop))
                        node.right?.let { compile(it) }
                        labelTable[label] = byteLines.size
                    }
                    else -> throw Exception()
                }
            }
            NodeType.UnaryExpression -> {
                node.argument?.let { compile(it) }
                if(node.operator == "void") {
                    byteLines.add(ByteOperation(OpCode.GetValue))
                    byteLines.add(ByteOperation(OpCode.Pop))
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
                byteLines.add(ByteOperation(code))
            }
            NodeType.ConditionalExpression -> {
                node.test?.let { compile(it) }
                val label = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.IfFalse, label))
                byteLines.add(ByteOperation(OpCode.Pop))
                node.consequent?.let { compile(it) }
                val label2 = "L${uniqueLabelIndex++}"
                byteLines.add(ByteOperation(OpCode.Goto, label2))
                labelTable[label] = byteLines.size
                byteLines.add(ByteOperation(OpCode.Pop))
                node.alternate?.let { compile(it) }
                labelTable[label2] = byteLines.size
            }
            NodeType.SequenceExpression -> {
                node.expressions?.forEachIndexed { i, expr ->
                    compile(expr)
                    byteLines.add(ByteOperation(OpCode.GetValue))
                    if(i+1 != node.expressions.size) {
                        byteLines.add(ByteOperation(OpCode.Pop))
                    }
                }
            }
            NodeType.AssignmentExpression -> {
                if(node.operator == "=") {
                    compile(node.left!!)
                    compile(node.right!!)
                    byteLines.add(ByteOperation(OpCode.Assign))
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
                    byteLines.add(ByteOperation(code))
                    byteLines.add(ByteOperation(OpCode.Assign))
                }
            }
            NodeType.CallExpression -> {
                compile(node.callee!!)
                node.arguments!!.forEach {
                    compile(it)
                }
                byteLines.add(ByteOperation(OpCode.Call, node.arguments.size.toString()))
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
        println("labels")
        labelTable.forEach { (t, u) ->
            println("$t -> $u")
        }
        for(i in byteLines.indices) {
            val operand = byteLines[i].operand ?: continue
            labelTable[operand]?.let {
                byteLines[i] = byteLines[i].copy(operand = "$it")
            }
        }
    }

    data class ByteOperation(
        val opCode: OpCode,
        val operand: String? = null
    ) {
        override fun toString(): String {
            if(operand == null) return opCode.name
            return "$opCode $operand"
        }
    }
    enum class OpCode {
        Push, Pop, Add, Sub, Mul, Div, Rem,
        ShiftL, ShiftR, ShiftUR, And, Or, Xor,
        GT, GTE, LT, LTE, InstanceOf, In,
        Eq, Neq, EqS, NeqS, IfTrue, IfFalse,
        Delete, TypeOf, ToNum, Neg, Not, LogicalNot, Goto,
        GetValue, IfEmpty, Swap, Assign, Copy, Call,
        Return
    }

    data class ScopeData(
        val type: NodeType,
        val label: String? = null,
        val labelBreak: String? = null,
        val labelContinue: String? = null
    )
}
