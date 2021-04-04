import TypeConverter.checkObjectCoercible
import TypeConverter.isCallable
import TypeConverter.toBoolean
import TypeConverter.toInt32
import TypeConverter.toNumber
import TypeConverter.toObject
import TypeConverter.toPrimitive
import TypeConverter.toString
import TypeConverter.toUInt32

class ToneVirtualMachine {
    private val referencePool = mutableListOf<ReferenceData>()
    private val constantPool = mutableListOf<EcmaPrimitive>()
    private val objectPool = mutableListOf<ObjectData>()
    var globalObjectData: GlobalObject? = null
    fun run(
        code: List<ByteCompiler.ByteOperation?>,
        refPool: List<ReferenceData>,
        constPool: List<EcmaPrimitive>,
        objPool: List<ObjectData>,
        global: GlobalObject
    ): StackData? {
        val mutCode = code.toMutableList()
        referencePool.clear()
        referencePool.addAll(refPool)
        constantPool.clear()
        constantPool.addAll(constPool)
        objectPool.clear()
        objectPool.addAll(objPool)
        globalObjectData = global
        val mainStack = ArrayDeque<StackData>()
        var counter = 0
        while(counter < mutCode.size) {
            println("$counter: $mainStack")
            val operation = mutCode[counter]
            println("op=$operation")
            if(operation == null) throw Exception()
            when(operation.opCode) {
                ByteCompiler.OpCode.Push -> {
                    mainStack.addFirst(operandToData(operation.operand))
                }
                ByteCompiler.OpCode.Pop -> {
                    mainStack.removeFirst()
                }
                ByteCompiler.OpCode.Mul,
                ByteCompiler.OpCode.Div,
                ByteCompiler.OpCode.Rem,
                ByteCompiler.OpCode.Sub -> {
                    val rRef = mainStack.removeFirst()
                    val rVal = getValue(rRef)
                    val lRef = mainStack.removeFirst()
                    val lVal = getValue(lRef)
                    val rightNum = toNumber(rVal)
                    val leftNum = toNumber(lVal)
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.Mul -> leftNum * rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Div -> leftNum / rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Rem -> leftNum % rightNum //TODO: 厳密な計算
                        ByteCompiler.OpCode.Sub -> leftNum - rightNum //TODO: 厳密な計算
                        else -> throw Exception()
                    }
                    mainStack.addFirst(result.toStack())
                }
                ByteCompiler.OpCode.Add -> {
                    val rRef = mainStack.removeFirst()
                    val rVal = getValue(rRef)
                    val lRef = mainStack.removeFirst()
                    val lVal = getValue(lRef)
                    val rPrim = toPrimitive(rVal)
                    val lPrim = toPrimitive(lVal)
                    if(lPrim is StringData || lPrim is ObjectData) {
                        throw NotImplementedError()
                    }
                    mainStack.addFirst((toNumber(lPrim) + toNumber(rPrim)).toStack())
                }
                ByteCompiler.OpCode.ShiftL,
                ByteCompiler.OpCode.ShiftR,
                ByteCompiler.OpCode.ShiftUR -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val shiftCount = toUInt32(getValue(rRef)) and 0x1F
                    val lNum = toInt32(getValue(lRef))
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.ShiftL -> lNum shl shiftCount
                        ByteCompiler.OpCode.ShiftR -> lNum shr shiftCount
                        ByteCompiler.OpCode.ShiftUR -> lNum ushr shiftCount
                        else -> throw Exception()
                    }
                    mainStack.addFirst(NumberData(NumberData.NumberKind.Real, result).toStack())
                }
                ByteCompiler.OpCode.And,
                ByteCompiler.OpCode.Or,
                ByteCompiler.OpCode.Xor -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val rNum = toInt32(getValue(rRef))
                    val lNum = toInt32(getValue(lRef))
                    val result = when(operation.opCode) {
                        ByteCompiler.OpCode.And -> lNum and rNum
                        ByteCompiler.OpCode.Or -> lNum or rNum
                        ByteCompiler.OpCode.Xor -> lNum xor rNum
                        else -> throw Exception()
                    }
                    mainStack.addFirst(NumberData(NumberData.NumberKind.Real, result).toStack())
                }
                ByteCompiler.OpCode.LT -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractRelationalComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(
                        if(r is UndefinedData) BooleanStackData(false) else r.toStack()
                    )
                }
                ByteCompiler.OpCode.GT -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractRelationalComparison(
                        getValue(rRef), getValue(lRef)
                    )
                    mainStack.addFirst(
                        if(r is UndefinedData) BooleanStackData(false) else r.toStack()
                    )
                }
                ByteCompiler.OpCode.LTE -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractRelationalComparison(
                        getValue(rRef), getValue(lRef)
                    )
                    mainStack.addFirst(
                        if(r is UndefinedData || (r is BooleanData && r.value)) BooleanStackData(false)
                        else BooleanStackData(true)
                    )
                }
                ByteCompiler.OpCode.GTE -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractRelationalComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(
                        if(r is UndefinedData || (r is BooleanData && r.value)) BooleanStackData(false)
                        else BooleanStackData(true)
                    )
                }
                ByteCompiler.OpCode.InstanceOf -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    throw NotImplementedError()
                }
                ByteCompiler.OpCode.In -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    throw NotImplementedError()
                }
                ByteCompiler.OpCode.Eq -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractEqualityComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(r.toStack())
                }
                ByteCompiler.OpCode.Neq -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = abstractEqualityComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(BooleanStackData(!r.value))
                }
                ByteCompiler.OpCode.EqS -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = strictEqualityComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(r.toStack())
                }
                ByteCompiler.OpCode.NeqS -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val r = strictEqualityComparison(
                        getValue(lRef), getValue(rRef)
                    )
                    mainStack.addFirst(BooleanStackData(!r.value))
                }
                ByteCompiler.OpCode.IfTrue -> {
                    val ref = mainStack.first()
                    val bin = toBoolean(getValue(ref))
                    if(bin) {
                        counter = operation.operand?.toInt()!!
                        continue
                    }
                }
                ByteCompiler.OpCode.IfFalse -> {
                    val ref = mainStack.first()
                    val bin = toBoolean(getValue(ref))
                    if(!bin) {
                        counter = operation.operand?.toInt()!!
                        continue
                    }
                }
                ByteCompiler.OpCode.IfEmpty -> {
                    val ref = mainStack.first()
                    if(ref is EmptyStackData) {
                        counter = operation.operand?.toInt()!!
                        continue
                    }
                }
                ByteCompiler.OpCode.Goto -> {
                    counter = operation.operand?.toInt()!!
                    continue
                }
                ByteCompiler.OpCode.Delete -> {
                    val ref = mainStack.removeFirst()
                    if(ref !is ReferenceStackData) {
                        mainStack.addFirst(BooleanStackData(true))
                    }
                    else {
                        val data = referencePool[ref.address]
                        if(data.isUnresolvableReference()) {
                            if(data.strict) throw Exception("SyntaxError")
                            else mainStack.addFirst(BooleanStackData(true))
                        }
                        else if(data.isPropertyReference()) {
                            val result = toObject(data.base as EcmaData).delete(data.referencedName, data.strict)
                            mainStack.addFirst(BooleanStackData(result))
                        }
                        else {
                            if(data.strict) throw Exception("SyntaxError")
                            val bindings = data.base as EnvironmentRecords
                            val result = bindings.deleteBinding(data.referencedName)
                            mainStack.addFirst(BooleanStackData(result))
                        }
                    }
                }
                ByteCompiler.OpCode.GetValue -> {
                    val expr = mainStack.removeFirst()
                    val value = getValue(expr)
                    mainStack.addFirst(value.toStack())
                }
                ByteCompiler.OpCode.TypeOf -> {
                    val ref = mainStack.removeFirst()
                    val value = if(ref is ReferenceStackData) {
                        val data = referencePool[ref.address]
                        if(data.isUnresolvableReference()) UndefinedData()
                        else data.base as EcmaData
                    } else {
                        getValue(ref)
                    }
                    val result = when(value) {
                        is UndefinedData -> "undefined"
                        is NullData -> "object"
                        is BooleanData -> "boolean"
                        is NumberData -> "number"
                        is StringData -> "string"
                        is ObjectData -> {
                            if(value.call != null) "function"
                            else if(value.isNative) "object"
                            else throw NotImplementedError()
                        }
                        else -> throw Exception()
                    }
                    mainStack.addFirst(StringStackData(result))
                }
                ByteCompiler.OpCode.ToNum -> {
                    val expr = mainStack.removeFirst()
                    mainStack.addFirst(
                        toNumber(getValue(expr)).toStack()
                    )
                }
                ByteCompiler.OpCode.Neg -> {
                    val expr = mainStack.removeFirst()
                    val oldValue = toNumber(getValue(expr))
                    mainStack.addFirst((-oldValue).toStack())
                }
                ByteCompiler.OpCode.Not -> {
                    val expr = mainStack.removeFirst()
                    val oldValue = toInt32(getValue(expr))
                    mainStack.addFirst(
                        NumberData.real(oldValue.inv()).toStack()
                    )
                }
                ByteCompiler.OpCode.LogicalNot -> {
                    val expr = mainStack.removeFirst()
                    val oldValue = toBoolean(getValue(expr))
                    mainStack.addFirst(
                        BooleanStackData(!oldValue)
                    )
                }
                ByteCompiler.OpCode.Swap -> {
                    val first = mainStack.removeFirst()
                    val second = mainStack.removeFirst()
                    mainStack.addFirst(first)
                    mainStack.addFirst(second)
                }
                ByteCompiler.OpCode.Assign -> {
                    val rRef = mainStack.removeFirst()
                    val lRef = mainStack.removeFirst()
                    val rVal = getValue(rRef)
                    if(lRef is ReferenceStackData) {
                        val lData = referencePool?.get(lRef.address)
                        if(lData?.strict == true) {
                            if((lData.base is EnvironmentRecords)
                                && (lData.referencedName == "eval" || lData.referencedName == "arguments")) {
                                throw Exception("SyntaxError")
                            }
                        }
                    }
                    putValue(lRef, rVal)
                    mainStack.addFirst(rVal.toStack())
                }
                ByteCompiler.OpCode.Copy -> {
                    val first = mainStack.first()
                    mainStack.addFirst(first)
                }
                ByteCompiler.OpCode.Call -> {
                    val argSize = (operandToData(operation.operand) as NumberStackData).value
                    val arguments = mutableListOf<EcmaData>()
                    for(i in 0 until argSize) {
                        val argRef = mainStack.removeFirst()
                        val value = getValue(argRef)
                        arguments.add(0, value)
                    }
                    val ref = mainStack.removeFirst()
                    val func = getValue(ref)
                    if(func !is ObjectData) throw Exception("TypeError")
                    if(!isCallable(func)) throw Exception("TypeError")
                    var thisValue: EcmaData
                    if(ref is ReferenceStackData) {
                        val referenceData = refPool[ref.address]!!
                        if(referenceData.isPropertyReference()) {
                            thisValue = referenceData.base as EcmaData
                        }
                        else {
                            thisValue = (referenceData.base as EnvironmentRecords).implicitThisValue()
                        }
                    }
                    else {
                        thisValue = UndefinedData()
                    }
                    mainStack.addFirst(func.call!!.invoke(thisValue, arguments))
                }
                ByteCompiler.OpCode.Return -> {
                    return mainStack.removeFirst()
                }
                ByteCompiler.OpCode.ResolveMember -> {
                    val propertyNameReference = mainStack.removeFirst()
                    val baseReference = mainStack.removeFirst()
                    val baseValue = getValue(baseReference)
                    val propertyNameValue = getValue(propertyNameReference)
                    checkObjectCoercible(baseValue)
                    val propertyNameString = toString(propertyNameValue)
                    val strict = false //TODO: strict
                    val index = referencePool.size
                    referencePool.add(ReferenceData(
                        base = baseValue,
                        referencedName = propertyNameString,
                        strict
                    ))
                    mainStack.addFirst(ReferenceStackData(index))
                }
                ByteCompiler.OpCode.Define -> {
                    val desc: ObjectData.PropertyDescriptor
                    val name: String
                    when(operation.operand) {
                        "init" -> {
                            val exprValue = mainStack.removeFirst()
                            val propName = mainStack.removeFirst()
                            name = toString(getValue(propName))
                            val propValue = getValue(exprValue)
                            desc = ObjectData.PropertyDescriptor.data(
                                propValue, writable = true,
                                enumerable = true, configurable = true
                            )
                        }
                        "get" -> {
                            val closure = mainStack.removeFirst()
                            val closureObj = getValue(closure) as ObjectData
                            val propName = mainStack.removeFirst()
                            name = toString(getValue(propName))
                            desc = ObjectData.PropertyDescriptor.accessor(
                                get = closureObj, enumerable = true, configurable = true
                            )
                        }
                        "set" -> {
                            val closure = mainStack.removeFirst()
                            val closureObj = getValue(closure) as ObjectData
                            val propName = mainStack.removeFirst()
                            name = toString(getValue(propName))
                            desc = ObjectData.PropertyDescriptor.accessor(
                                set = closureObj, enumerable = true, configurable = true
                            )
                        }
                        else -> throw Exception()
                    }
                    val obj = getValue(mainStack.first())
                    val previous = (obj as ObjectData).getOwnProperty(name)
                    if(previous != null) {
                        val strict = false
                        if(
                            (strict && desc.type == ObjectData.PropertyDescriptor.DescriptorType.Data)
                            || (previous.type == ObjectData.PropertyDescriptor.DescriptorType.Data
                                && desc.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor)
                            || (previous.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor
                                && desc.type == ObjectData.PropertyDescriptor.DescriptorType.Data)
                            || (previous.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor
                                && desc.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor
                                && ((previous.get != null && desc.get != null)
                                || (previous.set != null && desc.set != null))
                                )
                        ) {
                            throw Exception("SyntaxError")
                        }
                    }
                    obj.defineOwnProperty(name, desc, false)
                }
                ByteCompiler.OpCode.New -> {
                    val argSize = (operandToData(operation.operand) as NumberStackData).value
                    val arguments = mutableListOf<EcmaData>()
                    for(i in 0 until argSize) {
                        val argRef = mainStack.removeFirst()
                        val value = getValue(argRef)
                        arguments.add(0, value)
                    }
                    val ref = mainStack.removeFirst()
                    val constructor = getValue(ref)
                    if(constructor !is ObjectData) throw Exception("TypeError")
                    if(constructor.construct == null) throw Exception("TypeError")
                    return constructor.construct!!.invoke(arguments).toStack()
                }
                ByteCompiler.OpCode.With -> {
                    val codeLength = operation.operand?.toIntOrNull() ?: throw Exception()
                    val thisValue = getValue(mainStack.removeFirst())
                    val withObj = getValue(mainStack.removeFirst()) as WithObject
                    val result = withObj.resolve(
                        toObject(thisValue),
                        referencePool, constantPool, objectPool
                    )
                    println("refs=${result.refPool}")
                    println("consts=${result.constantPool}")
                    println("objs=${result.objectPool}")
                    referencePool.clear()
                    referencePool.addAll(result.refPool)
                    constantPool.clear()
                    constantPool.addAll(result.constantPool)
                    objectPool.clear()
                    objectPool.addAll(result.objectPool)
                    if(result.byteLines.size != codeLength) throw Exception()
                    for(i in 0 until codeLength) {
                        mutCode[counter + 1 + i] = result.byteLines[i]
                    }
                }
            }
            counter++
        }
        if(mainStack.size != 1) {
            println("stack finished unexpected size: ${mainStack.size}")
            return null
        }
        return mainStack.first()
    }

    private fun operandToData(operand: String?): StackData {
        if(operand == null) throw Exception()
        if(operand == "null") {
            return NullStackData()
        }
        if(operand == "undefined") {
            return UndefinedStackData()
        }
        if(operand == "true") {
            return BooleanStackData(true)
        }
        if(operand == "false") {
            return BooleanStackData(false)
        }
        if(operand == "empty") {
            return EmptyStackData()
        }
        if(operand.startsWith("#")) {
            operand.substring(1).toIntOrNull()?.let {
                return ReferenceStackData(it)
            }
        }
        if(operand.startsWith("@")) {
            operand.substring(1).toIntOrNull()?.let {
                return constantPool[it].toData().toStack()
            }
        }
        if(operand.startsWith("&")) {
            operand.substring(1).toIntOrNull()?.let {
                return objectPool[it].toStack()
            }
        }
        operand.toIntOrNull()?.let {
            return NumberStackData(NumberData.NumberKind.Real, it)
        }
        throw NotImplementedError()
    }

    private fun getValue(value: StackData): EcmaData {
        when(value) {
            is ReferenceStackData -> {
                //println("getValue(ref=$value)")
                val data = referencePool[value.address] ?: throw Exception()
                //println("data=$data")
                val base = data.base
                if(data.isUnresolvableReference()) throw Exception("ReferenceError")
                if(data.isPropertyReference()) {
                    if(!data.hasPrimitiveBase()) {
                        return (base as ObjectData).get(data.referencedName) ?: UndefinedData()
                    }
                    else {
                        val o = toObject(base as EcmaData)
                        val desc = o.getProperty(data.referencedName)
                        if(desc == null) return UndefinedData()
                        else if(desc.type == ObjectData.PropertyDescriptor.DescriptorType.Data) {
                            return desc.value!!
                        }
                        else {
                            val getter = desc.get ?: return UndefinedData()
                            //TODO: getter.[[Call]](this=base)
                            throw NotImplementedError()
                        }
                    }
                }
                else {
                    base as EnvironmentRecords
                    return base.getBindingValue(data.referencedName, data.strict)
                }
            }
            else -> return value.toEcmaData()
        }
    }

    //TODO: ???????????????????
    fun putValue(left: StackData, right: EcmaData) {
        if(left !is ReferenceStackData) throw Exception("ReferenceError")
        val value = referencePool[left.address] ?: throw Exception()
        val base = value.base
        if(value.isUnresolvableReference()) {
            if(value.strict) throw Exception("ReferenceError")
            globalObjectData!!.put(
                value.referencedName, right, false
            )
        }
        else if(value.isPropertyReference()) {
            if(!value.hasPrimitiveBase()) {
                (base as ObjectData).put(
                    value.referencedName, right, value.strict
                )
            }
            else {
                //special put method
                val o = toObject(base as EcmaData)
                if(!o.canPut(value.referencedName)) {
                    if(value.strict) throw Exception("TypeError")
                    return
                }
                val ownDesc = o.getOwnProperty(value.referencedName)
                if(ownDesc?.type == ObjectData.PropertyDescriptor.DescriptorType.Data) {
                    //Primitive型のプロパティを変更するのはあれだから？
                    if(value.strict) throw Exception("TypeError")
                    return
                }
                val desc = o.getProperty(value.referencedName)
                if(desc?.type == ObjectData.PropertyDescriptor.DescriptorType.Accessor) {
                    val setter = desc.set!!
                    //TODO: setter.[[Call]](this=base, right)
                    throw NotImplementedError()
                }
                else {
                    if(value.strict) throw Exception("TypeError")
                    return
                }
            }
        }
        else {
            (base as EnvironmentRecords).setMutableBinding(
                value.referencedName, right, value.strict
            )
        }
        return
    }

    //smallExpected < bigExpectedを評価する
    //BooleanData or UndefinedDataのみを返す
    private fun abstractRelationalComparison(
        smallExpected: EcmaData, bigExpected: EcmaData): EcmaData {
        val px = toPrimitive(smallExpected) //TODO: hint Number
        val py = toPrimitive(bigExpected) //TODO: hint Number
        //TODO: toPrimitiveに副作用がある場合は評価の順番を考える必要があるがどうか
        if(px is StringData && py is StringData) {
            throw NotImplementedError()
            //11.8.5-4
        }
        else {
            val nx = toNumber(px)
            val ny = toNumber(py)
            if(nx.kind == NumberData.NumberKind.NaN || ny.kind == NumberData.NumberKind.NaN) {
                return UndefinedData()
            }
            if(nx == ny) { //TODO: 等号の確認
                return BooleanData(false)
            }
            if(nx.kind == NumberData.NumberKind.ZeroP && ny.kind == NumberData.NumberKind.ZeroN) {
                return BooleanData(false)
            }
            if(nx.kind == NumberData.NumberKind.ZeroN && ny.kind == NumberData.NumberKind.ZeroP) {
                return BooleanData(false)
            }
            if(nx.kind == NumberData.NumberKind.InfinityP) return BooleanData(false)
            if(ny.kind == NumberData.NumberKind.InfinityP) return BooleanData(true)
            if(nx.kind == NumberData.NumberKind.InfinityN) return BooleanData(false)
            if(ny.kind == NumberData.NumberKind.InfinityN) return BooleanData(true)
            return BooleanData(nx.value < ny.value)
        }
    }

    private fun abstractEqualityComparison(x: EcmaData, y: EcmaData): BooleanData {
        if(x::class == y::class) { //同じTypeのとき
            if(x is UndefinedData) return BooleanData(true)
            if(x is NullData) return BooleanData(true)
            if(x is NumberData) {
                y as NumberData
                if(x.kind == NumberData.NumberKind.NaN
                    || y.kind == NumberData.NumberKind.NaN) return BooleanData(false)
                if(x == y) return BooleanData(true)
                if(x.kind == NumberData.NumberKind.ZeroP
                    && y.kind == NumberData.NumberKind.ZeroN) return BooleanData(true)
                if(x.kind == NumberData.NumberKind.ZeroN
                    && y.kind == NumberData.NumberKind.ZeroP) return BooleanData(true)
                return BooleanData(false)
            }
            if(x is StringData) {
                throw NotImplementedError()
            }
            if(x is ObjectData) {
                throw NotImplementedError()
            }
            if(x is BooleanData) {
                y as BooleanData
                return BooleanData(x.value == y.value)
            }
        }
        if(x is NullData && y is UndefinedData) return BooleanData(true)
        if(x is UndefinedData && y is NullData) return BooleanData(true)
        if(x is NumberData && y is StringData) {
            return abstractEqualityComparison(x,toNumber(y))
        }
        if(x is StringData && y is NumberData) {
            return abstractEqualityComparison(toNumber(x),y)
        }
        if(x is BooleanData) {
            return abstractEqualityComparison(toNumber(x),y)
        }
        if(y is BooleanData) {
            return abstractEqualityComparison(x,toNumber(y))
        }
        if((x is StringData || x is NumberData) && y is ObjectData) {
            return abstractEqualityComparison(x, toPrimitive(y))
        }
        if(x is ObjectData && (y is StringData || y is NumberData)) {
            return abstractEqualityComparison(toPrimitive(x), y)
        }
        return BooleanData(false)
    }

    private fun strictEqualityComparison(x: EcmaData, y: EcmaData): BooleanData {
        if(x::class != y::class) return BooleanData(false)
        if(x is UndefinedData) return BooleanData(true)
        if(x is NullData) return BooleanData(true)
        if(x is NumberData) {
            y as NumberData
            if(x.kind == NumberData.NumberKind.NaN
                || y.kind == NumberData.NumberKind.NaN) return BooleanData(false)
            if(x == y) return BooleanData(true)
            if(x.isZero() && y.isZero()) return BooleanData(true)
            return BooleanData(false)
        }
        if(x is BooleanData) return BooleanData(x.value == (y as BooleanData).value)
        if(x is StringData) throw NotImplementedError()
        if(x is ObjectData) throw NotImplementedError()
        return BooleanData(false) //TODO: String, Objectの比較
    }
}
