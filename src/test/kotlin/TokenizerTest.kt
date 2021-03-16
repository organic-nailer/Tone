import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun charReaderTest() {
        val reader = CharReader(
            null,
            "1234==\n" +
                "hello_2\n" +
                "x=\"wor\\\n" +
                "ld\"\n" +
                "//google\n" +
                "a/*hello\n" +
                "ok,*/"
        )
        assertEquals('1',reader.getNextChar())
        assertEquals(0, reader.index)
        assertEquals("1234",reader.readNumber())
        assertEquals(3,reader.index)
        assertEquals('=',reader.getNextChar())
        assertEquals(true,reader.prefixMatch("=="))
        reader.index += 1
        assertEquals('\n',reader.getNextChar())
        assertEquals('h',reader.getNextChar())
        assertEquals("hello_2",reader.readIdentifier())
        assertEquals(6,reader.index)
        assertEquals(2,reader.lineNumber)
        assertEquals('\n',reader.getNextChar())
        assertEquals('x',reader.getNextChar())
        assertEquals('=',reader.getNextChar())
        assertEquals('"',reader.getNextChar())
        assertEquals(listOf("wor","ld"),reader.readStringLiteral())
        assertEquals('\n',reader.getNextChar())
        assertEquals('/',reader.getNextChar())
        reader.readSingleComment()
        assertEquals('\n',reader.getNextChar())
        assertEquals('a',reader.getNextChar())
        assertEquals('/',reader.getNextChar())
        assertEquals(true,reader.readMultiLineComment())
        assertEquals(CharReader.EOF,reader.getNextChar())
    }

    @Test
    fun tokenizeTest() {
        var tokenizer = Tokenizer("3+34")
        assertEquals(listOf("3","+","34","$"), tokenizer.tokenized.map { it.raw })
        tokenizer = Tokenizer("this=23+4==1")
        assertEquals(
            listOf("this","=","23","+","4","==","1","$"),
            tokenizer.tokenized.map { it.raw })
        tokenizer = Tokenizer("hello.world=334")
        assertEquals(
            listOf("hello",".","world","=","334","$"),
            tokenizer.tokenized.map { it.raw })
//        assertEquals(
//            Tokenizer.TokenData("3", Tokenizer.TokenKind.NumberLiteral),
//            tokenizer.getNextToken())
//        assertEquals(
//            Tokenizer.TokenData("+", Tokenizer.TokenKind.Plus),
//            tokenizer.getNextToken())
//        assertEquals(
//            Tokenizer.TokenData("34", Tokenizer.TokenKind.NumberLiteral),
//            tokenizer.getNextToken())
//        assertEquals(
//            Tokenizer.TokenData("", Tokenizer.TokenKind.EOF),
//            tokenizer.getNextToken())
    }
}