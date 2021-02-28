package esTree

object EcmaGrammar {
    const val AdditiveExpression = "AdditiveExpression"
    const val Number = "Number"
    private const val OR = "|/"

    val es5Grammar = listOf(
        "$AdditiveExpression ::= $Number $OR $AdditiveExpression + $Number $OR $AdditiveExpression - $Number"
    )
    val es5StartSymbol = AdditiveExpression

    fun grammarParser(grammar: List<String>): List<LR1ParserGenerator.ProductionRuleData> {
        val result = mutableListOf<LR1ParserGenerator.ProductionRuleData>()
        for(grammar in es5Grammar) {
            val s = grammar.split(" ::= ")
            val left = s.first()
            val right = s[1].split(" $OR ")
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