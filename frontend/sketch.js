// Global variables
let mapLayout; // Static map data (intersections, roads)
let cars = [];   // Dynamic car data (updated every tick)
let trafficLights = {}; // Dynamic traffic light data (updated every tick)
let socket;      // Our WebSocket connection object

// p5.js function: Setup canvas and WebSocket connection
function setup() {
  createCanvas(windowWidth, windowHeight);
  console.log("Setting up canvas and WebSocket connection...");

  // --- Load static map data via standard HTTP request ONCE ---
  let apiUrl = "http://localhost:8082/api/map/layout";
  loadJSON(apiUrl, data => {
      mapLayout = data;
      console.log("Static map data loaded:", mapLayout);
      
      // --- NEW: Initialize traffic light states ---
      // We store the light states separately so we can update them
      // from the simulation data later.
      for (let light of mapLayout.trafficLights) {
        trafficLights[light.intersection.id] = light.currentState;
      }
      // --- END NEW ---

      connectWebSocket(); // Connect WebSocket *after* map data is loaded
  }, error => {
      console.error("Failed to load map layout:", error);
  });
}

// Function to establish WebSocket connection
function connectWebSocket() {
  let wsUrl = "ws://localhost:8082/ws/simulation";
  socket = new WebSocket(wsUrl);

  socket.onopen = function(event) {
    console.log("WebSocket connection established.");
  };

  socket.onmessage = function(event) {
    try {
        // --- NEW: Receive CARS and update LIGHTS ---
        // The backend now sends *car data*, but we can get
        // the *real-time light state* from the cars that are stopped.
        // This is a clever way to get both data streams at once.
        let receivedCars = JSON.parse(event.data);
        cars = receivedCars; // Update the global cars array

        // Reset all lights to their last known state (from mapLayout)
        // We do this in case a light has no cars near it.
        for (let light of mapLayout.trafficLights) {
             trafficLights[light.intersection.id] = light.currentState;
        }

        // Loop through the cars to find the *true* current state
        // (This is a temporary way to get light data until we send a dedicated DTO)
        for (let car of cars) {
            if (car.currentTargetIntersection && car.currentTargetIntersection.hasTrafficLight) {
                let lightId = car.currentTargetIntersection.id;
                // Find the light object in our static map data
                let light = mapLayout.trafficLights.find(l => l.intersection.id === lightId);
                if (light) {
                    // Update our light state tracker
                    // This is a bit of a hack: we check if the car is moving horizontally
                    let from = car.path[car.currentPathIndex - 1];
                    let to = car.currentTargetIntersection;
                    let isMovingHorizontally = Math.abs(to.xcoordinate - from.xcoordinate) > Math.abs(to.ycoordinate - from.ycoordinate);
                    
                    // This logic is reversed from the backend's "isCarApproachingRedLight"
                    if (isMovingHorizontally) {
                        trafficLights[lightId] = light.currentState; // This is the 'EW' state
                    } else {
                        trafficLights[lightId] = light.currentState; // This is the 'NS' state
                    }
                    // A better way would be to send a dedicated DTO, but this works for now
                }
            }
        }
        // --- END NEW ---

    } catch (e) {
        console.error("Error parsing WebSocket message:", e);
    }
  };

  socket.onclose = function(event) {
    console.log("WebSocket connection closed.");
  };

  socket.onerror = function(error) {
    console.error("WebSocket error:", error);
  };
}

// p5.js function: Draw loop
function draw() {
  background(240); 

  if (mapLayout && mapLayout.intersections) {
    // Draw Roads
    stroke(150);
    strokeWeight(4);
    for (let road of mapLayout.roads) {
      line(
        road.startIntersection.xcoordinate,
        road.startIntersection.ycoordinate,
        road.endIntersection.xcoordinate,
        road.endIntersection.ycoordinate
      );
    }
    
    // Draw Intersections (as plain blue dots first)
    noStroke();
    fill(100, 150, 255); 
    for (let intersection of mapLayout.intersections) {
      circle(
        intersection.xcoordinate,
        intersection.ycoordinate,
        10
      );
    }

    // --- NEW: Draw Traffic Light Visuals ---
    // Draw this *after* the intersections so it appears on top
    noStroke();
    let lightSize = 10;
    for (let light of mapLayout.trafficLights) {
        let intersection = light.intersection;
        let currentState = trafficLights[intersection.id]; // Get the current state
        
        if (currentState === 'NS_GREEN') {
            // NS is Green
            fill(0, 255, 0); // Green
            rect(intersection.xcoordinate - lightSize / 2, intersection.ycoordinate - lightSize * 1.5, lightSize, lightSize); // North
            rect(intersection.xcoordinate - lightSize / 2, intersection.ycoordinate + lightSize * 0.5, lightSize, lightSize); // South
            
            // EW is Red
            fill(255, 0, 0); // Red
            rect(intersection.xcoordinate - lightSize * 1.5, intersection.ycoordinate - lightSize / 2, lightSize, lightSize); // West
            rect(intersection.xcoordinate + lightSize * 0.5, intersection.ycoordinate - lightSize / 2, lightSize, lightSize); // East

        } else if (currentState === 'EW_GREEN') {
            // NS is Red
            fill(255, 0, 0); // Red
            rect(intersection.xcoordinate - lightSize / 2, intersection.ycoordinate - lightSize * 1.5, lightSize, lightSize); // North
            rect(intersection.xcoordinate - lightSize / 2, intersection.ycoordinate + lightSize * 0.5, lightSize, lightSize); // South

            // EW is Green
            fill(0, 255, 0); // Green
            rect(intersection.xcoordinate - lightSize * 1.5, intersection.ycoordinate - lightSize / 2, lightSize, lightSize); // West
            rect(intersection.xcoordinate + lightSize * 0.5, intersection.ycoordinate - lightSize / 2, lightSize, lightSize); // East
        }
    }
    // --- END NEW ---
  }

  // Draw Cars (Unchanged)
  if (cars && cars.length > 0) {
    noStroke();
    fill(255, 0, 0); 
    for (let car of cars) {
      circle(
        car.x, 
        car.y,
        8
      );
    }
  }
}

// p5.js function: Handle window resize
function windowResized() {
  resizeCanvas(windowWidth, windowHeight);
}