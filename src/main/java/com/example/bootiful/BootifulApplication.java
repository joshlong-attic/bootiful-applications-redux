package com.example.bootiful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

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
		SpringApplication.run(BootifulApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(CustomerRepository cc, Timer timer) {
		return
			route(GET("/customers"), r -> ok().body(cc.findAll(), Customer.class))
				.andRoute(GET("/"), r -> ok().render("ui", Map.of("customers", cc.findAll())))
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