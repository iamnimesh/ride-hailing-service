package com.example.ridehailing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SurgePricingApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void normalDemandKeepsBaseFare() throws Exception {
		long userId = registerUser("Normal", "9600000001");
		registerDriver("NormalDriver", "9600000101", "HATCHBACK", 5.0, -140.0);
		long rideId = book(userId, 5.0, -140.0, "HATCHBACK", null);

		end(rideId, 1.0, 1.0, 50.00, 0.00, 50.00);
	}

	@Test
	void mediumDemandAppliesOneAndAQuarter() throws Exception {
		long userId = registerUser("Medium", "9600000002");
		registerDriver("MediumDriver", "9600000102", "HATCHBACK", 15.0, -140.0);
		end(book(userId, 15.0, -140.0, "HATCHBACK", null), 7.0);
		long surgedRideId = book(userId, 15.0, -140.0, "HATCHBACK", null);

		end(surgedRideId, 7.0, 1.25, 67.50, 0.00, 67.50);
	}

	@Test
	void highDemandAppliesOneAndAHalf() throws Exception {
		long userId = registerUser("High", "9600000003");
		registerDriver("HighDriver", "9600000103", "HATCHBACK", 25.0, -140.0);
		for (int i = 0; i < 3; i++) {
			end(book(userId, 25.0, -140.0, "HATCHBACK", null), 1.0);
		}
		long surgedRideId = book(userId, 25.0, -140.0, "HATCHBACK", null);

		end(surgedRideId, 7.0, 1.5, 81.00, 0.00, 81.00);
	}

	@Test
	void surgeIsDiscountedByCoupon() throws Exception {
		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"SURGE20","discountPercentage":20}
								"""))
				.andExpect(status().isCreated());
		long userId = registerUser("Coupon", "9600000004");
		registerDriver("CouponDriver", "9600000104", "HATCHBACK", 35.0, -140.0);
		for (int i = 0; i < 3; i++) {
			end(book(userId, 35.0, -140.0, "HATCHBACK", null), 1.0);
		}
		long surgedRideId = book(userId, 35.0, -140.0, "HATCHBACK", "SURGE20");

		end(surgedRideId, 1.0, 1.5, 75.00, 15.00, 60.00);
	}

	@Test
	void freeUpgradeUsesHatchbackFareWithSurge() throws Exception {
		long userId = registerUser("Upgrade", "9600000005");
		registerDriver("UpgradeSedan", "9600000105", "SEDAN", 45.0, -140.0);
		end(book(userId, 45.0, -140.0, "HATCHBACK", null), 1.0);
		MvcResult surged = mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":45.0,"pickupLongitude":-140.0,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.assignedCarType").value("SEDAN"))
				.andExpect(jsonPath("$.pricingCarType").value("HATCHBACK"))
				.andReturn();
		long rideId = ((Number) JsonPath.read(surged.getResponse().getContentAsString(), "$.rideId")).longValue();

		end(rideId, 1.0, 1.25, 62.50, 0.00, 62.50);
	}

	@Test
	void surgeMultiplierStaysFixedAfterBooking() throws Exception {
		long userId = registerUser("Fixed", "9600000006");
		registerDriver("FixedDriver", "9600000106", "HATCHBACK", 55.0, -140.0);
		end(book(userId, 55.0, -140.0, "HATCHBACK", null), 1.0);
		long surgedRideId = book(userId, 55.0, -140.0, "HATCHBACK", null);

		end(surgedRideId, 1.0, 1.25, 62.50, 0.00, 62.50);
	}

	private void end(long rideId, double distanceKm) throws Exception {
		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":%s}
								""".formatted(distanceKm)))
				.andExpect(status().isOk());
	}

	private void end(long rideId, double distanceKm, double surgeMultiplier, double originalFare, double discountAmount,
			double finalFare) throws Exception {
		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":%s}
								""".formatted(distanceKm)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.surgeMultiplier").value(surgeMultiplier))
				.andExpect(jsonPath("$.originalFare").value(originalFare))
				.andExpect(jsonPath("$.discountAmount").value(discountAmount))
				.andExpect(jsonPath("$.finalFare").value(finalFare))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	private long registerUser(String name, String phone) throws Exception {
		MvcResult created = mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","phone":"%s"}
								""".formatted(name, phone)))
				.andExpect(status().isCreated())
				.andReturn();
		return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private void registerDriver(String name, String phone, String carType, double latitude, double longitude)
			throws Exception {
		mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","phone":"%s","carType":"%s","latitude":%s,"longitude":%s}
								""".formatted(name, phone, carType, latitude, longitude)))
				.andExpect(status().isCreated());
	}

	private long book(long userId, double latitude, double longitude, String carType, String couponCode)
			throws Exception {
		String couponField = couponCode == null ? "" : ",\"couponCode\":\"%s\"".formatted(couponCode);
		MvcResult created = mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":%s,"pickupLongitude":%s,"carType":"%s"%s}
								""".formatted(userId, latitude, longitude, carType, couponField)))
				.andExpect(status().isCreated())
				.andReturn();
		return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.rideId")).longValue();
	}

}
