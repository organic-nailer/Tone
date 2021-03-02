package esTree

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Parser {
    private val parserGenerator = LALR1ParserGenerator(
        EcmaGrammar.grammarParser(EcmaGrammar.es5Grammar),
        EcmaGrammar.es5StartSymbol
    )

    fun parse(input: List<Tokenizer.TokenData>): String? {
        val preTree = parseInternal(input) ?: return null

        val node = Node(
            type = NodeType.Program,
            loc = Location(start = Position(0,0), end = preTree.end ?: Position(-1,-1)),
            body = listOf(
                preTree.toNode()
            )
        )
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
                    for(t in rule.right.reversed()) {
                        if(stack.first().second == t) {
                            stack.removeFirst()
                            newNode.children.add(nodeStack.removeFirst())
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
                    newNode.start = nodeStack.firstOrNull()?.end ?: Position(0,0)
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
        println("nodeInternal: ${nodeStack.first()}")
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
            println(indent + this.kind)
            this.children.forEach { it.print("$indent  ") }
        }

        fun toNode(): Node {
            return when(kind) {
                EcmaGrammar.ExpressionStatement -> {
                    Node(
                        type = NodeType.ExpressionStatement,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        expression = children[0].toNode()
                    )
                }
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
                    if(children.size >= 3) {
                        Node(
                            type = NodeType.ConditionalExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            test = children[2].toNode(),
                            consequent = children[1].toNode(),
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
                        val argList = if(children[0].children.size == 2) null else children[0].children[1]
                        Node(
                            type = NodeType.CallExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            callee = children[1].toNode(),
                            arguments = argList?.children
                                ?.filter { a -> a.value != "," }
                                ?.map { a -> a.toNode() }
                                ?.asReversed()
                        )
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
                        val argList = if(children[0].children.size == 2) null else children[0].children[1]
                        Node(
                            type = NodeType.NewExpression,
                            loc = Location(
                                start = this.start ?: Position(-1,-1),
                                end = this.end ?: Position(-1,-1)
                            ),
                            callee = children[2].toNode(),
                            arguments = argList?.children
                                ?.filter { a -> a.value != "," }
                                ?.map { a -> a.toNode() }
                                ?.asReversed()
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
        val operator: String? = null,
        val computed: Boolean? = null,
        val value: String? = null, //変換したものをStringで出力
        val name: String? = null,
        val raw: String? = null
    )
    enum class NodeType {
        Program, ExpressionStatement, Literal,
        BinaryExpression, LogicalExpression,
        ConditionalExpression, AssignmentExpression,
        UnaryExpression, UpdateExpression,
        NewExpression, ThisExpression,
        CallExpression, MemberExpression,
        Identifier,
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
    )
}