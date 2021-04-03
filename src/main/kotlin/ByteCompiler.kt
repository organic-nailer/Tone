import Parser.*
import ByteCompiler.NonByteOperation.NonByteDataType.*
import ByteCompiler.NonByteOperation.NonByteData

class ByteCompiler {

    companion object {
        fun with(
            code: Node,
            nonBytes: List<NonByteOperation?>,
            scope: List<ScopeData>,
            labels: Map<String, Int>,
            labelIndex: Int,
            global: GlobalObject
        ): List<NonByteOperation?> {
            val compiler = ByteCompiler()
            compiler.runWith(code, nonBytes, scope, labels, labelIndex, global)
            return compiler.nonByteLines
        }
    }

    //val byteLines: MutableList<ByteOperation> = mutableListOf()
    val nonByteLines: MutableList<NonByteOperation?> = mutableListOf()
    private val labelTable: MutableMap<String, Int> = mutableMapOf()
    private var uniqueLabelIndex = 0
    private val contextStack = ArrayDeque<ExecutionContext>()
    private val scopeStack = ArrayDeque<ScopeData>()
    //val refPool = mutableListOf<ReferenceData>()
    //val constantPool = mutableListOf<EcmaPrimitive>()
    //val objectPool = mutableListOf<ObjectData>()
    var globalObject: GlobalObject? = null

    fun runGlobal(code: Node, global: GlobalObject) {
        //byteLines.clear()
        nonByteLines.clear()
        labelTable.clear()
        contextStack.clear()

        val progCxt = ExecutionContext.global(global, code, false) //TODO: Strict
        globalObject = global
        contextStack.addFirst(progCxt)
        writeOp(ContextOp.Push, Context, progCxt)
        scopeStack.addFirst(ScopeData(NodeType.Program))


        compile(code)

        scopeStack.removeFirst()
        contextStack.removeFirst()
        writeOp(ContextOp.Pop)

        replaceLabel()
    }

    fun runFunction(
        f: FunctionObject,
        thisArg: EcmaData?,
        arguments: List<EcmaData>,
        global: GlobalObject
    ) {
        //byteLines.clear()
        nonByteLines.clear()
        labelTable.clear()
        contextStack.clear()

        val context = ExecutionContext.function(
            f, thisArg, arguments, global
        )
        globalObject = global
        contextStack.addFirst(context)
        writeOp(ContextOp.Push, Context, context)
        scopeStack.addFirst(ScopeData(NodeType.FunctionDeclaration))

        compile(f.code)

        scopeStack.removeFirst()
        contextStack.removeFirst()
        writeOp(ContextOp.Pop)
        replaceLabel()
    }

    fun runWith(
        code: Node,
        nonBytes: List<NonByteOperation?>,
        scope: List<ScopeData>,
        labels: Map<String, Int>,
        labelIndex: Int,
        global: GlobalObject
    ) {
        //byteLines.clear()
        nonByteLines.clear()
        nonByteLines.addAll(nonBytes)
        labelTable.clear()
        labelTable.putAll(labels)
        uniqueLabelIndex = labelIndex
        contextStack.clear()
        scopeStack.clear()
        scopeStack.addAll(0,scope)

//        val newEnv = newObjectEnvironment(obj,currentContext.lexicalEnvironment,global)
//
//        val context = ExecutionContext(
//            newEnv,
//            currentContext.variableEnvironment,
//            currentContext.thisBinding
//        )
        globalObject = global
//        contextStack.addFirst(context)
//        writeOp(ContextOp.Push, Context, context)
        scopeStack.addFirst(ScopeData(NodeType.WithStatement))

        compile(code, noScope = true)

        scopeStack.removeFirst()
//        contextStack.removeFirst()
        writeOp(ContextOp.Pop)
        replaceLabel()
    }

