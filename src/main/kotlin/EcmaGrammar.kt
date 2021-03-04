package esTree

object EcmaGrammar {
    const val MultiplicativeExpression = "MultiplicativeExpression"
    const val AdditiveExpression = "AdditiveExpression"
    const val ShiftExpression = "ShiftExpression"
    const val RelationalExpression = "RelationalExpression"
    const val RelationalExpressionNoIn = "RelationalExpressionNoIn"
    const val EqualityExpression = "EqualityExpression"
    const val EqualityExpressionNoIn = "EqualityExpressionNoIn"
    const val BitwiseANDExpression = "BitwiseANDExpression"
    const val BitwiseANDExpressionNoIn = "BitwiseANDExpressionNoIn"
    const val BitwiseXORExpression = "BitwiseXORExpression"
    const val BitwiseXORExpressionNoIn = "BitwiseXORExpressionNoIn"
    const val BitwiseORExpression = "BitwiseORExpression"
    const val BitwiseORExpressionNoIn = "BitwiseORExpressionNoIn"
    const val LogicalANDExpression = "LogicalANDExpression"
    const val LogicalANDExpressionNoIn = "LogicalANDExpressionNoIn"
    const val LogicalORExpression = "LogicalORExpression"
    const val LogicalORExpressionNoIn = "LogicalORExpressionNoIn"
    const val ConditionalExpression = "ConditionalExpression"
    const val ConditionalExpressionNoIn = "ConditionalExpressionNoIn"
    const val AssignmentExpression = "AssignmentExpression"
    const val AssignmentExpressionNoIn = "AssignmentExpressionNoIn"
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
    const val ExpressionNoIn = "ExpressionNoIn"
    const val Program = "Program"
    const val SourceElements = "SourceElements"
    const val SourceElement = "SourceElement"
    const val Statement = "Statement"
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
    const val Block = "Block"
    const val StatementList = "StatementList"
    const val VariableStatement = "VariableStatement"
    const val VariableDeclarationList = "VariableDeclarationList"
    const val VariableDeclarationListNoIn = "VariableDeclarationListNoIn"
    const val VariableDeclaration = "VariableDeclaration"
    const val VariableDeclarationNoIn = "VariableDeclarationNoIn"
    const val Initializer = "Initializer"
    const val InitializerNoIn = "InitializerNoIn"
    const val EmptyStatement = "EmptyStatement"
    const val IfStatement = "IfStatement"
    const val IterationStatement = "IterationStatement"
    const val ContinueStatement = "ContinueStatement"
    const val BreakStatement = "BreakStatement"
    const val ReturnStatement = "ReturnStatement"
    const val WithStatement = "WithStatement"
    const val SwitchStatement = "SwitchStatement"
    const val CaseBlock = "CaseBlock"
    const val CaseClauses = "CaseClauses"
    const val CaseClause = "CaseClause"
    const val DefaultClause = "DefaultClause"
    const val LabelledStatement = "LabelledStatement"
    const val ThrowStatement = "ThrowStatement"
    const val TryStatement = "TryStatement"
    const val Catch = "Catch"
    const val Finally = "Finally"
    const val DebuggerStatement = "DebuggerStatement"
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
        "$RelationalExpressionNoIn ::= $ShiftExpression" +
            " $OR $RelationalExpressionNoIn < $ShiftExpression" +
            " $OR $RelationalExpressionNoIn > $ShiftExpression" +
            " $OR $RelationalExpressionNoIn <= $ShiftExpression" +
            " $OR $RelationalExpressionNoIn >= $ShiftExpression" +
            " $OR $RelationalExpressionNoIn instanceof $ShiftExpression",
        "$EqualityExpression ::= $RelationalExpression" +
            " $OR $EqualityExpression == $RelationalExpression" +
            " $OR $EqualityExpression != $RelationalExpression" +
            " $OR $EqualityExpression === $RelationalExpression" +
            " $OR $EqualityExpression !== $RelationalExpression",
        "$EqualityExpressionNoIn ::= $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn == $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn != $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn === $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn !== $RelationalExpressionNoIn",
        "$BitwiseANDExpression ::= $EqualityExpression" +
            " $OR $BitwiseANDExpression & $EqualityExpression",
        "$BitwiseANDExpressionNoIn ::= $EqualityExpressionNoIn" +
            " $OR $BitwiseANDExpressionNoIn & $EqualityExpressionNoIn",
        "$BitwiseXORExpression ::= $BitwiseANDExpression" +
            " $OR $BitwiseXORExpression ^ $BitwiseANDExpression",
        "$BitwiseXORExpressionNoIn ::= $BitwiseANDExpressionNoIn" +
            " $OR $BitwiseXORExpressionNoIn ^ $BitwiseANDExpressionNoIn",
        "$BitwiseORExpression ::= $BitwiseXORExpression" +
            " $OR $BitwiseORExpression | $BitwiseXORExpression",
        "$BitwiseORExpressionNoIn ::= $BitwiseXORExpressionNoIn" +
            " $OR $BitwiseORExpressionNoIn | $BitwiseXORExpressionNoIn",
        "$LogicalANDExpression ::= $BitwiseORExpression" +
            " $OR $LogicalANDExpression && $BitwiseORExpression",
        "$LogicalANDExpressionNoIn ::= $BitwiseORExpressionNoIn" +
            " $OR $LogicalANDExpressionNoIn && $BitwiseORExpressionNoIn",
        "$LogicalORExpression ::= $LogicalANDExpression" +
            " $OR $LogicalORExpression || $LogicalANDExpression",
        "$LogicalORExpressionNoIn ::= $LogicalANDExpressionNoIn" +
            " $OR $LogicalORExpressionNoIn || $LogicalANDExpressionNoIn",
        "$ConditionalExpression ::= $LogicalORExpression" +
            " $OR $LogicalORExpression ? $AssignmentExpression : $AssignmentExpression",
        "$ConditionalExpressionNoIn ::= $LogicalORExpressionNoIn" +
            " $OR $LogicalORExpressionNoIn ? $AssignmentExpressionNoIn : $AssignmentExpressionNoIn",
        "$AssignmentExpression ::= $ConditionalExpression" +
            " $OR $LeftHandSideExpression $AssignmentOperator $AssignmentExpression" +
            " $OR $LeftHandSideExpression = $AssignmentExpression",
        "$AssignmentExpressionNoIn ::= $ConditionalExpressionNoIn" +
            " $OR $LeftHandSideExpression $AssignmentOperator $AssignmentExpressionNoIn" +
            " $OR $LeftHandSideExpression = $AssignmentExpressionNoIn",
        "$Expression ::= $AssignmentExpression" +
            " $OR $Expression , $AssignmentExpression",
        "$ExpressionNoIn ::= $AssignmentExpressionNoIn" +
            " $OR $ExpressionNoIn , $AssignmentExpressionNoIn",
        "$ExpressionStatement ::= $Expression ;",
        "$Block ::= { }" +
            " $OR { $StatementList }",
        "$StatementList ::= $Statement" +
            " $OR $StatementList $Statement",
        "$VariableStatement ::= var $VariableDeclarationList ;",
        "$VariableDeclarationList ::= $VariableDeclaration" +
            " $OR $VariableDeclarationList , $VariableDeclaration",
        "$VariableDeclarationListNoIn ::= $VariableDeclarationNoIn" +
            " $OR $VariableDeclarationListNoIn , $VariableDeclarationNoIn",
        "$VariableDeclaration ::= $Identifier" +
            " $OR $Identifier $Initializer",
        "$VariableDeclarationNoIn ::= $Identifier" +
            " $OR $Identifier $InitializerNoIn",
        "$Initializer ::= = $AssignmentExpression",
        "$InitializerNoIn ::= = $AssignmentExpressionNoIn",
        "$EmptyStatement ::= ;",
        "$IfStatement ::= if ( $Expression ) $Statement else $Statement" +
            " $OR if ( $Expression ) $Statement",
        "$IterationStatement ::= do $Statement while ( $Expression ) ;" +
            " $OR while ( $Expression ) $Statement" +
            " $OR for ( $ExpressionNoIn ; $Expression ; $Expression ) $Statement" +
            " $OR for ( var $VariableDeclarationListNoIn ; $Expression ; $Expression ) $Statement" +
            " $OR for ( $LeftHandSideExpression in $Expression ) $Statement" +
            " $OR for ( var $VariableDeclarationNoIn in $Expression ) $Statement",
        "$ContinueStatement ::= continue ;" +
            " $OR continue $Identifier ;",
        "$BreakStatement ::= break ;" +
            " $OR break $Identifier ;",
        "$ReturnStatement ::= return ;" +
            " $OR return $Expression ;",
        "$WithStatement ::= with ( $Expression ) $Statement",
        "$SwitchStatement ::= switch ( $Expression ) $CaseBlock",
        "$CaseBlock ::= { }" +
            " $OR { $CaseClauses }" +
            " $OR { $DefaultClause }" +
            " $OR { $CaseClauses $DefaultClause }" +
            " $OR { $DefaultClause $CaseClauses }" +
            " $OR { $CaseClauses $DefaultClause $CaseClauses }",
        "$CaseClauses ::= $CaseClause" +
            " $OR $CaseClauses $CaseClause",
        "$CaseClause ::= case $Expression :" +
            " $OR case $Expression : $StatementList",
        "$DefaultClause ::= default :" +
            " $OR default : $StatementList",
        "$LabelledStatement ::= $Identifier : $Statement",
        "$ThrowStatement ::= throw $Expression ;",
        "$TryStatement ::= try $Block $Catch" +
            " $OR try $Block $Finally" +
            " $OR try $Block $Catch $Finally",
        "$Catch ::= catch ( $Identifier ) $Block",
        "$Finally ::= finally $Block",
        "$DebuggerStatement ::= debugger ;",
        "$Statement ::= $Block" +
            " $OR $VariableStatement" +
            " $OR $EmptyStatement" +
            " $OR $ExpressionStatement" +
            " $OR $IfStatement" +
            " $OR $IterationStatement" +
            " $OR $ContinueStatement" +
            " $OR $BreakStatement" +
            " $OR $ReturnStatement" +
            " $OR $WithStatement" +
            " $OR $LabelledStatement" +
            " $OR $SwitchStatement" +
            " $OR $ThrowStatement" +
            " $OR $TryStatement" +
            " $OR $DebuggerStatement",
        "$SourceElement ::= $Statement",
        "$SourceElements ::= $SourceElement" +
            " $OR $SourceElements $SourceElement",
        "$Program ::= $SourceElements"
    )
    const val es5StartSymbol = Program

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