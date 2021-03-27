import TypeConverter.toBoolean
import TypeConverter.toInt32
import TypeConverter.toNumber
import TypeConverter.toPrimitive
import TypeConverter.toUInt32
import sun.reflect.generics.reflectiveObjects.NotImplementedException

class ToneVirtualMachine {
    var referencePool: List<ReferenceData?>? = null
    fun run(
        code: List<ByteCompiler.ByteOperation>,
        refPool: List<ReferenceData?>
    ): StackData? {
        referencePool = refPool
        val mainStack = ArrayDeque<StackData>()
        var counter = 0
        while(counter < code.size) {
            val operation = code[counter]
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
                        throw NotImplementedError()
                    }
                }
                ByteCompiler.OpCode.GetValue -> {
                    val expr = mainStack.first()
                    getValue(expr)
                }
                ByteCompiler.OpCode.TypeOf -> {
                    throw NotImplementedError()
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
        if(operand?.startsWith("#") == true) {
            operand.substring(1).toIntOrNull()?.let {
                return ReferenceStackData(it)
            }
        }
        operand?.toIntOrNull()?.let {
            return NumberStackData(NumberData.NumberKind.Real, it)
        }
        throw NotImplementedError()
    }

    private fun getValue(value: StackData): EcmaData {
        return when(value) {
            is NumberStackData -> NumberData(value.kind,value.value)
            is NullStackData -> NullData()
            is UndefinedStackData -> UndefinedData()
            is BooleanStackData -> BooleanData(value.value)
            is StringStackData -> throw NotImplementedError()
            is ObjectStackData -> throw NotImplementedError()
            is ReferenceStackData -> {
                val data = referencePool?.get(value.address) ?: throw Exception()
                if(data.isUnresolvableReference()) throw Exception("ReferenceError")
                if(data.isPropertyReference()) {
                    //TODO: 読んでもよくわからんかった
                    data.base as EcmaData
                }
                else {
                    data.base as EcmaData
                }
            }
            else -> throw Exception()
        }
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