    private fun compile(node: Node, scopeLabel: String? = null, noScope: Boolean = false) {
        when(node.type) {
            NodeType.Program -> {
                if(node.body.isNullOrEmpty()) {
                    writeOp(OpCode.Push, Raw, "empty")
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, Raw, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                }
                return
            }
            NodeType.FunctionDeclaration -> {
                if(node.body.isNullOrEmpty()) {
                    writeOp(OpCode.Push, Raw, "undefined")
                    return
                }
                node.body.forEachIndexed { i, element ->
                    compile(element)
                    if(i != 0) {
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, Raw, label)
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
                    writeOp(OpCode.Push, Raw, "empty")
                }
                else {
                    //Stackには最新の結果のみを残すようにする
                    //ただしemptyの場合は前の結果を残す
                    writeOp(OpCode.Push, Raw, "empty")
                    node.body.forEach { element ->
                        compile(element)
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, Raw, label)
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
                writeOp(OpCode.Push, Raw, "empty")
            }
            NodeType.IfStatement -> {
                val labelBreak = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak = labelBreak))
                if(node.alternate == null) { //elseなし
                    compile(node.test!!)
                    val label = getUniqueLabel()
                    writeOp(OpCode.IfFalse, Raw, label)
                    writeOp(OpCode.Pop)
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = getUniqueLabel()
                    writeOp(OpCode.Goto, Raw, label2)
                    writeLabel(label)
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Push, Raw, "empty")
                    writeLabel(label2)
                }
                else { //elseあり
                    compile(node.test!!)
                    val label = getUniqueLabel()
                    writeOp(OpCode.IfFalse, Raw, label)
                    writeOp(OpCode.Pop)
                    node.consequent?.let { compile(it, noScope = true) }
                    val label2 = getUniqueLabel()
                    writeOp(OpCode.Goto, Raw, label2)
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

                writeOp(OpCode.Push, Raw, "empty")
                writeOp(OpCode.Push, Raw, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                writeOp(OpCode.Pop)
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, Raw, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)
                compile(node.test!!)
                writeOp(OpCode.IfTrue, Raw, labelStart)
                writeLabel(labelBreak)
                writeOp(OpCode.Pop)
                scopeStack.removeFirst()
            }
            NodeType.WhileStatement -> {
                val labelBreak = getUniqueLabel()
                val labelContinue = getUniqueLabel()
                scopeStack.addFirst(ScopeData(node.type, scopeLabel, labelBreak, labelContinue))

                writeOp(OpCode.Push, Raw, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                compile(node.test!!)
                val labelEnd = getUniqueLabel()
                writeOp(OpCode.IfFalse, Raw, labelEnd)
                writeOp(OpCode.Pop)
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, Raw, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)
                writeOp(OpCode.Goto, Raw, labelStart)
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
                writeOp(OpCode.Push, Raw, "empty")
                val labelStart = getUniqueLabel()
                writeLabel(labelStart)
                val labelEnd = getUniqueLabel()
                node.test?.let {
                    compile(it)
                    writeOp(OpCode.IfFalse, Raw, labelEnd)
                    writeOp(OpCode.Pop)
                }
                compile(node.bodySingle!!, noScope = true)
                val label = getUniqueLabel()
                writeOp(OpCode.IfEmpty, Raw, label)
                writeOp(OpCode.Swap)
                writeLabel(label)
                writeOp(OpCode.Pop)
                writeLabel(labelContinue)

                node.update?.let {
                    compile(it)
                    writeOp(OpCode.GetValue)
                    writeOp(OpCode.Pop)
                }

                writeOp(OpCode.Goto, Raw, labelStart)
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
                writeOp(OpCode.Push, Raw, "empty")

                compile(node.discriminant!!)


                val default = node.cases!!.indexOfFirst { it.test == null }
                val caseLabels = node.cases.map { getUniqueLabel() }
                node.cases.forEachIndexed { index, case ->
                    case.test?.let {
                        writeOp(OpCode.Copy)
                        compile(it)
                        writeOp(OpCode.EqS)
                        writeOp(OpCode.IfTrue, Raw, caseLabels[index])
                        writeOp(OpCode.Pop)
                    }
                }
                if(default >= 0) {
                    writeOp(OpCode.Push, Raw, "empty")
                    writeOp(OpCode.Goto, Raw, caseLabels[default])
                }
                else {
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Goto, Raw, labelEnd)
                }
                node.cases.forEachIndexed { index, case ->
                    writeLabel(caseLabels[index])
                    writeOp(OpCode.Pop)
                    writeOp(OpCode.Pop)
                    case.consequents?.forEach { element ->
                        compile(element)
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfEmpty, Raw, label)
                        writeOp(OpCode.Swap)
                        writeLabel(label)
                        writeOp(OpCode.Pop)
                    }
                    if(index+1 != node.cases.size) {
                        writeOp(OpCode.Push, Raw, "empty")
                        writeOp(OpCode.Push, Raw, "empty")
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
                    writeOp(OpCode.Goto, Raw, scope.labelBreak!!)
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
                    writeOp(OpCode.Goto, Raw, scope.labelBreak!!)
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
                    writeOp(OpCode.Goto, Raw, scope.labelContinue!!)
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
                    writeOp(OpCode.Goto, Raw, scope.labelContinue!!)
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
                    writeOp(OpCode.Push, Raw, "undefined")
                }
                writeOp(OpCode.GetValue)
                writeOp(OpCode.Return)
            }
            NodeType.DebuggerStatement -> {
                //TODO: debug
                writeOp(OpCode.Push, Raw, "empty")
            }
            NodeType.WithStatement -> {
                val result = ByteCompiler.with(
                    node.bodySingle!!,
                    nonByteLines,
                    scopeStack,
                    labelTable, uniqueLabelIndex,
                    globalObject!!
                )
                val withCode = result.subList(nonByteLines.size, result.size) //最初は渡した分なので除外する
                val codeLength = withCode.count { it?.opCode !is ContextOp }
                val part = NonByteOperation.WithPartData(withCode)
                writeOp(OpCode.Push, WithPart, part)
                compile(node.`object`!!)
                writeOp(OpCode.With, Raw, "$codeLength")
                for(i in 0 until codeLength) {
                    nonByteLines.add(null)
                }
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
                writeOp(OpCode.Push, Raw, "empty")
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
                writeOp(code)
                return
            }
            NodeType.LogicalExpression -> {
                when(node.operator) {
                    "&&" -> {
                        node.left?.let { compile(it) }
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfFalse, Raw, label)
                        writeOp(OpCode.Pop)
                        node.right?.let { compile(it) }
                        writeLabel(label)
                    }
                    "||" -> {
                        node.left?.let { compile(it) }
                        val label = getUniqueLabel()
                        writeOp(OpCode.IfTrue, Raw, label)
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
                    writeOp(OpCode.Push, Raw, "undefined")
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
                writeOp(code)
            }
            NodeType.ConditionalExpression -> {
                node.test?.let { compile(it) }
                val label = getUniqueLabel()
                writeOp(OpCode.IfFalse, Raw, label)
                writeOp(OpCode.Pop)
                node.consequent?.let { compile(it) }
                val label2 = getUniqueLabel()
                writeOp(OpCode.Goto, Raw, label2)
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
                    writeOp(code)
                    writeOp(OpCode.Assign)
                }
            }
            NodeType.CallExpression -> {
                compile(node.callee!!)
                node.arguments!!.forEach {
                    compile(it)
                }
                writeOp(OpCode.Call, Raw, node.arguments.size.toString())
            }
            NodeType.MemberExpression -> {
                if(node.computed!!) { //object[property]
                    compile(node.`object`!!)
                    compile(node.property!!)
                    writeOp(OpCode.ResolveMember)
                }
                else { //object.property
                    compile(node.`object`!!)
                    writeOp(OpCode.Push, Primitive, StringPrimitive(node.property!!.name!!))
                    writeOp(OpCode.ResolveMember)
                }
            }
            NodeType.NewExpression -> {
                compile(node.callee!!)
                node.arguments?.forEach {
                    compile(it)
                }
                writeOp(OpCode.New, Raw, (node.arguments?.size ?: 0).toString())
            }
            NodeType.ThisExpression -> {
//                writeOp(OpCode.Push, useObject(contextStack.first().thisBinding))
                writeOp(OpCode.Push, This)
            }
            NodeType.ObjectExpression -> {
                val obj = NormalObject()
                writeOp(OpCode.Push, Object, obj)
                node.properties?.forEach { property ->
                    if(property.key!!.type == NodeType.Literal) { //TODO: 正しいtoString
                        writeOp(OpCode.Push, Primitive, StringPrimitive(property.key.value!!))
                    }
                    else if(property.key.type == NodeType.Identifier) {
                        writeOp(OpCode.Push, Primitive, StringPrimitive(property.key.name!!))
                    }
                    else throw Exception()

                    if(property.kind!! == "init") {
                        compile(property.valueNode!!)
                        writeOp(OpCode.Define, Raw, "init")
                    }
                    else if(property.kind == "get") {
                        val closure = NonByteOperation.FunctionPart(
                            listOf(), property.valueNode!!.bodySingle!!
                        )
//                        val closure = FunctionObject(listOf(),
//                            property.valueNode!!.bodySingle!!,
//                            contextStack.first().lexicalEnvironment,
//                            false, globalObject!!) //TODO: strict
                        writeOp(OpCode.Push, FuncPart, closure)
                        writeOp(OpCode.Define, Raw, "get")
                    }
                    else if(property.kind == "set") {
                        val closure = NonByteOperation.FunctionPart(
                            property.valueNode!!.params!!.map { it.name!! },
                            property.valueNode.bodySingle!!
                        )
//                        val closure = FunctionObject(
//                            property.valueNode!!.params!!.map { it.name!! },
//                            property.valueNode.bodySingle!!,
//                            contextStack.first().lexicalEnvironment,
//                            false, globalObject!!) //TODO: strict
                        writeOp(OpCode.Push, FuncPart, closure)
                        writeOp(OpCode.Define, Raw, "set")
                    }
                }
            }
            NodeType.ArrayExpression -> {
                val obj = ArrayObject()
                writeOp(OpCode.Push, Object, obj)
                node.elements ?: return
                node.elements.forEachIndexed { index, element ->
                    writeOp(OpCode.Push, Raw, "$index")
                    element?.let {
                        compile(it)
                    } ?: kotlin.run {
                        writeOp(OpCode.Push, Raw, "undefined")
                    }
                    writeOp(OpCode.Define, Raw, "init")
                }
            }
            NodeType.Literal -> {
                if(node.raw == "null"
                    || node.raw == "true"
                    || node.raw == "false") {
                    writeOp(OpCode.Push, Raw, node.raw)
                    return
                }
                node.value?.toIntOrNull()?.let {
                    writeOp(OpCode.Push, Raw, it.toString())
                    return
                }
                //TODO: RegExp
                writeOp(OpCode.Push, Primitive, StringPrimitive(node.value!!))
            }
            NodeType.Identifier -> {
                val identifier = node.name!!
//                val resolved = contextStack.first()
//                    .resolveIdentifier(identifier, false) //TODO: strict
                writeOp(OpCode.Push, Identifier, identifier)
//                if(refPool.contains(resolved)) {
//                    val index = refPool.indexOf(resolved)
//                    writeOp(OpCode.Push, "#$index")
//                }
//                else {
//                    refPool.add(resolved)
//                    writeOp(OpCode.Push, "#${refPool.size-1}")
//                }
            }
            else -> {
                throw NotImplementedError("${node.type} is Not Implemented")
            }
        }
    }

    var contextStackOffset = 0
    private fun writeOp(operator: NonByteOp, type: NonByteOperation.NonByteDataType? = null, data: Any? = null) {
        if(operator is ContextOp) contextStackOffset++
//        byteLines.add(ByteOperation(operator, operand))
        if(type == null) {
            nonByteLines.add(NonByteOperation(operator))
        }
        else {
            nonByteLines.add(
                NonByteOperation(
                    operator,
                    NonByteData(type, data)
                )
            )
        }
    }
    private fun writeLabel(label: String) {
//        labelTable[label] = byteLines.size
        labelTable[label] = nonByteLines.size - contextStackOffset
    }
    private fun getUniqueLabel(): String {
        return "L${uniqueLabelIndex++}"
    }
