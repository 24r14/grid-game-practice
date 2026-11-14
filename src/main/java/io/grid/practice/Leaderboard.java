package io.grid.practice;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Classroom REST leaderboard: one best score per case-insensitive username. */
public final class Leaderboard {
    public record Entry(String username, int score) { }
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private CompletableFuture<HttpResponse<String>> send(String url, String method, String body) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15));
        if (body == null) b.GET();
        else b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String checked(HttpResponse<String> response) {
        if (response.statusCode() != 200) throw new IllegalStateException("Firebase HTTP "
                + response.statusCode() + ": check leaderboard rules and connection");
        return response.body();
    }

    public synchronized CompletableFuture<List<Entry>> load() {
        CompletableFuture<List<Entry>> result = pending.handle((v, e) -> null).thenCompose(v -> query());
        pending = result.handle((v, e) -> null);
        return result;
    }

    public synchronized CompletableFuture<List<Entry>> submit(String name, int score) {
        if (!validName(name) || score < 0) throw new IllegalArgumentException("Invalid username or score");
        String url = FirebaseConfig.DOCUMENTS_URL + "/leaderboard/" + name.toLowerCase(Locale.ROOT)
                + "?key=" + FirebaseConfig.API_KEY;
        CompletableFuture<List<Entry>> result = pending.handle((v, e) -> null)
                .thenCompose(v -> send(url, "GET", null)).thenCompose(existing -> {
                    if (existing.statusCode() == 200 && FirebaseScores.parseScore(existing.body()) >= score)
                        return query();
                    if (existing.statusCode() != 404) checked(existing);
                    String body = "{\"fields\":{\"username\":{\"stringValue\":\"" + name
                            + "\"},\"score\":{\"integerValue\":\"" + score + "\"}}}";
                    return send(url, "PATCH", body).thenCompose(saved -> {
                        if (saved.statusCode() == 403) {
                            // A second client may have saved a higher score since our GET.
                            return send(url, "GET", null).thenCompose(latest -> {
                                if (FirebaseScores.parseScore(checked(latest)) < score) checked(saved);
                                return query();
                            });
                        }
                        checked(saved);
                        return query();
                    });
                });
        pending = result.handle((v, e) -> null);
        return result;
    }

    private CompletableFuture<List<Entry>> query() {
        String body = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"leaderboard\"}],"
                + "\"orderBy\":[{\"field\":{\"fieldPath\":\"score\"},\"direction\":\"DESCENDING\"}],\"limit\":3}}";
        return send(FirebaseConfig.DOCUMENTS_URL + ":runQuery?key=" + FirebaseConfig.API_KEY, "POST", body)
                .thenApply(r -> parse(checked(r)));
    }

    static List<Entry> parse(String json) {
        List<Entry> entries = new ArrayList<>();
        JsonValue root = new JsonReader().parse(json);
        if (!root.isArray()) throw new IllegalStateException("Invalid leaderboard response");
        for (JsonValue row : root) {
            JsonValue doc = row.get("document");
            if (doc == null) continue; // Empty query returns only readTime.
            JsonValue fields = doc.get("fields");
            String name = fields.get("username").getString("stringValue");
            int score = FirebaseScores.parseScore(doc.toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json));
            if (!validName(name)) throw new IllegalStateException("Invalid leaderboard username");
            entries.add(new Entry(name, score));
        }
        return List.copyOf(entries);
    }
}
