package com.dinter.config.security;

import com.dinter.config.data.Oauth2ResourceServerProperties;
import com.dinter.config.data.JwtConfig;
import com.dinter.config.data.OAuth2ClientProperties;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuthenticationProviderConfig {

    private final JwtConfig jwtConfig;
    private final Oauth2ResourceServerProperties resourceServerProperties;
    private final OAuth2ClientProperties oAuth2ResourceServerProperties;

    public AuthenticationProviderConfig(JwtConfig jwtConfig, Oauth2ResourceServerProperties resourceServerProperties, OAuth2ClientProperties oAuth2ResourceServerProperties) {
        this.jwtConfig = jwtConfig;
        this.resourceServerProperties = resourceServerProperties;
        this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties;
    }


    @Bean
    public JwtAuthenticationProvider dinterJwtAuthenticationProvider(@Qualifier("dinterJwtDecoder") JwtDecoder dinterJwtDecoder) {
        return new JwtAuthenticationProvider(dinterJwtDecoder);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration")
    public JwtAuthenticationProvider oauth2JwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(oauth2JwtDecoders().get("google"));
    }

    @Bean
    public JwtDecoder dinterJwtDecoder()  {
        val secretKeySpec = new SecretKeySpec(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration")
    public Map<String, JwtDecoder> oauth2JwtDecoders() {
        val jwtDecoders = new HashMap<String, JwtDecoder>();
        resourceServerProperties.getProviders().forEach((key, value) -> {
            val decoder = NimbusJwtDecoder.withJwkSetUri(value.getJwkSetUri())
                    .build();
            jwtDecoders.put(key, decoder);
        });
        return jwtDecoders;
    }
}
