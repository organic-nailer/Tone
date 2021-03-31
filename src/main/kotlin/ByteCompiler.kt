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
                    writeOp(OpCode.Push, "empty")
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                }
                return
            }
            NodeType.FunctionDeclaration -> {
                if(node.body.isNullOrEmpty()) {
                    writeOp(OpCode.Push, "undefined")
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                }
                return
            }
            //Statementは評価後一つの値を残す。値がない場合はemptyを残す
            NodeType.ExpressionStatement -> {
                node.expression?.let {
                    compile(it)
                    writeOp(OpCode.GetValue)
                }
                return
            }
            NodeType.BlockStatement -> {
                val labelBreak = getUniqueLabel()
                if(!noScope) {
                    scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                }
                if(node.body.isNullOrEmpty()) {
                    writeOp(OpCode.Push, "empty")
                }
                else {
                    //Stackには最新の結果のみを残すようにする
                    //ただしemptyの場合は前の結果を残す
                    writeOp(OpCode.Push, "empty")
                    node.body.forEach { element ->
                        compile(element)
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                }
                if(!noScope) {
                    writeLabel(labelBreak)
                    scopeStack.removeFirst()
                }
            }
            NodeType.EmptyStatement -> {
                writeOp(OpCode.Push, "empty")
            }
            NodeType.IfStatement -> {
                val labelBreak = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                if(node.alternate == null) { //elseなし
                    compile(node.test!!)
                    val label = getUniqueLabel()
                    writeOp(OpCode.IfFalse, label)
                    writeOp(OpCode.Pop)
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = getUniqueLabel()
                    writeOp(OpCode.Goto, label2)
                    writeLabel(label)
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Push, "empty")
                    writeLabel(label2)
                }
                else { //elseあり
                    compile(node.test!!)
                    val label = getUniqueLabel()
                    writeOp(OpCode.IfFalse, label)
                    writeOp(OpCode.Pop)
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = getUniqueLabel()
                    writeOp(OpCode.Goto, label2)
                    writeLabel(label)
                    writeOp(OpCode.Pop)
                    compile(node.alternate, noScope = true)
                    writeLabel(label2)
                }
                writeLabel(labelBreak)
                scopeStack.removeFirst()
            }
            NodeType.DoWhileStatement -> {
                val labelBreak = getUniqueLabel()
                val labelContinue = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                writeOp(OpCode.Push, "empty")
                writeOp(OpCode.Push, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                writeOp(OpCode.Pop)
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)
                compile(node.test!!)
                writeOp(OpCode.IfTrue, labelStart)
                writeLabel(labelBreak)
                writeOp(OpCode.Pop)
                scopeStack.removeFirst()
            }
            NodeType.WhileStatement -> {
                val labelBreak = getUniqueLabel()
                val labelContinue = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                writeOp(OpCode.Push, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                compile(node.test!!)
                val labelEnd = getUniqueLabel()
                writeOp(OpCode.IfFalse, labelEnd)
                writeOp(OpCode.Pop)
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)
                writeOp(OpCode.Goto, labelStart)
                writeLabel(labelEnd)
                writeLabel(labelBreak)
                writeOp(OpCode.Pop)
                scopeStack.removeFirst()
            }
            NodeType.ForStatement -> {
                val labelBreak = getUniqueLabel()
                val labelContinue = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                if(node.init?.type == NodeType.VariableDeclaration) {
                    compile(node.init)
                }
                else {
                    node.init?.let { compile(it) }
                    writeOp(OpCode.GetValue)
                }
                writeOp(OpCode.Pop)
                writeOp(OpCode.Push, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                val labelEnd = getUniqueLabel()
                node.test?.let {
                    compile(it)
                    writeOp(OpCode.IfFalse, labelEnd)
                    writeOp(OpCode.Pop)
                }
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)

                node.update?.let {
                    compile(it)
                    writeOp(OpCode.GetValue)
                    writeOp(OpCode.Pop)
                }

                writeOp(OpCode.Goto, labelStart)
                writeLabel(labelEnd)
                writeLabel(labelBreak)
                writeOp(OpCode.Pop)

                scopeStack.removeFirst()
            }
            NodeType.ForInStatement -> {
                //TODO: break, continue
                scopeStack.addFirst(ScopeData(node.type))
                scopeStack.removeFirst()
                throw NotImplementedError()
            }
            NodeType.SwitchStatement -> {
                val labelBreak = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak))


                val labelEnd = getUniqueLabel()
                writeOp(OpCode.Push, "empty")

                compile(node.discriminant!!)


                val default = node.cases!!.indexOfFirst { it.test == null }
                val caseLabels = node.cases.map { getUniqueLabel() }
                node.cases.forEachIndexed { index, case ->
                    case.test?.let {
                        writeOp(OpCode.Copy)
                        compile(it)
                        writeOp(OpCode.EqS)
                        writeOp(OpCode.IfTrue, caseLabels[index])
                        writeOp(OpCode.Pop)
                    }
                }
                if(default >= 0) {
                    writeOp(OpCode.Push, "empty")
                    writeOp(OpCode.Goto, caseLabels[default])
                }
                else {
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Goto, labelEnd)
                }
                node.cases.forEachIndexed { index, case ->
                    writeLabel(caseLabels[index])
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Pop)
                    case.consequents?.forEach { element ->
                        compile(element)
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                    if(index+1 != node.cases.size) {
                        writeOp(OpCode.Push, "empty")
                        writeOp(OpCode.Push, "empty")
                    }
                }
                writeLabel(labelEnd)
                writeLabel(labelBreak)

                scopeStack.removeFirst()
            }
            NodeType.BreakStatement -> {
                if(node.label != null) {
                    val label = node.label.name!!
                    val scopeIndex = scopeStack.indexOfFirst {
                        it.label == label
                            || it.type == NodeType.FunctionDeclaration
                            || it.type == NodeType.FunctionExpression
                    }
                    if(scopeIndex < 0) throw Exception("SyntaxError")
                    val scope = scopeStack[scopeIndex]
                    if(scope.type == NodeType.FunctionExpression
                        || scope.type == NodeType.FunctionDeclaration) throw Exception("SyntaxError")
                    for(i in 0 until scopeIndex - (if(noScope) 1 else 0)) { //scopeがネストしている分だけPopする
                        writeOp(OpCode.Pop)
                    }
                    writeOp(OpCode.Goto, scope.labelBreak!!)
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
                        writeOp(OpCode.Pop)
                    }
                    writeOp(OpCode.Goto, scope.labelBreak!!)
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
                        writeOp(OpCode.Pop)
                    }
                    writeOp(OpCode.Goto, scope.labelContinue!!)
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
                        writeOp(OpCode.Pop)
                    }
                    writeOp(OpCode.Goto, scope.labelContinue!!)
                }
            }
            NodeType.LabeledStatement -> {
                if(scopeLabel != null) {
                    val labelBreak = getUniqueLabel()
                    scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                    compile(node)
                    writeLabel(labelBreak)
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
                    writeOp(OpCode.Push, "undefined")
                }
                writeOp(OpCode.Return)
            }
            NodeType.VariableDeclaration -> {
                node.declarations!!.forEach { declaration ->
                    if(declaration.init != null) {
                        compile(declaration.id!!)
                        compile(declaration.init)
                        writeOp(OpCode.Assign)
                        writeOp(OpCode.Pop)
                    }
                }
                writeOp(OpCode.Push, "empty")
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
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfFalse, label)
                        writeOp(OpCode.Pop)
                        node.right?.let { compile(it) }
                        writeLabel(label)
                    }
                    "||" -> {
                        node.left?.let { compile(it) }
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfTrue, label)
                        writeOp(OpCode.Pop)
                        node.right?.let { compile(it) }
                        writeLabel(label)
                    }
                    else -> throw Exception()
                }
            }
            NodeType.UnaryExpression -> {
                node.argument?.let { compile(it) }
                if(node.operator == "void") {
                    writeOp(OpCode.GetValue)
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Push, "undefined")
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
                val label = getUniqueLabel()
                writeOp(OpCode.IfFalse, label)
                writeOp(OpCode.Pop)
                node.consequent?.let { compile(it) }
                val label2 = getUniqueLabel()
                writeOp(OpCode.Goto, label2)
                writeLabel(label)
                writeOp(OpCode.Pop)
                node.alternate?.let { compile(it) }
                writeLabel(label2)
            }
            NodeType.SequenceExpression -> {
                node.expressions?.forEachIndexed { i, expr ->
                    compile(expr)
                    writeOp(OpCode.GetValue)
                    if(i+1 != node.expressions.size) {
                        writeOp(OpCode.Pop)
                    }
                }
            }
            NodeType.AssignmentExpression -> {
                if(node.operator == "=") {
                    compile(node.left!!)
                    compile(node.right!!)
                    writeOp(OpCode.Assign)
                }
                else {
                    compile(node.left!!)
                    writeOp(OpCode.Copy)
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
                    writeOp(OpCode.Assign)
                }
            }
            NodeType.CallExpression -> {
                compile(node.callee!!)
                node.arguments!!.forEach {
                    compile(it)
                }
                writeOp(OpCode.Call, node.arguments.size.toString())
            }
            NodeType.Literal -> {
                if(node.raw == "null"
                    || node.raw == "true"
                    || node.raw == "false") {
                    writeOp(OpCode.Push, node.raw)
                    return
                }
                node.value?.toIntOrNull()?.let {
                    writeOp(OpCode.Push, it.toString())
                    return
                }
                throw NotImplementedError()
            }
            NodeType.Identifier -> {
                val identifier = node.name!!
                val resolved = contextStack.first()
                    .resolveIdentifier(identifier, false) //TODO: strict
                if(refPool.contains(resolved)) {
                    val index = refPool.indexOf(resolved)
                    writeOp(OpCode.Push, "#$index")
                }
                else {
                    refPool.add(resolved)
                    writeOp(OpCode.Push, "#${refPool.size-1}")
                }
            }
            else -> {
                throw NotImplementedError("${node.type} is Not Implemented")
            }
        }
    }

    private fun writeOp(operator: OpCode, operand: String? = null) {
        byteLines.add(ByteOperation(operator, operand))
    }
    private fun writeLabel(label: String) {
        labelTable[label] = byteLines.size
    }
    private fun getUniqueLabel(): String {
        return "L${uniqueLabelIndex++}"
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
