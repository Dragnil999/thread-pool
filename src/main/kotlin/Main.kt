fun main(args: Array<String>) {
    test(size = 100, countOfThreads = 8)
    println()
    test(size = 1000, countOfThreads = 8)
    println()
    test(size = 10000, countOfThreads = 8)
}