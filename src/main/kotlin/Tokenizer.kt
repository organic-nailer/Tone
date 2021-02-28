package esTree

class Tokenizer(input: String) {
    var tokenized = listOf<TokenData>()
    private val reader = CharReader(null, input)
    private var readIndex = 0

    private val operators = listOf(
        TokenKind.InstanceOf,
        TokenKind.URightShift, TokenKind.Identity, TokenKind.NoIdentity,
        TokenKind.LeftShift, TokenKind.RightShift,TokenKind.In,TokenKind.LTE,TokenKind.GTE,TokenKind.Eq,TokenKind.InEq,
        TokenKind.Plus,TokenKind.Minus,TokenKind.LT,TokenKind.GT,TokenKind.And,TokenKind.Xor,TokenKind.Or,TokenKind.Multi,TokenKind.Remainder,TokenKind.Div
    )

    init {
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
                    res.add(TokenData(d, TokenKind.NumberLiteral, lineNumber, lineIndex))
                }
                continue
            }
            for(operator in operators) {
                if(reader.prefixMatch(operator.str)) {
                    res.add(TokenData(operator.str, operator, lineNumber, lineIndex))
                    reader.index += operator.str.length
                    continue
                }
            }
        }
        res.add(TokenData("$", TokenKind.END, reader.lineNumber, reader.index))
        readIndex = 0
        tokenized = res
    }

    fun getNextToken(): TokenData {
        if(tokenized.size <= readIndex) {
            return TokenData("", TokenKind.EOF, reader.lineNumber, reader.index)
        }
        return tokenized[readIndex++]
    }

    enum class TokenKind(val str: String) {
        NumberLiteral("Number"),
        Plus("+"), Minus("-"), Multi("*"), Remainder("%"), Div("/"),
        LeftShift("<<"), RightShift(">>"), URightShift(">>>"),
        In("in"), InstanceOf("instanceof"),
        LT("<"), GT(">"), LTE("<="), GTE(">="),
        Eq("=="), InEq("!="), Identity("==="), NoIdentity("!=="),
        And("&"), Xor("^"), Or("|"),
        EOF("EOF"), END("$")
    }
    data class TokenData(
        val raw: String,
        val kind: TokenKind,
        val startLine: Int,
        val startIndex: Int
    )
}