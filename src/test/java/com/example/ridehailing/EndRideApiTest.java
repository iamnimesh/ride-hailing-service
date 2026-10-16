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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndRideApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shortRideChargesMinimumFareAndReleasesDriver() throws Exception {
		long userId = registerUser("Asha", "9200000001");
		long driverId = registerDriver("Kiran", "9200000101", "HATCHBACK", 48.0, 2.0);
		long rideId = book(userId, 48.0, 2.0, "HATCHBACK");

		mockMvc.perform(get("/drivers/{id}", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":1.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rideId").value(rideId))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.distanceKm").value(1.0))
				.andExpect(jsonPath("$.finalFare").value(50.00))
				.andExpect(content().json("""
						{"rideId":%d,"status":"COMPLETED","distanceKm":1.0,"finalFare":50.00}
						""".formatted(rideId)));

		mockMvc.perform(get("/drivers/{id}", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void hatchbackAndSedanUseDifferentTieredFares() throws Exception {
		long hatchbackUser = registerUser("Meera", "9200000002");
		long sedanUser = registerUser("Ravi", "9200000003");
		registerDriver("Hatch", "9200000102", "HATCHBACK", 55.0, 10.0);
		registerDriver("Sedan", "9200000103", "SEDAN", 56.0, 10.0);
		long hatchbackRide = book(hatchbackUser, 55.0, 10.0, "HATCHBACK");
		long sedanRide = book(sedanUser, 56.0, 10.0, "SEDAN");

		mockMvc.perform(post("/rides/{rideId}/end", hatchbackRide)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.finalFare").value(54.00))
				.andExpect(content().json("""
						{"rideId":%d,"status":"COMPLETED","distanceKm":7.0,"finalFare":54.00}
						""".formatted(hatchbackRide)));

		mockMvc.perform(post("/rides/{rideId}/end", sedanRide)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.finalFare").value(82.00));
	}

	@Test
	void freeUpgradeChargesHatchbackFare() throws Exception {
		long userId = registerUser("Nisha", "9200000004");
		long sedanId = registerDriver("Upgrade", "9200000104", "SEDAN", 60.01, 20.0);
		registerDriver("Outside", "9200000105", "HATCHBACK", 60.2, 20.0);
		long rideId = book(userId, 60.0, 20.0, "HATCHBACK");

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.finalFare").value(54.00))
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		mockMvc.perform(get("/drivers/{id}", sedanId))
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void endingCompletedRideReturnsConflict() throws Exception {
		long userId = registerUser("Dev", "9200000005");
		registerDriver("Taken", "9200000106", "HATCHBACK", 62.0, -5.0);
		long rideId = book(userId, 62.0, -5.0, "HATCHBACK");

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ride already completed: " + rideId));
	}

	@Test
	void rejectsNegativeDistance() throws Exception {
		long userId = registerUser("Isha", "9200000006");
		registerDriver("Near", "9200000107", "HATCHBACK", 63.0, -8.0);
		long rideId = book(userId, 63.0, -8.0, "HATCHBACK");

		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":-1}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Distance must be a non-negative finite number"));
	}

	@Test
	void missingRideReturnsNotFound() throws Exception {
		mockMvc.perform(post("/rides/{rideId}/end", 999999)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":7.0}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Ride not found: 999999"));
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

	private long book(long userId, double latitude, double longitude, String carType) throws Exception {
		MvcResult created = mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":%s,"pickupLongitude":%s,"carType":"%s"}
								""".formatted(userId, latitude, longitude, carType)))
				.andExpect(status().isCreated())
				.andReturn();
		return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.rideId")).longValue();
	}

}
