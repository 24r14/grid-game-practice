package io.grid.practice;

/** Public Firebase client configuration, not database access permission.
 * The collection/document path and authentication must be configured before use.
 */
public final class FirebaseConfig {
    public static final String API_KEY = "AIzaSyCJUlXXau7QUIFUEDsbD17IyofMmDTkGbg";
    public static final String PROJECT_ID = "r14-1db8e";
    public static final String DATABASE_ID = "(default)";
    public static final String COLLECTION = "users";
    // Set this to YOUR document ID in r14-1db8e, not the teacher's example ID.
    public static final String DOCUMENT_ID = "player1";
    public static final String DOCUMENTS_URL =
            "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
            + "/databases/" + DATABASE_ID + "/documents";

    private FirebaseConfig() { }
}
