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
	RouterFunction<ServerResponse> routes(CustomerRepository cc) {
		return
			route(GET("/customers"), r -> ok().body(cc.findAll(), Customer.class))
				.andRoute(GET("/"), r -> ok().render("ui", Map.of("customers", cc.findAll())));
	}
}

@Configuration
class WebsocketConfig {

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
		return new SimpleUrlHandlerMapping() {
			{
				setUrlMap(Map.of("/ws/time", wsh()));
				setOrder(10);
			}
		};
	}

	@Bean
	WebSocketHandlerAdapter wsha() {
		return new WebSocketHandlerAdapter();
	}


	@Bean
	WebSocketHandler wsh() {

		var data = Flux
			.fromStream(Stream.generate(() -> "Hello @ " + Instant.now().toString()))
			.delayElements(Duration.ofSeconds(1));

		return session -> session.send(data.share().map(session::textMessage));
	}
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, String> {
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Customer {
	private String id, name;
}