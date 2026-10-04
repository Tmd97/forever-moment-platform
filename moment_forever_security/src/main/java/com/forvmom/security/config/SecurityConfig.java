package com.forvmom.security.config;

import com.forvmom.security.jwt_exception_handler.JwtAccessDeniedHandler;
import com.forvmom.security.jwt_exception_handler.JwtAuthenticationEntryPoint;
import com.forvmom.security.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Main security configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true,
                jsr250Enabled = true,
                prePostEnabled = true
)
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final PasswordEncoder passwordEncoder;
        private final JwtAuthenticationEntryPoint authenticationEntryPoint;
        private final JwtAccessDeniedHandler accessDeniedHandler;

        @Autowired
        public SecurityConfig(
                        CustomUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder,
                        JwtAuthenticationEntryPoint authenticationEntryPoint,
                        JwtAccessDeniedHandler accessDeniedHandler) {
                this.userDetailsService = userDetailsService;
                this.passwordEncoder = passwordEncoder;
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/auth/**",
                                                                "/public/**",
                                                                "/error",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/swagger-resources/**",
                                                                "/webjars/**",
                                                                "/actuator/**",
                                                                "/platform/actuator/**"
                                                ).permitAll()
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                                .requestMatchers("/vendor/**").hasAnyRole("VENDOR", "ADMIN", "SUPER_ADMIN")
                                                .requestMatchers(
                                                                "/user/**",
                                                                "/booking/**",
                                                                "/review/**")
                                                .hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                                                .anyRequest().authenticated())

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .addFilterBefore(new GatewayHeaderAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}