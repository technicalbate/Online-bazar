package com.khojo.auth;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class AuthSecurityConfiguration {

	@Bean
	SecurityFilterChain authSecurityFilterChain(
			HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/auth/**", "/error").permitAll()
						.anyRequest().permitAll())
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String frontendOrigin) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendOrigin));
		configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
