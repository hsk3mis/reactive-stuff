package com.gryglicki.rs

import mu.KotlinLogging
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.math.min

class MySubscriber(private val configuredRequestCounter: Long, configuredCancellationCounter: Long = Long.MAX_VALUE) : Subscriber<String> {

    private val logger = KotlinLogging.logger {}

    private var subscription: Subscription? = null
    private var requestedCounter = 0L
    private var cancellationCounter = 0L

    init {
        cancellationCounter = configuredCancellationCounter
        resetRequestedCounter()
    }

    private fun resetRequestedCounter(): Long {
        requestedCounter = min(configuredRequestCounter, cancellationCounter)
        return requestedCounter
    }

    private fun onNextStateChange() {
        requestedCounter--
        cancellationCounter--
    }

    override fun onComplete() {
        logger.info { "onComplete(): cleaning any reserved resources" }
    }

    override fun onSubscribe(s: Subscription) {
        subscription = s
        if (requestedCounter <= 0) {
            logger.info { "onSubscribe(): don't request data" }
        } else {
            logger.info { "onSubscribe(): request initial ($requestedCounter) data via Subscription" }
            subscription?.request(requestedCounter)
        }
    }

    override fun onNext(s: String) {
        logger.info { "onNext(): received $s" }
        onNextStateChange()
        if (cancellationCounter == 0L) {
            logger.info { "onNext(): cancel Subscription because we reached cancellation counter" }
            subscription?.cancel()
        } else if (requestedCounter == 0L) {
            val toRequest = resetRequestedCounter()
            logger.info { "onNext(): request more ($toRequest) data via Subscription" }
            subscription?.request(toRequest)
        }
    }

    override fun onError(t: Throwable) {
        logger.info { "onError(): $t" }
        /** No subscription.cancel() if error, because Publisher just told us that this subscription is broken */
    }
}