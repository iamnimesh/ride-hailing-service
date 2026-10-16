# Ride Hailing Service

A Spring Boot service for registering riders and drivers, matching a nearby cab, completing a ride at a tiered fare, and keeping a ride history. Coupons and two bonuses are included: a booking claim that only one concurrent request can win, and surge pricing chosen when the ride is booked.

## Technology stack

- Java 21
- Spring Boot 4.1.1 (Web MVC, Spring Data JPA, Validation)
- H2 in-memory database
- Bean Validation on request DTOs
- JUnit 5 and MockMvc
- Maven Wrapper (`mvnw`)

The application listens on `http://localhost:8080`. The database is `jdbc:h2:mem:ridehailing` with `spring.jpa.hibernate.ddl-auto=create-drop`.

## Architecture overview

Requests enter a controller, which validates the body and delegates to a service. Services own the booking, pricing, coupon, and surge rules. Repositories persist users, drivers, cabs, coupons, and rides.

```
controller  ->  service  ->  repository  ->  H2
                 |  |
                 |  +-> PricingStrategy (hatchback or sedan slabs)
                 +----> SurgeMultiplierProvider (demand / supply)
```

Entities are not returned from the API. Response DTOs carry the fields a client needs, including a cab's `driverId` rather than a nested driver. That avoids a cycle between driver and cab.

Packages under `com.example.ridehailing`:

| Package | Responsibility |
|---|---|
| `controller` | HTTP mapping and status codes |
| `service` | Matching, booking, completion, history |
| `repository` | JPA queries, including the conditional driver claim |
| `entity` | User, Driver, Cab, Ride, Coupon |
| `dto` | Request and response records |
| `pricing` | Fare strategies and the surge provider |
| `exception` | 400, 404, and 409 responses with `{ "message": "..." }` |

## API documentation

| Method | Path | Success | Failure |
|---|---|---|---|
| GET | `/health` | 200 `{ "status": "UP" }` | |
| POST | `/users` | 201 user | 400 invalid body |
| GET | `/users/{id}` | 200 user | 404 missing user |
| GET | `/users/{id}/rides` | 200 ride list, newest first | 404 missing user |
| POST | `/drivers` | 201 driver and cab, available | 400 invalid body or car type |
| GET | `/drivers/{id}` | 200 driver | 404 missing driver |
| GET | `/drivers/{id}/rides` | 200 ride list, newest first | 404 missing driver |
| PATCH | `/cabs/{id}/location` | 200 cab | 400 invalid coordinates, 404 missing cab |
| POST | `/coupons` | 201 coupon | 400 invalid or duplicate code |
| DELETE | `/coupons/{code}` | 204 empty body | 404 missing coupon |
| POST | `/rides/book` | 201 ongoing ride | 400 invalid body or coupon, 404 missing user, 409 no eligible driver |
| POST | `/rides/{rideId}/end` | 200 completed ride | 400 bad distance, 404 missing ride, 409 already completed |

Car types are `HATCHBACK` and `SEDAN`. Latitude is -90 through 90. Longitude is -180 through 180. An unknown enum or unreadable body returns 400 `Invalid request body`.

Create a user:

```json
{ "name": "Asha", "phone": "9100000001" }
```

Create a driver. `available` cannot be set here; a new driver is available.

