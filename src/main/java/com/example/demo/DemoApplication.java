package com.example.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(CustomerRepository customerRepository) {
		return route()//
			.GET("/hola", r -> {
				var message = r.principal().map(this::githubNameFor).orElseThrow(() -> new IllegalArgumentException("you're not authenticated!"));
				var body = Map.of("message", "Hola, " + message);
				return ok().body(body);
			})//
			.GET("/hi", request -> ok().body(Map.of("message", "Hola a todo el mundo!")))//
			.GET("/customers", request -> ok().body(customerRepository.findAll()))//
			.GET("/hola.php", request -> {
				var name = request.param("name");
				return name //
					.map(n -> ok().render("hola", Map.of("name", n)))
					.orElseThrow(() -> new IllegalArgumentException("you must provide a 'name' parameter!"));
			})//
			.build();
	}
 private String githubNameFor(Principal principal) {
		var oauthPrincipal = (OAuth2AuthenticationToken) principal;
		var oAuth2User = (DefaultOAuth2User) oauthPrincipal.getPrincipal();
		return (String) oAuth2User.getAttributes().get("login");
	}

}

@Route("home")
@Component
class HomeView extends VerticalLayout {

	HomeView(CustomerRepository repository) {

		var grid = new Grid<>(Customer.class);
		var button = new Button("Load Customers!", event -> {
			grid.setItems(repository.findAll());
			Notification.show("Data has been loaded.");
		});

		this.add(button);
		this.add(grid);
	}

}

@Component
class CustomHealthIndicator implements HealthIndicator {

	@Override
	public Health health() {
		return Health.up().build();
	}

}

@Component
@Log4j2
@RequiredArgsConstructor
class Initializer {

	private final CustomerRepository repository;

	@EventListener(ApplicationReadyEvent.class)
	void go() {
		List.of("Jane", "Josh", "Maria", "Veronica", "Bob", "Hugo").stream().map(nombre -> new Customer(null, nombre))
			.map(this.repository::save).forEach(log::info);

	}

}

interface CustomerRepository extends JpaRepository<Customer, Long> {

}
