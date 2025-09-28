package com.example.common.auth;

import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private AuthService authService;
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() {
        webClientBuilder = WebClient.builder();
        authService = new AuthService(webClientBuilder);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAuthTokenWithCert_GrantTypeAndBasicAuth() throws Exception {
        // Arrange
        String authUrl = "https://auth.example.com/token";
        String username = "clientId";
        String password = "clientSecret";
        String grantType = "client_credentials";
        String certPassword = "password";
        String expectedToken = "{\"access_token\":\"abc123\"}";

        // Mock certificate resource
        Resource certResource = mock(Resource.class);
        InputStream certStream = new ByteArrayInputStream(new byte[]{1,2,3});
        when(certResource.getInputStream()).thenReturn(certStream);

        // Mock WebClient and response
        WebClient mockWebClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(mockWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(authUrl)).thenReturn(bodySpec);
        when(bodySpec.header(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_FORM_URLENCODED_VALUE))).thenReturn(bodySpec);
        when(bodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue("grant_type=" + grantType)).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(expectedToken));

        // Spy AuthService and mock SSL context creation
        AuthService spyService = spy(authService);
        doReturn(mock(SslContext.class)).when(spyService).createSslContext(any(), any());
        // Replace the internal WebClient with our mock (reflection hack for test)
        java.lang.reflect.Field webClientField = AuthService.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(spyService, mockWebClient);

        // Act
        Mono<String> resultMono = spyService.getAuthTokenWithCert(authUrl, username, password, grantType, certResource, certPassword);
        String result = resultMono.block();

        // Assert
        assertEquals(expectedToken, result);
        verify(bodySpec).header(eq(HttpHeaders.AUTHORIZATION), startsWith("Basic "));
        verify(bodySpec).bodyValue("grant_type=" + grantType);
    }
}
