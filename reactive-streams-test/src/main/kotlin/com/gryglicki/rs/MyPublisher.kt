package com.gryglicki.rs

import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.*

class MyPublisher(private var completeLimit: Long = Long.MAX_VALUE, private var errorLimit: Long = Long.MAX_VALUE) : Publisher<String>, Subscription {
    private val logger = KotlinLogging.logger {}

    private var alreadySent: Long = 0L
    private var requested: Long = 0L
    private var insideRequest: Boolean = false

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
     * Trzeba uważać, żeby:
     *  - zmniejszyć completeLimit i errorLimit przed zrobieniem onNext, bo inaczej będzie za dużo elementeów
     *  - przypisać subscriber=null i sprawdzać to, przed wywołaniem onComplete() i onError(), bo inaczej będzie za dużo wywołań
     */
    override fun request(n: Long) {
        requested += n
        if (! insideRequest) {
            insideRequest = true
            logger.info { "request($n): I was requested for more data" }
            while (alreadySent < requested) {
                logger.debug { "!!!!!!!!!!!!!! requested=$requested, alreadySent=$alreadySent, cancelled=$cancelled, completeLimit=$completeLimit, errorLimit=$errorLimit" }
                if (!cancelled && completeLimit > 0L && errorLimit > 0L) {
                    completeLimit--
                    errorLimit--
                    subscriber?.onNext(generateMoreData())
                } else break
                alreadySent++
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
            insideRequest = false
        }
    }

    override fun cancel() {
        logger.info { "cancel(): subscription cancelled - send no more data" }
        cancelled = true
        /** No onComplete() if subscription was cancelled, because Subscriber just told us that this subscription is cancelled */
    }
}