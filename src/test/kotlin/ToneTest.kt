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
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assertEquals(7, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calc5Test() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("4/2-3%2*2+1")
        val parsed = parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            assert(false)
            return
        }
        val compiler = ByteCompiler()
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assertEquals(1, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calcBitTest() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("2&3^10>>1<<5+5>>>1")
        val parsed = parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            assert(false)
            return
        }
        val compiler = ByteCompiler()
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assertEquals(2562, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calcRelationTest() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("(1 > 2) <= (1 < 2)")
        val parsed = parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            assert(false)
            return
        }
        val compiler = ByteCompiler()
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assertEquals(true, (result as? ToneVirtualMachine.BooleanData)?.value)
    }

    @Test
    fun calcLogicalTest() {
        val parser: Parser = Parser()
        val tokenizer = Tokenizer("1==2||null&&1+2")
        val parsed = parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            assert(false)
            return
        }
        val compiler = ByteCompiler()
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        val result = vm.run(compiler.byteLines)
        assert(result is ToneVirtualMachine.NullData)
    }
}
