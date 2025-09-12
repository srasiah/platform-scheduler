package com.example.common.auth;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Component
public class AuthService {
    private final WebClient webClient;

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get an authentication token from an external REST service.
     * @param authUrl The authentication endpoint URL
     * @param username The username
     * @param password The password
     * @return The authentication token as a String
     */
    public Mono<String> getAuthToken(String authUrl, String username, String password) {
        return webClient.post()
                .uri(authUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue("{" +
                        "\"username\":\"" + username + "\"," +
                        "\"password\":\"" + password + "\"}")
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Get an authentication token from an external REST service using client certificate.
     * @param authUrl The authentication endpoint URL
     * @param username The username
     * @param password The password
     * @param certResource The PKCS12 certificate as a Spring Resource
     * @param certPassword The password for the certificate
     * @return The authentication token as a String
     */
    public Mono<String> getAuthTokenWithCert(String authUrl, String username, String password, Resource certResource, String certPassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream certStream = certResource.getInputStream()) {
                keyStore.load(certStream, certPassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, certPassword.toCharArray());

            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(kmf)
                    .build();

            HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
            WebClient certWebClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

            return certWebClient.post()
                    .uri(authUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue("{" +
                            "\"username\":\"" + username + "\"," +
                            "\"password\":\"" + password + "\"}")
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
