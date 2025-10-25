package com.shahilraghuwanshi.aetherium.simulation;

import com.fasterxml.jackson.databind.ObjectMapper; // Jackson for JSON conversion
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component // Make it a Spring bean
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    // A thread-safe list to keep track of all connected clients
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    // Jackson's ObjectMapper to convert Java objects to JSON strings
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Called when a new client connects
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket connection established: " + session.getId());
    }

    // Called when a client disconnects
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    // This method sends a message (our simulation state) to ALL connected clients
    public void broadcast(List<Car> cars) {
        try {
            // Convert the list of cars to a JSON string
            String carsJson = objectMapper.writeValueAsString(cars);
            TextMessage message = new TextMessage(carsJson);

            // Send the message to each session
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error broadcasting WebSocket message: " + e.getMessage());
        }
    }

    // We don't need to handle incoming messages for this project, but the method is required
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Do nothing for incoming messages in this simulation
        System.out.println("Received message (ignored): " + message.getPayload());
    }
}