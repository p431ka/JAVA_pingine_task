package com.pingine.fleetpulse.api;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.service.TripService;
import com.pingine.fleetpulse.exception.VehicleNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
class TripControllerTest {

    private final String VEHICLE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private final String LAST_TRIP_URL = "/api/v1/vehicles/{vehicleId}/last-trip";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @Test
    void returnsLastTripForExistingVehicle() throws Exception {
        when(tripService.getLastTrip(VEHICLE_ID)).thenReturn(lastTripResponse());

        mockMvc.perform(get(LAST_TRIP_URL, VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle.id").value(VEHICLE_ID))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("B-PG-1001"))
                .andExpect(jsonPath("$.vehicle.model").value("Mercedes Actros"))
                .andExpect(jsonPath("$.vehicle.vin").value("TESTVIN0000000001"))
                .andExpect(jsonPath("$.vehicle.driverName").value("Driver One"))
                .andExpect(jsonPath("$.startedAt").value("2026-04-27T10:00:00Z"))
                .andExpect(jsonPath("$.endedAt").value("2026-04-27T10:30:00Z"))
                .andExpect(jsonPath("$.distanceKm").value(9.493372865134493))
                .andExpect(jsonPath("$.avgSpeedKph").value(23.333333333333332))
                .andExpect(jsonPath("$.pointCount").value(3))
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].ts").value("2026-04-27T10:00:00Z"))
                .andExpect(jsonPath("$.points[0].lat").value(52.57))
                .andExpect(jsonPath("$.points[0].lon").value(13.50))
                .andExpect(jsonPath("$.points[0].speedKph").value(0.0));
    }

    @Test
    void returns404WhenVehicleDoesNotExist() throws Exception {
        String vehicleId = "unknown";

        when(tripService.getLastTrip(vehicleId))
                .thenThrow(new VehicleNotFoundException(vehicleId));

        mockMvc.perform(get(LAST_TRIP_URL, vehicleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Vehicle not found: " + vehicleId));
    }

    private TripResponse lastTripResponse() {
        return TripResponse.builder()
                .vehicle(vehicleResponse())
                .startedAt(Instant.parse("2026-04-27T10:00:00Z"))
                .endedAt(Instant.parse("2026-04-27T10:30:00Z"))
                .distanceKm(9.493372865134493)
                .avgSpeedKph(23.333333333333332)
                .pointCount(3)
                .points(List.of(
                        point("2026-04-27T10:00:00Z", 52.57, 13.50, 0),
                        point("2026-04-27T10:15:00Z", 52.60, 13.55, 70),
                        point("2026-04-27T10:30:00Z", 52.63, 13.60, 0)
                ))
                .build();
    }

    private VehicleResponse vehicleResponse() {
        return VehicleResponse.builder()
                .id(VEHICLE_ID)
                .licensePlate("B-PG-1001")
                .model("Mercedes Actros")
                .vin("TESTVIN0000000001")
                .driverName("Driver One")
                .build();
    }

    private TripResponse.PointDto point(String ts, double lat, double lon, double speedKph) {
        return TripResponse.PointDto.builder()
                .ts(Instant.parse(ts))
                .lat(lat)
                .lon(lon)
                .speedKph(speedKph)
                .build();
    }
}
