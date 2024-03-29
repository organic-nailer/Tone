import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ToneTest {

    private lateinit var parser: Parser

    @Before
    fun setup() {
        parser = Parser()
    }

    private fun run(code: String): StackData? {
        val tokenizer = Tokenizer(code)
        println(tokenizer.tokenized)
        parser.parse(tokenizer.tokenized)
        val node = parser.parsedNode ?: kotlin.run {
            return null
        }
        val compiler = ByteCompiler()
        compiler.runGlobal(node, GlobalObject())
        compiler.nonByteLines.forEach { op ->
            if(op == null) println("[empty]")
            else println("${op.opCode} ${op.data ?: ""}")
        }
        println("----------")
        val data = compiler.resolveReference(compiler.nonByteLines, null, compiler.globalObject!!)
        println("resolved")
        data.byteLines.forEach { op ->
            if(op == null) println("[empty]")
            else println("${op.opCode} ${op.operand ?: ""}")
        }
        println("refs=${data.refPool}")
        println("consts=${data.constantPool}")
        println("objs=${data.objectPool}")
        val vm = ToneVirtualMachine()
        return vm.run(data.byteLines,
            data.refPool,
            data.constantPool,
            data.objectPool,
            data.global
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
    @Test
    fun breakTest() {
        val result = run("""
            var a = 0;
            while(a < 5) {
                a += 1;
                if(a == 3) {
                    break;
                }
            }
            var b = 0;
            foo: {
                if(b == 0) break foo;
                b += 1;
            }
            a + b;
        """.trimIndent())
        println(result)
        Assert.assertEquals(3, (result as? NumberStackData)?.value)
    }

    @Test
    fun continueTest() {
        val result = run("""
            var a = 0;
            for(var i = 0; i < 5; i += 1) {
                if(i >= 3) {
                    continue;
                }
                a += 1;
            }
            var b = 0;
            foo: for(var i = 0; i < 5; i += 1) {
                if(i >= 3) {
                    continue foo;
                }
                b += 1;
            }
            a + b;
        """.trimIndent())
        println(result)
        Assert.assertEquals(6, (result as? NumberStackData)?.value)
    }

    @Test
    fun switchTest() {
        val result = run("""
            var a = 0;
            switch(a) {
                case -1:
                    break;
                case 0:
                    a += 1;
                case 1:
                case 2:
                    a += 2;
                default:
                    a += 4;
                    break;
                case 3:
                    a += 8;
                    break;
                case 4:
                    break;
            }
            a;
        """.trimIndent())
        println(result)
        Assert.assertEquals(7, (result as? NumberStackData)?.value)
    }

    @Test
    fun globalFunctionTest() {
        val code = """
            function add(a,b) {
                return a + b;
            }
            add(1,2);
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(3, (result as? NumberStackData)?.value)
    }

    @Test
    fun objectLiteralTest() {
        val code = """
            var x = {
                p1: 1+2,
                'p2': 3,
                set p3(a) {
                    this.p1 = a;
                },
                get p3() {
                    return this.p2;
                }
            };
            var y = x.p1 + x.p2;
            x.p3 = 0;
            y + x.p1 + x.p3;
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(9, (result as? NumberStackData)?.value)
    }

    @Test
    fun arrayLiteralTest() {
        val code = """
            var x = [0,1,,2];
            x[1] = 3;
            x[0] + x[1] + x.length;
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(7, (result as? NumberStackData)?.value)
    }

    @Test
    fun withStmtTest() {
        val code = """
            var x = {
                p1: 1,
                p2: 2,
            };
            var p1 = 10;
            with(x) {
                p2 = p2 + p1;
            }
            x.p2;
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(3, (result as? NumberStackData)?.value)
    }

    @Test
    fun functionExpressionTest() {
        val code = """
            var x = function factorial(a) {
                if(a == 1) return 1;
                return factorial(a-1) * a;
            }
            x(5);
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(120, (result as? NumberStackData)?.value)
    }

    @Test
    fun incrementTest() {
        val code = """
            var x = 1;
            var y = x++ + ++x * 2;
            --y + x-- * 2;
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(12, (result as? NumberStackData)?.value)
    }

    @Test
    fun forInTest() {
        val code = """
var x = 0;
var y = {
    p1: 1, p2: 2, p3: 3,
};
for(i in y) {
    x += y[i];
}
var z = null;
for(i in z) {
    x++;
}
for(var a in y) {
    x += y[a];
}
x;
        """.trimIndent()
        val result = run(code)
        println("\ncode=")
        println(code)
        println("\nresult=$result")
        Assert.assertEquals(12, (result as? NumberStackData)?.value)
    }
}
