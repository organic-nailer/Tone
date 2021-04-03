import kotlin.test.Test

class ByteCompilerTest {
    @Test
    fun compileAddMul() {
        val parser= Parser()
        val tokenizer = Tokenizer("1+2*3")
        parser.parse(tokenizer.tokenized)
        val compiler = ByteCompiler()
        parser.parsedNode?.let {
            compiler.runGlobal(it, GlobalObject())
            println("compiled:")
            compiler.nonByteLines.forEach { op ->
                if(op == null) println("[empty]")
                else println("${op.opCode} ${op.data ?: ""}")
            }
            assert(true)
        } ?: kotlin.run { assert(false) }
    }
}
