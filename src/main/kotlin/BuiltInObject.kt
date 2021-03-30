class GlobalObject: ObjectData() {
    override val prototype: ObjectData? = null //実装依存
    override val className: String = "Global" //実装依存

    init {
        namedProperties["NaN"] = PropertyDescriptor.data(
            NumberData.naN(), address = 0, writable = false,
            enumerable = false, configurable = false
        )
        namedProperties["Infinity"] = PropertyDescriptor.data(
            NumberData.infiniteP(), writable = false, address = 0,
            enumerable = false, configurable = false)
        namedProperties["undefined"] = PropertyDescriptor.data(
            UndefinedData(), writable = false, address = 0,
            enumerable = false, configurable = false)
    }

    //ValueProperties=[NaN,Infinity,undefined]
    //FunctionProperties=
    //eval
    //parseInt,parseFloat,isNaN,isFinite,
    //decodeURI,decodeURIComponent,encodeURI,encodeURIComponent
    //ConstructorProperties=
    //Object,Function,Array,String,Boolean,Number,
    //Date,RegExp,Error,EvalError,RangeError,ReferenceError,
    //SyntaxError,TypeError,URIError
    //OtherProperties=
    //Math,JSON
}

class NormalObject: ObjectData() {
    override val prototype: ObjectData = PrototypeObject()
}

class PrototypeObject: ObjectData() {
    override val prototype: ObjectData? = null
    override val className: String = "Object"
    override val extensible: Boolean = true

    //constructor = ??????(standard built-in Object constructor
    //toString
    //toLocaleString
    //valueOf
    //hasOwnProperty
    //isPrototypeOf
    //propertyIsEnumerable
}

class FunctionObject(
    formalParameterList: List<String>?,
    functionBody: Parser.Node,
    scope: Environment,
    val strict: Boolean
): ObjectData() {
    override val prototype: ObjectData = FunctionPrototypeObject()
    override val className: String = "Function"
    override fun get(propertyName: String): EcmaData? {
        val v = super.get(propertyName)
        if(propertyName == "caller" && v is FunctionObject && v.strict) {
            throw Exception("TypeError")
        }
        return v
    }

    override val call: ((EcmaData,List<EcmaData>,GlobalObject) -> StackData) = lambda@ { thisValue, arguments, globalObj ->
        val compiler = ByteCompiler()
        compiler.runFunction(
            this, thisValue, arguments, globalObj
        )
        return@lambda ToneVirtualMachine().run(
            compiler.byteLines, compiler.refPool, globalObj
        )!!
    }

//    override val construct: ((List<Any>) -> ObjectData) = { arguments ->
//        val proto = get("prototype")
//        val obj = object: ObjectData() {
//            override val className: String = "Object"
//            override val extensible: Boolean = true
//            override val prototype: ObjectData =
//                if(proto is ObjectData) proto
//                else PrototypeObject()
//        }
//        val result = call(obj, arguments)
//        if(result is ObjectData) result
//        else obj
//    }

    override val hasInstance: ((Any) -> Boolean) = lambda@ { v ->
        if(v !is ObjectData) return@lambda false
        val o = get("prototype")
        if(o !is ObjectData) throw Exception("TypeError")
        var proto: ObjectData? = v
        while(true) {
            proto = proto?.prototype
            if(proto == null) return@lambda false
            if(TypeConverter.sameValue(o, proto)) return@lambda true
        }
        throw Exception()
    }

    override val scope: Environment = scope

    override val formalParameters: List<String>? = formalParameterList

    override val code: Parser.Node = functionBody

    override val extensible: Boolean = true

    init {
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.real(formalParameterList?.size ?: 0), 0,
            writable = false, enumerable = false, configurable = false
        )
        val proto = NormalObject()
        proto.defineOwnProperty("constructor", PropertyDescriptor.data(
            this, 0,
            writable = true, enumerable = false, configurable = true
        ), false)
        namedProperties["prototype"] = PropertyDescriptor.data(
            proto, 0,
            writable = true, enumerable = false, configurable = true
        )
        if(strict) {
            //val thrower =  13.2.3
//            namedProperties["caller"] = PropertyDescriptor.accessor(
//                get = thrower, set = thrower,
//                enumerable = false, configurable = false, address = 0
//            )
//            namedProperties["arguments"] = PropertyDescriptor.accessor(
//                get = thrower, set = thrower,
//                enumerable = false, configurable = false, address = 0
//            )
        }
    }

    object ThrowTypeObject: ObjectData() {
        override val className: String = "Function"
        override val prototype: ObjectData? = FunctionPrototypeObject()
        override val call: ((EcmaData,List<EcmaData>,GlobalObject) -> StackData) = lambda@ { thisValue, arguments, globalObj ->
//            val compiler = ByteCompiler()
//            compiler.runFunction(
//                this, thisValue, formalParameters, globalObj
//            )
//            return@lambda ToneVirtualMachine().run(
//                compiler.byteLines, compiler.refPool, globalObj
//            )!!
            throw NotImplementedError()
        }
        override val scope: Environment? = throw NotImplementedError() //Global Environment
        override val formalParameters: List<String> = listOf()
        override val code: Parser.Node = throw Exception("TypeError")
        override val extensible: Boolean = false
        //ThrowTypeError = F
        init {
            namedProperties["length"] = PropertyDescriptor.data(
                NumberData.zeroP(), 0,
                writable = false, enumerable = false, configurable = false
            )
        }
    }
}

