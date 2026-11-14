# Grid Game Practice

A Java/libGDX grid game with random coins, sound effects and a Firebase top-three leaderboard.

## Play

Type a username (1-16 English letters, numbers or underscores), then click START or press Enter.
Use the arrow keys to move the red player onto the gold coin. Each coin gives one point,
plays a sound and adds 0.5 seconds. A round starts with 15 seconds. The player starts in a
random cell; each collected coin respawns randomly in an empty cell.

When time reaches zero, the game shows GAME OVER, plays an end sound and submits the score.
Click PLAY AGAIN or press Enter after saving to restart. Edit the username with Backspace
on either menu to play as someone else. F5 refreshes the leaderboard or retries a failed save.
An unsuccessful save is retained until retry, so the next round waits for it.

## Firebase leaderboard

The right panel shows the top three usernames and scores, sorted by score descending in
Firestore. Each case-insensitive username has one document in `leaderboard`; only its best
score is retained. Ties use Firestore's document-name ordering. Empty slots display `--`.
The board loads on startup and refreshes after a round or F5. Network requests are asynchronous.
The earlier classroom example `users/player1` also stores the latest round's score.

Copy `firestore.rules` into Firebase Console > Firestore > Rules and Publish before playing.
The leaderboard collection is created by the first successful score submission. Keep the
existing `users/player1` document with integer `score: 0` for the original example.
The included Firebase client config points to the classroom project `r14-1db8e`.
For your own database, edit `FirebaseConfig.java` and publish the same rules there.

This is a classroom demonstration, not a secure competitive leaderboard: usernames are
not authenticated, scores are client-reported, and anyone can submit a permitted score.
Rules validate fields, prevent decreasing best scores and limit list queries to three.
Demo access expires on **November 20, 2026 at 00:00 UTC**. After that the owner must update
the rules or implement authentication for continued use. The API key is public client
configuration, not an admin credential. No service-account credentials are included.

## Run

Install JDK 17 or newer. In IntelliJ, open this folder as a Gradle project, let dependencies
sync, and run `io.grid.practice.Lwjgl3Launcher` (or the Gradle `run` task).

Windows: `gradlew.bat run`

macOS/Linux: `sh gradlew run`

Internet is required for first-time dependencies and Firebase. Enable sound to hear both
effects. The Gradle wrapper is included. Build with `gradlew.bat build` (Windows) or
`sh gradlew build` (macOS/Linux).

## Credits

Based on the class Grid Project practice. Coin and game-over audio were supplied in the
teacher's project; they are included as classroom assets, with no claim of ownership.
