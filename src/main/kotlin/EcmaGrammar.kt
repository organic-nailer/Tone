package esTree

object EcmaGrammar {
    val es5Grammar = listOf(
        "AddExpression ::= Number |/ AddExpression + Number"
    )
    val es5StartSymbol = "AddExpression"

    fun grammarParser(grammar: List<String>): List<LR1ParserGenerator.ProductionRuleData> {
        val result = mutableListOf<LR1ParserGenerator.ProductionRuleData>()
        for(grammar in es5Grammar) {
            val s = grammar.split(" ::= ")
            val left = s.first()
            val right = s[1].split(" |/ ")
            for(r in right) {
                result.add(
                    LR1ParserGenerator.ProductionRuleData(
                    left, r.split(" ").filter { it.isNotBlank() }
                ))
            }
        }
        return result
    }
}