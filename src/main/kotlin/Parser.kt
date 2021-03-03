package esTree

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis

class Parser {
    private val parserGenerator = LALR1ParserGenerator(
        EcmaGrammar.grammarParser(EcmaGrammar.es5Grammar),
        EcmaGrammar.es5StartSymbol
    )

    fun parse(input: List<Tokenizer.TokenData>): String? {
        var node: Node? = null
        measureTimeMillis {
            val preTree = parseInternal(input) ?: return null
            node = preTree.toNode()
        }.run { println("Parsed in $this ms") }
        val json = Json.encodeToString(node)
        println(json)
        return json
    }

    private fun parseInternal(input: List<Tokenizer.TokenData>): NodeInternal? {
        println("input: ${input.joinToString("")}")
        val stack = ArrayDeque<Pair<String,String>>()// state,token
        val nodeStack = ArrayDeque<NodeInternal>()
        var parseIndex = 0
        var accepted = false
        stack.addFirst("J0" to "")
        while(parseIndex < input.size || stack.isNotEmpty()) {
            println("now: ${stack.first().first} to ${input.getOrNull(parseIndex)?.kind}")
            val transition = parserGenerator.transitionMap[stack.first().first to input[parseIndex].kind]
            when(transition?.kind) {
                LALR1ParserGenerator.TransitionKind.SHIFT -> {
                    stack.addFirst(transition.value!! to input[parseIndex].kind)
                    nodeStack.addFirst(NodeInternal(
                        stack.first().second,
                        input[parseIndex].raw,
                        mutableListOf(),
                        Position(input[parseIndex].startLine, input[parseIndex].startIndex),
                        Position(input[parseIndex].startLine, input[parseIndex].startIndex + input[parseIndex].raw.length)
                    ))
                    parseIndex++
                }
                LALR1ParserGenerator.TransitionKind.REDUCE -> {
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
                LALR1ParserGenerator.TransitionKind.ACCEPT -> {
                    accepted = true
                    break
                }
                else -> {
                    throw Exception("パースエラー: $stack, $parseIndex")
                }
            }
        }
        if(!accepted) {
            println("受理されませんでした ${nodeStack.first()}")
            return null
        }
        println("nodeInternal: ")
        //nodeStack.first().print("")
        return nodeStack.first()
    }

    data class NodeInternal(
        val kind: String,
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
                EcmaGrammar.Program -> {
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
                EcmaGrammar.Block -> {
                    val bodyElements = mutableListOf<Node>()
                    var node = children[1]
                    while(true) {
                        if(node.children.size == 1) {
                            bodyElements.add(0,node.children[0].toNode())
                            break
                        }
                        bodyElements.add(0,node.children[0].toNode())
                        node = node.children[1]
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
                EcmaGrammar.VariableStatement -> {
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
                EcmaGrammar.VariableDeclaration -> {
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
                EcmaGrammar.EmptyStatement -> {
                    Node(
                        type = NodeType.EmptyStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                    )
                }
                EcmaGrammar.ExpressionStatement -> {
                    Node(
                        type = NodeType.ExpressionStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        expression = children[1].toNode()
                    )
                }
                EcmaGrammar.SourceElement,
                EcmaGrammar.Statement,
                EcmaGrammar.Literal,
                EcmaGrammar.LeftHandSideExpression,
                EcmaGrammar.Expression -> {
                    children[0].toNode()
                }
                EcmaGrammar.AssignmentExpression -> {
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
                EcmaGrammar.ConditionalExpression -> {
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
                EcmaGrammar.LogicalANDExpression,
                EcmaGrammar.LogicalORExpression -> {
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
                EcmaGrammar.AdditiveExpression,
                EcmaGrammar.MultiplicativeExpression,
                EcmaGrammar.ShiftExpression,
                EcmaGrammar.RelationalExpression,
                EcmaGrammar.EqualityExpression,
                EcmaGrammar.BitwiseANDExpression,
                EcmaGrammar.BitwiseXORExpression,
                EcmaGrammar.BitwiseORExpression-> {
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
                EcmaGrammar.UnaryExpression -> {
                    if(children.size >= 2) {
                        if(children[0].value == "++" || children[0].value == "--") {
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
                EcmaGrammar.PostfixExpression -> {
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
                EcmaGrammar.NewExpression -> {
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
                EcmaGrammar.CallExpression -> {
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
                EcmaGrammar.MemberExpression -> {
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
                EcmaGrammar.PrimaryExpression -> {
                    if(children.size == 3) {
                        children[1].toNode()
                    }
                    else {
                        children[0].toNode()
                    }
                }
                EcmaGrammar.ThisLiteral -> {
                    Node(
                        type = NodeType.ThisExpression,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                    )
                }
                EcmaGrammar.Identifier -> {
                    Node(
                        type = NodeType.Identifier,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        name = this.value
                    )
                }
                EcmaGrammar.ArrayLiteral -> {
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
                EcmaGrammar.NumericLiteral -> {
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
                EcmaGrammar.NullLiteral -> {
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
                EcmaGrammar.BooleanLiteral -> {
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
            if(this.kind == EcmaGrammar.Elision) {
                elements.add(0,null)
                if(children.size == 1) return
                children[1].calcElementList(elements)
                return
            }
            if(this.kind == EcmaGrammar.ElementList) {
                for(child in this.children) {
                    child.calcElementList(elements)
                }
                return
            }
            if(this.kind == EcmaGrammar.AssignmentExpression) {
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
        EmptyStatement,
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