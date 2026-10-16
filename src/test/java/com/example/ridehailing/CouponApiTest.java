package com.example.ridehailing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void validCouponAppliesDiscountAfterMinimumFare() throws Exception {
		createCoupon("Save20", 20);
		long userId = registerUser("Asha", "9400000001");
		registerDriver("Kiran", "9400000101", "HATCHBACK", 10.0, 140.0);
		long rideId = book(userId, 10.0, 140.0, "HATCHBACK", "SAVE20");

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":1.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rideId").value(rideId))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.originalFare").value(50.00))
				.andExpect(jsonPath("$.discountAmount").value(10.00))
				.andExpect(jsonPath("$.finalFare").value(40.00));
	}

	@Test
	void invalidCouponDoesNotAssignDriver() throws Exception {
		long userId = registerUser("Meera", "9400000002");
		long driverId = registerDriver("Neha", "9400000102", "HATCHBACK", 11.5, 140.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":11.5,"pickupLongitude":140.0,"carType":"HATCHBACK","couponCode":"NOPE"}
								""".formatted(userId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid coupon: NOPE"));

		mockMvc.perform(get("/drivers/{id}", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(true));
		mockMvc.perform(get("/users/{id}/rides", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void rideWithoutCouponKeepsNormalFare() throws Exception {
		long userId = registerUser("Ravi", "9400000003");
		registerDriver("Aman", "9400000103", "HATCHBACK", 8.0, 140.0);
		long rideId = book(userId, 8.0, 140.0, "HATCHBACK", null);

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFare").value(54.00))
				.andExpect(jsonPath("$.discountAmount").value(0.00))
				.andExpect(jsonPath("$.finalFare").value(54.00))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void deletingCouponDoesNotChangeSnapshottedDiscount() throws Exception {
		createCoupon("SNAP20", 20);
		long userId = registerUser("Nisha", "9400000004");
		registerDriver("Dev", "9400000104", "HATCHBACK", 6.0, 140.0);
		long rideId = book(userId, 6.0, 140.0, "HATCHBACK", "snap20");

		mockMvc.perform(delete("/coupons/{code}", "SNAP20"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFare").value(54.00))
				.andExpect(jsonPath("$.discountAmount").value(10.80))
				.andExpect(jsonPath("$.finalFare").value(43.20))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	private void createCoupon(String code, int discountPercentage) throws Exception {
		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"%s","discountPercentage":%d}
								""".formatted(code, discountPercentage)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(code.trim().toUpperCase()));
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

	private long registerDriver(String name, String phone, String carType, double latitude, double longitude)
			throws Exception {
		MvcResult created = mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","phone":"%s","carType":"%s","latitude":%s,"longitude":%s}
								""".formatted(name, phone, carType, latitude, longitude)))
				.andExpect(status().isCreated())
				.andReturn();
		return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
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
