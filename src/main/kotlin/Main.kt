fun main(args: Array<String>) {
    if(args.size != 1) {
        println("引数のサイズが一致しません")
        return
    }
    val parser: Parser = Parser()
    val tokenizer = Tokenizer(args.first())
    val parsed = parser.parse(tokenizer.tokenized)
    val node = parser.parsedNode ?: kotlin.run {
        println("パースに失敗しました")
        return
    }
    val compiler = ByteCompiler()
    compiler.compile(node)
    val vm = ToneVirtualMachine()
    val result = vm.run(compiler.byteLines)
    println("result:")
    println("${args.first()} => $result")
    return
}
