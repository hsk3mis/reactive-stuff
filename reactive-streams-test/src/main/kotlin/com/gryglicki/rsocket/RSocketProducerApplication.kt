@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.gryglicki.rsocket

import io.rsocket.AbstractRSocket
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.SocketAcceptor
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    //runApplication<RsocketTestApplication>(*args)

    runRSocketProducer()
    readLine() //keeps the main thread alive
}

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
private fun runRSocketProducer() {
    val sa = SocketAcceptor { setup: ConnectionSetupPayload, sendingSocket: RSocket ->
        Mono.just(object : AbstractRSocket() {
            override fun requestStream(payload: Payload): Flux<Payload> {
                /** Emmit new record every interval */
                return Flux.interval(Duration.ofSeconds(3))
                    .map { DefaultPayload.create("interval: $it") }
                    .doOnSubscribe { logger.info { "!!!!!!!!!!!!!!!!! PRODUCER: Client subscribed to the stream" } }
                    .doOnCancel { logger.info { "!!!!!!!!!!!!!!!!! PRODUCER: Client cancelled the stream" } }
            }
        })
    }

    RSocketFactory
        .receive() /** Act as a Server */
        .acceptor(sa) /** What happen when RSocket client connects with any possible interaction method */
        .transport(TcpServerTransport.create("localhost", 7000)) /** Transport via TCP */
        .start()
        .onTerminateDetach() /** Some safety reasons... probably here it's not important */
        .subscribe { nettyContextClosable -> logger.info("!!!!!!!!!!!!!!!!! PRODUCER: started @ " + Instant.now().toString()) } /** subcribe to start the Server */
}


@SpringBootApplication
class RSocketProducerApplication : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) = runRSocketProducer()
}