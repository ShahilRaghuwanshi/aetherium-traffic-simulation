// Global variables
let mapLayout; // Still needed for drawing roads/intersections
let cars = [];   // This will hold the cars received from the WebSocket
let socket;      // Our WebSocket connection object

// p5.js function: Setup canvas and WebSocket connection
function setup() {
  createCanvas(windowWidth, windowHeight);
  console.log("Setting up canvas and WebSocket connection...");

  // --- Load static map data via standard HTTP request ONCE ---
  let apiUrl = "http://localhost:8082/api/map/layout"; // Make sure port is correct!
  loadJSON(apiUrl, data => {
      mapLayout = data;
      console.log("Static map data loaded:", mapLayout);
      connectWebSocket(); // Connect WebSocket *after* map data is loaded
  }, error => {
      console.error("Failed to load map layout:", error);
  });
}

// Function to establish WebSocket connection
function connectWebSocket() {
  // Connect to the backend WebSocket endpoint
  let wsUrl = "ws://localhost:8082/ws/simulation"; // Make sure port is correct!
  socket = new WebSocket(wsUrl);

  // --- WebSocket Event Handlers ---
  socket.onopen = function(event) {
    console.log("WebSocket connection established.");
  };

  socket.onmessage = function(event) {
    try {
        let receivedCars = JSON.parse(event.data);
        cars = receivedCars; // Update the global cars array
        // console.log("Received data:", cars); // Uncomment to see data
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
  background(240); // Light gray background

  // --- Draw Static Map Elements (if loaded) ---
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
    // Draw Intersections
    noStroke();
    fill(100, 150, 255); // Light blue
    for (let intersection of mapLayout.intersections) {
      circle(
        intersection.xcoordinate,
        intersection.ycoordinate,
        10 // Size
      );
    }
  }

  // --- Draw Cars (using data from WebSocket) ---
  if (cars && cars.length > 0) {
    noStroke();
    fill(255, 0, 0); // Red

    for (let car of cars) {
      circle(
        car.x, // Use the x, y directly from the WebSocket data
        car.y,
        8 // Size
      );
    }
  }
}

// p5.js function: Handle window resize
function windowResized() {
  resizeCanvas(windowWidth, windowHeight);
}