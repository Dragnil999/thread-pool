fun findAllEvenNumbers(n: Int): List<Int> {
    val evenNumbers = mutableListOf<Int>()
    for (i in 0..<n) {
        if (i % 2 == 0) {
            evenNumbers += i
        }
    }
    return evenNumbers
}