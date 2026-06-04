package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TripDetector {

    private static final double EARTH_RADIUS_KM = 6_371.0;

    public List<Trip> detect(List<TelemetryPoint> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }

        List<TelemetryPoint> sortedPoints = points.stream()
                .sorted(Comparator.comparing(TelemetryPoint::getTs))
                .toList();

        List<TelemetryPoint> pointsWithoutDuplicateTimestamps = removeDuplicateTimestamps(sortedPoints);

        List<Trip> trips = new ArrayList<>();
        List<TelemetryPoint> currentTripPoints = new ArrayList<>();
        boolean tripInProgress = false;

        for (TelemetryPoint point : pointsWithoutDuplicateTimestamps) {
            if (!tripInProgress && !point.isIgnition()) {
                continue;
            }

            if (!tripInProgress) {
                tripInProgress = true;
                currentTripPoints = new ArrayList<>();
            }

            currentTripPoints.add(point);

            if (!point.isIgnition()) {
                trips.add(buildTrip(currentTripPoints));
                tripInProgress = false;
                currentTripPoints = new ArrayList<>();
            }
        }

        return trips;
    }

    private static List<TelemetryPoint> removeDuplicateTimestamps(List<TelemetryPoint> points) {
        List<TelemetryPoint> result = new ArrayList<>();

        for (TelemetryPoint point : points) {
            if (result.isEmpty() || !hasSameTimestamp(result.get(result.size() - 1), point)) {
                result.add(point);
            }
        }

        return result;
    }

    private static boolean hasSameTimestamp(TelemetryPoint previousPoint, TelemetryPoint currentPoint) {
        return previousPoint.getTs().equals(currentPoint.getTs());
    }

    private static Trip buildTrip(List<TelemetryPoint> points) {
        TelemetryPoint firstPoint = points.get(0);
        TelemetryPoint lastPoint = points.get(points.size() - 1);

        List<Trip.TripPoint> tripPoints = points.stream()
                .map(TripDetector::toTripPoint)
                .toList();

        return Trip.builder()
                .vehicleId(firstPoint.getVehicleId())
                .startedAt(toInstant(firstPoint))
                .endedAt(toInstant(lastPoint))
                .distanceKm(calculateDistanceKm(points))
                .avgSpeedKph(calculateAverageSpeedKph(points))
                .points(tripPoints)
                .build();
    }

    private static Trip.TripPoint toTripPoint(TelemetryPoint point) {
        return Trip.TripPoint.builder()
                .ts(toInstant(point))
                .lat(point.getLat())
                .lon(point.getLon())
                .speedKph(point.getSpeed())
                .build();
    }

    private static Instant toInstant(TelemetryPoint point) {
        return point.getTs().toInstant(ZoneOffset.UTC);
    }

    private static double calculateAverageSpeedKph(List<TelemetryPoint> points) {
        return points.stream()
                .mapToDouble(TelemetryPoint::getSpeed)
                .average()
                .orElse(0.0);
    }

    private static double calculateDistanceKm(List<TelemetryPoint> points) {
        double distanceKm = 0.0;

        for (int i = 1; i < points.size(); i++) {
            TelemetryPoint previous = points.get(i - 1);
            TelemetryPoint current = points.get(i);

            distanceKm += haversineDistanceKm(
                    previous.getLat(),
                    previous.getLon(),
                    current.getLat(),
                    current.getLon()
            );
        }

        return distanceKm;
    }

    private static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
