# Solution Design Document (SDD)

## ARCHITECTURAL OVERVIEW

### Integration Pattern:
**Event-Driven + Parallel Microservices**
- The design leverages parallel API calls for efficiency and real-time data retrieval.
- Uses a **StateGraph-based workflow** in Spring AI to orchestrate the reasoning logic.
- Integrates with a **Model Context Protocol (MCP)-compliant Wikipedia search tool** (if using a local LLM).

### Key Components:
1. **Frontend Agent Layer:** Accepts user input (city name) and triggers parallel API calls.
2. **[SRVC-01] Weather Service:** Calls Open-Meteo API to fetch 3-day weather forecast.
   - Output: JSON with temperature, precipitation probability, and conditions.
3. **[SRVC-02] Wikipedia Attractions Service:** Calls Wikipedia API to retrieve "Top Attractions" for the city.
   - Output: Structured list of landmarks/attractions.
4. **StateGraph Orchestrator (Spring AI):** Routes data between APIs and applies conditional logic (e.g., weather-based filtering).
5. **[DB-01] Cache Layer:** Optional Redis cache to store API responses for rate-limiting and performance optimization.
6. **Output Generator:** Constructs a final recommendation string using both sources.

### Architectural Trade-Off:
[ARCHITECTURAL_TRADE_OFF]
- **Parallel vs. Sequential Processing:** Parallel calls improve throughput but introduce complexity in error handling (e.g., API failures).
  Recommendation: Implement dead-letter queues for failed requests and retry logic with exponential backoff.

---