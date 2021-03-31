import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flipkart.zjsonpatch.JsonDiff
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.Test

class ParserTest {

    @Test
    fun parseNumber() {
        val parser = Parser()
        val tokenizer = Tokenizer("35")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"expression\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":35,\"raw\":\"35\"}}]}",
            parsed
        )
    }

    @Test
    fun parseAddition() {
        val parser = Parser()
        val tokenizer = Tokenizer("33 + 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":6}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":33,\"raw\":\"33\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":5},\"end\":{\"line\":0,\"column\":6}},\"value\":4,\"raw\":\"4\"},\"operator\":\"+\"}}]}",
            parsed
        )
    }

    @Test
    fun parseMultiAddition() {
        val parser = Parser()
        val tokenizer = Tokenizer("3 + 3 + 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":9}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":9}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":9}},\"left\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":-1,\"column\":-1},\"end\":{\"line\":0,\"column\":5}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":1}},\"value\":3,\"raw\":\"3\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":4},\"end\":{\"line\":0,\"column\":5}},\"value\":3,\"raw\":\"3\"},\"operator\":\"+\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":8},\"end\":{\"line\":0,\"column\":9}},\"value\":4,\"raw\":\"4\"},\"operator\":\"+\"}}]}",
            parsed
        )
    }

    @Test
    fun parseSub() {
        val parser = Parser()
        val tokenizer = Tokenizer("33 - 4")
        val parsed = parser.parse(tokenizer.tokenized)
        assertEquals(
            "{\"type\":\"Program\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"body\":[{\"type\":\"ExpressionStatement\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"expression\":{\"type\":\"BinaryExpression\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":6}},\"left\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":0},\"end\":{\"line\":0,\"column\":2}},\"value\":33,\"raw\":\"33\"},\"right\":{\"type\":\"Literal\",\"loc\":{\"start\":{\"line\":0,\"column\":5},\"end\":{\"line\":0,\"column\":6}},\"value\":4,\"raw\":\"4\"},\"operator\":\"-\"}}]}",
            parsed
        )
    }

    @Test
    fun parseBinaryExpr() {
        val parser = Parser()
        val tokenizer = Tokenizer("1 + 2 * 3 >> 4 & 5 != 6 > 7 instanceof 8")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseLogicExpr() {
        val parser = Parser()
        val tokenizer = Tokenizer("1 + 2 && 3")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseUnary() {
        val parser = Parser()
        val tokenizer = Tokenizer("+ 1 + 2 ++")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseNull() {
        val parser = Parser()
        val tokenizer = Tokenizer("1 = null")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseBool() {
        val parser = Parser()
        val tokenizer = Tokenizer("1 = new true")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseCall() {
        val parser = Parser()
        val tokenizer = Tokenizer("1 ( 2 ) [ 3 ]")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseIdentifier() {
        val parser = Parser()
        val tokenizer = Tokenizer("hello.world=334")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseArray() {
        val parser = Parser()
        parser.parse(Tokenizer("[]").tokenized)
        parser.parse(Tokenizer("[1]").tokenized)
        parser.parse(Tokenizer("[1,]").tokenized)
        parser.parse(Tokenizer("[,,1,]").tokenized)
        parser.parse(Tokenizer("[,1,,2]").tokenized)
        assert(true)
    }

    @Test
    fun parseProgram() {
        val parser = Parser()
        val tokenizer = Tokenizer("x=2;y=3;")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseBlock() {
        val parser = Parser()
        val tokenizer = Tokenizer("{x=2;}")
        parser.parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun importFileTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("Hoge.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        assertEquals("hogehoge = 2;",text)

        val jsonFile = (ParserTest::class.java).classLoader.getResource("HogeExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        val expected = File(jsonFile.toURI()).readText()
        val tokenizer = Tokenizer("hogehoge = 2;")
        val parsed = parser.parse(tokenizer.tokenized) ?: ""
        val patch = JsonDiff.asJson(
            jacksonObjectMapper().readTree(expected),
            jacksonObjectMapper().readTree(parsed)
        )
        assertEquals("[]",patch.toString())
    }

    @Test
    fun expressionTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("ExpressionTest.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        val tokenizer = Tokenizer(text)
        val parsed = parser.parse(tokenizer.tokenized) ?: ""

        val jsonFile = (ParserTest::class.java).classLoader.getResource("ExpressionTestExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        Json.parseToJsonElement(File(jsonFile.toURI()).readText())
        assertJson(
            File(jsonFile.toURI()).readText(),
            parsed
        )
    }

    private fun assertJson(expected: String, actual: String) {
        val patch = JsonDiff.asJson(
            jacksonObjectMapper().readTree(expected),
            jacksonObjectMapper().readTree(actual)
        )
        assertEquals("[]",patch.toString())
    }

    @Test
    fun statementTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("StatementTest.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        val tokenizer = Tokenizer(text)
        val parsed = parser.parse(tokenizer.tokenized) ?: ""

        val jsonFile = (ParserTest::class.java).classLoader.getResource("StatementTestExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        assertJson(
            File(jsonFile.toURI()).readText(),
            parsed
        )
    }

    @Test
    fun functionTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("FunctionTest.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        val tokenizer = Tokenizer(text)
        val parsed = parser.parse(tokenizer.tokenized) ?: ""

        val jsonFile = (ParserTest::class.java).classLoader.getResource("FunctionTestExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        assertJson(
            File(jsonFile.toURI()).readText(),
            parsed
        )
    }

    @Test
    fun literalTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("LiteralTest.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        val tokenizer = Tokenizer(text)
        val parsed = parser.parse(tokenizer.tokenized) ?: ""

        val jsonFile = (ParserTest::class.java).classLoader.getResource("LiteralTestExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        assertJson(
            File(jsonFile.toURI()).readText(),
            parsed
        )
    }

    @Test
    fun asiTest() {
        val parser = Parser()
        val file = (ParserTest::class.java).classLoader.getResource("ASITest.js") ?: kotlin.run {
            throw Exception("File not found")
        }
        val text = File(file.toURI()).readText()
        val tokenizer = Tokenizer(text)
        val parsed = parser.parse(tokenizer.tokenized) ?: ""

        val jsonFile = (ParserTest::class.java).classLoader.getResource("ASITestExpected.json") ?: kotlin.run {
            throw Exception("File not found")
        }
        assertJson(
            File(jsonFile.toURI()).readText(),
            parsed
        )
    }

    @Test
    fun lr0Test() {
        LR0ParserGenerator(
            EcmaGrammar.grammarParserForLR0(EcmaGrammar.es5Grammar),
            EcmaGrammar.es5StartSymbol.ordinal
        )
    }

    @Test
    fun generatorTest() {
        DragonParserGenerator(
            EcmaGrammar.grammarParserForLR0(EcmaGrammar.es5Grammar),
            EcmaGrammar.es5StartSymbol.ordinal
        )
    }
}
