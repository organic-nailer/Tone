import sun.reflect.generics.reflectiveObjects.NotImplementedException

class ExecutionContext(
    val lexicalEnvironment: Environment,
    val variableEnvironment: Environment,
    val thisBinding: ObjectData
) {
    companion object {
        fun global(globalObj: GlobalObject, code: Parser.Node, strict: Boolean): ExecutionContext {
            val globalEnvironment = newObjectEnvironment(globalObj, null)
            val context =  ExecutionContext(
                globalEnvironment,
                globalEnvironment,
                globalObj
            )
            declarationBindingInstantiation(
                context.variableEnvironment, code,
                ContextMode.Global, strict,
                null, null,
                globalObj
            )
            return context
        }

        fun eval(): ExecutionContext {
            TODO()
        }

        fun function(
            f: FunctionObject,
            thisArg: EcmaData?,
            arguments: List<EcmaData>,
            globalObject: GlobalObject
        ): ExecutionContext {
            val thisBinding = if(f.strict) {
                thisArg
            } else if(thisArg == null || thisArg is UndefinedData || thisArg is NullData) {
                globalObject
            } else if(thisArg !is ObjectData) {
                TypeConverter.toObject(thisArg)
            } else {
                thisArg
            }
            val localEnv = newDeclarativeEnvironment(f.scope)
            val context = ExecutionContext(
                thisBinding = thisBinding as ObjectData, //TODO 角煮
                lexicalEnvironment = localEnv,
                variableEnvironment = localEnv
            )
            declarationBindingInstantiation(
                context.variableEnvironment, f.code,
                ContextMode.Function, f.strict,
                f, arguments,
                globalObject
            )
            return context
        }
    }

    fun resolveIdentifier(identifier: String, strict: Boolean): ReferenceData {
        return getIdentifierReference(lexicalEnvironment, identifier, strict)
    }
}

class Environment {
    lateinit var records: EnvironmentRecords
    var outer: Environment? = null
}


abstract class EnvironmentRecords {
    abstract fun hasBinding(identifier: String): Boolean
    abstract fun createMutableBinding(identifier: String, deletable: Boolean)
    abstract fun setMutableBinding(identifier: String, value: EcmaData, strict: Boolean)
    abstract fun getBindingValue(identifier: String, strict: Boolean): EcmaData
    abstract fun deleteBinding(identifier: String): Boolean
    abstract fun implicitThisValue(): EcmaData
}

class DeclarativeEnvironmentRecords : EnvironmentRecords() {
    data class RecordData(
        val value: EcmaData?,
        val mutable: Boolean,
        val deletable: Boolean
    )
    private val records: MutableMap<String, RecordData> = mutableMapOf()
    override fun hasBinding(identifier: String): Boolean {
        return records.containsKey(identifier)
    }

    override fun createMutableBinding(identifier: String, deletable: Boolean) {
        assert(!records.containsKey(identifier))
        records[identifier] = RecordData(UndefinedData(),
            true, deletable)
    }

    override fun setMutableBinding(identifier: String, value: EcmaData, strict: Boolean) {
        assert(records.containsKey(identifier))
        if(records[identifier]!!.mutable) {
            records[identifier] = records[identifier]!!.copy(value = value)
        }
        else {
            if(strict) throw Exception("TypeError") //TODO: TypeError
        }
    }

    override fun getBindingValue(identifier: String, strict: Boolean): EcmaData {
        assert(records.containsKey(identifier))
        val current = records[identifier]!!
        if(!current.mutable && current.value == null) {
            if(strict) throw Exception("ReferenceError") //TODO: ReferenceError
            return UndefinedData()
        }
        return current.value!!
    }

    override fun deleteBinding(identifier: String): Boolean {
        if(!records.containsKey(identifier)) return true
        if(!records[identifier]!!.deletable) return false
        records.remove(identifier)
        return true
    }

