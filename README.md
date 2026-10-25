# Nugget Fuel - backend prototype (Spring Boot 3, Java 17)

Run:  `mvn spring-boot:run`   (H2 in-memory DB, resets on restart; console at /h2-console, JDBC url `jdbc:h2:mem:nugget`)

Demo users (password: `password`): `alice` (attendant, Westlands), `bob` (attendant, Mombasa Rd), `manager`.

## Try it
```
# 1. login check + pumps alice can choose
curl -u alice:password localhost:8080/api/pumps

# 2. submit today's closing readings for pump id 1 (Diesel Pump A)
curl -u alice:password -H "Content-Type: application/json" \
  -d '{"pumpId":1,"manualReading":10450,"volumeReading":5420,"salesReading":812000}' \
  localhost:8080/api/readings

# 3. attendant's own readings for today (or ?date=YYYY-MM-DD)
curl -u alice:password localhost:8080/api/readings/mine

# 4. manager: stations, then everything (optionally ?stationId=1&date=YYYY-MM-DD)
curl -u manager:password localhost:8080/api/manager/stations
curl -u manager:password localhost:8080/api/manager/readings
```
