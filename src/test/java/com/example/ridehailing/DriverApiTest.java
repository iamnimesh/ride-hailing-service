package com.example.ridehailing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class DriverApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registerDriverCreatesAvailableDriverWithCab() throws Exception {
		MvcResult created = mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Ravi","phone":"9988776655","carType":"SEDAN","latitude":12.5,"longitude":77.25}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ravi"))
				.andExpect(jsonPath("$.phone").value("9988776655"))
				.andExpect(jsonPath("$.available").value(true))
				.andExpect(jsonPath("$.cab.carType").value("SEDAN"))
				.andExpect(jsonPath("$.cab.latitude").value(12.5))
				.andExpect(jsonPath("$.cab.longitude").value(77.25))
				.andReturn();

		String body = created.getResponse().getContentAsString();
		long driverId = ((Number) JsonPath.read(body, "$.id")).longValue();
		long cabDriverId = ((Number) JsonPath.read(body, "$.cab.driverId")).longValue();
		assertEquals(driverId, cabDriverId);
	}

	@Test
	void getDriverReturnsRegisteredDriver() throws Exception {
		MvcResult created = mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Neha","phone":"9000000001","carType":"HATCHBACK","latitude":-90.0,"longitude":180.0}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		long id = idOf(created);

		mockMvc.perform(get("/drivers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.name").value("Neha"))
				.andExpect(jsonPath("$.available").value(true))
				.andExpect(jsonPath("$.cab.carType").value("HATCHBACK"))
				.andExpect(jsonPath("$.cab.latitude").value(-90.0))
				.andExpect(jsonPath("$.cab.longitude").value(180.0));
	}

	@Test
	void registerDriverStaysAvailableWhenClientSendsAvailableFalse() throws Exception {
		mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Asha","phone":"9000000002","carType":"SEDAN","latitude":0,"longitude":0,"available":false}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void registerDriverRejectsUnknownCarType() throws Exception {
		mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Ravi","phone":"9988776655","carType":"SUV","latitude":12.5,"longitude":77.25}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request body"));
	}

	@Test
	void registerDriverRejectsOutOfRangeCoordinates() throws Exception {
		mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Ravi","phone":"9988776655","carType":"SEDAN","latitude":90.1,"longitude":-180.1}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void getMissingDriverReturnsNotFound() throws Exception {
		mockMvc.perform(get("/drivers/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Driver not found: 999999"));
	}

	private static long idOf(MvcResult result) throws Exception {
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
