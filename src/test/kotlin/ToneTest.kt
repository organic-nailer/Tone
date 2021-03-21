import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ToneTest {

    lateinit var parser: Parser

    @Before
    fun setup() {
        parser = Parser()
    }

    private fun run(code: String): ToneVirtualMachine.StackData? {
        val tokenizer = Tokenizer(code)
        parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            return null
        }
        val compiler = ByteCompiler()
        compiler.run(node)
        compiler.byteLines.forEach { op ->
            println("${op.opCode} ${op.operand ?: ""}")
        }
        val vm = ToneVirtualMachine()
        return vm.run(compiler.byteLines)
    }

    @Test
    fun addMulTest() {
        val result = run("1+2*3")
        Assert.assertEquals(7, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calc5Test() {
        val result = run("4/2-3%2*2+1")
        Assert.assertEquals(1, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calcBitTest() {
        val result = run("2&3^10>>1<<5+5>>>1")
        Assert.assertEquals(2562, (result as? ToneVirtualMachine.NumberData)?.value)
    }

    @Test
    fun calcRelationTest() {
        val result = run("(1 > 2) <= (1 < 2)")
        Assert.assertEquals(true, (result as? ToneVirtualMachine.BooleanData)?.value)
    }

    @Test
    fun calcLogicalTest() {
        val result = run("1==2||null&&1+2")
        assert(result is ToneVirtualMachine.NullData)
    }

    @Test
    fun calcConditionalExprTest() {
        val result = run("2?!1:3")
        Assert.assertEquals(false, (result as? ToneVirtualMachine.BooleanData)?.value)
    }

    @Test
    fun blockStmtTest() {
        val result = run("""
            {
                1+2;
                3-4;;
            }
        """.trimIndent())
        Assert.assertEquals(-1, (result as? ToneVirtualMachine.NumberData)?.value)
    }
    @Test
    fun ifStmtTest() {
        val result = run("""
            if(1==2) {
                3;
            } else {
                -1;
            }
        """.trimIndent())
        Assert.assertEquals(-1, (result as? ToneVirtualMachine.NumberData)?.value)
    }
}
