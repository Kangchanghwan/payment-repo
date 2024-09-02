package org.example.paymentservice.payment.test

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.util.*

@TestConfiguration
class PSPTestWebClientConfiguration(
    @Value("\${PSP.toss.url}") private val baseUrl: String,
    @Value("\${PSP.toss.secretKey}") private val secretKey: String,
) {
    fun createTestTossWebClient(vararg customHeaderKeyValue: Pair<String, String>): WebClient {
        val encodedSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").toByteArray())

       return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $encodedSecretKey")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeaders { httpHeaders -> customHeaderKeyValue.forEach { httpHeaders[it.first] = it.second }}
            .clientConnector(reactorClientConnector())
           .build()
    }

    private fun reactorClientConnector(): ClientHttpConnector {
        val provider = ConnectionProvider.builder("test-toss-payment").build()
        return ReactorClientHttpConnector(HttpClient.create(provider))
    }

}