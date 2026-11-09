package dk.timer.mcp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Calls to the ms-search backend use a token obtained via OAuth 2.0 Token Exchange (RFC 8693):
 * the user's incoming JWT (audience = this MCP server) is exchanged at Keycloak by the
 * confidential client "mcp-client" for a new token whose audience is the ms-search backend.
 */
@Configuration
@EnableConfigurationProperties(UrlProperties.class)
@Slf4j
public class GantConfig {

    private static final String REGISTRATION_ID = "keycloak";

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${token-exchange.audience}") String audience) {

        RestClientTokenExchangeTokenResponseClient tokenResponseClient = new RestClientTokenExchangeTokenResponseClient();
        // Ask Keycloak for a token targeted at the ms-search backend
        tokenResponseClient.setParametersCustomizer(params -> params.set("audience", audience));

        // Default subject token = the JWT of the current JwtAuthenticationToken (incoming MCP request)
        TokenExchangeOAuth2AuthorizedClientProvider tokenExchange = new TokenExchangeOAuth2AuthorizedClientProvider();
        tokenExchange.setAccessTokenResponseClient(tokenResponseClient);
        tokenExchange.setSubjectTokenResolver(context -> {
            if (context.getPrincipal() instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                log.info("Token exchange subject token: aud={}, azp={}, scope={}",
                        jwt.getAudience(), jwt.getClaimAsString("azp"), jwt.getClaimAsString("scope"));
                return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                        jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt());
            }
            return null;
        });
        // Service-based manager: no HTTP session needed (the MCP server is stateless).
        // Exchanged tokens are cached per user (principal name) until they expire.
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClientService);
        manager.setAuthorizedClientProvider(tokenExchange);
        return manager;
    }

    @Bean
    public RestClient projectsRestClient(UrlProperties urls,
                                         OAuth2AuthorizedClientManager authorizedClientManager,
                                         OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2ClientHttpRequestInterceptor oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        // On 401/403 from the backend, drop the cached exchanged token so the next call re-exchanges
        oauth2.setAuthorizationFailureHandler(
                OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClientService));

        return RestClient.builder()
                .baseUrl(urls.base())
                .requestInterceptor(oauth2)
                .build();
    }

}
