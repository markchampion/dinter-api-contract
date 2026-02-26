package com.dinter.config.security;

import com.dinter.config.data.OAuth2ClientProperties;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration")
public class Oauth2ClientRegistrationConfig {

    private final OAuth2ClientProperties properties;

    public Oauth2ClientRegistrationConfig(OAuth2ClientProperties oAuth2ClientProperties) {
        this.properties = oAuth2ClientProperties;
    }

    @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        val registrations = properties.getRegistration().entrySet().stream()
                .map(entry -> {
                    val clientProperty = entry.getValue();
                    val authenticationMethod = Optional.ofNullable(clientProperty.getAuthenticationMethod())
                            .map(ClientAuthenticationMethod::new).orElse(null);
                    val authorizationGrantType = Optional.ofNullable(clientProperty.getAuthorizationGrantType())
                            .map(AuthorizationGrantType::new).orElse(null);
                    return ClientRegistration.withRegistrationId(entry.getKey())
                            .clientId(clientProperty.getClientId())
                            .clientSecret(clientProperty.getClientSecret())
                            .tokenUri(clientProperty.getTokenUri())
                            .authorizationUri(clientProperty.getAuthorizationUri())
                            .clientAuthenticationMethod(authenticationMethod)
                            .authorizationGrantType(authorizationGrantType)
                            .scope(clientProperty.getScope().split(",\\s+"))
                            .build();
                }).collect(Collectors.toList());
        return new InMemoryReactiveClientRegistrationRepository(registrations);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository reactiveClientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService
    ) {
        val authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        val authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                reactiveClientRegistrationRepository, reactiveOAuth2AuthorizedClientService
        );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }
}
