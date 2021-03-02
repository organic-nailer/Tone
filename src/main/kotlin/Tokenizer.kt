package esTree

class Tokenizer(input: String) {
    var tokenized = listOf<TokenData>()
    private val reader = CharReader(null, input)
    private var readIndex = 0

    private val anonymousOperators = setOf(
        "==","!=","===","!==","<","<=",">",">=",
        "<<",">>",">>>","+","-","*","/","%","|",
        "^","&",
        "?", ":","!","~","++","--","(",")",
        "[","]",".",",",";","{","}"
    )
    private val assignmentOperators = setOf(
        "=","+=","-=","*=","/=","%=","<<=",">>=",
        ">>>=","|=","^=","&="
    )
    private val keywords = setOf(
        "this","in","instanceof","typeof","void","delete","new"
    )
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
            if(next.isWhitespace()) continue
            val lineNumber = reader.lineNumber
            val lineIndex = reader.index
            //println("now $next $lineIndex")
            if(next.isDigit()) {
                val d = reader.readNumber()
                if(d != null) {
                    res.add(TokenData(d, EcmaGrammar.NumericLiteral, lineNumber, lineIndex))
                }
                continue
            }
            val identifier = reader.readIdentifier()
            //println("readId=$identifier next=${reader.index}")
            if(identifier != null) {
                when {
                    identifier == "true" -> {
                        res.add(TokenData("true",EcmaGrammar.BooleanLiteral, lineNumber,lineIndex))
                    }
                    identifier == "false" -> {
                        res.add(TokenData("false",EcmaGrammar.BooleanLiteral, lineNumber,lineIndex))
                    }
                    identifier == "null" -> {
                        res.add(TokenData("null",EcmaGrammar.NullLiteral, lineNumber,lineIndex))
                    }
                    keywords.contains(identifier) -> {
                        res.add(TokenData(identifier, identifier, lineNumber, lineIndex))
                    }
                    else -> {
                        res.add(TokenData(identifier, EcmaGrammar.Identifier, lineNumber, lineIndex))
                    }
                }
                continue
            }
            for(operator in operators) {
                if(reader.prefixMatch(operator)) {
                    if(assignmentOperators.contains(operator)) {
                        res.add(TokenData(operator, EcmaGrammar.AssignmentOperator, lineNumber, lineIndex))
                        reader.index += operator.length-1
                        continue@loopMain
                    }
                    else {
                        res.add(TokenData(operator, operator, lineNumber, lineIndex))
                        reader.index += operator.length-1
                        continue@loopMain
                    }
                }
            }
        }
        res.add(TokenData("$", "$", reader.lineNumber, reader.index))
        readIndex = 0
        tokenized = res
    }

    fun getNextToken(): TokenData {
        if(tokenized.size <= readIndex) {
            return TokenData("", "EOF", reader.lineNumber, reader.index)
        }
        return tokenized[readIndex++]
    }

    data class TokenData(
        val raw: String,
        val kind: String,
        val startLine: Int,
        val startIndex: Int
    )
}