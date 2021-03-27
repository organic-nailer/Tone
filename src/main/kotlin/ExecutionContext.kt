import sun.reflect.generics.reflectiveObjects.NotImplementedException

class ExecutionContext(
    val lexicalEnvironment: Environment,
    val variableEnvironment: Environment,
    val thisBinding: ObjectData
) {
    companion object {
        fun global(refPoolManager: ByteCompiler.RefPoolManager): ExecutionContext {
            val globalObj = GlobalObject(refPoolManager)
            val globalEnvironment = newObjectEnvironment(globalObj, null)
            return ExecutionContext(
                globalEnvironment,
                globalEnvironment,
                globalObj
            )
        }

        fun eval(): ExecutionContext {
            TODO()
        }

        fun functionContext(): ExecutionContext {
            TODO()
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
    abstract fun getBindingValue(identifier: String, strict: Boolean): ObjectData.PropertyDescriptor
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

    override fun getBindingValue(identifier: String, strict: Boolean): ObjectData.PropertyDescriptor {
        assert(records.containsKey(identifier))
        val current = records[identifier]!!
        if(!current.mutable && current.value == null) {
            if(strict) throw Exception("ReferenceError") //TODO: ReferenceError
            return ObjectData.PropertyDescriptor.innocent(UndefinedData())
        }
        throw NotImplementedException()
        //return current!!
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

    override fun getBindingValue(identifier: String, strict: Boolean): ObjectData.PropertyDescriptor {
        val value = bindings.hasProperty(identifier)
        if(!value) {
            if(strict) return ObjectData.PropertyDescriptor.innocent(UndefinedData())
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
fun declarationBindingInstantiation(variableEnvironment: Environment, code: List<Parser.Node>, mode: ContextMode, strict: Boolean) {
    val env = variableEnvironment.records
    val configurableBindings = mode == ContextMode.Eval
    if(mode == ContextMode.Function) {
        TODO()
    }
//    code.filter { it.type == Parser.NodeType.FunctionDeclaration }.forEach { f ->
//        val fn = f.id!!.name!!
//        //val fo = result of instantiating FunctionDeclaration
//        val funcAlreadyDeclared = env.hasBinding(fn)
//        if(!funcAlreadyDeclared) {
//            env.createMutableBinding(fn, configurableBindings)
//        }
//        else if(mode == ContextMode.Global) {
//            //Global Object
//            val go = (variableEnvironment.records as ObjectEnvironmentRecords).bindings
//            val existingProp = go.getProperty(fn)
//            if(existingProp?.configurable == true) {
//                go.defineOwnProperty(fn, ObjectData.PropertyDescriptor.data(
//                    value = UndefinedData(), writable = true, address = 0,
//                    enumerable = true, configurable = configurableBindings
//                ), throwFlag = true)
//            }
//            else if(existingProp?.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor
//                || (existingProp?.writable != true || existingProp.enumerable != true)) {
//                throw Exception("TypeError") //TODO: TypeError
//            }
//        }
//        env.setMutableBinding(fn, fo, strict)
//    }
    val argumentsAlreadyDeclared = env.hasBinding("arguments")
    if(mode == ContextMode.Function && !argumentsAlreadyDeclared) {
        throw NotImplementedException()
//        val argsObj = createArgumentsObject(func, names, args, env, strict)
//        if(strict) {
//            env as DeclarativeEnvironmentRecords
//            env.createImmutableBinding("arguments")
//            env.initializeImmutableBinding("arguments", argsObj)
//        }
//        else {
//            env.createMutableBinding("arguments")
//            env.setMutableBinding("arguments", argsObj, false)
//        }
    }
    code.filter { it.type == Parser.NodeType.VariableDeclaration }.forEach { ds ->
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
