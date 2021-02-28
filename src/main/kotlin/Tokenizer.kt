package esTree

class Tokenizer(input: String) {
    var tokenized = listOf<TokenData>()
    private val reader = CharReader(null, input)
    private var readIndex = 0

    private val binaryOperators = setOf(
        "==","!=","===","!==","<","<=",">",">=",
        "<<",">>",">>>","+","-","*","/","%","|",
        "^","&","in","instanceof"
    )
    private val assignmentOperators = setOf(
        "=","+=","-=","*=","/=","%=","<<=",">>=",
        ">>>=","|=","^=","&="
    )
    private val conditionalOperators = setOf(
        "?", ":"
    )
    private val operators = mutableListOf<String>()

    init {
        operators.clear()
        operators.addAll(binaryOperators)
        operators.addAll(assignmentOperators)
        operators.addAll(conditionalOperators)
        operators.sortDescending()
        tokenize()
    }

    private fun tokenize() {
        val res = mutableListOf<TokenData>()
        while(true) {
            val next = reader.getNextChar()
            if(next == CharReader.EOF) break
            if(next.isWhitespace()) continue
            val lineNumber = reader.lineNumber
            val lineIndex = reader.index
            if(next.isDigit()) {
                val d = reader.readNumber()
                if(d != null) {
                    res.add(TokenData(d, EcmaGrammar.Number, lineNumber, lineIndex))
                }
                continue
            }
            for(operator in operators) {
                if(reader.prefixMatch(operator)) {
                    if(assignmentOperators.contains(operator)) {
                        res.add(TokenData(operator, EcmaGrammar.AssignmentOperator, lineNumber, lineIndex))
                        reader.index += operator.length
                        continue
                    }
                    else {
                        res.add(TokenData(operator, operator, lineNumber, lineIndex))
                        reader.index += operator.length
                        continue
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