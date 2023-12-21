package threadpool

import java.util.Vector
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PoolManager(private val size: Int) {

    @Volatile private var shutdown = false

    /** Объект для синхронизации public методов */
    private val managerLock = ReentrantLock()

    /** Поток менеджера */
    private val managerThread: Thread = Thread {
        while (!shutdown) {
            try {
                freeThreads.take().executeRequest(requests.take())
            } catch (ignored: Exception) { }
        }
    }

    private val threads = Vector<PoolThread>()
    /** Очередь свободных потоков*/
    private val freeThreads: BlockingQueue<PoolThread> = LinkedBlockingQueue()
    /** Очередь пришедших запросов*/
    private val requests: BlockingQueue<PoolRequest> = LinkedBlockingQueue()

    init {
        for (i in 0..<size) {
            threads += PoolThread()
        }
        freeThreads.addAll(threads)
        managerThread.start()
    }

    /** Добавление нового запроса в очередь*/
    public fun addRequest(request: PoolRequest) {
        managerLock.withLock {
            if (shutdown) throw RuntimeException("Thread Pool is shutdown")
            try {
                requests.put(request)
            } catch (e: Exception) {
                throw RuntimeException("Failed to add request to queue", e)
            }
        }
    }

    /** Выключение менеджера потоков
     * Менеджер прекращает принимать новые запросы
     * У всех запросов вызывается onCancel()
     */
    public fun shutdown() {
        managerLock.withLock {
            shutdown = true
            managerThread.interrupt()
            signalThreads()
            cancelRequests()
        }
    }

    /** Пробуждение всех потоков*/
    private fun signalThreads() {
        threads.forEach { thread ->
            thread.lockObject.withLock {
                thread.condition.signal()
            }
        }
    }

    /** Отмена всех запросов в очереди*/
    private fun cancelRequests() {
        try {
            while (requests.isNotEmpty()) {
                requests.take().onCancel()
            }
        } catch (ignored: Exception) { }
    }

    /** Помещает поток в очередь свободных потоков*/
    private fun freeThread(thread: PoolThread) {
        freeThreads.put(thread)
    }

    private inner class PoolThread {

        /** Объект для блокировки потока*/
        val lockObject = ReentrantLock()
        val condition: Condition = lockObject.newCondition()
        private var request: PoolRequest? = null

        private val runnable: Runnable = Runnable {
            while (!shutdown) {
                try {
                    /** Ставит поток на ожидание нового запроса*/
                    waitForRequest()
                    if (request == null) {
                        continue
                    }
                    /** Обработка запроса*/
                    processRequest(request!!)
                    request = null
                    /** Помещение потока в очередь свободных потоков*/
                    freeThread(this)

                } catch (ignored: Exception) { }
            }
        }

        init {
            Thread(runnable).start()
        }

        fun executeRequest(request: PoolRequest) {
            lockObject.withLock {
                this.request = request
                condition.signal()
            }
        }

        /** Обработка запроса*/
        private fun processRequest(request: PoolRequest) {
            try {
                request.execute()
                request.onFinish()
            } catch (e: Exception) {
                request.onException()
            }
        }

        /** Заморозка потока*/
        private fun waitForRequest() {
            lockObject.withLock {
                if (request == null) {
                    condition.await()
                }
            }
        }
    }
}