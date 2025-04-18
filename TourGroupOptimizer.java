
import java.util.*;

public class TourGroupOptimizer {
    // TODO: need to deal with this if the number of elements not divisible by the elements of the group 
    static final int NUM_PLACES = 40;
    static final int GROUP_SIZE = 5;
    static final int NUM_GROUPS =( NUM_PLACES % GROUP_SIZE == 0 )?NUM_PLACES / GROUP_SIZE : NUM_PLACES / GROUP_SIZE+1 ; // this result in outofBound exception

    public static List<List<Integer>> randomInitialGroups(Random rand) {
        List<Integer> allPlaces = new ArrayList<>();
        for (int i = 0; i < NUM_PLACES; i++) allPlaces.add(i);
        Collections.shuffle(allPlaces, rand);

        List<List<Integer>> groups = new ArrayList<>();
        for (int i = 0; i < NUM_GROUPS; i++) {
            groups.add(new ArrayList<>(allPlaces.subList(i * GROUP_SIZE, (i + 1) * GROUP_SIZE)));
        }
        return groups;
    }
    
    public static double intraGroupDistance(List<Integer> group, double[][] dist) {
    if (group.size() <= 1) return 0;
    
    List<List<Integer>> permutations = generatePermutations(group);
    double minDistance = Double.MAX_VALUE;
    List<Integer> bestPath = null;
    
    for (List<Integer> path : permutations) {
        double pathDistance = 0;
        // Calculate distance for this permutation
        for (int i = 0; i < path.size() - 1; i++) {
            pathDistance += dist[path.get(i)][path.get(i+1)];
        }
        // Complete the loop
        pathDistance += dist[path.get(path.size()-1)][path.get(0)];
        
        if (pathDistance < minDistance) {
            minDistance = pathDistance;
            bestPath = path;
        }
    }
    
    // Modify the original group to match the best path order
    if (bestPath != null) {
        group.clear();
        group.addAll(bestPath);
    }
    
    return minDistance;
}

// The generatePermutations method remains exactly the same
private static List<List<Integer>> generatePermutations(List<Integer> original) {
    if (original.isEmpty()) {
        List<List<Integer>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        return result;
    }
    
    Integer first = original.get(0);
    List<Integer> rest = original.subList(1, original.size());
    
    List<List<Integer>> permutations = new ArrayList<>();
    for (List<Integer> p : generatePermutations(rest)) {
        for (int i = 0; i <= p.size(); i++) {
            List<Integer> newPerm = new ArrayList<>(p);
            newPerm.add(i, first);
            permutations.add(newPerm);
        }
    }
    return permutations;
}

    public static double totalDistance(List<List<Integer>> groups, double[][] dist) {
        double sum = 0;
        for (List<Integer> group : groups) {
            sum += intraGroupDistance(group, dist);
        }
        return sum;
    }

    public static List<List<Integer>> deepCopyGroups(List<List<Integer>> groups) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> group : groups) {
            copy.add(new ArrayList<>(group));
        }
        return copy;
    }

    public static List<List<Integer>> optimize(double[][] dist, int iterations, Random rand) {
        List<List<Integer>> groups = randomInitialGroups(rand);
        double bestScore = totalDistance(groups, dist);
        List<List<Integer>> bestGroups = deepCopyGroups(groups);

        // Random rand = new Random();

        for (int iter = 0; iter < iterations; iter++) {
            int g1 = rand.nextInt(NUM_GROUPS);
            int g2 = rand.nextInt(NUM_GROUPS);
            while (g2 == g1) g2 = rand.nextInt(NUM_GROUPS);

            int i1 = rand.nextInt(GROUP_SIZE);
            int i2 = rand.nextInt(GROUP_SIZE);

            // Swap
            List<List<Integer>> newGroups = deepCopyGroups(groups);
            int temp = newGroups.get(g1).get(i1);
            newGroups.get(g1).set(i1, newGroups.get(g2).get(i2));
            newGroups.get(g2).set(i2, temp);

            double newScore = totalDistance(newGroups, dist);
            if (newScore < bestScore) {
                groups = newGroups;
                bestScore = newScore;
                bestGroups = deepCopyGroups(newGroups);
            }
        }

        // System.out.println("Best Total Intra-Group Distance: " + bestScore);
        return bestGroups;
    }

    public static void printGroups(List<List<Integer>> groups) {
        for (int i = 0; i < groups.size(); i++) {
            System.out.println("Group " + (i + 1) + ": " + groups.get(i));
        }
    }

    // public static void main(String[] args) {
    //     // === Test Case ===
    //     // Generate dummy symmetrical matrix for testing
    //     double[][] dist = new double[NUM_PLACES][NUM_PLACES];
    //     Random rand = new Random(30);

    //     for (int i = 0; i < NUM_PLACES; i++) {
    //         for (int j = 0; j <= i; j++) {
    //             if (i == j) dist[i][j] = 0;
    //             else {
    //                 double d = rand.nextDouble() * 100;
    //                 dist[i][j] = d;
    //                 dist[j][i] = d;
    //                 // System.out.println(dist[i][j]);
    //             }
    //         }
    //     }

    //     List<List<Integer>> bestGroups = optimize(dist, 5000, rand);
    //     printGroups(bestGroups);
    // }
    public static void main(String[] args) {
    // Generate distance matrix
    double[][] dist = new double[NUM_PLACES][NUM_PLACES];
    Random baseRand = new Random(42); // Fixed seed for consistent distance matrix
    
    // Initialize distance matrix
    for (int i = 0; i < NUM_PLACES; i++) {
        for (int j = 0; j <= i; j++) {
            if (i == j) {
                dist[i][j] = 0;
            } else {
                double d = baseRand.nextDouble() * 100;
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }
    }

    // Parameters for multi-start optimization
    final int NUM_SEEDS = 960;       // Number of different seeds to try
    final int ITERATIONS = 1000;    // Iterations per optimization run
    
    // Track best overall solution
    double globalBestScore = Double.MAX_VALUE;
    List<List<Integer>> globalBestGroups = null;
    int bestSeed = -1;

    // Test multiple random seeds
    for (int seed = 920; seed < NUM_SEEDS; seed++) {
        Random groupRand = new Random(seed);
        // System.out.println("\nRunning optimization with seed: " + seed);
        
        List<List<Integer>> currentGroups = optimize(dist, ITERATIONS, groupRand);
        double currentScore = totalDistance(currentGroups, dist);
        
        if (currentScore < globalBestScore) {
            globalBestScore = currentScore;
            globalBestGroups = deepCopyGroups(currentGroups);
            bestSeed = seed;
            // System.out.println("New best score found: " + globalBestScore);
        }
    }

    // Output final results
    System.out.println("\n=== FINAL RESULTS ===");
    System.out.println("Best seed: " + bestSeed);
    System.out.println("Best total distance: " + globalBestScore);
    System.out.println("Optimal groups:");
    printGroups(globalBestGroups);
}
}
