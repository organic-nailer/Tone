import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis
import EcmaGrammar.Symbols.*

class Parser {
    private val parserGenerator = DragonParserGenerator(
        EcmaGrammar.grammarParserForLR0(EcmaGrammar.es5Grammar),
        EcmaGrammar.es5StartSymbol.ordinal
    )

    var parsedNode: Node? = null

    fun parse(input: List<Tokenizer.TokenData>): String? {
        measureTimeMillis {
            val preTree = parseInternal(input) ?: return null
            parsedNode = preTree.toNode()
        }.run { println("Parsed in $this ms") }
        val json = Json.encodeToString(parsedNode)
        println(json)
        return json
    }

    enum class ASIState {
        WAIT_SHIFT, EXPECT_REDUCE, NONE
    }

    private fun parseInternal(input: List<Tokenizer.TokenData>): NodeInternal? {
        //println("input: ${input.joinToString("")}")
        val inputMut = input.toMutableList()
        val stack = ArrayDeque<Pair<String,Int>>()// state,token
        val nodeStack = ArrayDeque<NodeInternal>()
        var parseIndex = 0
        var accepted = false
        var previousIsLineTerminator: Tokenizer.TokenData? = null
        var asiState = ASIState.NONE
        stack.addFirst("I0" to 0)
        while(parseIndex < inputMut.size || stack.isNotEmpty()) {
            //println("now: ${stack.first().first} to ${inputMut.getOrNull(parseIndex)?.kind}")
            if(inputMut[parseIndex].kind == LineTerminator.ordinal) {
                previousIsLineTerminator = inputMut[parseIndex]
                parseIndex++
                continue
            }
            val transition = parserGenerator.transitionMap[stack.first().first to inputMut[parseIndex].kind]
            if(transition?.kind == DragonParserGenerator.TransitionKind.SHIFT
                && previousIsLineTerminator != null) {
                //自動セミコロン挿入(Restricted Token)
                //println("!!!!${stack.first()} .. ${inputMut[parseIndex].kind}")
                if(((stack.first().second == LeftHandSideExpression.ordinal
                        || stack.first().second == LeftHandSideExpressionForStmt.ordinal)
                    && (inputMut[parseIndex].kind == EcmaGrammar.operatorsMap["++"] || inputMut[parseIndex].kind == EcmaGrammar.operatorsMap["--"]))
                    || stack.first().second == EcmaGrammar.operatorsMap["continue"]
                    || stack.first().second == EcmaGrammar.operatorsMap["break"]
                    || stack.first().second == EcmaGrammar.operatorsMap["return"]) {
                    val prev = inputMut.take(parseIndex).findLast { it.kind != LineTerminator.ordinal }
                    inputMut.add(parseIndex, Tokenizer.TokenData(
                        ";", EcmaGrammar.operatorsMap[";"]!!,
                        prev?.startLine ?: 1,
                        (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0) - 1,
                        prev?.startLine ?: 1, (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0),
                    )
                    )
                    previousIsLineTerminator = null
                    asiState = ASIState.WAIT_SHIFT
                    //println("ASI: $parseIndex => $inputMut")
                    continue
                }
            }
            when(transition?.kind) {
                DragonParserGenerator.TransitionKind.SHIFT -> {
                    if(asiState == ASIState.EXPECT_REDUCE) {
                        throw Exception("パースエラー($asiState): $stack, $parseIndex")
                    }
                    stack.addFirst(transition.value!! to inputMut[parseIndex].kind)
                    nodeStack.addFirst(
                        NodeInternal(
                        stack.first().second,
                        inputMut[parseIndex].raw,
                        mutableListOf(),
                        Position(inputMut[parseIndex].startLine, inputMut[parseIndex].startIndex),
                        Position(inputMut[parseIndex].endLine, inputMut[parseIndex].endIndex)
                    )
                    )
                    previousIsLineTerminator = null
                    parseIndex++
                    if(asiState == ASIState.WAIT_SHIFT) {
                        asiState = ASIState.EXPECT_REDUCE
                    }
                }
                DragonParserGenerator.TransitionKind.REDUCE -> {
                    if(asiState == ASIState.EXPECT_REDUCE) {
                        if(transition.rule?.left == EmptyStatement.ordinal) {
                            throw Exception("パースエラー($asiState): $stack, $parseIndex")
                        }
                        asiState = ASIState.NONE
                    }
                    val rule = transition.rule!!
                    val newNode = NodeInternal(rule.left, null, mutableListOf(), null, nodeStack.first().end)
                    var startStack = nodeStack.first()
                    for(t in rule.right.reversed()) {
                        if(stack.first().second == t) {
                            stack.removeFirst()
                            startStack = nodeStack.removeFirst()
                            newNode.children.add(startStack)
                        }
                        else {
                            throw Exception("還元時エラー $rule, $stack, $t")
                        }
                    }
                    parserGenerator.transitionMap[stack.first().first to rule.left]?.value?.let {
                        stack.addFirst(it to rule.left)
                    } ?: kotlin.run {
                        throw Exception("還元shiftエラー $rule, $stack")
                    }
                    newNode.start = startStack.start ?: Position(1,0)
                    nodeStack.addFirst(newNode)
                }
                DragonParserGenerator.TransitionKind.ACCEPT -> {
                    accepted = true
                    break
                }
                else -> {
                    if(previousIsLineTerminator != null) {
                        //当該トークンの前に改行がある
                        val prev = inputMut.take(parseIndex).findLast { it.kind != LineTerminator.ordinal }
                        //inputMut.getOrNull(parseIndex-1)
                        inputMut.add(parseIndex, Tokenizer.TokenData(
                            ";", EcmaGrammar.operatorsMap[";"]!!,
                            prev?.startLine ?: 1,
                            (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0) - 1,
                            prev?.startLine ?: 1,
                            (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0)
                        )
                        )
                        previousIsLineTerminator = null
                        asiState = ASIState.WAIT_SHIFT
                        //println("ASI: $parseIndex => $inputMut")
                        continue
                    }
                    else if(inputMut[parseIndex].kind == EcmaGrammar.operatorsMap["}"] || inputMut[parseIndex].kind == EcmaGrammar.operatorsMap["$"]) {
                        //当該トークンが閉じ中括弧である
                        //or ファイル末尾で解釈不能
                        val prev = inputMut.take(parseIndex).findLast { it.kind != LineTerminator.ordinal }
                        inputMut.add(parseIndex, Tokenizer.TokenData(
                            ";", EcmaGrammar.operatorsMap[";"]!!,
                            prev?.startLine ?: 1,
                            (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0) - 1,
                            prev?.startLine ?: 1,
                            (prev?.startIndex ?: 0) + (prev?.raw?.length ?: 0)
                        )
                        )
                        previousIsLineTerminator = null
                        asiState = ASIState.WAIT_SHIFT
                        //println("ASI: $parseIndex => $inputMut")
                        continue
                    }
                    throw Exception("パースエラー: $stack, $parseIndex")
                }
            }
        }
        if(!accepted) {
            println("受理されませんでした ${nodeStack.first()}")
            return null
        }
        //println("nodeInternal: ")
        //nodeStack.first().print("")
        return nodeStack.first()
    }