```json
{
  "name": "Kiran",
  "phone": "9100000101",
  "carType": "HATCHBACK",
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

Move that driver's cab. The id is `cab.id` from the driver response.

```json
{ "latitude": 12.98, "longitude": 77.60 }
```

Create a coupon. The code is trimmed and stored in uppercase. Discount must be greater than 0 and at most 100.

```json
{ "code": "SAVE20", "discountPercentage": 20 }
```

Book a ride. `couponCode` may be omitted.

```json
{
  "userId": 1,
  "pickupLatitude": 12.9716,
  "pickupLongitude": 77.5946,
  "carType": "HATCHBACK",
  "couponCode": "SAVE20"
}
```

Booking response fields: `rideId`, `driverId`, `cabId`, `requestedCarType`, `assignedCarType`, `pricingCarType`, `status`.

End a ride:

```json
{ "distanceKm": 7 }
```

End response fields: `rideId`, `status`, `distanceKm`, `surgeMultiplier`, `originalFare`, `discountAmount`, `finalFare`.

History fields: `rideId`, `userId`, `driverId`, `requestedCarType`, `assignedCarType`, `status`, `pickupLatitude`, `pickupLongitude`, `distanceKm`, `finalFare`. Distance and fare are null while the ride is ongoing.

Matching uses haversine distance and `ride.matching.radius-km` (5 km, inclusive). Among cabs of the requested type, the nearest wins. An equal distance keeps the lower driver id. A hatchback request may use the nearest sedan when no hatchback is eligible. A sedan request has no hatchback fallback.

The Postman collection `RideHailing-Demo.postman_collection.json` and environment `RideHailing-Local.postman_environment.json` exercise this contract in demo order. The narration is in `DEMO_GUIDE.md`.

## Pricing strategy with examples

`PricingStrategy` has one implementation per car type. `TieredFareCalculator` prices three slabs, applies the minimum, and rounds half up to 2 decimals.

| | Hatchback | Sedan |
|---|---|---|
| Minimum | Rs 50.00 | Rs 70.00 |
| First 2 km | Rs 10 / km | Rs 15 / km |
| Next 3 km | Rs 8 / km | Rs 12 / km |
| Beyond 5 km | Rs 5 / km | Rs 8 / km |

- 1 km hatchback is 1 × 10 = Rs 10, so the minimum makes it **Rs 50.00**.
- 7 km hatchback is 2 × 10 + 3 × 8 + 2 × 5 = **Rs 54.00**.
- 7 km sedan is 2 × 15 + 3 × 12 + 2 × 8 = **Rs 82.00**.
- 16.2 km hatchback is 2 × 10 + 3 × 8 + 11.2 × 5 = **Rs 100.00**.

Completion order:

1. Tiered fare for `pricingCarType`, including the minimum.
2. Multiply by the surge multiplier stored on the ride.
3. Apply the snapshotted coupon percent.
4. Round each money result to 2 decimals.

`originalFare` is the amount after surge and before the coupon. With no coupon, `discountAmount` is 0.00 and `finalFare` equals `originalFare`.

## Free Sedan upgrade

A hatchback request assigns a sedan only when no hatchback is available inside the radius. `assignedCarType` is then SEDAN. `pricingCarType` stays HATCHBACK, so a 7 km upgraded ride is Rs 54.00 before surge, not Rs 82.00. A sedan request never assigns a hatchback.

## Coupon application

The coupon is checked before a driver is claimed. An unknown code returns 400 and leaves every driver available. A valid code copies `couponCode` and `discountPercentage` onto the ride. Deleting the coupon later does not change that copy.

The discount is `originalFare × percentage / 100`, applied to the surged fare. It is not applied to the pre-surge slabs. A negative result is clamped to 0.00. Twenty percent off a surged fare of Rs 150.00 is a discount of Rs 30.00 and a final fare of Rs 120.00.

## Concurrency safety implementation

Two bookings can both see the same driver as available. The claim does not rely on a JVM lock. `DriverRepository.claimIfAvailable` runs:

```sql
UPDATE Driver d
SET d.available = false
WHERE d.id = :driverId AND d.available = true
```

The update returns the row count. Exactly one transaction gets 1. The booking and the claim share one transaction, so a failed claim does not leave a ride behind. If the chosen driver was taken, the service tries the next ranked candidate, including a sedan upgrade for a hatchback request. When none can be claimed, the API returns 409.

The update clears the persistence context, so a driver loaded earlier cannot be flushed back as available. The user and the claimed driver are reloaded before the ride is saved.

`ConcurrentRideBookingTest` starts two bookings together with an executor and a latch. It expects one 201, one 409, one ongoing ride, and the driver unavailable. Sequential HTTP calls do not show this, because the second call begins after the first transaction has finished.

## Surge pricing implementation

`SurgeMultiplierProvider` is the plug-in. `DemandSupplySurgeProvider` is the implementation used at booking.

For the pickup, inside the same radius as matching:

- Demand is booking attempts in the last 5 minutes, including this attempt.
- Supply is the count of available nearby drivers passed in before the claim.

| Ratio | Multiplier |
|---|---|
| below 2 | 1.00 |
| 2 up to, but not including, 4 | 1.25 |
| 4 or more | 1.50 |

Zero supply with any demand uses 1.50, so the ratio is never a division by zero. The booking still returns 409 when no driver can be claimed. Zero demand and zero supply use 1.00.

The multiplier is saved on the ride. Completion multiplies the tiered fare by that stored value. A later change in nearby drivers does not change it.

Worked example: 16.2 km hatchback is Rs 100.00, multiplier 1.50, coupon 20 percent. Original fare Rs 150.00, discount Rs 30.00, final fare Rs 120.00.

Settings in `application.properties`:

```properties
ride.matching.radius-km=5
ride.surge.window-minutes=5
ride.surge.medium-ratio=2
ride.surge.high-ratio=4
ride.surge.medium-multiplier=1.25
ride.surge.high-multiplier=1.5
```

Recent requests are kept in a synchronized in-memory queue. Each quote drops entries older than the window, appends the current point, and counts points inside the radius.

## Assumptions and limitations

- One driver has one cab. New drivers start available. Phone numbers are not unique.
- The matching radius and the surge area are the same 5 km circle around the pickup.
- Demand includes the current attempt. It also includes an attempt that later returns 409 because no driver could be claimed. A missing user or an invalid coupon is rejected earlier and is not counted.
- Supply is the nearby available-driver count from the booking service. It is not read again inside the surge provider.
- The demand queue belongs to one JVM. It is lost on restart, it is not shared by other instances, and a rolled-back transaction does not remove a recorded attempt.
- Expired points are removed on the next quote, from the front of a time-ordered queue. Counting a window is a linear scan.
- Supply and demand are not one database snapshot.
- H2 data is also lost on restart. `create-drop` rebuilds the schema when the process starts.
- There is no authentication, cancellation, payment, or driver rating.

## Design decisions and trade-offs

- Fare rules sit behind `PricingStrategy` so hatchback and sedan slabs can change without rewriting completion.
- Surge sits behind `SurgeMultiplierProvider` so another rule can replace demand/supply without changing the booking flow.
- Coupon percent and surge multiplier are copied onto the ride. Later edits to a coupon, or a change in nearby supply, do not rewrite a booked price.
- The driver claim is a conditional update so two transactions cannot both win the same driver. A synchronized block around the service would not hold across more than one application instance.
- The demand queue is in memory so the demo needs no extra service. The cost is that surge state disappears on restart and is wrong if two instances run at once.
- DTOs keep the JSON stable and avoid returning both sides of the driver-cab relationship.
- Free upgrade changes the assigned cab and leaves the priced cab type as the type the rider requested.

## Production improvements

- Replace the in-memory H2 database with a durable database and managed migrations.
- Put surge counts in a shared store with a time-to-live, so every instance sees the same window and a restart does not wipe demand.
- Index driver locations for a radius query instead of loading every available driver and computing haversine in the service.
- Add authentication and authorize rider, driver, and admin actions.
- Add cancellation, payment, and an immutable fare breakdown stored with the ride.
- Keep the conditional claim; add a retry limit and metrics for how often the next candidate is used.
- Publish ride events for notifications and history reads, and add tracing, structured logs, and health checks that include the database.

## Test instructions

JDK 21 is required. From the project root:

```powershell
.\mvnw.cmd clean test
```

The suite covers registration, location updates, matching, tiered fares, history, coupons, concurrent booking, and surge. The concurrent race is only in `ConcurrentRideBookingTest`:

```powershell
.\mvnw.cmd -Dtest=ConcurrentRideBookingTest test
```

`ConcurrentRideBookingTest` uses MockMvc in its own process. It does not call a server started with `spring-boot:run`, and it does not need port 8080.

Run the application with:

```powershell
.\mvnw.cmd spring-boot:run
```

Import the Postman collection and the local environment, select **Ride Hailing Local**, and follow `DEMO_GUIDE.md`. Restart the process before a second full run.

## How AI/Cursor was used

### AI Tool Usage

Cursor was used to accelerate development, generate boilerplate, and assist with implementation.

Design decisions, business logic, and correctness were evaluated through code review and automated testing.
