# Grid Game Practice

A Java/libGDX game made for computer science class.

## How to play

- Enter a username, then click START or press Enter.
- Use the arrow keys to move the red player and collect gold coins.
- Start with 15 seconds. Each coin adds 1 point and 0.5 seconds.
- The player starts at a random position, and coins respawn randomly.
- When time runs out, a game-over screen and sound play. Click PLAY AGAIN to restart.
- The Firebase leaderboard shows the top 3 usernames and their best scores. Press F5 to refresh or retry a failed save.

## Run

Open the project in IntelliJ with JDK 17 or newer and run the Gradle `run` task.
On Windows, you can also use `gradlew.bat run`. Internet is needed for Firebase.

For Firebase setup, publish `firestore.rules` in the Firestore console. These classroom rules allow unauthenticated scores until July 1, 2027 (UTC).

Sound effects are from the teacher-provided project.
