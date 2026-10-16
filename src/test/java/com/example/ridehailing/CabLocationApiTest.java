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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CabLocationApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void updateLocationReturnsUpdatedCoordinates() throws Exception {
		RegisteredDriver driver = registerDriver();

		mockMvc.perform(patch("/cabs/{id}/location", driver.cabId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":13.25,"longitude":-77.5}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(driver.cabId()))
				.andExpect(jsonPath("$.driverId").value(driver.driverId()))
				.andExpect(jsonPath("$.carType").value("HATCHBACK"))
				.andExpect(jsonPath("$.latitude").value(13.25))
				.andExpect(jsonPath("$.longitude").value(-77.5));

		mockMvc.perform(get("/drivers/{id}", driver.driverId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cab.latitude").value(13.25))
				.andExpect(jsonPath("$.cab.longitude").value(-77.5));
	}

	@Test
	void updateLocationAcceptsCoordinateBoundaries() throws Exception {
		RegisteredDriver driver = registerDriver();

		mockMvc.perform(patch("/cabs/{id}/location", driver.cabId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":-90.0,"longitude":180.0}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(-90.0))
				.andExpect(jsonPath("$.longitude").value(180.0));
	}

	@Test
	void updateLocationRejectsOutOfRangeLatitude() throws Exception {
		RegisteredDriver driver = registerDriver();

		mockMvc.perform(patch("/cabs/{id}/location", driver.cabId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":-90.1,"longitude":10}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void updateMissingCabReturnsNotFound() throws Exception {
		mockMvc.perform(patch("/cabs/{id}/location", 999999)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":1.0,"longitude":2.0}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Cab not found: 999999"));
	}

	private RegisteredDriver registerDriver() throws Exception {
		MvcResult created = mockMvc.perform(post("/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Kiran","phone":"9000000003","carType":"HATCHBACK","latitude":1.5,"longitude":2.5}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String body = created.getResponse().getContentAsString();
		long driverId = ((Number) JsonPath.read(body, "$.id")).longValue();
		long cabId = ((Number) JsonPath.read(body, "$.cab.id")).longValue();
		return new RegisteredDriver(driverId, cabId);
	}

	private record RegisteredDriver(long driverId, long cabId) {
	}

}
