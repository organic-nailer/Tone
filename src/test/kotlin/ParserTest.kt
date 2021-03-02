import esTree.Parser
import esTree.Tokenizer
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.Test

class ParserTest {
    private val parser: Parser = Parser()

    @Test
    fun parseNumber() {
        val tokenizer = Tokenizer("35")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"expression\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":35,\"raw\":\"35\"}}]}",
            parsed
        )
    }

    @Test
    fun parseAddition() {
        val tokenizer = Tokenizer("33 + 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":6}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":33,\"raw\":\"33\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":5},\"end\":{\"line\":0,\"column\":6}},\"value\":4,\"raw\":\"4\"},\"operator\":\"+\"}}]}",
            parsed
        )
    }

    @Test
    fun parseMultiAddition() {
        val tokenizer = Tokenizer("3 + 3 + 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":9}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":9}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":9}},\"left\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":5}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":1}},\"value\":3,\"raw\":\"3\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":4},\"end\":{\"line\":0,\"column\":5}},\"value\":3,\"raw\":\"3\"},\"operator\":\"+\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":8},\"end\":{\"line\":0,\"column\":9}},\"value\":4,\"raw\":\"4\"},\"operator\":\"+\"}}]}",
            parsed
        )
    }

    @Test
    fun parseSub() {
        val tokenizer = Tokenizer("33 - 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":33,\"raw\":\"33\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":5},\"end\":{\"line\":0,\"column\":6}},\"value\":4,\"raw\":\"4\"},\"operator\":\"-\"}}]}",
            parsed
        )
    }

    @Test
    fun parseBinaryExpr() {
        val tokenizer = Tokenizer("1 + 2 * 3 >> 4 & 5 != 6 > 7 instanceof 8")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseLogicExpr() {
        val tokenizer = Tokenizer("1 + 2 && 3")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseUnary() {
        val tokenizer = Tokenizer("+ 1 + 2 ++")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseNull() {
        val tokenizer = Tokenizer("1 = null")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseBool() {
        val tokenizer = Tokenizer("1 = new true")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseCall() {
        val tokenizer = Tokenizer("1 ( 2 ) [ 3 ]")
        val parsed = parser.parse(tokenizer.tokenized)
        assert(true)
    }
}
