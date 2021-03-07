package esTree

import java.io.File
import java.lang.Exception
import kotlin.system.measureTimeMillis

class DragonParserGenerator(
    private val rules: List<LR0ParserGenerator.ProductionRuleData>,
    private val startSymbol: String) {
    val lr0ParserGenerator = LR0ParserGenerator(rules, startSymbol)
    val firstMap = mutableMapOf<String, MutableSet<String>>()
    //val gotoMap = mutableMapOf<Pair<String, String>, String>() //state to token, nextState
    val closureMap = mutableMapOf<Set<LALR1ProductionRuleData>, String>() //rules, state

    val transitionMap = mutableMapOf<Pair<String, String>, TransitionData>()
    enum class TransitionKind { SHIFT, REDUCE, ACCEPT }
    data class TransitionData(
        val kind: TransitionKind,
        val value: String?,
        val rule: LR0ParserGenerator.ProductionRuleData?
    ) {
        override fun toString(): String {
            return when(kind) {
                TransitionKind.SHIFT -> "s${value?.substring(1)}"
                TransitionKind.REDUCE -> "r${rule?.left}"
                TransitionKind.ACCEPT -> "acc"
            }
        }
    }

    companion object {
        private const val EMPTY = "ε"
    }

    init {
        measureTimeMillis {
            calcFirst()
        }.run { println("FirstSet calculated in $this ms") }
        measureTimeMillis {
            calcGotoMap()
        }.run { println("GotoMap calculated in $this ms") }
        printClosureMap()
        measureTimeMillis {
            calcTransition()
        }.run { println("TransitionMap calculated in $this ms") }
        printClosureMap()
        printTransitionMap()
        println("Closure size = ${closureMap.size}")
        println("Transition size = ${transitionMap.size}")
    }

    private fun getFirst(value: List<String>, suffixes: Set<String>): Set<String> {
        if(value.isEmpty()) return suffixes
        if(value.first() == EMPTY) {//{ε}
            return suffixes
        }
        if(lr0ParserGenerator.terminalTokens.contains(value.first())
            || value.first() == "$") {
            return setOf(value.first())//{α}
        }
        firstMap[value.first()]?.let {
            if(!it.contains(EMPTY)) return it //First(Y)
            if(value.size == 1) {
                if(suffixes.isNotEmpty()) return it.minus(EMPTY).union(suffixes)
                return it
            }
            return it.minus(EMPTY).union(getFirst(value.slice(1 until value.size),suffixes)) //(First(Y)-{ε})∪First(α)
        }
        return emptySet()
    }

    private fun addFirst(key: String, set: Set<String>) {
        firstMap[key]?.addAll(set) ?: kotlin.run {
            firstMap[key] = set.toMutableSet()
        }
    }

    private fun calcFirst() { //First集合を計算する
        var updated = true
        while(updated) {
            updated = false
            for(rule in rules) {
                val firstAlpha = getFirst(rule.right, emptySet())
                val firstX = firstMap[rule.left] ?: emptySet()
                val diff = firstAlpha.minus(firstX)
                if(diff.isNotEmpty()) {
                    updated = true
                    addFirst(rule.left, diff)
                }
            }
        }
    }

    private fun calcGotoMap() {
        val initialGrammar = LR0ParserGenerator.ProductionRuleData("$startSymbol'", listOf(startSymbol))
        val extendedRules = rules.toMutableList().apply {
            add(initialGrammar)
        }
        val kernelMap = lr0ParserGenerator.getKernelMap()
        val kernels = mutableSetOf<Pair<String,LR0ParserGenerator.LRProductionRuleData>>()
        kernelMap.forEach { it.value.forEach { v -> kernels.add(it.key to v) } }
        val followsMap = kernels.map { it to mutableSetOf<String>() }.toMap()
        val propagateMap = mutableMapOf<
            Pair<String,LR0ParserGenerator.LRProductionRuleData>,
            Set<Pair<String,LR0ParserGenerator.LRProductionRuleData>>
            >()
        val updatedKernels = mutableSetOf<Pair<String,LR0ParserGenerator.LRProductionRuleData>>()
        //propagateMapの生成とfollowsMapの初期化
        for(kernel in kernels) {
            val j = getClosure(setOf(kernel.second.toLALR1(setOf("#"))),extendedRules)
            //println("closure=$j")
            val propSet = mutableSetOf<Pair<String,LR0ParserGenerator.LRProductionRuleData>>()
            for(data in j.filter { !it.reducible }) {
                val shiftTargetToken = data.right[data.index]
                val shifted = data.shift().toLR()
                val nextState = lr0ParserGenerator.gotoMap[kernel.first to shiftTargetToken]
                data.follow.forEach { f ->
                    if(f == "#") {
                        propSet.add(nextState!! to shifted)
                    }
                    else {
                        followsMap[nextState!! to shifted]?.add(f)
                        updatedKernels.add(nextState to shifted)
                    }
                }
            }
            propagateMap[kernel] = propSet
        }
        followsMap["I0" to initialGrammar.toLR()]?.add("$")
        updatedKernels.add("I0" to initialGrammar.toLR())

        //println("props=$propagateMap")

        while(updatedKernels.isNotEmpty()) {
            //println("アップデート必要 $updatedKernels")
            //println("f=$followsMap")
            val updated = updatedKernels.toSet()
            updatedKernels.clear()
            for(kernel in updated) {
                val f = followsMap[kernel] ?: continue
                propagateMap[kernel]?.forEach {
                    val u = followsMap[it]?.addAll(f) ?: false
                    if(u) updatedKernels.add(it)
                }
            }
        }

        for(entry in kernelMap) {
            //各クロージャを計算してclosureMapに入れる
            closureMap[
                getClosure(entry.value.map {
                    it.toLALR1(followsMap[entry.key to it] ?: throw Exception("死"))
                }.toSet())
            ] = entry.key
        }
    }

    private fun calcTransition() {
        for(entry in lr0ParserGenerator.gotoMap) {
            if(transitionMap.containsKey(entry.key)) {
                throw Exception("LALR競合1 $entry")
            }
            transitionMap[entry.key] = TransitionData(
                TransitionKind.SHIFT, entry.value, null
            )
        }
        //printTransitionMap()
        val errorTransitions = mutableListOf<ErrorTransitionData>()
        for(entry in closureMap) {
            entry.key.filter { r -> r.reducible }.forEach {
                for(token in it.follow) {
                    if(transitionMap.containsKey(entry.value to token)) {
                        if(token == "else") continue //elseの競合はshift優先
                        //FunExprとFunDecの還元競合はFunDec優先
                        if(it.left == EcmaGrammar.FunctionExpression) continue
                        if(it.left == EcmaGrammar.FunctionDeclaration) {
                            transitionMap[entry.value to token] = TransitionData(
                                TransitionKind.REDUCE, null, it.toRule()
                            )
                            continue
                        }
                        errorTransitions.add(ErrorTransitionData(
                            entry.value, token,
                            TransitionData(
                                TransitionKind.REDUCE,
                                null,
                                it.toRule()
                            )
                        ))
                        continue
                    }
                    transitionMap[entry.value to token] = TransitionData(
                        TransitionKind.REDUCE, null, it.toRule()
                    )
                }
            }
            if(entry.key.any { r -> r.right == listOf(startSymbol) && r.follow.contains("$") && r.index == r.right.size }) {
//                if(transitionMap.containsKey(entry.value to "$")) {
//                    throw Exception("SLR競合3 $entry")
//                }
                transitionMap[entry.value to "$"] = TransitionData(
                    TransitionKind.ACCEPT, null, null
                )
            }
        }
        if(errorTransitions.isNotEmpty()) {
            errorTransitions.forEach {
                println(it)
            }
            printTransitionMap()
            throw Exception("SLR競合2")
        }
    }

    private fun printClosureMap() {
        val file = File("D:\\tmp\\closureMap.txt")
        try {
            if(file.createNewFile()) {
                println("Closures")
                closureMap.forEach { (t, u) ->
                    file.appendText("${u.padEnd(4)}: $t\n")
                }
            }
        } catch(e: java.lang.Exception) {
            println("closureMap書き込み失敗${e.message}")
        }
    }

    fun csvEncode(text: String?): String {
        if(text == null) return ""
        return "\"${text.replace("\"","\"\"")}\""
    }

    private fun printTransitionMap() {
        val file = File("D:\\tmp\\transitionMap.csv")
        try {
            if(file.createNewFile()) {
                //file.appendText("Transition Table\n")
                file.appendText(",")
                lr0ParserGenerator.terminalTokens.forEach { file.appendText("${csvEncode(it)},") }
                file.appendText("$,")
                lr0ParserGenerator.nonTerminalTokens.forEach { file.appendText("${csvEncode(it)},") }
                file.appendText("\n")
                for(c in closureMap.values) {
                    file.appendText("$c,")
                    lr0ParserGenerator.terminalTokens.forEach { file.appendText("${csvEncode(transitionMap[c to it]?.toString())},") }
                    file.appendText("${csvEncode(transitionMap[c to "$"]?.toString())},")
                    lr0ParserGenerator.nonTerminalTokens.forEach { file.appendText("${csvEncode(transitionMap[c to it]?.toString())},") }
                    file.appendText("\n")
                }
            }
        } catch(e: java.lang.Exception) {
            println("TransitionMap書き込み失敗${e.message}")
        }
    }

    data class ErrorTransitionData(val state: String, val token: String, val data: TransitionData)


    private fun getClosure(
        input: Set<LALR1ProductionRuleData>,
        grammarRules: List<LR0ParserGenerator.ProductionRuleData> = rules
    ): Set<LALR1ProductionRuleData> {
        //println("getClosure: $input")
        val result = mutableMapOf<LALR1ProductionRuleData, MutableSet<String>>()
        input.forEach { result[it.copy(follow = emptySet())] = it.follow.toMutableSet() }
        var updated = true
        while(updated) {
            updated = false
            for(rule in result.toList()) {
                if(rule.first.reducible) continue
                val target = rule.first.right[rule.first.index]
                if(lr0ParserGenerator.nonTerminalTokens.contains(target)) {
                    val firstSet = getFirst(
                        rule.first.right.slice(rule.first.index+1 until rule.first.right.size),
                        rule.second
                    )
                    grammarRules.filter { r -> r.left == target }.forEach {
                        val r = it.toLALR1(emptySet())
                        val u = result[r]?.addAll(firstSet) ?: kotlin.run {
                            result[r] = firstSet.toMutableSet()
                            if(firstSet.isNotEmpty()) updated = true
                        }
                        if(u == true) updated = true
                    }
                }
            }
        }
        //println("result: $result")
        return result.map { it.key.copy(follow = it.value) }.toSet()
    }

    data class LALR1ProductionRuleData(
        val left: String,
        val right: List<String>,
        val index: Int,
        val reducible: Boolean,
        val follow: Set<String>
    ) {
        fun shift(): LALR1ProductionRuleData {
            if(reducible) throw Exception("シフトできません $this")
            return LALR1ProductionRuleData(
                left, right, index+1,
                reducible = index+1 >= right.size,
                follow
            )
        }

        override fun toString(): String {
            return "$left->${right.toMutableList().apply { add(index,"・") }.joinToString("")} - $follow"
        }

        fun toRule() = LR0ParserGenerator.ProductionRuleData(
            left, right
        )

        fun toLR() = LR0ParserGenerator.LRProductionRuleData(
            left = left, right = right,
            index = this.index, reducible = reducible
        )
    }
}

