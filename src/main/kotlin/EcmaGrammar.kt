package esTree

object EcmaGrammar {
    const val MultiplicativeExpression = "MultiplicativeExpression"
    const val MultiplicativeExpressionForStmt = "MultiplicativeExpressionForStmt"
    const val AdditiveExpression = "AdditiveExpression"
    const val AdditiveExpressionForStmt = "AdditiveExpressionForStmt"
    const val ShiftExpression = "ShiftExpression"
    const val ShiftExpressionForStmt = "ShiftExpressionForStmt"
    const val RelationalExpression = "RelationalExpression"
    const val RelationalExpressionForStmt = "RelationalExpressionForStmt"
    const val RelationalExpressionNoIn = "RelationalExpressionNoIn"
    const val EqualityExpression = "EqualityExpression"
    const val EqualityExpressionForStmt = "EqualityExpressionForStmt"
    const val EqualityExpressionNoIn = "EqualityExpressionNoIn"
    const val BitwiseANDExpression = "BitwiseANDExpression"
    const val BitwiseANDExpressionForStmt = "BitwiseANDExpressionForStmt"
    const val BitwiseANDExpressionNoIn = "BitwiseANDExpressionNoIn"
    const val BitwiseXORExpression = "BitwiseXORExpression"
    const val BitwiseXORExpressionForStmt = "BitwiseXORExpressionForStmt"
    const val BitwiseXORExpressionNoIn = "BitwiseXORExpressionNoIn"
    const val BitwiseORExpression = "BitwiseORExpression"
    const val BitwiseORExpressionForStmt = "BitwiseORExpressionForStmt"
    const val BitwiseORExpressionNoIn = "BitwiseORExpressionNoIn"
    const val LogicalANDExpression = "LogicalANDExpression"
    const val LogicalANDExpressionForStmt = "LogicalANDExpressionForStmt"
    const val LogicalANDExpressionNoIn = "LogicalANDExpressionNoIn"
    const val LogicalORExpression = "LogicalORExpression"
    const val LogicalORExpressionForStmt = "LogicalORExpressionForStmt"
    const val LogicalORExpressionNoIn = "LogicalORExpressionNoIn"
    const val ConditionalExpression = "ConditionalExpression"
    const val ConditionalExpressionForStmt = "ConditionalExpressionForStmt"
    const val ConditionalExpressionNoIn = "ConditionalExpressionNoIn"
    const val AssignmentExpression = "AssignmentExpression"
    const val AssignmentExpressionForStmt = "AssignmentExpressionForStmt"
    const val AssignmentExpressionNoIn = "AssignmentExpressionNoIn"
    const val LeftHandSideExpression = "LeftHandSideExpression"
    const val LeftHandSideExpressionForStmt = "LeftHandSideExpressionForStmt"
    const val NewExpression = "NewExpression"
    const val NewExpressionForStmt = "NewExpressionForStmt"
    const val MemberExpression = "MemberExpression"
    const val MemberExpressionForStmt = "MemberExpressionForStmt"
    const val PrimaryExpression = "PrimaryExpression"
    const val PrimaryExpressionForStmt = "PrimaryExpressionForStmt"
    const val UnaryExpression = "UnaryExpression"
    const val UnaryExpressionForStmt = "UnaryExpressionForStmt"
    const val PostfixExpression = "PostfixExpression"
    const val PostfixExpressionForStmt = "PostfixExpressionForStmt"
    const val ExpressionStatement = "ExpressionStatement"
    const val CallExpression = "CallExpression"
    const val CallExpressionForStmt = "CallExpressionForStmt"
    const val Arguments = "Arguments"
    const val ArgumentList = "ArgumentList"
    const val Expression = "Expression"
    const val ExpressionForStmt = "ExpressionForStmt"
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
    const val FunctionDeclaration = "FunctionDeclaration"
    const val FunctionExpression = "FunctionExpression"
    const val FormalParameterList = "FormalParameterList"
    const val FunctionBody = "FunctionBody"
    const val ObjectLiteral = "ObjectLiteral"
    const val PropertyNameAndValueList = "PropertyNameAndValueList"
    const val PropertyAssignment = "PropertyAssignment"
    const val PropertyName = "PropertyName"
    const val PropertySetParameterList = "PropertySetParameterList"
    const val LineTerminator = "LineTerminator"
    private const val OR = "|/"

