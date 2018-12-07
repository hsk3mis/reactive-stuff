package com.gryglicki.rs

import org.junit.Test
import reactor.core.publisher.Flux

class ReactiveStreamsTest {

    @Test
    fun subscriberShouldOnlyRequestDataIfNeeded() {
        //Given
        val flux = Flux.fromIterable(1..10).map(Int::toString)

        //When
        flux.subscribe(MySubscriber(3))
    }

    @Test
    fun subscriberShouldCancelSubscriptionIfItsNotRequiredAnyMore() {
        //Given
        val flux = Flux.fromIterable(1..10).map(Int::toString)

        //When
        flux.subscribe(MySubscriber(3, 7))

        //Then - NO onComplete() call in subscription was cancelled
    }

    @Test
    fun publisherShouldGenerateDataOnlyIfRequested() {
        //Given
        val publisher = MyPublisher()

        //When
        publisher.subscribe(MySubscriber(3, 7))
    }

    @Test
    fun publisherShouldGenerateOnlyLimitedDataUntilCompleted() {
        //Given
        val publisher = MyPublisher(7)

        //When
        publisher.subscribe(MySubscriber(3))

        //Then - only 7 elements are generated and then onComplete()
    }

    @Test
    fun publisherShouldGenerateOnlyLimitedDataUntilError() {
        //Given
        val publisher = MyPublisher(7)

        //When
        publisher.subscribe(MySubscriber(3))

        //Then - only 7 elements are generated and then onError()
    }


}