class LR0ParserGenerator(
    private val rules: List<ProductionRuleData>,
    private val startSymbol: String) {

    companion object {
        private const val EMPTY = "ε"
    }

    val terminalTokens = mutableListOf<String>()
    val nonTerminalTokens = mutableListOf<String>()
    val gotoMap = mutableMapOf<Pair<String, String>, String>()
    val closureMap = mutableMapOf<Set<LRProductionRuleData>, String>()

    init {
        measureTimeMillis {
            calcTokenKind()
        }.run { println("Token calculated in $this ms") }
        measureTimeMillis {
            calcGoto()
        }.run { println("LR0GotoMap calculated in $this ms") }
    }

    private fun calcTokenKind() {
        println("CalcTokenKind")
        terminalTokens.clear()
        nonTerminalTokens.clear()
        nonTerminalTokens.addAll(rules.map { r -> r.left }.distinct())
        rules.forEach { r ->
            r.right.forEach { c ->
                if(!nonTerminalTokens.contains(c) && !terminalTokens.contains(c)) {
                    terminalTokens.add(c)
                }
            }
        }
        terminalTokens.remove(EMPTY)
        println("T=$terminalTokens")
        println("N=$nonTerminalTokens")
    }

    private fun calcGoto() {
        val initialGrammar = ProductionRuleData("$startSymbol'", listOf(startSymbol))
        val extendedRules = rules.toMutableList().apply {
            add(initialGrammar)
        }
        closureMap.clear()
        var closureIndex = 0
        closureMap[getClosure(setOf(initialGrammar.toLR()), extendedRules)] = "I${closureIndex++}"
        var updated = true
        while(updated) {
            updated = false
            for(entry in closureMap.toMap()) {
                val transitionalTokens = entry.key
                    .filter { r -> !r.reducible }
                    .map { r -> r.right[r.index] }
                    .distinct()
                for(t in transitionalTokens) {
                    if(gotoMap.containsKey(entry.value to t)) continue
                    updated = true
                    val gotoSet = getGoto(entry.key, t, extendedRules)
                    if(gotoSet.isNotEmpty()) {
                        closureMap[gotoSet]?.let {
                            gotoMap[entry.value to t] = it
                        } ?: kotlin.run {
                            val label = "I${closureIndex++}"
                            closureMap[gotoSet] = label
                            gotoMap[entry.value to t] = label
                        }
                    }
                }
            }
        }
    }

    private fun getClosure(
        input: Set<LRProductionRuleData>,
        grammarRules: List<ProductionRuleData> = rules
    ): Set<LRProductionRuleData> {
        //println("getClosure: $input")
        val result = input.toMutableSet()
        val addedTokens = mutableSetOf<String>()
        var updated = true
        while(updated) {
            updated = false
            for(rule in result.toSet()) {
                if(rule.reducible) continue
                val target = rule.right[rule.index]
                if(!addedTokens.contains(target)
                    && nonTerminalTokens.contains(target)) {
                    addedTokens.add(target)
                    val u = result.addAll(grammarRules.filter { r -> r.left == target }.map { r -> r.toLR() })
                    if(u) updated = true
                }
            }
        }
        //println("result: $result")
        return result
    }

    private fun getGoto(
        input: Set<LRProductionRuleData>,
        token: String,
        grammarRules: List<ProductionRuleData> = rules
    ): Set<LRProductionRuleData> {
        //println("GetGoto: $input, $token")
        return getClosure(
            input
                .filter { r -> !r.reducible && r.right[r.index] == token }
                .map { r -> r.shift() }
                .toSet(),
            grammarRules
        )
    }

    fun getKernelMap(): Map<String, Set<LRProductionRuleData>> {
        val result = mutableMapOf<String, Set<LRProductionRuleData>>()
        for(entry in closureMap) {
            result[entry.value] = entry.key.filter { d -> d.index != 0 || d.left == "$startSymbol'" }.toSet()
        }
        return result
    }

    fun printClosureMap() {
        println("Closures")
        closureMap.forEach { (t, u) ->
            println("${u.padEnd(3)}: $t")
        }
    }

    fun printGotoMap() {
        println("Goto Table")
        print("   ")
        terminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("  $  ")
        nonTerminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("\n")
        for(c in closureMap.values) {
            print(c.padEnd(3))
            terminalTokens.forEach { print(" ${gotoMap[c to it]?.padStart(3) ?: "   "} ") }
            print(" ${gotoMap[c to "$"]?.padStart(3) ?: "   "} ")
            nonTerminalTokens.forEach { print(" ${gotoMap[c to it]?.padStart(3) ?: "   "} ") }
            print("\n")
        }
    }

    fun isLR0Grammar(): Boolean {
        for(entry in closureMap) {
            if(entry.key.isEmpty()) continue
            if(entry.key.all { !it.reducible }) continue

            //shift/reduce競合
            if(entry.key.any { it.reducible } && entry.key.any{ !it.reducible }) return false

            //reduce/reduce競合
            if(entry.key.count { it.reducible } >= 2) return false
        }
        return true
    }

    data class LRProductionRuleData(
        val left: String,
        val right: List<String>,
        val index: Int,
        val reducible: Boolean
    ) {
        fun shift(): LRProductionRuleData {
            if(reducible) throw Exception("シフトできません $this")
            return LRProductionRuleData(
                left, right, index+1,
                reducible = index+1 >= right.size
            )
        }

        fun toLALR1(follow: Set<String>) = DragonParserGenerator.LALR1ProductionRuleData(
            left = left, right = right,
            index = index, reducible = reducible, follow
        )

        override fun toString(): String {
            return "$left->${right.toMutableList().apply { add(index,"・") }.joinToString("")}"
        }
    }

    data class ProductionRuleData(
        val left: String,
        val right: List<String> //tokenized
    ) {
        fun toLR() = LRProductionRuleData(
            left = left, right = right,
            index = 0, reducible = right.size == 1 && right[0] == EMPTY
        )

        fun toLALR1(follow: Set<String>) = DragonParserGenerator.LALR1ProductionRuleData(
            left = left, right = right,
            index = 0, reducible = right.size == 1 && right[0] == EMPTY, follow
        )
    }
}