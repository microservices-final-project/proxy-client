package com.selimhorri.app.config.security;



import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class TestConfiguration{
    
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return mock(UserDetailsService.class);
    }
}