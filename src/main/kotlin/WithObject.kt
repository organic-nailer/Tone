class WithObject(
    val body: List<ByteCompiler.NonByteOperation?>,
    val currentContext: ExecutionContext,
    val globalObject: GlobalObject
): ObjectData() {
    fun resolve(
        thisObj: ObjectData,
        currentRef: List<ReferenceData>,
        currentConst: List<EcmaPrimitive>,
        currentObj: List<ObjectData>
    ): ByteCompiler.ByteData {
        val compiler = ByteCompiler()
        val newEnv = newObjectEnvironment(thisObj, currentContext.lexicalEnvironment, globalObject)
        val context = ExecutionContext(
            newEnv, currentContext.variableEnvironment, currentContext.thisBinding
        )
        return compiler.resolveReference(
            body, context, globalObject,
            currentRef, currentConst, currentObj
        )
//        compiler.runWith(code, thisObj, scopeList, currentContext, global)
//        return ToneVirtualMachine().run(
//            data.byteLines,
//            data.refPool,
//            data.constantPool,
//            data.objectPool,
//            data.global
//        )
    }
}
