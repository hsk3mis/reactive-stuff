package com.gryglicki.rs

import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.*

class MyRecursivePublisher(private var completeLimit: Long = Long.MAX_VALUE, private var errorLimit: Long = Long.MAX_VALUE) : Publisher<String>, Subscription {
    private val logger = KotlinLogging.logger {}

    private var subscriber: Subscriber<in String>? = null
    private var cancelled: Boolean = false

    private fun generateMoreData(): String {
        logger.info { "I'm generating more data "}
        return UUID.randomUUID().toString()
    }

    override fun subscribe(inSubscriber: Subscriber<in String>) {
        subscriber = inSubscriber
        logger.info { "subscribe(): I was subscribed with a given subscriber and I'll return back the Subscription" }
        subscriber?.onSubscribe(this)
    }

    /**
     * UWAGA: publisher.request(n) i subscriber.onNext(elem) rekurencyjnie się nawzajem wywołują!!
     * Trzeba uważać, żeby:
     *  - zmniejszyć completeLimit i errorLimit przed zrobieniem onNext, bo inaczej będzie za dużo elementeów
     *  - przypisać subscriber=null i sprawdzać to, przed wywołaniem onComplete() i onError(), bo inaczej będzie za dużo wywołań
     */
    override fun request(n: Long) {
        logger.info { "request($n): I was requested for more data" }
        for (i in 1..n) {
            logger.debug { "!!!!!!!!!!!!!! i=$i, cancelled=$cancelled, completeLimit=$completeLimit, errorLimit=$errorLimit" }
            if (!cancelled && completeLimit > 0L && errorLimit > 0L) {
                completeLimit--
                errorLimit--
                subscriber?.onNext(generateMoreData())
            } else break
        }
        if (completeLimit == 0L && subscriber != null) {
            logger.info { "stream reached the end" }
            subscriber?.onComplete()
            subscriber = null
        }
        if (errorLimit == 0L && subscriber != null) {
            logger.info { "stream encounter an error" }
            subscriber?.onError(IllegalStateException("Stream exception"))
            subscriber = null
        }
    }

    override fun cancel() {
        logger.info { "cancel(): subscription cancelled - send no more data" }
        cancelled = true
        /** No onComplete() if subscription was cancelled, because Subscriber just told us that this subscription is cancelled */
    }
}