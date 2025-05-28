package optimisation;

import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SingleTelescopeConcordeTSPPolicy extends DispatchPolicy {

    private static final String CONCORDE_IMAGE = "alehkot/concorde-tsp:1.0";
    private static final String CONTAINER_TSPLIB_DIR = "/usr/local/opt/concorde/";

    @Override
    public Connection[] findNextPaths(Pointable[] pointables) {
        Connection[] nextPaths = new Connection[Simulation.NUMTELESCOPES];

        for (int t = 0; t < Simulation.NUMTELESCOPES; t++) {
            Pointable current = pointables[t];
            List<Connection> neigh = current.getNeighbours();

            if (neigh.isEmpty()) {
                nextPaths[t] = null;
                continue;
            }
            if (neigh.size() == 1) {
                nextPaths[t] = neigh.get(0);
                continue;
            }

            try {
                long[][] distanceMatrix = buildDistanceMatrix(current, t);
                Path tspFile = writeTsplibLong(distanceMatrix);
                Path solFile = runConcorde(tspFile);
                List<Integer> tour = parseSolution(solFile);

                int nextIndex = findNextNodeIndex(tour);
                if (nextIndex == -1) {
                    nextPaths[t] = null;
                    continue;
                }

                Connection next = current.getNeighbours().get(nextIndex - 1);
                Pointable p = next.getOtherTarget(current);
                TelescopeState possState = telescopes[t].getStateForShortestSlew(
                        p.getHorizonCoordinates(telescopes[t].getLocation(), Clock.getScheduleClock()[t].getTime())
                );

                currentTelescopeStates[t] = possState;
                telescopes[t].applyNewState(possState);
                schedules[t].addLink(next, possState);

                nextPaths[t] = next;
            } catch (Exception e) {
                throw new RuntimeException("Failed to solve TSP for telescope " + t, e);
            }
        }

        return nextPaths;
    }

    private long[][] buildDistanceMatrix(Pointable current, int telescopeIdx) {
        int size = current.getNeighbours().size() + 1;
        long[][] distanceMatrix = new long[size][size];

        // Depot (current position)
        distanceMatrix[0][0] = 0;

        // Distances from depot to neighbors and vice versa
        for (int i = 1; i < size; i++) {
            Connection conn = current.getNeighbours().get(i - 1);
            Pointable neighbor = conn.getOtherTarget(current);
            Observable obs = ((Target) neighbor).findObservableByObservationTime();

            TelescopeState state = telescopes[telescopeIdx].getStateForShortestSlew(
                    neighbor.getHorizonCoordinates(telescopes[telescopeIdx].getLocation(),
                            Clock.getScheduleClock()[telescopeIdx].getTime())
            );

            long cost = (long) state.getSlewTime() + obs.getExpectedIntegrationTime();
            distanceMatrix[i][0] = cost;
            distanceMatrix[0][i] = cost;
        }

        // Distances between neighbors
        for (int i = 1; i < size; i++) {
            Connection conn1 = current.getNeighbours().get(i - 1);
            Pointable p1 = conn1.getOtherTarget(current);

            for (int j = 1; j < size; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    Connection conn2 = current.getNeighbours().get(j - 1);
                    Pointable p2 = conn2.getOtherTarget(current);
                    Observable obs = ((Target) p2).findObservableByObservationTime();

                    long slewTime = Telescope.calculateShortestSlewTimeBetween(
                            p1.getHorizonCoordinates(telescopes[telescopeIdx].getLocation(),
                                    Clock.getScheduleClock()[telescopeIdx].getTime()),
                            p2.getHorizonCoordinates(telescopes[telescopeIdx].getLocation(),
                                    Clock.getScheduleClock()[telescopeIdx].getTime())
                    );

                    distanceMatrix[i][j] = slewTime + obs.getExpectedIntegrationTime();
                }
            }
        }

        return distanceMatrix;
    }

    private int findNextNodeIndex(List<Integer> tour) {
        int index0 = tour.indexOf(0);
        if (index0 == -1) return -1;

        int nextNode = tour.get((index0 + 1) % tour.size());
        if (nextNode == 0) {
            // Handle case where next node is depot
            for (int i = 2; i < tour.size(); i++) {
                nextNode = tour.get((index0 + i) % tour.size());
                if (nextNode != 0) break;
            }
        }
        return nextNode;
    }

    private Path writeTsplibLong(long[][] cost) throws IOException {
        int n = cost.length;
        String projectDir = System.getProperty("user.dir");
        Path dir = Paths.get(projectDir, "tsplib");
        Files.createDirectories(dir);
        Path file = dir.resolve("tsp.tsp");

        try (BufferedWriter w = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            w.write("NAME: tsp\n");
            w.write("TYPE: TSP\n");
            w.write("DIMENSION: " + n + "\n");
            w.write("EDGE_WEIGHT_TYPE: EXPLICIT\n");
            w.write("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n");
            w.write("EDGE_WEIGHT_SECTION\n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    w.write(cost[i][j] + " ");
                }
                w.write("\n");
            }
            w.write("EOF\n");
        }

        return file;
    }

    private Path runConcorde(Path tspFile) {
        try {
            String projectDir = System.getProperty("user.dir");
            String hostDir = Paths.get(projectDir, "tsplib").toAbsolutePath().toString();

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                hostDir = hostDir.replace("\\", "/");
                if (hostDir.matches("^[A-Za-z]:/.*")) {
                    hostDir = "/" + hostDir.substring(0, 1).toLowerCase() + hostDir.substring(2);
                }
            }

            List<String> command = Arrays.asList(
                    "docker", "run", "--rm", "-t",
                    "-v", hostDir + ":" + CONTAINER_TSPLIB_DIR,
                    CONCORDE_IMAGE,
                    tspFile.getFileName().toString()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Read and discard output (or log if needed)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Discard output
                }
            }

            int exitCode = proc.waitFor();
            if (exitCode != 0 && exitCode != 255) {
                throw new RuntimeException("Concorde failed with exit code: " + exitCode);
            }

            String base = tspFile.getFileName().toString().replaceAll("\\.tsp$", "");
            return tspFile.getParent().resolve(base + ".sol");
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Concorde: " + e.getMessage(), e);
        }
    }

    private List<Integer> parseSolution(Path solFile) throws IOException {
        List<String> lines = Files.readAllLines(solFile, StandardCharsets.UTF_8);
        List<Integer> tour = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            // skip header/info lines that don’t start with a digit
            if (line.isEmpty() || !Character.isDigit(line.charAt(0)))
                continue;

            String[] tokens = line.split("\\s+");
            for (String tok : tokens) {
                int v = Integer.parseInt(tok);
                if (v == -1)     // Concorde’s end-of-tour marker
                    break;
                tour.add(v);
            }
        }

        if (tour.isEmpty()) {
            throw new IOException("No tour found in " + solFile);
        }
        return tour;
    }

}