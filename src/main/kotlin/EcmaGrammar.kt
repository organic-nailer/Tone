package esTree

object EcmaGrammar {
    const val MultiplicativeExpression = "MultiplicativeExpression"
    const val AdditiveExpression = "AdditiveExpression"
    const val ShiftExpression = "ShiftExpression"
    const val RelationalExpression = "RelationalExpression"
    const val EqualityExpression = "EqualityExpression"
    const val BitwiseANDExpression = "BitwiseANDExpression"
    const val BitwiseXORExpression = "BitwiseXORExpression"
    const val BitwiseORExpression = "BitwiseORExpression"
    const val LogicalANDExpression = "LogicalANDExpression"
    const val LogicalORExpression = "LogicalORExpression"
    const val ExpressionStatement = "ExpressionStatement"
    const val Number = "Number"
    private const val OR = "|/"

    val es5Grammar = listOf(
        "$MultiplicativeExpression ::= $Number" +
            " $OR $MultiplicativeExpression * $Number" +
            " $OR $MultiplicativeExpression / $Number" +
            " $OR $MultiplicativeExpression % $Number",
        "$AdditiveExpression ::= $MultiplicativeExpression" +
            " $OR $AdditiveExpression + $MultiplicativeExpression" +
            " $OR $AdditiveExpression - $MultiplicativeExpression",
        "$ShiftExpression ::= $AdditiveExpression" +
            " $OR $ShiftExpression << $AdditiveExpression" +
            " $OR $ShiftExpression >> $AdditiveExpression" +
            " $OR $ShiftExpression >>> $AdditiveExpression",
        "$RelationalExpression ::= $ShiftExpression" +
            " $OR $RelationalExpression < $ShiftExpression" +
            " $OR $RelationalExpression > $ShiftExpression" +
            " $OR $RelationalExpression <= $ShiftExpression" +
            " $OR $RelationalExpression >= $ShiftExpression" +
            " $OR $RelationalExpression instanceof $ShiftExpression" +
            " $OR $RelationalExpression in $ShiftExpression",
        "$EqualityExpression ::= $RelationalExpression" +
            " $OR $EqualityExpression == $RelationalExpression" +
            " $OR $EqualityExpression != $RelationalExpression" +
            " $OR $EqualityExpression === $RelationalExpression" +
            " $OR $EqualityExpression !== $RelationalExpression",
        "$BitwiseANDExpression ::= $EqualityExpression" +
            " $OR $BitwiseANDExpression & $EqualityExpression",
        "$BitwiseXORExpression ::= $BitwiseANDExpression" +
            " $OR $BitwiseXORExpression ^ $BitwiseANDExpression",
        "$BitwiseORExpression ::= $BitwiseXORExpression" +
            " $OR $BitwiseORExpression | $BitwiseXORExpression",
        "$LogicalANDExpression ::= $BitwiseORExpression" +
            " $OR $LogicalANDExpression && $BitwiseORExpression",
        "$LogicalORExpression ::= $LogicalANDExpression" +
            " $OR $LogicalORExpression || $LogicalANDExpression",
        "$ExpressionStatement ::= $LogicalORExpression"
    )
    val es5StartSymbol = ExpressionStatement

    fun grammarParser(grammar: List<String>): List<LR1ParserGenerator.ProductionRuleData> {
        val result = mutableListOf<LR1ParserGenerator.ProductionRuleData>()
        for(g in grammar) {
            val s = g.split(" ::= ")
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