package esTree

import java.io.BufferedReader
import java.lang.Exception

class CharReader(
    private val reader: BufferedReader?,
    inputStr: String? = null
) {
    companion object {
        private const val EOFInt = 26
        val EOF = EOFInt.toChar()
    }

    private val inputStrs = inputStr?.split("\n") ?: listOf()
    private var inputIndex = 0

    private var line = ""
    private var lineLength = 0
    var index = 0
    var lineNumber = -1

    fun getNextChar(): Char {
        if(index + 1 < lineLength) {
            return line[++index]
        }
        if(++index == lineLength - 1) {
            return '\n'
        }

        try {
            if(reader == null && inputStrs.size == inputIndex) return EOF
            line = reader?.readLine() ?: inputStrs[inputIndex++]
            lineNumber++
            lineLength = line.length
            index = -1
            println(line)
            return getNextChar()
        } catch(e: Exception) {
            println(e.message)
        }
        return EOF
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
}
