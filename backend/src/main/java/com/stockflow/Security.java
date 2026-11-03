package com.stockflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Security {
 @Bean UserDetailsService users(@Value("${stockflow.username}") String name, @Value("${stockflow.password}") String password) {
  return new InMemoryUserDetailsManager(User.withUsername(name).password(new BCryptPasswordEncoder().encode(password)).roles("OWNER").build());
 }
 @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
  // Only the explicit Authorization header authenticates requests; there are no cookie sessions.
  return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a -> a.anyRequest().authenticated()).httpBasic(Customizer.withDefaults()).build();
 }
}
