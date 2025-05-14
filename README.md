# Fundraising Event & Collection Box Management API -  Sii Polska recruitment assignment

A backend REST API for managing collection boxes during fundraising events for charity organizations.

## Project Description

The system enables:
- Creating and managing fundraising events (each with its own account in a single currency)
- Registering and managing collection boxes (can hold money in multiple currencies)
- Transferring funds from boxes to event accounts with automatic currency conversion
- Generating financial reports

## Technologies

- Java 23
- Spring Boot 3.x (Spring MVC, Spring Data JPA)
- Maven
- H2 Database (in-memory)
- Lombok
- WebClient for exchange rates API integration
- JUnit 5 and Mockito for testing

## Requirements

- Java JDK 23+
- Apache Maven 3.6.x+
- ExchangeRate-API key

## Configuration and Running

1. **Clone repository:**
    ```bash
    git clone https://github.com/Szafranee/sii-java-assignment
    cd sii-java-assignment
    ```

2. **Configure API key:**

   Add to `application.properties`:
    ```properties
    exchange.rate.api.key=your_api_key
    ```
   Or set environment variable:
    ```bash
    export EXCHANGE_RATE_API_KEY=your_api_key
    ```

3. **Build:**
    ```bash
    ./mvnw clean package
    ```

4. **Run:**
    ```bash
    java -jar target/siiAssignment-0.0.1-SNAPSHOT.jar
    ```

Application will be available at `http://localhost:8080`.
H2 Console: `http://localhost:8080/h2-console`

## REST API Endpoints

Base URL: `http://localhost:8080/api`

### Fundraising Events

1. **Create new event**
    - `POST /fundraising-events`
   ```json
   {
     "name": "Children's Hospital Fundraiser",
     "accountCurrency": "PLN"
   }
   ```

2. **Financial report**
    - `GET /fundraising-events/report`
   ```json
   [
     {
       "fundraisingEventName": "Support for Children in Need",
       "amount": 1500.75,
       "currency": "PLN"
     }
   ]
   ```

### Collection Boxes

1. **Register new box**
    - `POST /collection-boxes`

2. **List all boxes**
    - `GET /collection-boxes`

3. **Unregister box**
    - `DELETE /collection-boxes/{id}`

4. **Assign box to event**
    - `PUT /collection-boxes/{boxId}/assign/{eventId}`

5. **Deposit money**
    - `PUT /collection-boxes/{boxId}/deposit`
   ```json
   {
     "currency": "USD",
     "amount": 50.75
   }
   ```

6. **Empty box**
    - `PUT /collection-boxes/{boxId}/empty`

## Currency Handling

The system uses ExchangeRate-API to fetch current exchange rates. Currency validation is performed on two levels:

1. **Format validation**: Checks if the currency code:
    - Is not null
    - Consists of exactly 3 uppercase letters (ISO 4217 format)
    - Uses regex pattern: `[A-Z]{3}`

2. **Existence validation**: Verifies if the currency:
    - Is present in the rates fetched from the API
    - Is currently supported and available for conversion

Features:
- Automatic rate refresh every hour
- Rate caching for better performance
- API error handling and currency code validation
- Precise conversion with 6 decimal places

## Error Handling

Global exception handling with appropriate HTTP status codes:

- `400 Bad Request` - invalid currency or amount
- `404 Not Found` - box or event not found
- `409 Conflict` - attempt to assign non-empty box
- `503 Service Unavailable` - exchange rate API issues

## Project Structure
```
src/
├── main/
│   ├── java/org/sii/siiassignment/
│   │   ├── config/
│   │   │   └── WebClientConfig.java
│   │   ├── controller/
│   │   │   ├── CollectionBoxController.java
│   │   │   └── FundraisingEventController.java
│   │   ├── DTO/
│   │   │   ├── CollectionBox/
│   │   │   │   ├── CollectionBoxDTO.java
│   │   │   │   └── DepositDTO.java
│   │   │   ├── FundraisingEvent/
│   │   │   │   ├── FundraisingEventDTO.java
│   │   │   │   └── FundraisingEventReportDTO.java
│   │   │   └── ErrorResponse.java
│   │   ├── exception/
│   │   │   ├── CollectionBoxStateException.java
│   │   │   ├── ExchangeRateException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── InvalidAmountException.java
│   │   │   ├── InvalidCurrencyException.java
│   │   │   └── ResourceNotFoundException.java
│   │   ├── model/
│   │   │   ├── CollectionBox.java
│   │   │   └── FundraisingEvent.java
│   │   ├── repository/
│   │   │   ├── CollectionBoxRepository.java
│   │   │   └── FundraisingEventRepository.java
│   │   ├── service/
│   │   │   ├── CollectionBoxService.java
│   │   │   ├── CollectionBoxServiceImpl.java
│   │   │   ├── ExchangeRateService.java
│   │   │   ├── FundraisingEventService.java
│   │   │   └── FundraisingEventServiceImpl.java
│   │   ├── SiiAssignmentApplication.java
│   └── resources/
│       ├── application.properties
│       └── data.sql
└── test/
    └── java/org/sii/siiassignment/
        ├── service/
        │   ├── CollectionBoxServiceImplTest.java
        │   └── FundraisingEventServiceImplTest.java
        └── SiiAssignmentApplicationTests.java
```

## Tests

Comprehensive unit tests for main components:
- Collection box service tests
- Fundraising event service tests
- Currency conversion tests
- Error handling tests

Run tests:
```bash
./mvnw test
```