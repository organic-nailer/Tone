import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ToneTest {

    lateinit var parser: Parser

    @Before
    fun setup() {
        parser = Parser()
    }

    private fun run(code: String): StackData? {
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
        compiler.refPool.forEachIndexed { index, referenceData ->
            println("$index: ${referenceData.referencedName}")
        }
        val vm = ToneVirtualMachine()
        return vm.run(compiler.byteLines,
            compiler.refPool,
            compiler.globalObject!!
        )
    }

    @Test
    fun addMulTest() {
        val result = run("1+2*3")
        Assert.assertEquals(7, (result as? NumberStackData)?.value)
    }

    @Test
    fun calc5Test() {
        val result = run("4/2-3%2*2+1")
        Assert.assertEquals(1, (result as? NumberStackData)?.value)
    }

    @Test
    fun calcBitTest() {
        val result = run("2&3^10>>1<<5+5>>>1")
        Assert.assertEquals(2562, (result as? NumberStackData)?.value)
    }

    @Test
    fun calcRelationTest() {
        val result = run("(1 > 2) <= (1 < 2)")
        Assert.assertEquals(true, (result as? BooleanStackData)?.value)
    }

    @Test
    fun calcLogicalTest() {
        val result = run("1==2||null&&1+2")
        assert(result is NullStackData)
    }

    @Test
    fun calcConditionalExprTest() {
        val result = run("2?!1:3")
        Assert.assertEquals(false, (result as? BooleanStackData)?.value)
    }

    @Test
    fun blockStmtTest() {
        val result = run("""
            {
                1+2;
                3-4;;
            }
        """.trimIndent())
        Assert.assertEquals(-1, (result as? NumberStackData)?.value)
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
        Assert.assertEquals(-1, (result as? NumberStackData)?.value)
    }

    @Test
    fun globalVariableTest() {
        val result = run("""
            var x = 2;
            x + 1;
        """.trimIndent())
        println(result)
        Assert.assertEquals(3, (result as? NumberStackData)?.value)
    }

    @Test
    fun assignmentTest() {
        val result = run("""
            var x;
            x = 2;
            x *= 2;
            x + 1
        """.trimIndent())
        println(result)
        Assert.assertEquals(5, (result as? NumberStackData)?.value)
    }

    @Test
    fun doWhileTest() {
        val result = run("""
            var i = 0;
            do {
                i += 1;
            } while(i < 5)
            i;
        """.trimIndent())
        println(result)
        Assert.assertEquals(5, (result as? NumberStackData)?.value)
    }

    @Test
    fun whileTest() {
        val result = run("""
            var i = 0;
            while(i < 5) {
                i += 1;
            }
            i;
        """.trimIndent())
        println(result)
        Assert.assertEquals(5, (result as? NumberStackData)?.value)
    }

    @Test
    fun forTest() {
        val result = run("""
            var i;
            var j = 0;
            for(i = 0; i < 5; i += 1) {
                j = i;
            }
            i + j;
        """.trimIndent())
        println(result)
        Assert.assertEquals(9, (result as? NumberStackData)?.value)
    }

    @Test
    fun forVarTest() {
        val result = run("""
            for(var i = 0; i < 5; i += 1) {
                i + 1;
            }
            i;
        """.trimIndent())
        println(result)
        Assert.assertEquals(5, (result as? NumberStackData)?.value)
    }
}
