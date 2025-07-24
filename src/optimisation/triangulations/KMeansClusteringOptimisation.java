package optimisation.triangulations;

import astrometrics.EquatorialCoordinates;
import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;

import java.util.*;
import java.util.stream.Collectors;

public class KMeansClusteringOptimisation extends NNOptimisation {
    private final int maxIterations;
    private final double convergenceThreshold;

    public KMeansClusteringOptimisation() {
        this(50, 1e-4);
    }

    public KMeansClusteringOptimisation(int maxIterations, double convergenceThreshold) {
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
    }

    /**
     * Creates connections using K-means clustering strategy.
     * Targets are clustered and assigned to telescopes based on proximity.
     */
    public void createKMeansLinks(List<Target> targets, Pointable[] currents, double ratio,
                                  Clock[] clocks, Location loc, Telescope[] telescopes)
            throws OutOfObservablesException {

        // Clear existing neighbours for all telescopes
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            currents[i].clearNeighbours();
        }

        // Filter available targets (not currently being observed and ready for observation)
        List<Target> availableTargets = getAvailableTargets(targets, currents, clocks, loc);

        if (availableTargets.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Partition targets using K-means clustering
        List<List<Target>> partitions = partitionKMeans(availableTargets, currents);

        // Create connections for each telescope based on its partition
        for (int telescopeIndex = 0; telescopeIndex < Simulation.NUMTELESCOPES; telescopeIndex++) {
            List<Target> assignedTargets = partitions.get(telescopeIndex);

            // Create connections from current position to all assigned targets
            for (Target target : assignedTargets) {
                // Double-check target is still valid for this specific telescope
                if (!isReadyForObservation(target, clocks[telescopeIndex], loc)) {
                    continue;
                }

                double dist = calculateDistance(currents[telescopeIndex], target, loc, clocks[telescopeIndex]);
                Connection connection = new Connection(currents[telescopeIndex], target, dist);
                currents[telescopeIndex].addNeighbour(connection);
            }
        }

        // Ensure at least one telescope has valid neighbours
        boolean hasValidNeighbours = false;
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            if (!currents[i].getNeighbours().isEmpty()) {
                hasValidNeighbours = true;
                break;
            }
        }

