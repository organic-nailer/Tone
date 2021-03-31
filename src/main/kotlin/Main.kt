fun main(args: Array<String>) {
    if(args.size != 1) {
        println("引数のサイズが一致しません")
        return
    }
    val parser = Parser()
    val tokenizer = Tokenizer(args.first())
    parser.parse(tokenizer.tokenized)
    val node = parser.parsedNode ?: run {
        println("パースに失敗しました")
        return
    }
    val compiler = ByteCompiler()
    compiler.runGlobal(node, GlobalObject())
    val vm = ToneVirtualMachine()
    val result = vm.run(compiler.byteLines, compiler.refPool, compiler.globalObject!!)
    println("result:")
    println("${args.first()} => $result")
    return
}
