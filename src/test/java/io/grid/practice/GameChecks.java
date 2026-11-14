package io.grid.practice;

/** Small offline regression checks; run with Gradle check. */
public class GameChecks {
    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("Check failed");
    }
    public static void main(String[] args) throws Exception {
        require(Leaderboard.validName("Player_1"));
        require(!Leaderboard.validName(""));
        require(!Leaderboard.validName("bad/name"));
        require(!Leaderboard.validName("a\"b"));
        require(!Leaderboard.validName("abcdefghijklmnopq"));
        require(Leaderboard.parse("[{\"readTime\":\"2026-11-13T00:00:00Z\"}]").isEmpty());
        String rows = "[{\"document\":{\"fields\":{\"username\":{\"stringValue\":\"A\"},\"score\":{\"integerValue\":\"100\"}}}},"
                + "{\"document\":{\"fields\":{\"username\":{\"stringValue\":\"B\"},\"score\":{\"integerValue\":\"70\"}}}},"
                + "{\"document\":{\"fields\":{\"username\":{\"stringValue\":\"C\"},\"score\":{\"integerValue\":\"50\"}}}}]";
        var leaders = Leaderboard.parse(rows);
        require(leaders.size() == 3 && leaders.get(0).score() == 100
                && leaders.get(2).username().equals("C"));
        try { FirebaseScores.parseScore("{\"fields\":{\"score\":{\"integerValue\":\"-1\"}}}");
            throw new AssertionError("Negative score accepted");
        } catch (IllegalStateException expected) { }
        Main game = new Main();
        var reset = Main.class.getDeclaredMethod("resetBoard"); reset.setAccessible(true);
        var gridField = Main.class.getDeclaredField("grid"); gridField.setAccessible(true);
        var timeField = Main.class.getDeclaredField("time"); timeField.setAccessible(true);
        var scoreField = Main.class.getDeclaredField("score"); scoreField.setAccessible(true);
        for (int n = 0; n < 500; n++) {
            reset.invoke(game);
            int players = 0, coins = 0;
            for (int[] row : (int[][])gridField.get(game)) for (int cell : row) {
                if (cell == 1) players++; if (cell == 2) coins++;
            }
            require(players == 1 && coins == 1);
            require(timeField.getFloat(game) == 15f && scoreField.getInt(game) == 0);
        }
        System.out.println("PASS: usernames, leaderboard parsing, invalid scores, 500 random board resets");
    }
}
