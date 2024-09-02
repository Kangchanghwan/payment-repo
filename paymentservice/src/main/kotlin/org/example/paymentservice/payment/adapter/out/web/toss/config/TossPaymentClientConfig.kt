package org.example.paymentservice.payment.adapter.out.web.toss.config

import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.util.*
import java.util.concurrent.TimeUnit

@Configuration
class TossPaymentClientConfig(
    @Value("\${PSP.toss.url}") private val baseUrl: String,
    @Value("\${PSP.toss.secretKey}") private val secretKey: String,
) {
    @Bean
    fun tossPaymentWebClient(): WebClient {
        val encodedSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").toByteArray())

      return  WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $encodedSecretKey") // 헤더설정
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json") // 헤더 설정
            .clientConnector(reactorClientConnector())
            .codecs { it.defaultCodecs() }
            .build()
    }
    /*
    * reactorClientConnector():
    * 이 메소드는 netty의 HttpClient를 설정하고 래핑하는 커스텀 ClientHttpConnector를 생성한다.
    * ConnectionProvider는 netty의 연결 풀에 대한 세부 사항을 설정하는데 사용되며,
    * 여기서 연결 풀의 이름이 "toss-payment"로 설정됩니다.
    * */
    private fun reactorClientConnector(): ClientHttpConnector {
        val provider = ConnectionProvider.builder("toss-payment").build()

        val clientBase = HttpClient.create(provider)
            .doOnConnected{ // 연결 후
                it.addHandlerLast(ReadTimeoutHandler(30, TimeUnit.SECONDS))
            } // 읽기지연 타임아웃 발생 핸들러 추가.

        return ReactorClientHttpConnector(clientBase)
    }
}