package com.shahilraghuwanshi.aetherium;

import com.shahilraghuwanshi.aetherium.simulation.SimulationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    // This method will run automatically after the application starts
    @Bean
    public CommandLineRunner demo(SimulationService simulationService) {
        return (args) -> {
            System.out.println("Spawning initial cars...");
            // Spawn 5 cars for testing
            for (int i = 0; i < 5; i++) {
                simulationService.spawnCar();
            }
            System.out.println("Initial cars spawned.");
        };
    }
}