class FunctionPrototypeObject: ObjectData() {
    override val prototype: ObjectData = PrototypeObject()
    override val className: String = "Function"
    override val extensible: Boolean = true

    init {
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.zeroP(), 0,
            writable = false, enumerable = false, configurable = false
        )
//        namedProperties["toString"] = throw NotImplementedError("15.3.4.2")
//        namedProperties["apply"] = throw NotImplementedError("15.3.4.3")
//        namedProperties["call"] = throw NotImplementedError("15.3.4.4")
//        namedProperties["bind"] = throw NotImplementedError("15.3.4.5")
    }
}

class ArgumentsObject(
    func: FunctionObject,
    names: List<String>,
    args: List<EcmaData>,
    variableEnvironment: Environment,
    strict: Boolean
): ObjectData() {
    override val className: String = "Arguments"
    override val prototype: ObjectData = PrototypeObject()
    var overrideMethods: Boolean = false
    var map: ObjectData
    init {
        val len = args.size
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.real(len), 0, writable = true,
            enumerable = false, configurable = true
        )
        map = NormalObject()
        val mappedNames = mutableListOf<String>()
        var index = len - 1
        while(index >= 0) {
            val value = args[index]
            defineOwnProperty(
                TypeConverter.toString(value),
                PropertyDescriptor.data(
                    value, 0, writable = true,
                    enumerable = true, configurable = true
                ),
                false
            )
            if(index < names.size) {
                val name = names[index]
                if(!strict && !mappedNames.contains(name)) {
                    mappedNames.add(name)
                    val g = makeArgGetter(name, variableEnvironment)
                    val p = makeArgSetter(name, variableEnvironment)
                    map.defineOwnProperty(
                        TypeConverter.toString(NumberData.real(index)),
                        PropertyDescriptor.accessor(
                            0, p, g,  configurable = true
                        ),
                        false
                    )
                }
            }
            index--
        }
        overrideMethods = mappedNames.isNotEmpty()
        if(mappedNames.isNotEmpty()) {
            //get, getOwnProperty, defineOwnProperty, delete
        }
        if(!strict) {
            namedProperties["callee"] = PropertyDescriptor.data(
                func, 0, writable = true,
                enumerable = false, configurable = true
            )
        }
        else {
            val thrower = FunctionObject.ThrowTypeObject
            namedProperties["caller"] = PropertyDescriptor.accessor(
                0, thrower, thrower,
                enumerable = false, configurable = false
            )
            namedProperties["callee"] = PropertyDescriptor.accessor(
                0, thrower, thrower,
                enumerable = false, configurable = false
            )
        }
    }

    private fun makeArgGetter(name: String, env: Environment): FunctionObject {
        return FunctionObject(
            null,
            ToneEngine.parseAsStatement("return $name;"),
            env,
            true
        )
    }

    private fun makeArgSetter(name: String, env: Environment): FunctionObject {
        val param = "${name}_arg"
        val body = ToneEngine.parseAsStatement("$name = $param;")
        return FunctionObject(
            listOf(param),
            body,
            env,
            true
        )
    }

    override val parameterMap: ObjectData? = if(overrideMethods) map else super.parameterMap
    override fun get(propertyName: String): EcmaData? {
        if(overrideMethods) {
            val isMapped = map.getOwnProperty(propertyName)
            if(isMapped == null) {
                val v = super.get(propertyName)
                if(propertyName == "caller" && v is FunctionObject && v.strict) {
                    throw Exception("TypeError")
                }
                return v
            }
            return map.get(propertyName)
        }
        else {
            return super.get(propertyName)
        }
    }

    override fun getOwnProperty(propertyName: String): PropertyDescriptor? {
        if(overrideMethods) {
            val desc = super.getOwnProperty(propertyName) ?: return null
            val isMapped = map.getOwnProperty(propertyName)
            if(isMapped == null) {
                namedProperties[propertyName] = namedProperties[propertyName]!!.copy(
                    value = map.get(propertyName)
                )
            }
            return desc
        }
        else {
            return super.getOwnProperty(propertyName)
        }
    }

    override fun defineOwnProperty(propertyName: String, descriptor: PropertyDescriptor, throwFlag: Boolean): Boolean {
        if(overrideMethods) {
            val isMapped = map.getOwnProperty(propertyName)
            val allowed = super.defineOwnProperty(propertyName, descriptor, false)
            if(!allowed) {
                if(throwFlag) throw Exception("TypeError")
                else return false
            }
            if(isMapped != null) {
                if(descriptor.type == PropertyDescriptor.DescriptorType.Accessor) {
                    map.delete(propertyName, false)
                }
                else {
                    if(descriptor.value != null) {
                        map.put(propertyName, descriptor.value, throwFlag)
                    }
                    if(descriptor.writable == false) {
                        map.delete(propertyName, false)
                    }
                }
            }
            return true
        }
        else {
            return super.defineOwnProperty(propertyName, descriptor, throwFlag)
        }
    }

    override fun delete(propertyName: String, throwFlag: Boolean): Boolean {
        return if(overrideMethods) {
            val isMapped = map.getOwnProperty(propertyName)
            val result = super.delete(propertyName, throwFlag)
            if(result && isMapped != null) {
                map.delete(propertyName, false)
            }
            result
        } else {
            super.delete(propertyName, throwFlag)
        }
    }
}
