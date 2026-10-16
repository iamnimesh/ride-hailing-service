# 8-minute demo

Restart the application before this script. A restart clears H2 and the in-memory surge window. Select the Postman environment **Ride Hailing Local**, then run the collection from the top, or send the named requests in order.

## Start (0:00–0:30)

From the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait until the process is listening on port 8080. In Postman, send **A1 Confirm the application is up**.

Expected: `200` and `{ "status": "UP" }`.

## Setup (0:30–1:30)

Send A2 through A8.

| Request | Expected |
|---|---|
| A2 Register user Asha | `201`, `id`, `name` Asha, `phone` 9100000001. Saved as `userAId`. |
| A3 Register user Meera | `201`. Saved as `userBId`. |
| A4 Register user Ravi | `201`. Saved as `userCId`. |
| A5 Register hatchback driver | `201`, `available` true, `cab.carType` HATCHBACK. Saved as `hatchbackDriverId` and `hatchbackCabId`. |
| A6 Register sedan driver | `201`, `cab.carType` SEDAN. The sedan is registered far from the first pickup. |
| A7 Update hatchback cab location | `200`. Cab moves to latitude 12.9716, longitude 77.5946. |
| A8 Create coupon SAVE20 | `201`, `code` SAVE20, `discountPercentage` 20. |

Say that registration returns the generated ids, and later requests use those variables.

## Happy path (1:30–3:00)

Send B1 through B6. One hatchback is inside 5 km, and this is the first request in that area, so the surge multiplier is 1.00.

| Request | What to show |
|---|---|
| B1 Book hatchback ride | `201`. `driverId` is the hatchback. `requestedCarType`, `assignedCarType`, and `pricingCarType` are HATCHBACK. `status` is ONGOING. |
| B2 Verify driver assignment | `200`. `available` is false. |
| B3 End ride at 7 km and verify fare | `200`. `status` COMPLETED, `distanceKm` 7, `surgeMultiplier` 1.00, `discountAmount` 0.00. The 7 km hatchback fare is 2×10 + 3×8 + 2×5 = Rs 54.00. At 1.00x, `originalFare` and `finalFare` are 54.00. |
| B4 Get user ride history | `200`. Newest first. The ride is COMPLETED with `distanceKm` 7 and the same `finalFare`. |
| B5 Get driver ride history | `200`. The same ride is on the driver's list. |
| B6 Verify driver is available again | `200`. `available` is true. |

## Edge cases (3:00–5:30)

Show these four live. The collection runner still executes the rest.

1. **C1 No driver within radius.** Booking at 0, 0 returns `409` and `No eligible driver found`.
2. **C2 Invalid user.** User 999999 returns `404` and `User not found: 999999`. This fails before a driver is claimed.
3. **C3 Invalid coupon**, then **C4**. Coupon `NOPE` returns `400` and `Invalid coupon: NOPE`. The hatchback stays `available: true`.
4. **C5**, **C6**, **C7**, **C8**. C5 holds the only nearby hatchback. C6 returns `409` because the sedan is outside the radius, so there is no free upgrade. C7 is the reset: it ends the held ride and releases the driver. C8 ends that same ride again and returns `409` with `Ride already completed`.

If there is time, continue:

- **C9–C11 Minimum fare.** The cab is moved to a fresh area. A 1 km ride has slab fare Rs 10, so the Rs 50.00 minimum applies. `surgeMultiplier` is 1.00, and `finalFare` is 50.00.
- **C12–C14 Free upgrade.** Only the sedan is near 16.00, 77.60. The booking shows `assignedCarType` SEDAN and `pricingCarType` HATCHBACK. The 7 km fare uses the hatchback base of Rs 54.00, not the sedan fare of Rs 82.00.
- **C15–C16 Delete coupon.** DELETE `/coupons/SAVE20` returns `204`. Booking with SAVE20 afterward returns `400`.

## Surge (5:30–7:15)

Demand is booking attempts in the last 5 minutes inside 5 km, including the current attempt. Supply is available drivers nearby before the claim. The multiplier is stored on the ride. Ending the ride does not calculate it again.

Each band starts by moving the hatchback to a new latitude. That move is the reset for that band. Repeating a band inside 5 minutes changes the ratio, so restart the app before running folder D again.

| Requests | Demand / supply | Multiplier | Check |
|---|---|---|---|
| D1–D3 | 1 / 1 | 1.00 | 7 km base Rs 54.00 × 1.00 |
| D4–D8 | first request, then 2 / 1 | 1.25 on D8 | 54.00 × returned multiplier |
| D9–D17 | three priming requests, then 4 / 1 | 1.50 on D17 | 54.00 × returned multiplier |
| D18–D27 | three new priming requests, then 4 / 1 with SURGE20 | 1.50 | 16.2 km base is Rs 100.00. Then × 1.50, then 20% off. |

On D27, point at the fields rather than a hardcoded total:

- `surgeMultiplier` 1.50
- `originalFare` = 100.00 × 1.50 = 150.00
- `discountAmount` = 30.00
- `finalFare` = 120.00

The Postman test computes those amounts from the returned multiplier.

## Concurrency (7:15–8:00)

Do not send two book requests by hand and call that the race. Send **D28** only to show the app is up, then from another terminal run:

```powershell
.\mvnw.cmd -Dtest=ConcurrentRideBookingTest test
```

Say what the test proves: two bookings start together against one driver; exactly one returns 201; the other returns 409; the driver is unavailable; exactly one ride is ongoing. The winner is the transaction whose conditional update changes one row: set `available` false only where `available` is still true.

## Reset

Stop `spring-boot:run` and start it again.

That is the only reset. H2 is in-memory with `create-drop`, and surge demand lives in the application process. Both disappear on restart. There is no HTTP call that clears them.

Inside one run, these requests are the local resets:

- C7 ends the held ride so the hatchback can be booked again.
- C9, C12, D1, D4, D9, and D19 move a cab into an area that has no demand yet.
