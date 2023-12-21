package threadpool

abstract class PoolRequest {

    abstract fun execute()

    abstract fun onFinish()

    abstract fun onException()

    abstract fun onCancel()
}