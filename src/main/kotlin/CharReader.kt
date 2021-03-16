import java.io.BufferedReader
import java.lang.Exception

class CharReader(
    private val reader: BufferedReader?,
    inputStr: String? = null
) {
    companion object {
        private const val EOFInt = 26
        const val EOF = EOFInt.toChar()
        const val LINE_TERMINATOR = '\n'
    }

    private val inputStrings = inputStr?.split("\n") ?: listOf()
    private var inputIndex = 0

    private var line = ""
    private var lineLength = 0
    var index = 0 //getNextCharで返ってきたまさにその文字を指している
    var lineNumber = 0

    fun getNextChar(): Char {
        if(index + 1 < lineLength) {
            return line[++index]
        }
        if(++index == lineLength && (reader != null || inputStrings.size != inputIndex)) {
            return LINE_TERMINATOR
        }

        return readNextLine() ?: EOF
    }

    private fun readNextLine(): Char? {
        try {
            if(reader == null && inputStrings.size == inputIndex) return EOF
            line = reader?.readLine() ?: inputStrings[inputIndex++]
            lineNumber++
            lineLength = line.length
            index = -1
            println(line)
            return getNextChar()
        } catch(e: Exception) {
            println(e.message)
        }
        return null
    }

    fun prefixMatch(value: String): Boolean {
        if(index >= lineLength) return false
        return line.startsWith(value, index, false)
    }

    fun readNumber(): String? {
        val currentIndex = index
        if(!line[index].isDigit()) return null
        while(index < lineLength && line[index].isDigit()) {
            index++
        }
        index--
        return line.substring(currentIndex..index)
    }

    fun readSingleComment() {
        index = lineLength - 1
    }

    fun readMultiLineComment(): Boolean { //改行の有る無し
        val firstMatch = line.indexOf("*/", index)
        if(firstMatch >= 0) {
            index = firstMatch + 1
            return false
        }
        while(true) {
            val next = readNextLine()
            if(next != null && next != EOF) {
                val match = line.indexOf("*/")
                if(match >= 0) {
                    index = match + 1
                    return true
                }
            }
            else {
                //終端まで何もなかった場合
                index = lineLength - 1
                return true
            }
        }
    }

    fun readStringLiteral(): List<String> {
        val currentIndex = index
        //println("line=$line,index=$index,c=${line[index]}")
        if(line[index] != '"' && line[index] != '\'') throw Exception("エラー")
        val quote = line[index]
        for(i in index + 1 until lineLength) {
            if(line[i] == quote && line[i-1] != '\\') {
                index = i
                return listOf(line.substring(currentIndex + 1 until i))
            }
        }
        if(line.last() == '\\') {
            val res = mutableListOf(
                line.substring(currentIndex + 1, line.length-1)
            )
            while(true) {
                val next = readNextLine()
                if(next != null && next != EOF) {
                    for(i in 0 until lineLength) {
                        if(line[i] == quote && line[i-1] != '\\') {
                            res.add(
                                line.substring(0 until i)
                            )
                            index = i
                            return res
                        }
                    }
                    if(line.last() == '\\') {
                        res.add(
                            line.dropLast(1)
                        )
                    }
                    else {
                        throw Exception("エラー")
                    }
                }
                else {
                    throw Exception("エラー")
                }
            }
        }
        else {
            throw Exception("エラー")
        }
    }

    private val identifierPattern = Regex("""^[A-Za-z\$\_][A-Za-z0-9\$\_]*""")
    fun readIdentifier(): String? {
        val match = identifierPattern.find(line.substring(index)) ?: return null
        index += match.value.length - 1
        return match.value
    }
}
