import esTree.Parser
import esTree.Tokenizer
import org.junit.Test

class ParserTest {
    @Test
    fun parseNumber() {
        val tokenizer = Tokenizer("35")
        Parser().parse(tokenizer.tokenized)
        assert(true)
    }

    @Test
    fun parseAddition() {
        val tokenizer = Tokenizer("33 + 4")
        Parser().parse(tokenizer.tokenized)
        assert(true)
    }
}

