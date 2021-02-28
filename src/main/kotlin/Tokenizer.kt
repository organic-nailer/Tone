package esTree

class Tokenizer(input: String) {
    var tokenized = listOf<TokenData>()
    private val reader = CharReader(null, input)
    private var readIndex = 0
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
            if(reader.prefixMatch(TokenKind.Plus.str)) {
                res.add(TokenData(TokenKind.Plus.str, TokenKind.Plus, lineNumber, lineIndex))
                continue
            }
            if(reader.prefixMatch(TokenKind.Minus.str)) {
                res.add(TokenData(TokenKind.Minus.str, TokenKind.Minus, lineNumber, lineIndex))
                continue
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
        Plus("+"), Minus("-"),
        EOF("EOF"), END("$")
    }
    data class TokenData(
        val raw: String,
        val kind: TokenKind,
        val startLine: Int,
        val startIndex: Int
    )
}