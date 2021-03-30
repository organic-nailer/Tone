object ToneEngine {
    val parser = Parser()

    fun parseAsStatement(code: String): Parser.Node {
        val tokenized = Tokenizer(code).tokenized
        parser.parse(tokenized)
        return parser.parsedNode!!.body!![0]
    }
}
