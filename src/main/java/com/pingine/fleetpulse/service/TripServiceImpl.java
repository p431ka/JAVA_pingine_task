package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.exception.TripNotFoundException;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final int RECENT_POINTS_LIMIT = 1000;

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Override
    public TripResponse getLastTrip(String vehicleId) {
        VehicleResponse vehicle = vehicleService.getById(vehicleId);

        List<TelemetryPoint> points = telemetryRepository.findRecentPoints(vehicleId, RECENT_POINTS_LIMIT);
        List<Trip> completedTrips = tripDetector.detect(points);

        if (completedTrips.isEmpty()) {
            throw new TripNotFoundException(vehicleId);
        }

        Trip lastCompletedTrip = Collections.max(
                completedTrips,
                Comparator.comparing(Trip::getEndedAt)
        );

        return toResponse(vehicle, lastCompletedTrip);
    }

    private static TripResponse toResponse(VehicleResponse vehicle, Trip trip) {
        List<TripResponse.PointDto> points = trip.getPoints().stream()
                .map(TripServiceImpl::toPointDto)
                .toList();

        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(trip.getStartedAt())
                .endedAt(trip.getEndedAt())
                .distanceKm(trip.getDistanceKm())
                .avgSpeedKph(trip.getAvgSpeedKph())
                .pointCount(points.size())
                .points(points)
                .build();
    }

    private static TripResponse.PointDto toPointDto(Trip.TripPoint point) {
        return TripResponse.PointDto.builder()
                .ts(point.getTs())
                .lat(point.getLat())
                .lon(point.getLon())
                .speedKph(point.getSpeedKph())
                .build();
    }
}