//    private fun useConst(value: EcmaPrimitive): String {
//        constantPool.add(value)
//        return "@${constantPool.size-1}"
//    }
//    private fun useObject(value: ObjectData): String {
//        objectPool.add(value)
//        return "&${objectPool.size-1}"
//    }
//    private fun useCompletion(type: String, target: String, pop: Int): String {
//        return "C:$type,$target,$pop"
//    }

    private fun replaceLabel() {
        println("labels")
        labelTable.forEach { (t, u) ->
            println("$t -> $u")
        }
        for(i in nonByteLines.indices) {
            val line = nonByteLines[i] ?: continue
            val operand = line.data?.value as? String ?: continue
            if(operand.startsWith("C:")) {
                val target = operand.split(",")[1]
                nonByteLines[i] = line.copy(
                    data = NonByteData(
                        line.data.type,
                        operand.replace(target,labelTable[target].toString())
                    )
                )
                continue
            }
            labelTable[operand]?.let {
                nonByteLines[i] = line.copy(
                    data = NonByteData(
                        line.data.type,
                        "$it"
                    )
                )
            }
        }
//        for(i in byteLines.indices) {
//            val operand = byteLines[i].operand ?: continue
//            if(operand.startsWith("C:")) {
//                val target = operand.split(",")[1]
//                byteLines[i] = byteLines[i].copy(operand = operand.replace(target,labelTable[target].toString()))
//                continue
//            }
//            labelTable[operand]?.let {
//                byteLines[i] = byteLines[i].copy(operand = "$it")
//            }
//        }
    }

    fun resolveReference(
        input: List<NonByteOperation?>,
        context: ExecutionContext?,
        global: GlobalObject,
        currentRef: List<ReferenceData>? = null,
        currentConst: List<EcmaPrimitive>? = null,
        currentObj: List<ObjectData>? = null
    ): ByteData {
        val contextStack = ArrayDeque<ExecutionContext>()
        if(context != null) {
            contextStack.addFirst(context)
        }
        val refPool = mutableListOf<ReferenceData>()
        if(currentRef != null) refPool.addAll(currentRef)
        val constantPool = mutableListOf<EcmaPrimitive>()
        if(currentConst != null) constantPool.addAll(currentConst)
        val objectPool = mutableListOf<ObjectData>()
        if(currentObj != null) objectPool.addAll(currentObj)
        val byteLines = mutableListOf<ByteOperation?>()
        for(operation in input) {
            if(operation == null) {
                byteLines.add(null)
                continue
            }
            if(operation.opCode is ContextOp) {
                if(operation.opCode == ContextOp.Push) {
                    contextStack.addFirst(operation.data?.value as ExecutionContext)
                }
                else {
                    contextStack.removeFirst()
                }
                continue
            }
            else if(operation.opCode is OpCode) {
                when(operation.data?.type) {
                    null -> {
                        byteLines.add(ByteOperation(operation.opCode))
                    }
                    Raw -> {
                        byteLines.add(ByteOperation(operation.opCode, operation.data.value as String))
                    }
                    This -> {
                        val thisObj = contextStack.first().thisBinding
                        objectPool.add(thisObj)
                        byteLines.add(ByteOperation(
                            operation.opCode, "&${objectPool.size-1}"
                        ))
                    }
                    Object -> {
                        val obj = operation.data.value as ObjectData
                        objectPool.add(obj)
                        byteLines.add(ByteOperation(
                            operation.opCode, "&${objectPool.size-1}"
                        ))
                    }
                    FuncPart -> {
                        val part = operation.data.value as NonByteOperation.FunctionPart
                        val func = FunctionObject(
                            part.args, part.body,
                            contextStack.first().lexicalEnvironment,
                            false, globalObject!!
                        ) //TODO: strict
                        objectPool.add(func)
                        byteLines.add(ByteOperation(
                            operation.opCode, "&${objectPool.size-1}"
                        ))
                    }
                    Identifier -> {
                        val name = operation.data.value as String
                        val resolved = contextStack.first()
                            .resolveIdentifier(name, false) //TODO: strict
                        if(refPool.contains(resolved)) {
                            val index = refPool.indexOf(resolved)
                            byteLines.add(ByteOperation(
                                operation.opCode, "#$index"
                            ))
                        }
                        else {
                            refPool.add(resolved)
                            byteLines.add(ByteOperation(
                                operation.opCode, "#${refPool.size-1}"
                            ))
                        }
                    }
                    Primitive -> {
                        val primitive = operation.data.value as EcmaPrimitive
                        constantPool.add(primitive)
                        byteLines.add(ByteOperation(
                            operation.opCode, "@${constantPool.size-1}"
                        ))
                    }
                    WithPart -> {
                        val part = operation.data.value as NonByteOperation.WithPartData
                        val obj = WithObject(
                            part.body, contextStack.first(), global
                        )
                        objectPool.add(obj)
                        byteLines.add(ByteOperation(
                            operation.opCode, "&${objectPool.size-1}"
                        ))
                    }
                    Context -> throw Exception()
                }
            }
        }
        return ByteData(
            byteLines, refPool,
            constantPool, objectPool,
            global
        )
    }

    data class ByteData(
        val byteLines: List<ByteOperation?>,
        val refPool: List<ReferenceData>,
        val constantPool: List<EcmaPrimitive>,
        val objectPool: List<ObjectData>,
        val global: GlobalObject
    )

    data class NonByteOperation(
        val opCode: NonByteOp,
        val data: NonByteData? = null
    ) {
        data class NonByteData(
            val type: NonByteDataType,
            val value: Any? = null
        )
        enum class NonByteDataType {
            This, Object, Identifier,
            Raw, Primitive, Context,
            FuncPart, WithPart
        }
        data class FunctionPart(
            val args: List<String>,
            val body: Node
        )
        data class WithPartData(
            val body: List<NonByteOperation?>
        )
    }
    interface NonByteOp
    enum class ContextOp: NonByteOp {
        Push, Pop
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
    enum class OpCode: NonByteOp {
        Push, Pop, Add, Sub, Mul, Div, Rem,
        ShiftL, ShiftR, ShiftUR, And, Or, Xor,
        GT, GTE, LT, LTE, InstanceOf, In,
        Eq, Neq, EqS, NeqS, IfTrue, IfFalse,
        Delete, TypeOf, ToNum, Neg, Not, LogicalNot, Goto,
        GetValue, IfEmpty, Swap, Assign, Copy, Call,
        Return, ResolveMember, Define, New, With
    }

    data class ScopeData(
        val type: NodeType,
        val label: String? = null,
        val labelBreak: String? = null,
        val labelContinue: String? = null
    )
}
