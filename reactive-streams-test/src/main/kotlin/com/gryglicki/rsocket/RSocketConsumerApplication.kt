package com.gryglicki.rsocket

import io.rsocket.Payload
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    //runApplication<RsocketTestApplication>(*args)

    runRSocketConsumer()
    readLine() //keeps the main thread alive
}

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
private fun runRSocketConsumer() {
    RSocketFactory
        .connect() /** Act as a Client and initialize connection to Server */
        .transport(TcpClientTransport.create("localhost", 7000))  /** Transport via TCP */
        .start()
        .flatMapMany { socket -> /** Convert Mono to Flux, because we'll use "requestStream" method */
            socket
                .requestStream(DefaultPayload.create("Hello")) /** Request Stream with given payload */
                .map(Payload::getDataUtf8) /** convert received binary payload to UTF-8 String */
                .doFinally {signal -> socket.dispose() } /** Close RSocket if this Flux is cancelled or just terminates */
        }
        .subscribe { logger.info("!!!!!!!!!!!!!!!!! CONSUMER: received: $it") }
}

@SpringBootApplication
class RSocketConsumerApplication : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) = runRSocketConsumer()
}