        if (!hasValidNeighbours) {
            throw new OutOfObservablesException();
        }
    }

    /**
     * Partition targets using K-means clustering algorithm
     */
    List<List<Target>> partitionKMeans(List<Target> targets, Pointable[] currentPositions) {
        if (targets.isEmpty()) {
            return createEmptyPartitions();
        }

        int k = Math.min(Simulation.NUMTELESCOPES, targets.size());

        // Extract equatorial coordinates from targets
        List<EquatorialCoordinates> points = targets.stream()
                .map(Target::getEquatorialCoordinates)
                .collect(Collectors.toList());

        // Initialize centroids using current telescope positions
        List<EquatorialCoordinates> centroids = initializeCentroids(points, currentPositions, k);

        // K-means clustering
        List<Integer> assignments = performKMeans(points, centroids);

        // Group targets by cluster
        List<List<Target>> partitions = createEmptyPartitions();
        for (int i = 0; i < targets.size(); i++) {
            int cluster = assignments.get(i);
            if (cluster < Simulation.NUMTELESCOPES) {
                partitions.get(cluster).add(targets.get(i));
            }
        }

        return partitions;
    }

    private List<EquatorialCoordinates> initializeCentroids(List<EquatorialCoordinates> points,
                                                            Pointable[] currentPositions, int k) {
        List<EquatorialCoordinates> centroids = new ArrayList<>();

        // Use current positions as initial centroids when possible
        for (Pointable position : currentPositions) {
            if (centroids.size() >= k) break;

            if (position instanceof Target) {
                Target target = (Target) position;
                centroids.add(target.getEquatorialCoordinates());
            }
        }

        // K-means++ initialization for remaining centroids
        Random random = new Random();
        while (centroids.size() < k) {
            if (centroids.isEmpty()) {
                centroids.add(points.get(random.nextInt(points.size())));
            } else {
                // Find point with maximum minimum distance to existing centroids
                EquatorialCoordinates farthest = null;
                double maxDistance = 0;

                for (EquatorialCoordinates point : points) {
                    double minDist = centroids.stream()
                            .mapToDouble(c -> angularDistance(
                                    point.getRightAscension(), point.getDeclination(),
                                    c.getRightAscension(), c.getDeclination()
                            ))
                            .min().orElse(0);

                    if (minDist > maxDistance) {
                        maxDistance = minDist;
                        farthest = point;
                    }
                }

                if (farthest != null) {
                    centroids.add(farthest);
                } else {
                    centroids.add(points.get(random.nextInt(points.size())));
                }
            }
        }

        return centroids;
    }

    private List<Integer> performKMeans(List<EquatorialCoordinates> points, List<EquatorialCoordinates> centroids) {
        List<Integer> assignments = new ArrayList<>(Collections.nCopies(points.size(), 0));
        boolean converged = false;

        for (int iter = 0; iter < maxIterations && !converged; iter++) {
            // Assign points to nearest centroid
            List<Integer> newAssignments = new ArrayList<>();
            for (EquatorialCoordinates point : points) {
                int nearestCentroid = 0;
                double minDistance = angularDistance(
                        point.getRightAscension(), point.getDeclination(),
                        centroids.get(0).getRightAscension(), centroids.get(0).getDeclination()
                );

                for (int i = 1; i < centroids.size(); i++) {
                    double distance = angularDistance(
                            point.getRightAscension(), point.getDeclination(),
                            centroids.get(i).getRightAscension(), centroids.get(i).getDeclination()
                    );
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCentroid = i;
                    }
                }
                newAssignments.add(nearestCentroid);
            }

            // Check assignment convergence
            converged = assignments.equals(newAssignments);
            assignments = newAssignments;

            // Update centroids
            List<EquatorialCoordinates> newCentroids = new ArrayList<>();
            for (int i = 0; i < centroids.size(); i++) {
                final int clusterId = i;
                List<Integer> finalAssignments = assignments;
                List<EquatorialCoordinates> clusterPoints = points.stream()
                        .filter(p -> finalAssignments.get(points.indexOf(p)) == clusterId)
                        .collect(Collectors.toList());

                if (!clusterPoints.isEmpty()) {
                    double avgRA = clusterPoints.stream()
                            .mapToDouble(EquatorialCoordinates::getRightAscension)
                            .average().orElse(0);
                    double avgDec = clusterPoints.stream()
                            .mapToDouble(EquatorialCoordinates::getDeclination)
                            .average().orElse(0);
                    newCentroids.add(new EquatorialCoordinates(avgRA, avgDec));
                } else {
                    // Handle empty cluster by placing it at a random point
                    newCentroids.add(points.get(new Random().nextInt(points.size())));
                }
            }

            // Check centroid movement convergence
            if (!converged) {
                converged = true;
                for (int i = 0; i < centroids.size(); i++) {
                    double distance = angularDistance(
                            centroids.get(i).getRightAscension(), centroids.get(i).getDeclination(),
                            newCentroids.get(i).getRightAscension(), newCentroids.get(i).getDeclination()
                    );
                    if (distance > convergenceThreshold) {
                        converged = false;
                        break;
                    }
                }
            }
            centroids = newCentroids;
        }

        return assignments;
    }

    /**
     * Get list of targets that are available for observation
     */
    public List<Target> getAvailableTargets(List<Target> targets, Pointable[] currents,
                                             Clock[] clocks, Location loc) {
        List<Target> available = new ArrayList<>();

        for (Target target : targets) {
            // Skip if target doesn't need observing or is complete
            if (!target.needsObserving() || target.hasCompleteObservation()) {
                continue;
            }

            // Skip if target is currently being observed by any telescope
            boolean currentlyObserved = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (currents[k] == target) {
                    currentlyObserved = true;
                    break;
                }
            }
            if (currentlyObserved) {
                continue;
            }

            // Check if target is observable by at least one telescope
            boolean observableByAny = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (isReadyForObservation(target, clocks[k], loc)) {
                    observableByAny = true;
                    break;
                }
            }

            if (observableByAny) {
                available.add(target);
            }
        }

        return available;
    }

    /**
     * Calculate angular distance between two pointables
     */
    double calculateDistance(Pointable from, Target to, Location loc, Clock clock) {
        try {
            return from.angularDistanceTo(to, loc, clock.getTime());
        } catch (WrongTypeException e) {
            try {
                return to.angularDistanceTo(from, loc, clock.getTime());
            } catch (WrongTypeException e1) {
                e1.printStackTrace();
                return Double.POSITIVE_INFINITY; // Fallback in case of error
            }
        }
    }

    /**
     * Calculate angular distance between two celestial coordinates using the haversine formula.
     * All angles are in degrees, result is in radians.
     */
    private double angularDistance(double ra1, double dec1, double ra2, double dec2) {
        // Convert degrees to radians
        ra1 = Math.toRadians(ra1);
        dec1 = Math.toRadians(dec1);
        ra2 = Math.toRadians(ra2);
        dec2 = Math.toRadians(dec2);

        double deltaRA = ra2 - ra1;
        double cosDistance = Math.sin(dec1) * Math.sin(dec2) +
                Math.cos(dec1) * Math.cos(dec2) * Math.cos(deltaRA);

        // Clamp to valid range to avoid numerical errors
        cosDistance = Math.max(-1.0, Math.min(1.0, cosDistance));

        return Math.acos(cosDistance);
    }

    /**
     * Create empty partitions for all telescopes
     */
    private List<List<Target>> createEmptyPartitions() {
        List<List<Target>> partitions = new ArrayList<>();
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            partitions.add(new ArrayList<>());
        }
        return partitions;
    }
}