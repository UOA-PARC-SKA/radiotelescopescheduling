package optimisation.partitioners;

import astrometrics.EquatorialCoordinates;
import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fixed K-means clustering partitioner with improved initialization, distance calculations,
 * and empty cluster handling.
 */
public class KMeansClusteringPartitioner implements PulsarPartitioner {
    private final int maxIterations;
    private final double convergenceThreshold;

    public KMeansClusteringPartitioner() {
        this(50, 1e-4);
    }

    public KMeansClusteringPartitioner(int maxIterations, double convergenceThreshold) {
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
    }

    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        if (pulsars.isEmpty()) {
            return createEmptyPartitions();
        }

        int k = Math.min(Simulation.NUMTELESCOPES, pulsars.size());

        // Extract equatorial coordinates from targets
        List<EquatorialCoordinates> points = pulsars.stream()
                .map(Target::getEquatorialCoordinates)
                .collect(Collectors.toList());

        // Initialize centroids using current telescope positions
        List<EquatorialCoordinates> centroids = initializeCentroids(points, currentPositions, k);

        // K-means clustering
        List<Integer> assignments = performKMeans(points, centroids);

        // Group targets by cluster
        List<List<Target>> partitions = createEmptyPartitions();
        for (int i = 0; i < pulsars.size(); i++) {
            int cluster = assignments.get(i);
            if (cluster < Simulation.NUMTELESCOPES) {
                partitions.get(cluster).add(pulsars.get(i));
            }
        }

        // Update current positions to the last target in each partition
        for (int i = 0; i < Math.min(partitions.size(), currentPositions.length); i++) {
            if (!partitions.get(i).isEmpty()) {
                currentPositions[i] = partitions.get(i).get(partitions.get(i).size() - 1);
            }
        }

        return partitions;
    }

    private List<EquatorialCoordinates> initializeCentroids(List<EquatorialCoordinates> points, Pointable[] currentPositions, int k) {
        List<EquatorialCoordinates> centroids = new ArrayList<>();

        // Use current positions as initial centroids when possible
        for (Pointable position : currentPositions) {
            if (centroids.size() >= k) break;

            if (position instanceof Target) {
                Target target = (Target) position;
                centroids.add(target.getEquatorialCoordinates());
            } else if (position instanceof EquatorialCoordinates) {
                centroids.add((EquatorialCoordinates) position);
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

}