    override fun implicitThisValue(): EcmaData {
        return UndefinedData()
    }

    fun createImmutableBinding(identifier: String) {
        assert(!records.containsKey(identifier))
        records[identifier] = RecordData(
            null, mutable = false, deletable = false //TODO: deletableがこれ出会ってるか
        )
    }

    fun initializeImmutableBinding(identifier: String, value: EcmaData) {
        assert(records.containsKey(identifier))
        records[identifier] = records[identifier]!!.copy(value = value)
    }
}

class ObjectEnvironmentRecords(
    val bindings: ObjectData
) : EnvironmentRecords() {
    var provideThis: Boolean = false
    override fun hasBinding(identifier: String): Boolean {
        return bindings.hasProperty(identifier)
    }

    override fun createMutableBinding(identifier: String, deletable: Boolean) {
        assert(!bindings.hasProperty(identifier))
        bindings.defineOwnProperty(identifier,
            ObjectData.PropertyDescriptor.data(
                UndefinedData(), 0,
                writable = true, enumerable = true, deletable
            ),
            throwFlag = true
        )
    }

    override fun setMutableBinding(identifier: String, value: EcmaData, strict: Boolean) {
        bindings.put(identifier, value, strict)
    }

    override fun getBindingValue(identifier: String, strict: Boolean): EcmaData {
        val value = bindings.hasProperty(identifier)
        if(!value) {
            if(strict) return UndefinedData()
            throw Exception("ReferenceError") //TODO: ReferenceError
        }
        return bindings.get(identifier)!!
    }

    override fun deleteBinding(identifier: String): Boolean {
        return bindings.delete(identifier, false)
    }

    override fun implicitThisValue(): EcmaData {
        if(provideThis) return bindings
        return UndefinedData()
    }
}

fun getIdentifierReference(lex: Environment?, name: String, strict: Boolean): ReferenceData {
    if(lex == null) return ReferenceData(null, name, strict)
    val envRec = lex.records
    val exists = envRec.hasBinding(name)
    if(exists) {
        return ReferenceData(
            envRec, name, strict
        )
    }
    else {
        val outer = lex.outer
        return getIdentifierReference(outer, name, strict)
    }
}

data class ReferenceData(
    val base: Any?,//baseがundefinedであるというのはnullで表現する
    val referencedName: String,
    val strict: Boolean
) {
    fun hasPrimitiveBase(): Boolean {
        return base is BooleanData || base is StringData || base is NumberData
    }

    fun isPropertyReference(): Boolean {
        return base is ObjectData || hasPrimitiveBase()
    }

    fun isUnresolvableReference(): Boolean {
        return base == null
    }
}

fun newDeclarativeEnvironment(outer: Environment?): Environment {
    val env = Environment()
    val envRec = DeclarativeEnvironmentRecords()
    env.records = envRec
    env.outer = outer
    return env
}

fun newObjectEnvironment(obj: ObjectData, outer: Environment?): Environment {
    val env = Environment()
    val envRec = ObjectEnvironmentRecords(obj)
    env.records = envRec
    env.outer = outer
    return env
}