    data class NodeInternal(
        val kind: Int,
        val value: String?,
        val children: MutableList<NodeInternal>,
        var start: Position?,
        var end: Position?,
    ) {
        fun print(indent: String) {
            val values = if(value != null) "$start,$end,$value" else "$start,$end"
            if(children.isEmpty()) {
                println("$indent$kind($values)")
                return
            }
            println("$indent$kind($values):c=[")
            children.reversed().forEach { it.print("$indent  ") }
            println("$indent]")
        }

        override fun toString(): String {
            return if(value != null){
                "$kind($start,$end,$value):c=$children"
            } else {
                "$kind($start,$end):c=$children"
            }
        }

        fun toNode(): Node {
            return when(kind) {
                Program.ordinal -> {
                    val bodyElements = mutableListOf<Node>()
                    var node = children[0]
                    while(true) {
                        if(node.children.size == 1) {
                            bodyElements.add(0,node.children[0].toNode())
                            break
                        }
                        bodyElements.add(0,node.children[0].toNode())
                        node = node.children[1]
                    }
                    Node(
                        type = NodeType.Program,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        body = bodyElements
                    )
                }
                FunctionDeclaration.ordinal -> {
                    if(children.size == 7) {
                        val bodyElements = mutableListOf<Node>()
                        if(children[1].children.size == 1) {
                            var node = children[1].children[0]
                            while(true) {
                                if(node.children.size == 1) {
                                    bodyElements.add(0,node.children[0].toNode())
                                    break
                                }
                                bodyElements.add(0,node.children[0].toNode())
                                node = node.children[1]
                            }
                        }
                        Node(
                            type = NodeType.FunctionDeclaration,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            id = children[5].toNode(),
                            params = listOf(),
                            bodySingle = Node(
                                type = NodeType.BlockStatement,
                                loc = Location(
                                    start = children[2].start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                body = bodyElements
                            )
                        )
                    }
                    else {
                        val argList = mutableListOf<Node>()
                        var node = children[4]
                        while(true) {
                            if(node.children.size == 1) {
                                argList.add(0,node.children[0].toNode())
                                break
                            }
                            argList.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                        val bodyElements = mutableListOf<Node>()
                        if(children[1].children.size == 1) {
                            node = children[1].children[0]
                            while(true) {
                                if(node.children.size == 1) {
                                    bodyElements.add(0,node.children[0].toNode())
                                    break
                                }
                                bodyElements.add(0,node.children[0].toNode())
                                node = node.children[1]
                            }
                        }
                        Node(
                            type = NodeType.FunctionDeclaration,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            id = children[6].toNode(),
                            params = argList,
                            bodySingle = Node(
                                type = NodeType.BlockStatement,
                                loc = Location(
                                    start = children[2].start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                body = bodyElements
                            )
                        )
                    }
                }
                FunctionExpression.ordinal -> {
                    val bodyElements = mutableListOf<Node>()
                    if(children[1].children.size == 1) {
                        var node = children[1].children[0]
                        while(true) {
                            if(node.children.size == 1) {
                                bodyElements.add(0,node.children[0].toNode())
                                break
                            }
                            bodyElements.add(0,node.children[0].toNode())
                            node = node.children[1]
                        }
                    }
                    val argList = mutableListOf<Node>()
                    if(children[4].kind == FormalParameterList.ordinal) {
                        var node = children[4]
                        while(true) {
                            if(node.children.size == 1) {
                                argList.add(0,node.children[0].toNode())
                                break
                            }
                            argList.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                    }
                    val identifier = if(children.size == 7 && children[5].kind == Identifier.ordinal) {
                        children[5].toNode()
                    } else if(children.size == 8 && children[6].kind == Identifier.ordinal) {
                        children[6].toNode()
                    } else null
                    Node(
                        type = NodeType.FunctionExpression,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        id = identifier,
                        params = argList,
                        bodySingle = Node(
                            type = NodeType.BlockStatement,
                            loc = Location(
                                start = children[2].start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            body = bodyElements
                        )
                    )
                }
                Block.ordinal -> {
                    val bodyElements = mutableListOf<Node>()
                    if(children[1].children.isNotEmpty()) {
                        var node = children[1]
                        while(true) {
                            if(node.children.size == 1) {
                                bodyElements.add(0,node.children[0].toNode())
                                break
                            }
                            bodyElements.add(0,node.children[0].toNode())
                            node = node.children[1]
                        }
                    }
                    Node(
                        type = NodeType.BlockStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        body = bodyElements
                    )
                }
                VariableStatement.ordinal -> {
                    val decsElements = mutableListOf<Node>()
                    var node = children[1]
                    while(true) {
                        if(node.children.size == 1) {
                            decsElements.add(0,node.children[0].toNode())
                            break
                        }
                        decsElements.add(0,node.children[0].toNode())
                        node = node.children[2]
                    }
                    Node(
                        type = NodeType.VariableDeclaration,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        declarations = decsElements,
                        kind = "var"
                    )
                }
                IfStatement.ordinal -> {
                    if(children.size == 7) {
                        Node(
                            type = NodeType.IfStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[4].toNode(),
                            consequent = children[2].toNode(),
                            alternate = children[0].toNode()
                        )
                    }
                    else {
                        Node(
                            type = NodeType.IfStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[2].toNode(),
                            consequent = children[0].toNode(),
                            alternate = null
                        )
                    }
                }
                IterationStatement.ordinal -> {
                    if(children.size == 7 && children[6].value == "do") {
                        Node(
                            type = NodeType.DoWhileStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[2].toNode(),
                            bodySingle = children[5].toNode()
                        )
                    }
                    else if(children.size == 5) {
                        Node(
                            type = NodeType.WhileStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[2].toNode(),
                            bodySingle = children[0].toNode()
                        )
                    }
                    else if(children.size == 9) {
                        Node(
                            type = NodeType.ForStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            init = children[6].toNode(),
                            test = children[4].toNode(),
                            update = children[2].toNode(),
                            bodySingle = children[0].toNode()
                        )
                    }
                    else if(children.size == 10) {
                        val decsElements = mutableListOf<Node>()
                        var node = children[6]
                        while(true) {
                            if(node.children.size == 1) {
                                decsElements.add(0,node.children[0].toNode())
                                break
                            }
                            decsElements.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                        val initNode = Node(
                            type = NodeType.VariableDeclaration,
                            loc = Location(
                                start = children[7].start ?: Position(-1,-1),
                                end = children[6].end ?: Position(-1,-1)
                            ),
                            declarations = decsElements,
                            kind = "var"
                        )
                        Node(
                            type = NodeType.ForStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            init = initNode,
                            test = children[4].toNode(),
                            update = children[2].toNode(),
                            bodySingle = children[0].toNode()
                        )
                    }
                    else if(children.size == 7) {
                        Node(
                            type = NodeType.ForInStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            left = children[4].toNode(),
                            right = children[2].toNode(),
                            bodySingle = children[0].toNode()
                        )
                    }
                    else {
                        Node(
                            type = NodeType.ForInStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            left = Node(
                                type = NodeType.VariableDeclaration,
                                loc = Location(
                                    start = children[5].start ?: Position(-1,-1),
                                    end = children[4].end ?: Position(-1,-1)
                                ),
                                declarations = listOf(
                                    children[4].toNode()
                                ),
                                kind = "var"
                            ),
                            right = children[2].toNode(),
                            bodySingle = children[0].toNode()
                        )
                    }
                }
                VariableDeclarationNoIn.ordinal,
                VariableDeclaration.ordinal -> {
                    if(children.size == 2) {
                        Node(
                            type = NodeType.VariableDeclarator,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            id = children[1].toNode(),
                            init = children[0].children[0].toNode()
                        )
                    }
                    else {
                        Node(
                            type = NodeType.VariableDeclarator,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            id = children[0].toNode()
                        )
                    }
                }
                EmptyStatement.ordinal -> {
                    Node(
                        type = NodeType.EmptyStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                    )
                }
                ExpressionStatement.ordinal -> {
                    Node(
                        type = NodeType.ExpressionStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        expression = children[1].toNode()
                    )
                }
                ContinueStatement.ordinal -> {
                    Node(
                        type = NodeType.ContinueStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        label = if(children.size == 3) children[1].toNode() else null
                    )
                }
                BreakStatement.ordinal -> {
                    Node(
                        type = NodeType.BreakStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        label = if(children.size == 3) children[1].toNode() else null
                    )
                }
                ReturnStatement.ordinal -> {
                    Node(
                        type = NodeType.ReturnStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        argument = if(children.size == 3) children[1].toNode() else null
                    )
                }
                WithStatement.ordinal -> {
                    Node(
                        type = NodeType.WithStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        `object` = children[2].toNode(),
                        bodySingle = children[0].toNode()
                    )
                }
                SwitchStatement.ordinal -> {
                    val cases = mutableListOf<Node>()
                    children[0].children.forEach { c ->
                        when(c.kind) {
                            CaseClauses.ordinal -> {
                                var node = c
                                while(true) {
                                    if(node.children.size == 1) {
                                        cases.add(0,node.children[0].toNode())
                                        break
                                    }
                                    cases.add(0,node.children[0].toNode())
                                    node = node.children[1]
                                }
                            }
                            DefaultClause.ordinal -> {
                                cases.add(0,c.toNode())
                            }
                        }
                    }
                    Node(
                        type = NodeType.SwitchStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        discriminant = children[2].toNode(),
                        cases = cases
                    )
                }
                CaseClause.ordinal -> {
                    val consequents = mutableListOf<Node>()
                    if(children.size == 4) {
                        var node = children[0]
                        while(true) {
                            if(node.children.size == 1) {
                                consequents.add(0,node.children[0].toNode())
                                break
                            }
                            consequents.add(0,node.children[0].toNode())
                            node = node.children[1]
                        }
                    }
                    Node(
                        type = NodeType.SwitchCase,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        test = if(children.size == 4) children[2].toNode() else children[1].toNode(),
                        consequents = consequents
                    )
                }
                DefaultClause.ordinal -> {
                    val consequents = mutableListOf<Node>()
                    if(children.size == 3) {
                        var node = children[0]
                        while(true) {
                            if(node.children.size == 1) {
                                consequents.add(0,node.children[0].toNode())
                                break
                            }
                            consequents.add(0,node.children[0].toNode())
                            node = node.children[1]
                        }
                    }
                    Node(
                        type = NodeType.SwitchCase,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        test = null,
                        consequents = consequents
                    )
                }
                LabelledStatement.ordinal -> {
                    Node(
                        type = NodeType.LabeledStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        label = children[2].toNode(),
                        bodySingle = children[0].toNode()
                    )
                }
                ThrowStatement.ordinal -> {
                    Node(
                        type = NodeType.ThrowStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        argument = children[1].toNode()
                    )
                }
                TryStatement.ordinal -> {
                    if(children.size == 3 && children[0].kind == Catch.ordinal) {
                        Node(
                            type = NodeType.TryStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            block = children[1].toNode(),
                            handler = children[0].toNode()
                        )
                    }
                    else if(children.size == 3) {
                        Node(
                            type = NodeType.TryStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            block = children[1].toNode(),
                            finalizer = children[0].toNode()
                        )
                    }
                    else {
                        Node(
                            type = NodeType.TryStatement,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            block = children[2].toNode(),
                            handler = children[1].toNode(),
                            finalizer = children[0].toNode()
                        )
                    }
                }
                Catch.ordinal -> {
                    Node(
                        type = NodeType.CatchClause,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        param = children[2].toNode(),
                        bodySingle = children[0].toNode()
                    )
                }
                DebuggerStatement.ordinal -> {
                    Node(
                        type = NodeType.DebuggerStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        )
                    )
                }
                Finally.ordinal,
                SourceElement.ordinal,
                Statement.ordinal,
                Literal.ordinal,
                LeftHandSideExpression.ordinal,
                LeftHandSideExpressionForStmt.ordinal,
                ExpressionNoIn.ordinal,
                ExpressionForStmt.ordinal -> {
                    children[0].toNode()
                }
                Expression.ordinal -> {
                    if(children.size == 3) {
                        val expressionsList = mutableListOf<Node>()
                        var node = this
                        while(true) {
                            if(node.children.size == 1) {
                                expressionsList.add(0,node.children[0].toNode())
                                break
                            }
                            expressionsList.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                        Node(
                            type = NodeType.SequenceExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            expressions = expressionsList
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                AssignmentExpressionForStmt.ordinal,
                AssignmentExpressionNoIn.ordinal,
                AssignmentExpression.ordinal -> {
                    if(children.size >= 2) {
                        Node(
                            type = NodeType.AssignmentExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            left = children[2].toNode(),
                            right = children[0].toNode(),
                            operator = children[1].value
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                ConditionalExpressionForStmt.ordinal,
                ConditionalExpressionNoIn.ordinal,
                ConditionalExpression.ordinal -> {
                    if(children.size == 5) {
                        Node(
                            type = NodeType.ConditionalExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[4].toNode(),
                            consequent = children[2].toNode(),
                            alternate = children[0].toNode()
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                LogicalORExpressionForStmt.ordinal,
                LogicalANDExpressionForStmt.ordinal,
                LogicalANDExpressionNoIn.ordinal,
                LogicalORExpressionNoIn.ordinal,
                LogicalANDExpression.ordinal,
                LogicalORExpression.ordinal -> {
                    if(children.size >= 2) {
                        Node(
                            type = NodeType.LogicalExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            left = children[2].toNode(),
                            right = children[0].toNode(),
                            operator = children[1].value
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                AdditiveExpressionForStmt.ordinal,
                MultiplicativeExpressionForStmt.ordinal,
                ShiftExpressionForStmt.ordinal,
                RelationalExpressionForStmt.ordinal,
                EqualityExpressionForStmt.ordinal,
                BitwiseANDExpressionForStmt.ordinal,
                BitwiseXORExpressionForStmt.ordinal,
                BitwiseORExpressionForStmt.ordinal,
                AdditiveExpression.ordinal,
                MultiplicativeExpression.ordinal,
                ShiftExpression.ordinal,
                RelationalExpressionNoIn.ordinal,
                EqualityExpressionNoIn.ordinal,
                BitwiseANDExpressionNoIn.ordinal,
                BitwiseXORExpressionNoIn.ordinal,
                BitwiseORExpressionNoIn.ordinal,
                RelationalExpression.ordinal,
                EqualityExpression.ordinal,
                BitwiseANDExpression.ordinal,
                BitwiseXORExpression.ordinal,
                BitwiseORExpression.ordinal-> {
                    if(children.size >= 2) {
                        Node(
                            type = NodeType.BinaryExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            left = children[2].toNode(),
                            right = children[0].toNode(),
                            operator = children[1].value
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                UnaryExpressionForStmt.ordinal,
                UnaryExpression.ordinal -> {
                    if(children.size >= 2) {
                        if(children[1].value == "++" || children[1].value == "--") {
                            Node(
                                type = NodeType.UpdateExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                prefix = true,
                                argument = children[0].toNode(),
                                operator = children[1].value
                            )
                        }
                        else {
                            Node(
                                type = NodeType.UnaryExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                argument = children[0].toNode(),
                                operator = children[1].value
                            )
                        }
                    }
                    else {
                        children[0].toNode()
                    }
                }
                PostfixExpressionForStmt.ordinal,
                PostfixExpression.ordinal -> {
                    if(children.size >= 2) {
                        Node(
                            type = NodeType.UpdateExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            prefix = false,
                            argument = children[1].toNode(),
                            operator = children[0].value
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                NewExpressionForStmt.ordinal,
                NewExpression.ordinal -> {
                    if(children.size >= 2) {
                        Node(
                            type = NodeType.NewExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            callee = children[0].toNode(),
                            arguments = listOf()
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                CallExpressionForStmt.ordinal,
                CallExpression.ordinal -> {
                    if(children.size == 4) {
                        Node(
                            type = NodeType.MemberExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            `object` = children[3].toNode(),
                            property = children[1].toNode(),
                            computed = true,
                        )
                    }
                    else if(children.size == 3) {
                        Node(
                            type = NodeType.MemberExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            `object` = children[2].toNode(),
                            property = children[0].toNode(),
                            computed = false
                        )
                    }
                    else {
                        if(children[0].children.size == 2) {
                            Node(
                                type = NodeType.CallExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                callee = children[1].toNode(),
                                arguments = listOf()
                            )
                        }
                        else {
                            val argList = mutableListOf<Node>()
                            var node = children[0].children[1]
                            while(true) {
                                if(node.children.size == 1) {
                                    argList.add(0,node.children[0].toNode())
                                    break
                                }
                                argList.add(0,node.children[0].toNode())
                                node = node.children[2]
                            }
                            Node(
                                type = NodeType.CallExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                callee = children[1].toNode(),
                                arguments = argList
                            )
                        }
                    }
                }
                MemberExpressionForStmt.ordinal,
                MemberExpression.ordinal -> {
                    if(children.size == 4) {
                        Node(
                            type = NodeType.MemberExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            `object` = children[3].toNode(),
                            property = children[1].toNode(),
                            computed = true
                        )
                    }
                    else if(children.size == 3 && children[2].value == "new") {
                        if(children[0].children.size == 2) {
                            Node(
                                type = NodeType.NewExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                callee = children[1].toNode(),
                                arguments = listOf()
                            )
                        }
                        else {
                            val argList = mutableListOf<Node>()
                            var node = children[0].children[1]
                            while(true) {
                                if(node.children.size == 1) {
                                    argList.add(0,node.children[0].toNode())
                                    break
                                }
                                argList.add(0,node.children[0].toNode())
                                node = node.children[2]
                            }
                            Node(
                                type = NodeType.NewExpression,
                                loc = Location(
                                    start = this.start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                callee = children[1].toNode(),
                                arguments = argList
                            )
                        }
                    }
                    else if(children.size == 3) {
                        Node(
                            type = NodeType.MemberExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            `object` = children[2].toNode(),
                            property = children[0].toNode(),
                            computed = false
                        )
                    }
                    else {
                        children[0].toNode()
                    }
                }
                PrimaryExpressionForStmt.ordinal,
                PrimaryExpression.ordinal -> {
                    if(children.size == 3) {
                        children[1].toNode()
                    }
                    else {
                        children[0].toNode()
                    }
                }
                ThisLiteral.ordinal -> {
                    Node(
                        type = NodeType.ThisExpression,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                    )
                }
                Identifier.ordinal -> {
                    Node(
                        type = NodeType.Identifier,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        name = this.value
                    )
                }
                ArrayLiteral.ordinal -> {
                    val elements = mutableListOf<Node?>()
                    children.subList(1,children.size-1).forEach { node ->
                        node.calcElementList(elements)
                    }
                    Node(
                        type = NodeType.ArrayExpression,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        elements = elements
                    )
                }
                ObjectLiteral.ordinal -> {
                    if(children.size == 2) {
                        Node(
                            type = NodeType.ObjectExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            properties = listOf()
                        )
                    }
                    else if(children.size == 3) {
                        val propsList = mutableListOf<Node>()
                        var node = children[1]
                        while(true) {
                            if(node.children.size == 1) {
                                propsList.add(0,node.children[0].toNode())
                                break
                            }
                            propsList.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                        Node(
                            type = NodeType.ObjectExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            properties = propsList
                        )
                    }
                    else {
                        val propsList = mutableListOf<Node>()
                        var node = children[2]
                        while(true) {
                            if(node.children.size == 1) {
                                propsList.add(0,node.children[0].toNode())
                                break
                            }
                            propsList.add(0,node.children[0].toNode())
                            node = node.children[2]
                        }
                        Node(
                            type = NodeType.ObjectExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            properties = propsList
                        )
                    }
                }
                PropertyAssignment.ordinal -> {
                    if(children.size == 3) {
                        Node(
                            type = NodeType.Property,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            valueNode = children[0].toNode(),
                            key = children[2].children[0].toNode(),
                            kind = "init"
                        )
                    }
                    else if(children.size == 7) {
                        val bodyElements = mutableListOf<Node>()
                        if(children[1].children.size == 1) {
                            var node = children[1].children[0]
                            while(true) {
                                if(node.children.size == 1) {
                                    bodyElements.add(0,node.children[0].toNode())
                                    break
                                }
                                bodyElements.add(0,node.children[0].toNode())
                                node = node.children[1]
                            }
                        }
                        Node(
                            type = NodeType.Property,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            valueNode = Node(
                                type = NodeType.FunctionExpression,
                                loc = Location(
                                    start = children[4].start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                params = listOf(),
                                bodySingle = Node(
                                    type = NodeType.BlockStatement,
                                    loc = Location(
                                        start = children[2].start ?: Position(-1,-1),
                                        end = this.end ?: Position(-1,-1)
                                    ),
                                    body = bodyElements
                                )
                            ),
                            key = children[5].children[0].toNode(),
                            kind = "get"
                        )
                    }
                    else {
                        val bodyElements = mutableListOf<Node>()
                        if(children[1].children.size == 1) {
                            var node = children[1].children[0]
                            while(true) {
                                if(node.children.size == 1) {
                                    bodyElements.add(0,node.children[0].toNode())
                                    break
                                }
                                bodyElements.add(0,node.children[0].toNode())
                                node = node.children[1]
                            }
                        }
                        Node(
                            type = NodeType.Property,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            valueNode = Node(
                                type = NodeType.FunctionExpression,
                                loc = Location(
                                    start = children[5].start ?: Position(-1,-1),
                                    end = this.end ?: Position(-1,-1)
                                ),
                                params = listOf(
                                    children[4].children[0].toNode()
                                ),
                                bodySingle = Node(
                                    type = NodeType.BlockStatement,
                                    loc = Location(
                                        start = children[2].start ?: Position(-1,-1),
                                        end = this.end ?: Position(-1,-1)
                                    ),
                                    body = bodyElements
                                )
                            ),
                            key = children[6].children[0].toNode(),
                            kind = "set"
                        )
                    }
                }
                StringLiteral.ordinal -> {
                    Node(
                        type = NodeType.Literal,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        value = this.value!!.toString(),
                        raw = this.value
                    )
                }
                NumericLiteral.ordinal -> {
                    Node(
                        type = NodeType.Literal,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        value = this.value?.toIntOrNull()?.toString(),
                        raw = this.value
                    )
                }
                NullLiteral.ordinal -> {
                    Node(
                        type = NodeType.Literal,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        value = null,
                        raw = this.value
                    )
                }
                BooleanLiteral.ordinal -> {
                    Node(
                        type = NodeType.Literal,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        value = (this.value == "true").toString(),
                        raw = this.value
                    )
                }
                else -> {
                    println("UnKnownType Detected: $kind")
                    Node(
                        type = NodeType.UNKNOWN,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                    )
                }
            }
        }

        private fun calcElementList(elements: MutableList<Node?>) {
            if(this.kind == Elision.ordinal) {
                elements.add(0,null)
                if(children.size == 1) return
                children[1].calcElementList(elements)
                return
            }
            if(this.kind == ElementList.ordinal) {
                for(child in this.children) {
                    child.calcElementList(elements)
                }
                return
            }
            if(this.kind == AssignmentExpression.ordinal) {
                elements.add(0, this.toNode())
                return
            }
        }
    }

    @Serializable
    data class Node(
        val type: NodeType,
        val loc: Location,
        val body: List<Node>? = null,
        val expression: Node? = null,
        val left: Node? = null,
        val right: Node? = null,
        val test: Node? = null,
        val alternate: Node? = null,
        val consequent: Node? = null,
        val argument: Node? = null,
        val prefix: Boolean? = null,
        val arguments: List<Node>? = null,
        val callee: Node? = null,
        val `object`: Node? = null,
        val property: Node? = null,
        val elements: List<Node?>? = null,
        val declarations: List<Node>? = null,
        val id: Node? = null,
        val init: Node? = null,
        val bodySingle: Node? = null,
        val update: Node? = null,
        val label: Node? = null,
        val discriminant: Node? = null,
        val cases: List<Node>? = null,
        val consequents: List<Node>? = null,
        val block: Node? = null,
        val handler: Node? = null,
        val finalizer: Node? = null,
        val param: Node? = null,
        val params: List<Node>? = null,
        val properties: List<Node>? = null,
        val valueNode: Node? = null,
        val key: Node? = null,
        val expressions: List<Node>? = null,
        val operator: String? = null,
        val computed: Boolean? = null,
        val value: String? = null, //変換したものをStringで出力
        val name: String? = null,
        val raw: String? = null,
        val kind: String? = null
    )
    enum class NodeType {
        Program, ExpressionStatement, Literal,
        BinaryExpression, LogicalExpression,
        ConditionalExpression, AssignmentExpression,
        UnaryExpression, UpdateExpression,
        NewExpression, ThisExpression,
        CallExpression, MemberExpression,
        Identifier, ArrayExpression,
        BlockStatement,
        VariableDeclaration, VariableDeclarator,
        EmptyStatement, IfStatement,
        WhileStatement, DoWhileStatement, ForStatement,
        ForInStatement, ContinueStatement, BreakStatement,
        ReturnStatement, WithStatement, SwitchStatement,
        SwitchCase, LabeledStatement, ThrowStatement,
        TryStatement, CatchClause, DebuggerStatement,
        FunctionDeclaration, FunctionExpression,
        ObjectExpression, Property, SequenceExpression,
        UNKNOWN
    }
    @Serializable
    data class Location(
        val start: Position,
        val end: Position
    )
    @Serializable
    data class Position(
        val line: Int,
        val column: Int,
    ) {
        override fun toString(): String {
            return "P$line:$column"
        }
    }
}