package com.project.tableforyou.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.tableforyou.handler.logoutHandler.CustomLogoutHandler;
import com.project.tableforyou.handler.logoutHandler.SuccessLogoutHandler;
import com.project.tableforyou.jwt.filter.JwtAuthenticationFilter;
import com.project.tableforyou.jwt.filter.JwtExceptionFilter;
import com.project.tableforyou.jwt.handler.OAuth2SuccessHandler;
import com.project.tableforyou.security.auth.PrincipalDetailsService;
import com.project.tableforyou.security.oauth.PrincipalOAuth2UserService;
import com.project.tableforyou.token.service.AccessTokenService;
import com.project.tableforyou.utils.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsFilter corsFilter;
    private final PrincipalOAuth2UserService principalOAuth2UserService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomLogoutHandler customLogoutHandler;
    private final PrincipalDetailsService principalDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AccessTokenService accessTokenService;
    private final SuccessLogoutHandler successLogoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .addFilter(corsFilter)

                .formLogin(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/api/user/**").authenticated()
                                .requestMatchers("/owner/**").hasAnyRole("OWNER", "ADMIN")
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                                .anyRequest().permitAll())


                .oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(endPoint ->
                                        endPoint.userService(principalOAuth2UserService))
                                .successHandler(oAuth2SuccessHandler))

                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, objectMapper, accessTokenService),
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(new JwtExceptionFilter(objectMapper), JwtAuthenticationFilter.class)

                .logout(logout ->
                        logout
                                .addLogoutHandler(customLogoutHandler)
                                .logoutSuccessHandler(successLogoutHandler));


        return http.build();
    }

    /* DaoAuthenticationProvider 구성 */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(principalDetailsService);
        authenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        authenticationProvider.setHideUserNotFoundExceptions(false);
        // hideUserNotFoundExceptions를 false하며 UsernameNotFoundException 활성화
        return  authenticationProvider;
    }

    //AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }



}
