package esTree

import EcmaGrammar
import EcmaGrammar.anonymousOperators
import EcmaGrammar.assignmentOperators
import EcmaGrammar.keywords

class Tokenizer(input: String) {
    var tokenized = listOf<TokenData>()
    private val reader = CharReader(null, input)
    private var readIndex = 0

    private val operators = mutableListOf<String>()

    init {
        operators.clear()
        operators.addAll(anonymousOperators)
        operators.addAll(assignmentOperators)
        operators.distinct()
        operators.sortDescending()
        tokenize()
    }

    private fun tokenize() {
        val res = mutableListOf<TokenData>()
        loopMain@ while(true) {
            val next = reader.getNextChar()
            if(next == CharReader.EOF) break
            val lineNumber = reader.lineNumber
            val lineIndex = reader.index
            //println("now $next $lineIndex")
            if(next == CharReader.LINE_TERMINATOR) {
                res.add(TokenData(next.toString(), EcmaGrammar.Symbols.LineTerminator.ordinal, lineNumber, lineIndex))
                continue
            }
            if(next.isWhitespace()) continue
            if(next.isDigit()) {
                val d = reader.readNumber()
                if(d != null) {
                    res.add(TokenData(d, EcmaGrammar.Symbols.NumericLiteral.ordinal, lineNumber, lineIndex))
                }
                continue
            }
            val identifier = reader.readIdentifier()
            //println("readId=$identifier next=${reader.index}")
            if(identifier != null) {
                when {
                    identifier == "true" -> {
                        res.add(TokenData("true", EcmaGrammar.Symbols.BooleanLiteral.ordinal, lineNumber,lineIndex))
                    }
                    identifier == "false" -> {
                        res.add(TokenData("false", EcmaGrammar.Symbols.BooleanLiteral.ordinal, lineNumber,lineIndex))
                    }
                    identifier == "null" -> {
                        res.add(TokenData("null", EcmaGrammar.Symbols.NullLiteral.ordinal, lineNumber,lineIndex))
                    }
                    keywords.contains(identifier) -> {
                        res.add(TokenData(identifier, EcmaGrammar.operatorsMap[identifier]!!, lineNumber, lineIndex))
                    }
                    else -> {
                        res.add(TokenData(identifier, EcmaGrammar.Symbols.Identifier.ordinal, lineNumber, lineIndex))
                    }
                }
                continue
            }
            for(operator in operators) {
                if(reader.prefixMatch(operator)) {
                    if(assignmentOperators.contains(operator)) {
                        res.add(TokenData(operator, EcmaGrammar.Symbols.AssignmentOperator.ordinal, lineNumber, lineIndex))
                        reader.index += operator.length-1
                        continue@loopMain
                    }
                    else {
                        res.add(TokenData(operator, EcmaGrammar.operatorsMap[operator]!!, lineNumber, lineIndex))
                        reader.index += operator.length-1
                        continue@loopMain
                    }
                }
            }
        }
        res.add(TokenData("$", EcmaGrammar.operatorsMap["$"]!!, reader.lineNumber, reader.index))
        readIndex = 0
        tokenized = res
    }

    fun getNextToken(): TokenData {
        if(tokenized.size <= readIndex) {
            return TokenData("", EcmaGrammar.operatorsMap["EOF"]!!, reader.lineNumber, reader.index)
        }
        return tokenized[readIndex++]
    }

    data class TokenData(
        val raw: String,
        val kind: Int,
        val startLine: Int,
        val startIndex: Int
    )
}