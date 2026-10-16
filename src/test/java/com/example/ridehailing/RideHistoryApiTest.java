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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideHistoryApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void userSeesOngoingAndCompletedRidesLatestFirst() throws Exception {
		long userId = registerUser("Asha", "9300000001");
		registerDriver("Kiran", "9300000101", "HATCHBACK", 25.0, -50.0);
		long completedRideId = book(userId, 25.0, -50.0, "HATCHBACK");
		end(completedRideId, 7.0);
		long ongoingRideId = book(userId, 25.0, -50.0, "HATCHBACK");

		mockMvc.perform(get("/users/{id}/rides", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].rideId").value(ongoingRideId))
				.andExpect(jsonPath("$[0].userId").value(userId))
				.andExpect(jsonPath("$[0].requestedCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$[0].assignedCarType").value("HATCHBACK"))
				.andExpect(jsonPath("$[0].status").value("ONGOING"))
				.andExpect(jsonPath("$[0].pickupLatitude").value(25.0))
				.andExpect(jsonPath("$[0].pickupLongitude").value(-50.0))
				.andExpect(jsonPath("$[0].distanceKm").value(nullValue()))
				.andExpect(jsonPath("$[0].finalFare").value(nullValue()))
				.andExpect(jsonPath("$[1].rideId").value(completedRideId))
				.andExpect(jsonPath("$[1].status").value("COMPLETED"))
				.andExpect(jsonPath("$[1].distanceKm").value(7.0))
				.andExpect(jsonPath("$[1].finalFare").value(54.00));
	}

	@Test
	void driverSeesAssignedOngoingAndCompletedRides() throws Exception {
		long userId = registerUser("Meera", "9300000002");
		long otherUserId = registerUser("Ravi", "9300000003");
		long driverId = registerDriver("Neha", "9300000102", "SEDAN", -25.0, -50.0);
		long otherDriverId = registerDriver("Aman", "9300000103", "SEDAN", -27.0, -50.0);
		long completedRideId = book(userId, -25.0, -50.0, "SEDAN");
		end(completedRideId, 7.0);
		long ongoingRideId = book(userId, -25.0, -50.0, "SEDAN");
		long otherRideId = book(otherUserId, -27.0, -50.0, "SEDAN");

		mockMvc.perform(get("/drivers/{id}/rides", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].rideId").value(ongoingRideId))
				.andExpect(jsonPath("$[0].driverId").value(driverId))
				.andExpect(jsonPath("$[0].userId").value(userId))
				.andExpect(jsonPath("$[0].status").value("ONGOING"))
				.andExpect(jsonPath("$[0].distanceKm").value(nullValue()))
				.andExpect(jsonPath("$[0].finalFare").value(nullValue()))
				.andExpect(jsonPath("$[1].rideId").value(completedRideId))
				.andExpect(jsonPath("$[1].driverId").value(driverId))
				.andExpect(jsonPath("$[1].status").value("COMPLETED"))
				.andExpect(jsonPath("$[1].distanceKm").value(7.0))
				.andExpect(jsonPath("$[1].finalFare").value(82.00));

		mockMvc.perform(get("/drivers/{id}/rides", otherDriverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].rideId").value(otherRideId))
				.andExpect(jsonPath("$[0].driverId").value(otherDriverId));
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

	private void end(long rideId, double distanceKm) throws Exception {
		mockMvc.perform(post("/rides/{rideId}/end", rideId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"distanceKm":%s}
								""".formatted(distanceKm)))
				.andExpect(status().isOk());
	}

}
