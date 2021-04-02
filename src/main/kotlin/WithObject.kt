class WithObject(
    override val code: Parser.Node,
    private val scopeList: List<ByteCompiler.ScopeData>,
    private val currentContext: ExecutionContext,
    private val global: GlobalObject
): ObjectData() {
    fun run(thisObj: ObjectData): CompletionStackData {
        val compiler = ByteCompiler()
        compiler.runWith(code, thisObj, scopeList, currentContext, global)
        return ToneVirtualMachine().run(
            compiler.byteLines, compiler.refPool,
            compiler.constantPool, compiler.objectPool, global
        ) as CompletionStackData
    }
}