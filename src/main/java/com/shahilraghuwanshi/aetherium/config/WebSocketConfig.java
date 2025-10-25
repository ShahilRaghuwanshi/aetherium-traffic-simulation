package com.shahilraghuwanshi.aetherium.config;

import com.shahilraghuwanshi.aetherium.simulation.SimulationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired; // <-- Import Autowired
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // Enables WebSocket server support
public class WebSocketConfig implements WebSocketConfigurer {

    // Declare the handler field
    private final SimulationWebSocketHandler simulationWebSocketHandler;

    // Use Constructor Injection to get the Spring-managed bean
    @Autowired
    public WebSocketConfig(SimulationWebSocketHandler simulationWebSocketHandler) {
        this.simulationWebSocketHandler = simulationWebSocketHandler;
    }

    // Register the injected handler
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(simulationWebSocketHandler, "/ws/simulation") // Use the injected field
                .setAllowedOrigins("*"); // Allow connections from any origin (for development)
    }

    // Ensure there is NO @Bean method creating the handler here
}