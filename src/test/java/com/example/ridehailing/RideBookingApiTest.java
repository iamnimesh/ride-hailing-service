package com.example.ridehailing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideBookingApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void bookRideAssignsMatchingDriverAndMarksDriverUnavailable() throws Exception {
		long userId = registerUser("Asha", "9100000001");
		long driverId = registerDriver("Kiran", "9100000101", "HATCHBACK", 12.0, -120.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":12.0,"pickupLongitude":-120.0,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rideId").isNumber())
				.andExpect(jsonPath("$.driverId").value(driverId))
				.andExpect(jsonPath("$.cabId").isNumber())
				.andExpect(jsonPath("$.requestedCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$.assignedCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$.pricingCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$.status").value("ONGOING"));

		mockMvc.perform(get("/drivers/{id}", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void selectsNearestDriverOfRequestedCarType() throws Exception {
		long userId = registerUser("Meera", "9100000002");
		long nearerHatchback = registerDriver("Near", "9100000102", "HATCHBACK", -32.99, 151.0);
		long fartherHatchback = registerDriver("Far", "9100000103", "HATCHBACK", -32.97, 151.0);
		long closerSedan = registerDriver("CloseSedan", "9100000104", "SEDAN", -33.0, 151.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":-33.0,"pickupLongitude":151.0,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.driverId").value(nearerHatchback))
				.andExpect(jsonPath("$.assignedCarType").value("HATCHBACK"));

		mockMvc.perform(get("/drivers/{id}", fartherHatchback))
				.andExpect(jsonPath("$.available").value(true));
		mockMvc.perform(get("/drivers/{id}", closerSedan))
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void returnsConflictWhenNoDriverIsWithinRadius() throws Exception {
		long userId = registerUser("Isha", "9100000003");
		registerDriver("Distant", "9100000105", "HATCHBACK", 64.1, -21.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":64.0,"pickupLongitude":-21.0,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No eligible driver found"));
	}

	@Test
	void upgradesHatchbackRequestToNearestSedan() throws Exception {
		long userId = registerUser("Nisha", "9100000004");
		long sedanId = registerDriver("Upgrade", "9100000106", "SEDAN", 35.71, 139.7);
		long farHatchback = registerDriver("Outside", "9100000107", "HATCHBACK", 35.9, 139.7);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":35.7,"pickupLongitude":139.7,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.driverId").value(sedanId))
				.andExpect(jsonPath("$.requestedCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$.assignedCarType").value("SEDAN"))
				.andExpect(jsonPath("$.pricingCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$.status").value("ONGOING"));

		mockMvc.perform(get("/drivers/{id}", sedanId))
				.andExpect(jsonPath("$.available").value(false));
		mockMvc.perform(get("/drivers/{id}", farHatchback))
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void sedanRequestDoesNotAssignHatchback() throws Exception {
		long userId = registerUser("Tara", "9100000005");
		long hatchbackId = registerDriver("OnlyHatch", "9100000108", "HATCHBACK", -33.9, 18.4);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":-33.9,"pickupLongitude":18.4,"carType":"SEDAN"}
								""".formatted(userId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No eligible driver found"));

		mockMvc.perform(get("/drivers/{id}", hatchbackId))
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void alreadyAssignedDriverIsNotAssignedAgain() throws Exception {
		long firstUser = registerUser("Dev", "9100000006");
		long secondUser = registerUser("Aman", "9100000007");
		long nearerDriver = registerDriver("Taken", "9100000109", "HATCHBACK", 41.0, -74.0);
		long fartherDriver = registerDriver("Next", "9100000110", "HATCHBACK", 41.02, -74.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":41.0,"pickupLongitude":-74.0,"carType":"HATCHBACK"}
								""".formatted(firstUser)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.driverId").value(nearerDriver));

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":41.0,"pickupLongitude":-74.0,"carType":"HATCHBACK"}
								""".formatted(secondUser)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.driverId").value(fartherDriver));

		mockMvc.perform(get("/drivers/{id}", nearerDriver))
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void usesLowerDriverIdWhenDistancesAreEqual() throws Exception {
		long userId = registerUser("Rhea", "9100000008");
		long lowerId = registerDriver("First", "9100000111", "SEDAN", -15.0, 30.0);
		long higherId = registerDriver("Second", "9100000112", "SEDAN", -15.0, 30.0);

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":-15.0,"pickupLongitude":30.0,"carType":"SEDAN"}
								""".formatted(userId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.driverId").value(lowerId));

		mockMvc.perform(get("/drivers/{id}", higherId))
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void returnsNotFoundWhenUserDoesNotExist() throws Exception {
		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":999999,"pickupLatitude":28.6,"pickupLongitude":77.2,"carType":"HATCHBACK"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User not found: 999999"));
	}

	@Test
	void rejectsInvalidCoordinates() throws Exception {
		long userId = registerUser("Invalid", "9100000009");

		mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":91,"pickupLongitude":77.2,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
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

}
