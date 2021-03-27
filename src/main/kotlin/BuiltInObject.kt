class GlobalObject(refPoolManager: ByteCompiler.RefPoolManager): ObjectData(refPoolManager) {
    override val prototype: ObjectData? = null //実装依存
    override val className: String = "Global" //実装依存

    init {
        namedProperties["NaN"] = refPoolManager.setReference(PropertyDescriptor.data(
            NumberData.naN(), address = 0, writable = false,
            enumerable = false, configurable = false
        ), "NaN", false) //TODO: strict
        namedProperties["Infinity"] = refPoolManager.setReference(PropertyDescriptor.data(
            NumberData.infiniteP(), writable = false, address = 0,
            enumerable = false, configurable = false), "Infinity", false) //TODO: strict
        namedProperties["undefined"] = refPoolManager.setReference(PropertyDescriptor.data(
            UndefinedData(), writable = false, address = 0,
            enumerable = false, configurable = false), "undefined", false) //TODO: strict
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

class NormalObject(refPoolManager: ByteCompiler.RefPoolManager): ObjectData(refPoolManager) {
    override val prototype: ObjectData = PrototypeObject(refPoolManager)
}

class PrototypeObject(refPoolManager: ByteCompiler.RefPoolManager): ObjectData(refPoolManager) {
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

class FunctionObject(refPoolManager: ByteCompiler.RefPoolManager): ObjectData(refPoolManager) {
    override val prototype: ObjectData = FunctionPrototypeObject(refPoolManager)
}

class FunctionPrototypeObject(refPoolManager: ByteCompiler.RefPoolManager): ObjectData(refPoolManager) {
    override val prototype: ObjectData = PrototypeObject(refPoolManager)
    override val className: String = "Function"
    override val extensible: Boolean = true

    //length = 0
}
