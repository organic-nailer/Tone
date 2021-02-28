package esTree

import java.lang.Exception

class LALR1ParserGenerator(
    rules: List<LR1ParserGenerator.ProductionRuleData>,
    private val startSymbol: String) {

    val gotoMap = mutableMapOf<Pair<String, String>, String>()
    val closureMap = mutableMapOf<Set<LALR1ProductionRuleData>, String>()
    val lr1ParserGenerator: LR1ParserGenerator = LR1ParserGenerator(rules, startSymbol)

    val transitionMap = mutableMapOf<Pair<String, String>, TransitionData>()
    enum class TransitionKind { SHIFT, REDUCE, ACCEPT }
    data class TransitionData(
        val kind: TransitionKind,
        val value: String?,
        val rule: LR1ParserGenerator.ProductionRuleData?
    ) {
        override fun toString(): String {
            return when(kind) {
                TransitionKind.SHIFT -> "s${value?.substring(1)}"
                TransitionKind.REDUCE -> "r${rule?.left}"
                TransitionKind.ACCEPT -> "acc"
            }
        }
    }

    init {
        calcLALR1Map()
        calcTransition()
        printClosureMap()
        //printGotoMap()
        printTransitionMap()
    }

    private fun calcLALR1Map() {
        val ruleGroups = mutableListOf<LR1RuleCoreGroup>()
        for(entry in lr1ParserGenerator.closureMap) {
            val cores = entry.key.map { r -> LR1RuleCore(r.left, r.right, r.index, r.reducible) }.toSet()
            val match = ruleGroups.firstOrNull { r -> r.cores == cores }
            if(match != null) {
                match.states.add(entry.value)
            }
            else {
                ruleGroups.add(LR1RuleCoreGroup(
                    cores, mutableListOf(entry.value)
                ))
            }
        }
        ruleGroups.sortBy { r -> r.states.minOrNull() } //数字の若い順に
        val closureMapStateKey = lr1ParserGenerator.closureMap.toList().map { it.second to it.first }.toMap()
        val gotoTransformData = mutableMapOf<String, String>()
        ruleGroups.forEachIndexed { index, group ->
            val newState = "J$index"
            for(state in group.states) {
                gotoTransformData[state] = newState
            }
            val ruleData = group.cores.map { c ->
                val follows = mutableSetOf<String>()
                group.states.mapNotNull { s -> closureMapStateKey[s]
                    ?.filter { r -> r.left == c.left && r.right == c.right && r.index == c.index }
                }.forEach { rs ->
                    rs.forEach { r -> follows.add(r.follow) }
                }
                LALR1ProductionRuleData(
                    c.left, c.right, c.index, c.reducible,
                    follows.toList()
                )
            }.toSet()
            closureMap[ruleData] = newState
        }
        for(entry in lr1ParserGenerator.gotoMap) {
            val newState = gotoTransformData[entry.key.first] ?: continue
            val toNewState = gotoTransformData[entry.value] ?: continue
            gotoMap[newState to entry.key.second] = toNewState
        }
    }

    private fun calcTransition() {
        for(entry in gotoMap) {
            if(lr1ParserGenerator.terminalTokens.contains(entry.key.second) || true) {
                if(transitionMap.containsKey(entry.key)) {
                    throw Exception("LALR競合1 $entry")
                }
                transitionMap[entry.key] = TransitionData(
                    TransitionKind.SHIFT, entry.value, null
                )
            }
        }
        //printTransitionMap()
        for(entry in closureMap) {
            entry.key.filter { r -> r.reducible }.forEach {
                it.follow.forEach { token ->
                    if(transitionMap.containsKey(entry.value to token)) {
                        throw Exception("SLR競合2 $token $entry")
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
        lr1ParserGenerator.terminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("  $  ")
        lr1ParserGenerator.nonTerminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("\n")
        for(c in closureMap.values) {
            print(c.padEnd(3))
            lr1ParserGenerator.terminalTokens.forEach { print(" ${gotoMap[c to it]?.padStart(3) ?: "   "} ") }
            print(" ${gotoMap[c to "$"]?.padStart(3) ?: "   "} ")
            lr1ParserGenerator.nonTerminalTokens.forEach { print(" ${gotoMap[c to it]?.padStart(3) ?: "   "} ") }
            print("\n")
        }
    }

    fun printTransitionMap() {
        println("Transition Table")
        print("   ")
        lr1ParserGenerator.terminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("  $  ")
        lr1ParserGenerator.nonTerminalTokens.forEach { print(" ${it.padStart(3)} ") }
        print("\n")
        for(c in closureMap.values) {
            print(c.padEnd(3))
            lr1ParserGenerator.terminalTokens.forEach { print(" ${transitionMap[c to it]?.toString()?.padStart(3) ?: "   "} ") }
            print(" ${transitionMap[c to "$"]?.toString()?.padStart(3) ?: "   "} ")
            lr1ParserGenerator.nonTerminalTokens.forEach { print(" ${transitionMap[c to it]?.toString()?.padStart(3) ?: "   "} ") }
            print("\n")
        }
    }

    data class LR1RuleCoreGroup(
        val cores: Set<LR1RuleCore>,
        val states: MutableList<String>
    )

    data class LR1RuleCore(
        val left: String,
        val right: List<String>,
        val index: Int,
        val reducible: Boolean
    )

    data class LALR1ProductionRuleData(
        val left: String,
        val right: List<String>,
        val index: Int,
        val reducible: Boolean,
        val follow: List<String>
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

        fun toRule() = LR1ParserGenerator.ProductionRuleData(
            left, right
        )
    }
}

class LR1ParserGenerator(
    private val rules: List<ProductionRuleData>,
    private val startSymbol: String) {
    companion object {
        private const val EMPTY = "ε"
    }

    val terminalTokens = mutableListOf<String>()
    val nonTerminalTokens = mutableListOf<String>()
    val firstMap = mutableMapOf<String, MutableSet<String>>()
    val gotoMap = mutableMapOf<Pair<String, String>, String>()
    val closureMap = mutableMapOf<Set<LR1ProductionRuleData>, String>()

    init {
        calcTokenKind()
        calcFirst()
        calcGoto()
    }

    private fun calcTokenKind() {
        //println("CalcTokenKind")
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
        //println("T=$terminalTokens")
        //println("N=$nonTerminalTokens")
    }

    private fun getFirst(value: List<String>): Set<String> {
        if(value.isEmpty()) return setOf()
        if(terminalTokens.contains(value.first()) || value.first() == EMPTY || value.first() == "$") {
            return setOf(value.first())//{α} or {ε}
        }
        firstMap[value.first()]?.let {
            if(!it.contains(EMPTY) || value.size == 1) {
                return it //First(Y)
            }
            return it.minus(EMPTY).union(getFirst(value.slice(1 until value.size))) //(First(Y)-{ε})∪First(α)
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
                val firstAlpha = getFirst(rule.right)
                val firstX = firstMap[rule.left] ?: emptySet()
                val diff = firstAlpha.minus(firstX)
                if(diff.isNotEmpty()) {
                    updated = true
                    addFirst(rule.left, diff)
                }
            }
        }
    }

    private fun getClosure(
        input: Set<LR1ProductionRuleData>,
        grammarRules: List<ProductionRuleData> = rules
    ): Set<LR1ProductionRuleData> {
        //println("getClosure: $input")
        val result = input.toMutableSet()
        var updated = true
        while(updated) {
            updated = false
            for(rule in result.toSet()) {
                if(rule.reducible) continue
                val target = rule.right[rule.index]
                if(nonTerminalTokens.contains(target)) {
                    val firstSet = getFirst(
                        rule.right.slice(rule.index+1 until rule.right.size).toMutableList().apply { add(rule.follow) }
                    )
                    firstSet.forEach { f ->
                        val u = result.addAll(grammarRules.filter { r -> r.left == target }.map { r -> r.toLR1(f) })
                        if(u) updated = true
                    }
                }
            }
        }
        //println("result: $result")
        return result
    }

    private fun calcGoto() {
        val initialGrammar = ProductionRuleData("$startSymbol'", listOf(startSymbol))
        val extendedRules = rules.toMutableList().apply {
            add(initialGrammar)
        }
        closureMap.clear()
        var closureIndex = 0
        closureMap[getClosure(setOf(initialGrammar.toLR1("$")), extendedRules)] = "I${closureIndex++}"
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

    private fun getGoto(
        input: Set<LR1ProductionRuleData>,
        token: String,
        grammarRules: List<ProductionRuleData> = rules
    ): Set<LR1ProductionRuleData> {
        //println("GetGoto: $input, $token")
        return getClosure(
            input
                .filter { r -> !r.reducible && r.right[r.index] == token }
                .map { r -> r.shift() }
                .toSet(),
            grammarRules
        )
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

    data class LR1ProductionRuleData(
        val left: String,
        val right: List<String>,
        val index: Int,
        val reducible: Boolean,
        val follow: String
    ) {
        fun shift(): LR1ProductionRuleData {
            if(reducible) throw Exception("シフトできません $this")
            return LR1ProductionRuleData(
                left, right, index+1,
                reducible = index+1 >= right.size,
                follow
            )
        }

        override fun toString(): String {
            return "$left->${right.toMutableList().apply { add(index,"・") }.joinToString("")} - $follow"
        }
    }

    data class ProductionRuleData(
        val left: String,
        val right: List<String> //tokenized
    ) {
        fun toLR1(follow: String) = LR1ProductionRuleData(
            left = left, right = right,
            index = 0, reducible = right.size == 1 && right[0] == EMPTY,
            follow
        )
    }
}
