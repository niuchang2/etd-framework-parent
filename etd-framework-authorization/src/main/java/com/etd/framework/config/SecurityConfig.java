package com.etd.framework.config;

import com.etd.framework.service.impl.EtdUserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置默认的登录表单
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()

                )
                .formLogin(withDefaults());
        return http.build();
    }

    /**
     * 创建用户信息
     *
     * @return
     */
    @Bean
    UserDetailsService userDetailsService() {
        return new EtdUserDetailsServiceImpl();
    }

}
