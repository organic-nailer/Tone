class ToneVirtualMachine {
    fun run(code: List<ByteCompiler.ByteOperation>): Int? {
        val mainStack = ArrayDeque<Int>()
        for(operation in code) {
            when(operation.opCode) {
                ByteCompiler.OpCode.Push -> {
                    mainStack.addFirst(operation.operand!!)
                }
                ByteCompiler.OpCode.Mul -> {
                    val right = mainStack.removeFirst()
                    val left = mainStack.removeFirst()
                    mainStack.addFirst(left * right)
                }
                ByteCompiler.OpCode.Add -> {
                    val right = mainStack.removeFirst()
                    val left = mainStack.removeFirst()
                    mainStack.addFirst(left + right)
                }
            }
        }
        if(mainStack.size != 1) {
            println("stack finished unexpected size: ${mainStack.size}")
            return null
        }
        return mainStack.first()
    }
}
