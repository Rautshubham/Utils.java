
/**
 * @author  mithape
 * @version 1.0
 * @purpose This class is used to define Spring Security Configuration.
 *
 * @History
 * ===============================================================================================================================================
 *     @Version         @Date           @Author                 @Purpose
 * ===============================================================================================================================================
 *     1.0                      15-01-18        Mayur I                 This class is used to define Spring Security Configuration.
 * ===============================================================================================================================================
 *
 */
package com.en.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.en.login.authentication.CustomAuthenticationProvider;
import com.en.login.authentication.CustomAuthorizeRequests;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    @Qualifier("dataSource1")
    DataSource                   dataSource;
    @Autowired
    @Qualifier("customAuthenticationProvider")
    CustomAuthenticationProvider customAuthenticationProvider;
    @Autowired
    @Qualifier("customAuthorizeRequests")
    CustomAuthorizeRequests      customAuthorizeRequests;

    @Autowired
    public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        customAuthorizeRequests.configure(http, dataSource);
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(11);
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
