package io.github.sidneyroberto9.spring_session_lite.autoconfigure;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import io.github.sidneyroberto9.spring_session_lite.domain.Session;
import io.github.sidneyroberto9.spring_session_lite.domain.SessionRepository;
import io.github.sidneyroberto9.spring_session_lite.scheduler.ExpiredSessionCleanupTask;
import io.github.sidneyroberto9.spring_session_lite.security.SessionAuthenticationFilter;
import io.github.sidneyroberto9.spring_session_lite.service.CookieManager;
import io.github.sidneyroberto9.spring_session_lite.service.IpHasher;
import io.github.sidneyroberto9.spring_session_lite.service.IpResolver;
import io.github.sidneyroberto9.spring_session_lite.service.SessionLiteService;
import io.github.sidneyroberto9.spring_session_lite.service.UserSessionLiteService;
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
@AutoConfigurationPackage(basePackageClasses = Session.class)
@ConditionalOnClass({EntityManagerFactory.class, SecurityFilterChain.class})
@ConditionalOnProperty(prefix = "spring-session-lite", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpringSessionLiteProperties.class)
@EnableScheduling
@Import(SpringSessionLiteWebMvcConfiguration.class)
public class SpringSessionLiteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IpResolver ipResolver(SpringSessionLiteProperties properties) {
        return new IpResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IpHasher ipHasher(SpringSessionLiteProperties properties) {
        return new IpHasher(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CookieManager cookieManager(SpringSessionLiteProperties properties) {
        return new CookieManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionLiteService sessionLiteService(
            SpringSessionLiteProperties properties,
            SessionRepository repository,
            CookieManager cookieManager,
            IpResolver ipResolver,
            IpHasher ipHasher
    ) {
        return new SessionLiteService(properties, repository, cookieManager, ipResolver, ipHasher);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserSessionLiteService userSessionLiteService() {
        return new UserSessionLiteService();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionAuthenticationFilter sessionAuthenticationFilter(
            SessionLiteService sessionLiteService,
            SpringSessionLiteProperties properties
    ) {
        return new SessionAuthenticationFilter(sessionLiteService, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpiredSessionCleanupTask expiredSessionCleanupTask(SessionLiteService sessionLiteService) {
        return new ExpiredSessionCleanupTask(sessionLiteService);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain sessionLiteSecurityFilterChain(
            HttpSecurity http,
            SessionAuthenticationFilter sessionAuthenticationFilter,
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
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
