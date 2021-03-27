import kotlin.test.Test

class ByteCompilerTest {
    @Test
    fun compileAddMul() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("1+2*3")
        val parsed = parser.parse(tokenizer.tokenized)
        val compiler = ByteCompiler()
        parser.parsedNode?.let {
            compiler.run(it)
            println("compiled:")
            compiler.byteLines.forEach { op ->
                println("${op.opCode} ${op.operand ?: ""}")
            }
            assert(true)
        } ?: kotlin.run { assert(false) }
    }
}
