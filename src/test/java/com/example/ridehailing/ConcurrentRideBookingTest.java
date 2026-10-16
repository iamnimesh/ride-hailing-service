package com.example.ridehailing;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentRideBookingTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void onlyOneConcurrentBookingClaimsTheDriver() throws Exception {
		long firstUserId = registerUser("ConcurrentA", "9500000001");
		long secondUserId = registerUser("ConcurrentB", "9500000002");
		long driverId = registerDriver("OnlyDriver", "9500000101", "HATCHBACK", 80.5, 45.5);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch start = new CountDownLatch(1);
			Future<Integer> first = executor.submit(() -> bookWhenReleased(firstUserId, ready, start));
			Future<Integer> second = executor.submit(() -> bookWhenReleased(secondUserId, ready, start));
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();

			List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
			assertEquals(1, statuses.stream().filter(status -> status == 201).count(), "statuses=" + statuses);
			assertEquals(1, statuses.stream().filter(status -> status == 409).count(), "statuses=" + statuses);
		} finally {
			executor.shutdownNow();
		}

		mockMvc.perform(get("/drivers/{id}", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
		mockMvc.perform(get("/drivers/{id}/rides", driverId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("ONGOING"));
		assertEquals(1, rideCount(firstUserId) + rideCount(secondUserId));
	}

	private int bookWhenReleased(long userId, CountDownLatch ready, CountDownLatch start) throws Exception {
		ready.countDown();
		assertTrue(start.await(5, TimeUnit.SECONDS));
		MvcResult result = mockMvc.perform(post("/rides/book")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":%d,"pickupLatitude":80.5,"pickupLongitude":45.5,"carType":"HATCHBACK"}
								""".formatted(userId)))
				.andReturn();
		return result.getResponse().getStatus();
	}

	private int rideCount(long userId) throws Exception {
		MvcResult result = mockMvc.perform(get("/users/{id}/rides", userId))
				.andExpect(status().isOk())
				.andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.length()")).intValue();
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
