package io.grid.practice;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Desktop Java REST client; Firestore rules still control permission. */
public final class FirebaseScores {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    public static boolean isConfigured() {
        return !FirebaseConfig.DOCUMENT_ID.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String url() {
        if (!isConfigured()) throw new IllegalStateException("Firebase document ID is missing");
        return FirebaseConfig.DOCUMENTS_URL + "/" + encode(FirebaseConfig.COLLECTION)
                + "/" + encode(FirebaseConfig.DOCUMENT_ID) + "?key=" + encode(FirebaseConfig.API_KEY);
    }

    public synchronized CompletableFuture<Integer> getScoreFromFirebase() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url()))
                .timeout(Duration.ofSeconds(15)).GET().build();
        return enqueue(request);
    }

    public synchronized CompletableFuture<Integer> updateScoreInFirebase(int score) {
        if (score < 0) throw new IllegalArgumentException("Score cannot be negative");
        String body = "{\"fields\":{\"score\":{\"integerValue\":\"" + score + "\"}}}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url() + "&updateMask.fieldPaths=score"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
        return enqueue(request);
    }

    // Keep consecutive rounds' writes in order, without blocking gameplay.
    private CompletableFuture<Integer> enqueue(HttpRequest request) {
        CompletableFuture<Integer> result = pending.handle((value, error) -> null)
                .thenCompose(value -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status != 200) {
                        String detail = status == 403 || status == 401 ? "Check Firestore rules / authentication"
                                : status == 404 ? "Check database and document ID" : "Request failed";
                        throw new IllegalStateException("HTTP " + status + ": " + detail);
                    }
                    return parseScore(response.body());
                });
        pending = result.handle((value, error) -> null);
        return result;
    }

    static int parseScore(String json) {
        JsonValue fields = new JsonReader().parse(json).get("fields");
        JsonValue score = fields == null ? null : fields.get("score");
        if (score == null || !score.has("integerValue")) {
            throw new IllegalStateException("Document needs an integer score field");
        }
        int value = Integer.parseInt(score.getString("integerValue"));
        if (value < 0) throw new IllegalStateException("Invalid score");
        return value;
    }
}
