package com.example.bootiful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Log4j2
@SpringBootApplication
public class BootifulApplication {

	public static void main(String[] args) {
		System.setProperty("spring.main.lazy-initialization", "true");
		SpringApplication.run(BootifulApplication.class, args);
	}

	private static Publisher<String> githubOauth2LoginFor(Mono<? extends Principal> p) {
		return p
			.map(principal -> {
				Assert.isInstanceOf(OAuth2AuthenticationToken.class, principal);
				var token = OAuth2AuthenticationToken.class.cast(principal);
				var result = token.getPrincipal().getAttributes().getOrDefault("login", principal.getName());
				return String.class.cast(result);
			});
	}

	@Bean
	RouterFunction<ServerResponse> routes(CustomerRepository cc, Timer timer) {
		return
			route(GET("/customers"), r -> ok().body(cc.findAll(), Customer.class))
				.andRoute(GET("/"), r -> ok().render("ui", Map.of("customers", cc.findAll(), "user", githubOauth2LoginFor(r.principal()))))
				.andRoute(GET("/sse"), r -> ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(timer.greet(), String.class));
	}
}

@Configuration
class WebsocketConfig {

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
		return new SimpleUrlHandlerMapping() {
			{
				setUrlMap(Map.of("/ws/time", wsh));
				setOrder(10);
			}
		};
	}

	@Bean
	WebSocketHandlerAdapter wsha() {
		return new WebSocketHandlerAdapter();
	}

	@Bean
	WebSocketHandler wsh(Timer timer) {
		return session -> session.send(timer.greet().share().map(session::textMessage));
	}
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, String> {
}

@Component
class Timer {

	Flux<String> greet() {
		return Flux
			.fromStream(Stream.generate(() -> Instant.now().toString()))
			.delayElements(Duration.ofSeconds(1));
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Customer {
	private String id, name;
}