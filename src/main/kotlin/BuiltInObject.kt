class GlobalObject: ObjectData() {
    override val prototype: ObjectData? = null //実装依存
    override val className: String = "Global" //実装依存

    init {
        namedProperties["NaN"] = PropertyDescriptor.data(
            NumberData.naN(), writable = false,
            enumerable = false, configurable = false
        )
        namedProperties["Infinity"] = PropertyDescriptor.data(
            NumberData.infiniteP(), writable = false,
            enumerable = false, configurable = false)
        namedProperties["undefined"] = PropertyDescriptor.data(
            UndefinedData(), writable = false,
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
    val strict: Boolean,
    private val globalObj: GlobalObject
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

    override val call: ((EcmaData,List<EcmaData>) -> StackData) = lambda@ { thisValue, arguments ->
        val compiler = ByteCompiler()
        compiler.runFunction(
            this, thisValue, arguments, globalObj
        )
        return@lambda ToneVirtualMachine().run(
            compiler.byteLines, compiler.refPool,
            compiler.constantPool, compiler.objectPool, globalObj
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
            NumberData.real(formalParameterList?.size ?: 0),
            writable = false, enumerable = false, configurable = false
        )
        val proto = NormalObject()
        proto.defineOwnProperty("constructor", PropertyDescriptor.data(
            this,
            writable = true, enumerable = false, configurable = true
        ), false)
        namedProperties["prototype"] = PropertyDescriptor.data(
            proto,
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
        override val prototype: ObjectData = FunctionPrototypeObject()
        override val call: ((EcmaData,List<EcmaData>) -> StackData) = lambda@ { thisValue, arguments ->
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
                NumberData.zeroP(),
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
            NumberData.zeroP(),
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
    strict: Boolean,
    private val globalObj: GlobalObject
): ObjectData() {
    override val className: String = "Arguments"
    override val prototype: ObjectData = PrototypeObject()
    private var overrideMethods: Boolean = false
    private var map: ObjectData
    init {
        val len = args.size
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.real(len), writable = true,
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
                    value, writable = true,
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
                            p, g,  configurable = true
                        ),
                        false
                    )
                }
            }
            index--
        }
        overrideMethods = mappedNames.isNotEmpty()
        if(!strict) {
            namedProperties["callee"] = PropertyDescriptor.data(
                func, writable = true,
                enumerable = false, configurable = true
            )
        }
        else {
            val thrower = FunctionObject.ThrowTypeObject
            namedProperties["caller"] = PropertyDescriptor.accessor(
                thrower, thrower,
                enumerable = false, configurable = false
            )
            namedProperties["callee"] = PropertyDescriptor.accessor(
                thrower, thrower,
                enumerable = false, configurable = false
            )
        }
    }

    private fun makeArgGetter(name: String, env: Environment): FunctionObject {
        return FunctionObject(
            null,
            ToneEngine.parseAsStatement("return $name;"),
            env,
            true,
            globalObj
        )
    }

    private fun makeArgSetter(name: String, env: Environment): FunctionObject {
        val param = "${name}_arg"
        val body = ToneEngine.parseAsStatement("$name = $param;")
        return FunctionObject(
            listOf(param),
            body,
            env,
            true,
            globalObj
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

class ArrayObject: ObjectData() {
    val isSparse: Boolean
        get() {
            val len = get("length")!!
            for(i in 0 until TypeConverter.toUInt32(len)) {
                val elem = getOwnProperty(TypeConverter.toString(NumberData.real(i))) ?: return true
            }
            return false
        }

    override val prototype: ObjectData = ArrayPrototypeObject()
    override val className: String = "Array"
    override val extensible: Boolean = true
    override fun defineOwnProperty(propertyName: String, descriptor: PropertyDescriptor, throwFlag: Boolean): Boolean {
        var oldLenDesc = getOwnProperty("length")!!
        var oldLen = (oldLenDesc.value as NumberData).value
        if(propertyName == "length") {
            if(descriptor.value == null) {
                return super.defineOwnProperty("length", descriptor, throwFlag)
            }
            val newLen = TypeConverter.toUInt32(descriptor.value)
            if(newLen != TypeConverter.toNumber(descriptor.value).value) throw Exception("RangeError")
            var newLenDesc = descriptor.copy(value = NumberData.real(newLen))
            if(newLen >= oldLen) {
                return super.defineOwnProperty("length", newLenDesc, throwFlag)
            }
            if(oldLenDesc.writable == false) {
                if(throwFlag) throw Exception("TypeError")
                return false
            }
            val newWritable = if(newLenDesc.writable == null || newLenDesc.writable == true) {
                true
            }
            else {
                newLenDesc = newLenDesc.copy(writable = true)
                false
            }
            val succeeded = super.defineOwnProperty(
                "length", newLenDesc, throwFlag
            )
            if(!succeeded) return false
            while(newLen < oldLen) {
                oldLen--
                val deleteSucceeded = delete(
                    TypeConverter.toString(NumberData.real(oldLen)),
                    false
                )
                if(!deleteSucceeded) {
                    newLenDesc = newLenDesc.copy(value = NumberData.real(oldLen+1))
                    if(!newWritable) {
                        newLenDesc = newLenDesc.copy(writable = false)
                    }
                    super.defineOwnProperty("length", newLenDesc, false)
                    if(throwFlag) throw Exception("TypeError")
                    return false
                }
            }
            if(!newWritable) {
                super.defineOwnProperty("length",
                    PropertyDescriptor(writable = false, type = PropertyDescriptor.DescriptorType.Data),
                    false
                )
                return true
            }
            return true
        }
        val p = propertyName.toIntOrNull()
        if(p != null) { //TODO: if P is an array index
            val index = TypeConverter.toUInt32(StringData(propertyName))
            if(index >= oldLen && oldLenDesc.writable == false) {
                if(throwFlag) throw Exception("TypeError")
                return false
            }
            val succeeded = super.defineOwnProperty(propertyName, descriptor, false)
            if(index >= oldLen) {
                oldLenDesc = oldLenDesc.copy(value = NumberData.real(index+1))
                super.defineOwnProperty("length", oldLenDesc, false)
            }
            return true
        }
        return super.defineOwnProperty(propertyName, descriptor, throwFlag)
    }

    init {
        namedProperties["prototype"] = PropertyDescriptor.data(
            prototype, writable = false,
            enumerable = false, configurable = false
        )
        //isArray(arg)
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.zeroP(),
            writable = true, enumerable = false, configurable = false
        )
    }
}

class ArrayPrototypeObject: ObjectData() {
    override val prototype: ObjectData = PrototypeObject()
    override val className: String = "Array"
    override val extensible: Boolean = true

    init {
        namedProperties["length"] = PropertyDescriptor.data(
            NumberData.zeroP(),
            writable = false, enumerable = false, configurable = false
        )
//        namedProperties["toString"] = throw NotImplementedError("15.4.4.2")
//        namedProperties["toLocaleString"] = throw NotImplementedError("15.4.4.3")
//        namedProperties["concat"] = throw NotImplementedError("15.4.4.4")
//        namedProperties["join"] = throw NotImplementedError("15.4.4.5")
//        namedProperties["pop"] = throw NotImplementedError("15.4.4.6")
//        namedProperties["push"] = throw NotImplementedError("15.4.4.7")
//        namedProperties["reverse"] = throw NotImplementedError("15.4.4.8")
        //shift slice sort splice unshift indexOf lastIndexOf every some forEach map
    // filter reduce reduceRight
    }

    override fun defineOwnProperty(propertyName: String, descriptor: PropertyDescriptor, throwFlag: Boolean): Boolean {
        var oldLenDesc = getOwnProperty("length")!!
        var oldLen = (oldLenDesc.value as NumberData).value
        if(propertyName == "length") {
            if(descriptor.value == null) {
                return super.defineOwnProperty("length", descriptor, throwFlag)
            }
            val newLen = TypeConverter.toUInt32(descriptor.value)
            if(newLen != TypeConverter.toNumber(descriptor.value).value) throw Exception("RangeError")
            var newLenDesc = descriptor.copy(value = NumberData.real(newLen))
            if(newLen >= oldLen) {
                return super.defineOwnProperty("length", newLenDesc, throwFlag)
            }
            if(oldLenDesc.writable == false) {
                if(throwFlag) throw Exception("TypeError")
                return false
            }
            val newWritable = if(newLenDesc.writable == null || newLenDesc.writable == true) {
                true
            }
            else {
                newLenDesc = newLenDesc.copy(writable = true)
                false
            }
            val succeeded = super.defineOwnProperty(
                "length", newLenDesc, throwFlag
            )
            if(!succeeded) return false
            while(newLen < oldLen) {
                oldLen--
                val deleteSucceeded = delete(
                    TypeConverter.toString(NumberData.real(oldLen)),
                    false
                )
                if(!deleteSucceeded) {
                    newLenDesc = newLenDesc.copy(value = NumberData.real(oldLen+1))
                    if(!newWritable) {
                        newLenDesc = newLenDesc.copy(writable = false)
                    }
                    super.defineOwnProperty("length", newLenDesc, false)
                    if(throwFlag) throw Exception("TypeError")
                    return false
                }
            }
            if(!newWritable) {
                super.defineOwnProperty("length",
                    PropertyDescriptor(writable = false, type = PropertyDescriptor.DescriptorType.Data),
                    false
                )
                return true
            }
            return true
        }
        val p = propertyName.toIntOrNull()
        if(p != null) { //TODO: if P is an array index
            val index = TypeConverter.toUInt32(StringData(propertyName))
            if(index >= oldLen && oldLenDesc.writable == false) {
                if(throwFlag) throw Exception("TypeError")
                return false
            }
            val succeeded = super.defineOwnProperty(propertyName, descriptor, false)
            if(index >= oldLen) {
                oldLenDesc = oldLenDesc.copy(value = NumberData.real(index+1))
                super.defineOwnProperty("length", oldLenDesc, false)
            }
            return true
        }
        return super.defineOwnProperty(propertyName, descriptor, throwFlag)
    }
}