    val es5Grammar = listOf(
        "$ObjectLiteral ::= { }" +
            " $OR { $PropertyNameAndValueList }" +
            " $OR { $PropertyNameAndValueList , }",
        "$PropertyNameAndValueList ::= $PropertyAssignment" +
            " $OR $PropertyNameAndValueList , $PropertyAssignment",
        "$PropertyAssignment ::= $PropertyName : $AssignmentExpression" +
            " $OR get $PropertyName ( ) { $FunctionBody }" +
            " $OR set $PropertyName ( $PropertySetParameterList ) { $FunctionBody }",
        "$PropertyName ::= $Identifier" +
            " $OR $NumericLiteral",
        "$PropertySetParameterList ::= $Identifier",
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
            " $OR $ObjectLiteral" +
            " $OR ( $Expression )",
        "$PrimaryExpressionForStmt ::= $ThisLiteral" +
            " $OR $Identifier" +
            " $OR $Literal" +
            " $OR $ArrayLiteral" +
            " $OR ( $Expression )",
        "$MemberExpression ::= $PrimaryExpression" +
            " $OR $FunctionExpression" +
            " $OR $MemberExpression [ $Expression ]" +
            " $OR new $MemberExpression $Arguments" +
            " $OR $MemberExpression . $Identifier",
        "$MemberExpressionForStmt ::= $PrimaryExpressionForStmt" +
            " $OR $MemberExpressionForStmt [ $Expression ]" +
            " $OR new $MemberExpressionForStmt $Arguments" +
            " $OR $MemberExpressionForStmt . $Identifier",
        "$NewExpression ::= $MemberExpression" +
            " $OR new $NewExpression",
        "$NewExpressionForStmt ::= $MemberExpressionForStmt" +
            " $OR new $NewExpressionForStmt",
        "$CallExpression ::= $MemberExpression $Arguments" +
            " $OR $CallExpression $Arguments" +
            " $OR $CallExpression [ $Expression ]" +
            " $OR $CallExpression . $Identifier",
        "$CallExpressionForStmt ::= $MemberExpressionForStmt $Arguments" +
            " $OR $CallExpressionForStmt $Arguments" +
            " $OR $CallExpressionForStmt [ $Expression ]" +
            " $OR $CallExpressionForStmt . $Identifier",
        "$Arguments ::= ( )" +
            " $OR ( $ArgumentList )",
        "$ArgumentList ::= $AssignmentExpression" +
            " $OR $ArgumentList , $AssignmentExpression",
        "$LeftHandSideExpression ::= $NewExpression" +
            " $OR $CallExpression",
        "$LeftHandSideExpressionForStmt ::= $NewExpressionForStmt" +
            " $OR $CallExpressionForStmt",
        "$PostfixExpression ::= $LeftHandSideExpression" +
            " $OR $LeftHandSideExpression ++" +
            " $OR $LeftHandSideExpression --",
        "$PostfixExpressionForStmt ::= $LeftHandSideExpressionForStmt" +
            " $OR $LeftHandSideExpressionForStmt ++" +
            " $OR $LeftHandSideExpressionForStmt --",
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
        "$UnaryExpressionForStmt ::= $PostfixExpressionForStmt" +
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
        "$MultiplicativeExpressionForStmt ::= $UnaryExpressionForStmt" +
            " $OR $MultiplicativeExpressionForStmt * $UnaryExpression" +
            " $OR $MultiplicativeExpressionForStmt / $UnaryExpression" +
            " $OR $MultiplicativeExpressionForStmt % $UnaryExpression",
        "$AdditiveExpression ::= $MultiplicativeExpression" +
            " $OR $AdditiveExpression + $MultiplicativeExpression" +
            " $OR $AdditiveExpression - $MultiplicativeExpression",
        "$AdditiveExpressionForStmt ::= $MultiplicativeExpressionForStmt" +
            " $OR $AdditiveExpressionForStmt + $MultiplicativeExpression" +
            " $OR $AdditiveExpressionForStmt - $MultiplicativeExpression",
        "$ShiftExpression ::= $AdditiveExpression" +
            " $OR $ShiftExpression << $AdditiveExpression" +
            " $OR $ShiftExpression >> $AdditiveExpression" +
            " $OR $ShiftExpression >>> $AdditiveExpression",
        "$ShiftExpressionForStmt ::= $AdditiveExpressionForStmt" +
            " $OR $ShiftExpressionForStmt << $AdditiveExpression" +
            " $OR $ShiftExpressionForStmt >> $AdditiveExpression" +
            " $OR $ShiftExpressionForStmt >>> $AdditiveExpression",
        "$RelationalExpression ::= $ShiftExpression" +
            " $OR $RelationalExpression < $ShiftExpression" +
            " $OR $RelationalExpression > $ShiftExpression" +
            " $OR $RelationalExpression <= $ShiftExpression" +
            " $OR $RelationalExpression >= $ShiftExpression" +
            " $OR $RelationalExpression instanceof $ShiftExpression" +
            " $OR $RelationalExpression in $ShiftExpression",
        "$RelationalExpressionForStmt ::= $ShiftExpressionForStmt" +
            " $OR $RelationalExpressionForStmt < $ShiftExpression" +
            " $OR $RelationalExpressionForStmt > $ShiftExpression" +
            " $OR $RelationalExpressionForStmt <= $ShiftExpression" +
            " $OR $RelationalExpressionForStmt >= $ShiftExpression" +
            " $OR $RelationalExpressionForStmt instanceof $ShiftExpression" +
            " $OR $RelationalExpressionForStmt in $ShiftExpression",
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
        "$EqualityExpressionForStmt ::= $RelationalExpressionForStmt" +
            " $OR $EqualityExpressionForStmt == $RelationalExpression" +
            " $OR $EqualityExpressionForStmt != $RelationalExpression" +
            " $OR $EqualityExpressionForStmt === $RelationalExpression" +
            " $OR $EqualityExpressionForStmt !== $RelationalExpression",
        "$EqualityExpressionNoIn ::= $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn == $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn != $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn === $RelationalExpressionNoIn" +
            " $OR $EqualityExpressionNoIn !== $RelationalExpressionNoIn",
        "$BitwiseANDExpression ::= $EqualityExpression" +
            " $OR $BitwiseANDExpression & $EqualityExpression",
        "$BitwiseANDExpressionForStmt ::= $EqualityExpressionForStmt" +
            " $OR $BitwiseANDExpressionForStmt & $EqualityExpression",
        "$BitwiseANDExpressionNoIn ::= $EqualityExpressionNoIn" +
            " $OR $BitwiseANDExpressionNoIn & $EqualityExpressionNoIn",
        "$BitwiseXORExpression ::= $BitwiseANDExpression" +
            " $OR $BitwiseXORExpression ^ $BitwiseANDExpression",
        "$BitwiseXORExpressionForStmt ::= $BitwiseANDExpressionForStmt" +
            " $OR $BitwiseXORExpressionForStmt ^ $BitwiseANDExpression",
        "$BitwiseXORExpressionNoIn ::= $BitwiseANDExpressionNoIn" +
            " $OR $BitwiseXORExpressionNoIn ^ $BitwiseANDExpressionNoIn",
        "$BitwiseORExpression ::= $BitwiseXORExpression" +
            " $OR $BitwiseORExpression | $BitwiseXORExpression",
        "$BitwiseORExpressionForStmt ::= $BitwiseXORExpressionForStmt" +
            " $OR $BitwiseORExpressionForStmt | $BitwiseXORExpression",
        "$BitwiseORExpressionNoIn ::= $BitwiseXORExpressionNoIn" +
            " $OR $BitwiseORExpressionNoIn | $BitwiseXORExpressionNoIn",
        "$LogicalANDExpression ::= $BitwiseORExpression" +
            " $OR $LogicalANDExpression && $BitwiseORExpression",
        "$LogicalANDExpressionForStmt ::= $BitwiseORExpressionForStmt" +
            " $OR $LogicalANDExpressionForStmt && $BitwiseORExpression",
        "$LogicalANDExpressionNoIn ::= $BitwiseORExpressionNoIn" +
            " $OR $LogicalANDExpressionNoIn && $BitwiseORExpressionNoIn",
        "$LogicalORExpression ::= $LogicalANDExpression" +
            " $OR $LogicalORExpression || $LogicalANDExpression",
        "$LogicalORExpressionForStmt ::= $LogicalANDExpressionForStmt" +
            " $OR $LogicalORExpressionForStmt || $LogicalANDExpression",
        "$LogicalORExpressionNoIn ::= $LogicalANDExpressionNoIn" +
            " $OR $LogicalORExpressionNoIn || $LogicalANDExpressionNoIn",
        "$ConditionalExpression ::= $LogicalORExpression" +
            " $OR $LogicalORExpression ? $AssignmentExpression : $AssignmentExpression",
        "$ConditionalExpressionForStmt ::= $LogicalORExpressionForStmt" +
            " $OR $LogicalORExpressionForStmt ? $AssignmentExpression : $AssignmentExpression",
        "$ConditionalExpressionNoIn ::= $LogicalORExpressionNoIn" +
            " $OR $LogicalORExpressionNoIn ? $AssignmentExpressionNoIn : $AssignmentExpressionNoIn",
        "$AssignmentExpression ::= $ConditionalExpression" +
            " $OR $LeftHandSideExpression $AssignmentOperator $AssignmentExpression" +
            " $OR $LeftHandSideExpression = $AssignmentExpression",
        "$AssignmentExpressionForStmt ::= $ConditionalExpressionForStmt" +
            " $OR $LeftHandSideExpressionForStmt $AssignmentOperator $AssignmentExpression" +
            " $OR $LeftHandSideExpressionForStmt = $AssignmentExpression",
        "$AssignmentExpressionNoIn ::= $ConditionalExpressionNoIn" +
            " $OR $LeftHandSideExpression $AssignmentOperator $AssignmentExpressionNoIn" +
            " $OR $LeftHandSideExpression = $AssignmentExpressionNoIn",
        "$Expression ::= $AssignmentExpression" +
            " $OR $Expression , $AssignmentExpression",
        "$ExpressionForStmt ::= $AssignmentExpressionForStmt" +
            " $OR $ExpressionForStmt , $AssignmentExpression",
        "$ExpressionNoIn ::= $AssignmentExpressionNoIn" +
            " $OR $ExpressionNoIn , $AssignmentExpressionNoIn",
        "$ExpressionStatement ::= $ExpressionForStmt ;",
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
        "$SourceElement ::= $Statement" +
            " $OR $FunctionDeclaration",
        "$SourceElements ::= $SourceElement" +
            " $OR $SourceElements $SourceElement",
        "$FunctionDeclaration ::= function $Identifier ( ) { $FunctionBody }" +
            " $OR function $Identifier ( $FormalParameterList ) { $FunctionBody }",
        "$FunctionExpression ::= function ( ) { $FunctionBody }" +
            " $OR function ( $FormalParameterList ) { $FunctionBody }" +
            " $OR function $Identifier ( ) { $FunctionBody }" +
            " $OR function $Identifier ( $FormalParameterList ) { $FunctionBody }",
        "$FormalParameterList ::= $Identifier" +
            " $OR $FormalParameterList , $Identifier",
        "$FunctionBody ::= ε" +
            " $OR $SourceElements",
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

    fun grammarParserForLR0(grammar: List<String>): List<LR0ParserGenerator.ProductionRuleData> {
        val result = mutableListOf<LR0ParserGenerator.ProductionRuleData>()
        for(g in grammar) {
            val s = g.split(" ::= ")
            val left = s.first()
            val right = s[1].split(" $OR ")
            for(r in right) {
                result.add(
                    LR0ParserGenerator.ProductionRuleData(
                        left, r.split(" ").filter { it.isNotBlank() }
                    ))
            }
        }
        return result
    }
}