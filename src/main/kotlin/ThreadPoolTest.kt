import threadpool.PoolManager
import threadpool.PoolRequest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun test(size: Int, countOfThreads: Int) {
    val count = AtomicInteger(0)
    val numbers = List(size) { Random.nextInt(1000, 5000) }

    val requests = mutableListOf<PoolRequest>()

    for (i in 0..<size) {
        requests.add(
            object : PoolRequest() {
                override fun execute() {
                    findAllEvenNumbers(numbers[i])
                }

                override fun onFinish() {
                    count.incrementAndGet()
                }

                override fun onException() {
                    println("onException() invoked")
                }

                override fun onCancel() {
                    println("onCancel() invoked")
                }

                override fun toString(): String = "Number of pool $i"
            }
        )
    }

    count.set(0)

    // Последовательное выполнение
    println(
        "Последовательное выполнение = ${
            measureTimeMillis {
                requests.forEach {
                    it.execute()
                    it.onFinish()
                }
                while (count.get() != size) { }
            }
        }"
    )

    count.set(0)

    // Параллельное выполнение
    println(
        "Параллельное выполнение = ${
            measureTimeMillis {
                requests.forEach {
                    Thread {
                        it.execute()
                        it.onFinish()
                    }.start()
                }
                while (count.get() != size) { }
            }
        }"
    )

    count.set(0)

    val poolManager = PoolManager(countOfThreads)

    // Пулл потоков
    println(
        "Пулл потоков = ${
            measureTimeMillis {
                requests.forEach {
                    poolManager.addRequest(it)
                }
                while (count.get() != size) { }
            }
        }"
    )

    poolManager.shutdown()
}