enum class ContextMode { Eval, Function, Global }
fun declarationBindingInstantiation(
    variableEnvironment: Environment,
    code: Parser.Node,
    mode: ContextMode,
    strict: Boolean,
    func: FunctionObject?,
    args: List<EcmaData>?,
    globalObj: GlobalObject
) {
    val env = variableEnvironment.records
    val configurableBindings = mode == ContextMode.Eval
    if(mode == ContextMode.Function) {
        val names = func!!.formalParameters ?: listOf()
        val argCount = args!!.size
        var n = 0
        for(argName in names) {
            val v = if(n < argCount) args[n] else UndefinedData()
            val argAlreadyDeclared = env.hasBinding(argName)
            if(!argAlreadyDeclared) {
                env.createMutableBinding(argName, false) //TODO: deletable
            }
            env.setMutableBinding(argName, v, strict)
            n++
        }
    }
    code.body?.filter { it.type == Parser.NodeType.FunctionDeclaration }?.forEach { f ->
        val fn = f.id!!.name!!
        val fo = FunctionObject(f.params!!.map { it.name!! }, f.bodySingle!!, variableEnvironment, false) //TODO: strict
        val funcAlreadyDeclared = env.hasBinding(fn)
        if(!funcAlreadyDeclared) {
            env.createMutableBinding(fn, configurableBindings)
        }
        else if(mode == ContextMode.Global) {
            val go = globalObj
            val existingProp = go.getProperty(fn)
            if(existingProp?.configurable == true) {
                go.defineOwnProperty(fn, ObjectData.PropertyDescriptor.data(
                    value = UndefinedData(), writable = true, address = 0,
                    enumerable = true, configurable = configurableBindings
                ), throwFlag = true)
            }
            else if(existingProp?.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor
                || (existingProp?.writable != true || existingProp.enumerable != true)) {
                throw Exception("TypeError") //TODO: TypeError
            }
        }
        env.setMutableBinding(fn, fo, strict)
    }
    val argumentsAlreadyDeclared = env.hasBinding("arguments")
    if(mode == ContextMode.Function && !argumentsAlreadyDeclared) {
        val names = func!!.formalParameters ?: listOf()
        val argsObj = ArgumentsObject(func, names, args!!, variableEnvironment, strict)
        if(strict) {
            env as DeclarativeEnvironmentRecords
            env.createImmutableBinding("arguments")
            env.initializeImmutableBinding("arguments", argsObj)
        }
        else {
            env.createMutableBinding("arguments", false) //TODO deletable
            env.setMutableBinding("arguments", argsObj, false)
        }
    }
    findVariableDeclaration(code.body ?: listOf()).forEach { ds ->
        ds.declarations?.forEach { d ->
            val dn = d.id!!.name!!
            val varAlreadyDeclared = env.hasBinding(dn)
            if(!varAlreadyDeclared) {
                env.createMutableBinding(dn, configurableBindings)
                env.setMutableBinding(dn, UndefinedData(), strict)
            }
        }
    }
}

private fun findVariableDeclaration(code: List<Parser.Node>): List<Parser.Node> {
    val result = mutableListOf<Parser.Node>()
    code.forEach { c ->
        when(c.type) {
            Parser.NodeType.VariableDeclaration -> result.add(c)
            Parser.NodeType.BlockStatement,
            Parser.NodeType.WhileStatement,
            Parser.NodeType.DoWhileStatement -> {
                c.body?.let { result.addAll(findVariableDeclaration(it)) }
            }
            Parser.NodeType.IfStatement -> {
                c.consequent?.let { result.addAll(findVariableDeclaration(listOf(it))) }
                c.alternate?.let { result.addAll(findVariableDeclaration(listOf(it))) }
            }
            Parser.NodeType.ForStatement -> {
                c.body?.let { result.addAll(findVariableDeclaration(it)) }
                if(c.init?.type == Parser.NodeType.VariableDeclaration) {
                    result.add(c.init)
                }
            }
            Parser.NodeType.ForInStatement -> {
                c.body?.let { result.addAll(findVariableDeclaration(it)) }
                if(c.left?.type == Parser.NodeType.VariableDeclaration) {
                    result.add(c.left)
                }
            }
            Parser.NodeType.SwitchStatement -> {
                c.cases?.forEach { case ->
                    case.consequents?.let { result.addAll(findVariableDeclaration(it)) }
                }
            }
            Parser.NodeType.TryStatement -> {
                c.block?.let { result.addAll(findVariableDeclaration(listOf(it))) }
                //catchは別？
                c.finalizer?.let { result.addAll(findVariableDeclaration(listOf(it))) }
            }
            else -> { }
        }
    }
    return result
}
