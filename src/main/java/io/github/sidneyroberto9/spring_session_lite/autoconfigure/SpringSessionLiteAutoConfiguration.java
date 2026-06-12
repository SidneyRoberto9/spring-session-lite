package io.github.sidneyroberto9.spring_session_lite.autoconfigure;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import io.github.sidneyroberto9.spring_session_lite.domain.SpringLiteSession;
import io.github.sidneyroberto9.spring_session_lite.domain.SpringLiteSessionRepository;
import io.github.sidneyroberto9.spring_session_lite.scheduler.SpringSessionLiteCleanupTask;
import io.github.sidneyroberto9.spring_session_lite.security.SpringSessionLiteAuthenticationFilter;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteCookieManager;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteIpHasher;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteIpResolver;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteService;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteUserService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration(
        before = JpaRepositoriesAutoConfiguration.class,
        after = HibernateJpaAutoConfiguration.class
)
@AutoConfigurationPackage(basePackageClasses = SpringLiteSession.class)
@ConditionalOnClass({EntityManagerFactory.class, SecurityFilterChain.class})
@ConditionalOnProperty(prefix = "spring-session-lite", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpringSessionLiteProperties.class)
@EnableScheduling
@Import(SpringSessionLiteWebMvcConfiguration.class)
public class SpringSessionLiteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteIpResolver ipResolver(SpringSessionLiteProperties properties) {
        return new SpringSessionLiteIpResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteIpHasher ipHasher(SpringSessionLiteProperties properties) {
        return new SpringSessionLiteIpHasher(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteCookieManager cookieManager(SpringSessionLiteProperties properties) {
        return new SpringSessionLiteCookieManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteService springSessionLiteService(
            SpringSessionLiteProperties properties,
            SpringLiteSessionRepository repository,
            SpringSessionLiteCookieManager cookieManager,
            SpringSessionLiteIpResolver ipResolver,
            SpringSessionLiteIpHasher ipHasher
    ) {
        return new SpringSessionLiteService(properties, repository, cookieManager, ipResolver, ipHasher);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteUserService springSessionLiteUserService() {
        return new SpringSessionLiteUserService();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteAuthenticationFilter springSessionLiteAuthenticationFilter(
            SpringSessionLiteService springSessionLiteService,
            SpringSessionLiteProperties properties
    ) {
        return new SpringSessionLiteAuthenticationFilter(springSessionLiteService, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringSessionLiteCleanupTask springSessionLiteCleanupTask(SpringSessionLiteService springSessionLiteService) {
        return new SpringSessionLiteCleanupTask(springSessionLiteService);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain sessionLiteSecurityFilterChain(
            HttpSecurity http,
            SpringSessionLiteAuthenticationFilter springSessionLiteAuthenticationFilter,
            SpringSessionLiteProperties properties
    ) throws Exception {

        AuthenticationEntryPoint entryPoint = (req, res, ex) -> {
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}");
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.getPermitAllPaths().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(springSessionLiteAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
