Requirement: "Weekend Getaway Planner"
Objective:
Develop an integration flow where an AI agent retrieves data from two distinct, independent APIs to provide a consolidated travel recommendation.
1. Integration Nodes (APIs):
   API 1 (Open-Meteo): Retrieve a 3-day weather forecast for a user-specified city.
   API 2 (Wikipedia Search): Retrieve the "Top Attractions" section for the same city.
2. Flow Logic:
   Step 1: The agent accepts a city name as input.
   Step 2 (Parallel Call): The agent calls the weather API to check for rain or extreme temperatures and simultaneously searches Wikipedia for local landmarks.
   Step 3 (Reasoning): The agent analyzes the weather data to filter the landmarks. (e.g., If it is raining, prioritize indoor museums; if sunny, prioritize parks).
   Step 4 (Output): Provide a structured plan: "It is sunny in [City], so I recommend visiting [Park Name]."
3. Success Criteria:
   The agent must successfully map the JSON output from the weather API to the search queries for the second API.
   The final response must cite data from both sources.
4. Suggested Tech Stack:
   Framework: Spring AI with StateGraph for logic routing.
   Protocol: Use the Model Context Protocol (MCP) to connect the Wikipedia search tool if using a local LLM like Ministral 3B.