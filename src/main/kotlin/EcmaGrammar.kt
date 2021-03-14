import EcmaGrammar.Symbols.*
import esTree.LR0ParserGenerator

object EcmaGrammar {
    enum class Symbols {
        MultiplicativeExpression,
        MultiplicativeExpressionForStmt,
        AdditiveExpression,
        AdditiveExpressionForStmt,
        ShiftExpression,
        ShiftExpressionForStmt,
        RelationalExpression,
        RelationalExpressionForStmt,
        RelationalExpressionNoIn,
        EqualityExpression,
        EqualityExpressionForStmt,
        EqualityExpressionNoIn,
        BitwiseANDExpression,
        BitwiseANDExpressionForStmt,
        BitwiseANDExpressionNoIn,
        BitwiseXORExpression,
        BitwiseXORExpressionForStmt,
        BitwiseXORExpressionNoIn,
        BitwiseORExpression,
        BitwiseORExpressionForStmt,
        BitwiseORExpressionNoIn,
        LogicalANDExpression,
        LogicalANDExpressionForStmt,
        LogicalANDExpressionNoIn,
        LogicalORExpression,
        LogicalORExpressionForStmt,
        LogicalORExpressionNoIn,
        ConditionalExpression,
        ConditionalExpressionForStmt,
        ConditionalExpressionNoIn,
        AssignmentExpression,
        AssignmentExpressionForStmt,
        AssignmentExpressionNoIn,
        LeftHandSideExpression,
        LeftHandSideExpressionForStmt,
        NewExpression,
        NewExpressionForStmt,
        MemberExpression,
        MemberExpressionForStmt,
        PrimaryExpression,
        PrimaryExpressionForStmt,
        UnaryExpression,
        UnaryExpressionForStmt,
        PostfixExpression,
        PostfixExpressionForStmt,
        ExpressionStatement,
        CallExpression,
        CallExpressionForStmt,
        Arguments,
        ArgumentList,
        Expression,
        ExpressionForStmt,
        ExpressionNoIn,
        Program,
        SourceElements,
        SourceElement,
        Statement,
        AssignmentOperator,
        Literal,
        NumericLiteral,
        BooleanLiteral,
        NullLiteral,
        ThisLiteral,
        Identifier,
        ArrayLiteral,
        ElementList,
        Elision,
        Block,
        StatementList,
        VariableStatement,
        VariableDeclarationList,
        VariableDeclarationListNoIn,
        VariableDeclaration,
        VariableDeclarationNoIn,
        Initializer,
        InitializerNoIn,
        EmptyStatement,
        IfStatement,
        IterationStatement,
        ContinueStatement,
        BreakStatement,
        ReturnStatement,
        WithStatement,
        SwitchStatement,
        CaseBlock,
        CaseClauses,
        CaseClause,
        DefaultClause,
        LabelledStatement,
        ThrowStatement,
        TryStatement,
        Catch,
        Finally,
        DebuggerStatement,
        FunctionDeclaration,
        FunctionExpression,
        FormalParameterList,
        FunctionBody,
        ObjectLiteral,
        PropertyNameAndValueList,
        PropertyAssignment,
        PropertyName,
        PropertySetParameterList,
        StringLiteral,
        EMPTY,
        LineTerminator;

        companion object {
            fun fromInt(v: Int): Symbols = values().find { it.ordinal == v }!!
        }
    }
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
            " $OR $StringLiteral" +
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
            " $OR $NumericLiteral" +
            " $OR $StringLiteral",
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
        "$FunctionBody ::= $EMPTY" +
            " $OR $SourceElements",
        "$Program ::= $SourceElements"
    )
    val es5StartSymbol = Program

    val anonymousOperators = listOf(
        "==","!=","===","!==","<","<=",">",">=",
        "<<",">>",">>>","+","-","*","/","%","|",
        "^","&","&&","||",
        "?", ":","!","~","++","--","(",")",
        "[","]",".",",",";","{","}","="
    )
    val assignmentOperators = listOf(
        "+=","-=","*=","/=","%=","<<=",">>=", // '=' は外でも使うのではずし
        ">>>=","|=","^=","&="
    )
    val keywords = listOf(
        "this","in","instanceof","typeof","void","delete","new","var",
        "if","else","while","for","continue","break","return",
        "with","switch","case","default","throw",
        "try","catch","finally","debugger","function","get","set","do"
    )
    val operatorsMap = hashMapOf<String,Int>()

    fun grammarParserForLR0(grammar: List<String>): List<LR0ParserGenerator.ProductionRuleData> {
        var i = Symbols.values().size + 10
        anonymousOperators.forEach { operatorsMap[it] = i++ }
        assignmentOperators.forEach { operatorsMap[it] = i++ }
        keywords.forEach { operatorsMap[it] = i++ }
        operatorsMap["$"] = i++
        operatorsMap["EOF"] = i++
        val result = mutableListOf<LR0ParserGenerator.ProductionRuleData>()
        for(g in grammar) {
            val s = g.split(" ::= ")
            val left = Symbols.valueOf(s.first()).ordinal
            val right = s[1].split(" $OR ")
            for(r in right) {
                r.split(" ").filter { it.isNotBlank() }.map { raw ->
                    return@map operatorsMap[raw] ?: Symbols.valueOf(raw).ordinal
                }.let {
                    result.add(
                        LR0ParserGenerator.ProductionRuleData(
                            left, it
                        )
                    )
                }
            }
        }
        return result
    }

    fun decode(value: Int): String {
        return operatorsMap.entries.find { it.value == value }?.key
            ?: Symbols.fromInt(value).name
    }
}
