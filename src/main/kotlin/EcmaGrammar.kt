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
    const val ConditionalExpression = "ConditionalExpression"
    const val AssignmentExpression = "AssignmentExpression"
    const val LeftHandSideExpression = "LeftHandSideExpression"
    const val NewExpression = "NewExpression"
    const val MemberExpression = "MemberExpression"
    const val PrimaryExpression = "PrimaryExpression"
    const val UnaryExpression = "UnaryExpression"
    const val PostfixExpression = "PostfixExpression"
    const val ExpressionStatement = "ExpressionStatement"
    const val CallExpression = "CallExpression"
    const val Arguments = "Arguments"
    const val ArgumentList = "ArgumentList"
    const val Expression = "Expression"
    const val AssignmentOperator = "AssignmentOperator"
    const val Literal = "Literal"
    const val NumericLiteral = "NumericLiteral"
    const val BooleanLiteral = "BooleanLiteral"
    const val NullLiteral = "NullLiteral"
    const val ThisLiteral = "this"
    const val Identifier = "Identifier"
    const val ArrayLiteral = "ArrayLiteral"
    const val ElementList = "ElementList"
    const val Elision = "Elision"
    private const val OR = "|/"

    val es5Grammar = listOf(
        "$ArrayLiteral ::= [ ]" +
            " $OR [ $Elision ]" +
            " $OR [ $ElementList ]" +
            " $OR [ $ElementList , ]" +
            " $OR [ $ElementList , $Elision ]",
        "$ElementList ::= $AssignmentExpression" +
            " $OR $Elision $AssignmentExpression" +
            " $OR $ElementList , $AssignmentExpression" +
            " $OR $ElementList , $Elision $AssignmentExpression",
        "$Elision ::= ," +
            " $OR $Elision ,",
        "$Literal ::= $NullLiteral" +
            " $OR $BooleanLiteral" +
            " $OR $NumericLiteral",
        "$PrimaryExpression ::= $ThisLiteral" +
            " $OR $Identifier" +
            " $OR $Literal" +
            " $OR $ArrayLiteral" +
            " $OR ( $Expression )",
        "$MemberExpression ::= $PrimaryExpression" +
            " $OR $MemberExpression [ $Expression ]" +
            " $OR new $MemberExpression $Arguments" +
            " $OR $MemberExpression . $Identifier",
        "$NewExpression ::= $MemberExpression" +
            " $OR new $NewExpression",
        "$CallExpression ::= $MemberExpression $Arguments" +
            " $OR $CallExpression $Arguments" +
            " $OR $CallExpression [ $Expression ]" +
            " $OR $CallExpression . $Identifier",
        "$Arguments ::= ( )" +
            " $OR ( $ArgumentList )",
        "$ArgumentList ::= $AssignmentExpression" +
            " $OR $ArgumentList , $AssignmentExpression",
        "$LeftHandSideExpression ::= $NewExpression" +
            " $OR $CallExpression",
        "$PostfixExpression ::= $LeftHandSideExpression" +
            " $OR $LeftHandSideExpression ++" +
            " $OR $LeftHandSideExpression --",
        "$UnaryExpression ::= $PostfixExpression" +
            " $OR delete $UnaryExpression" +
            " $OR void $UnaryExpression" +
            " $OR typeof $UnaryExpression" +
            " $OR ++ $UnaryExpression" +
            " $OR -- $UnaryExpression" +
            " $OR + $UnaryExpression" +
            " $OR - $UnaryExpression" +
            " $OR ~ $UnaryExpression" +
            " $OR ! $UnaryExpression",
        "$MultiplicativeExpression ::= $UnaryExpression" +
            " $OR $MultiplicativeExpression * $UnaryExpression" +
            " $OR $MultiplicativeExpression / $UnaryExpression" +
            " $OR $MultiplicativeExpression % $UnaryExpression",
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
        "$ConditionalExpression ::= $LogicalORExpression" +
            " $OR $LogicalORExpression ? $AssignmentExpression : $AssignmentExpression",
        "$AssignmentExpression ::= $ConditionalExpression" +
            " $OR $LeftHandSideExpression $AssignmentOperator $AssignmentExpression",
        "$Expression ::= $AssignmentExpression" +
            " $OR $Expression , $AssignmentExpression",
        "$ExpressionStatement ::= $Expression"
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