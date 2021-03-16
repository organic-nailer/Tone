import kotlin.test.Test
import kotlin.test.assertEquals

class ToneTest {
    @Test
    fun addMulTest() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("1+2*3")
        val parsed = parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            assert(false)
            return
        }
        val compiler = ByteCompiler()
        compiler.compile(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assertEquals(7, result)
    }
}
