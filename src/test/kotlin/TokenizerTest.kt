import esTree.Tokenizer
import org.junit.Test
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun tokenizeTest() {
        val tokenizer = Tokenizer("3 + 34")
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