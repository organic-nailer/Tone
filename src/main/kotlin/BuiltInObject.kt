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

class FunctionObject: ObjectData() {
    override val prototype: ObjectData = FunctionPrototypeObject()
}

class FunctionPrototypeObject: ObjectData() {
    override val prototype: ObjectData = PrototypeObject()
    override val className: String = "Function"
    override val extensible: Boolean = true

    //length = 0
}
