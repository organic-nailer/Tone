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
                Node(
                    type = NodeType.ExpressionStatement,
                    loc = Location(start = Position(0,0), end = preTree.end ?: Position(-1,-1)),
                    expression = preTree.toNode()
                )
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
            println("now: ${stack.first().first} to ${input.getOrNull(parseIndex)?.kind?.str}")
            val transition = parserGenerator.transitionMap[stack.first().first to input[parseIndex].kind.str]
            when(transition?.kind) {
                LALR1ParserGenerator.TransitionKind.SHIFT -> {
                    stack.addFirst(transition.value!! to input[parseIndex].kind.str)
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
                    newNode.start = nodeStack.firstOrNull()?.end
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
                EcmaGrammar.AdditiveExpression -> {
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
                EcmaGrammar.Number -> {
                    Node(
                        type = NodeType.Literal,
                        loc = Location(
                            start = this.start ?: Position(-1,-1),
                            end = this.end ?: Position(-1,-1)
                        ),
                        value = this.value?.toIntOrNull(),
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
        val operator: String? = null,
        val value: Int? = null,
        val raw: String? = null
    )
    enum class NodeType {
        Program, ExpressionStatement, Literal, BinaryExpression, UNKNOWN
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