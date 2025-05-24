# Firebase Auth With Electric 

This example demonstrates how to implement Firebase Authentication in an Electric application. It shows a complete authentication flow including Google OAuth sign-in, secure server-side verification of Firebase ID tokens, and protected resources. The demo includes a simple notes feature where authenticated users can save private text, demonstrating how to secure server-side operations. While the example uses in-memory storage, the patterns shown here apply equally when working with databases or other protected resources.

**Security Notice**: While this example follows Firebase's documentation and best practices, the author is not a security expert. This code is provided as a learning resource and starting point - you should thoroughly review and test any authentication implementation before using it in production. Use at your own risk.

## Setup

Before using this example, you'll need to:

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Enable Google Authentication in the Authentication section

2. **Get Firebase Web Config**
   - In Firebase Console, go to Project Settings
   - Under "Your apps", add a web app if you haven't already
   - Copy the Firebase config object
   - Replace the `firebase-config` in `src/electric_starter_app/firebase_client.cljs` with your config

3. **Get Admin SDK Credentials**
   - In Firebase Console, go to Project Settings > Service Accounts
   - Click "Generate New Private Key"
   - Create a `.secret` directory in your project root
   - Save the downloaded JSON file in the `.secret` directory
   - Update the path in `src/electric_starter_app/firebase_server.clj` to match your JSON filename
   - Note: In production, you should use environment variables instead of this JSON file

**Security Notes**: 
- Never commit the Firebase Admin SDK JSON file to version control
- The `.gitignore` file already excludes `.secret/` to help prevent this
- For production deployment, use environment variables for Firebase credentials instead of JSON files

**Further Reading**:
- [Firebase Web Authentication Guide](https://firebase.google.com/docs/auth/web/start) - Essential reading for understanding how Firebase authentication works in web applications
- [Firebase Token Verification Guide](https://firebase.google.com/docs/auth/admin/verify-id-tokens) - Important documentation about server-side token verification

## db.cljs
The `db.cljs` file serves as a simple client-side state management system for the Firebase authentication data. Here are its main purposes:

1. It maintains a central atom (`!client-db`) that stores authentication-related state on the client side, specifically:
   - The current user object (`:user` key)
   - The current ID token (`:id-token` key)

2. It provides a clean API with getter and setter functions to manipulate this state:
   - `set-user`/`remove-user`/`get-user` - to manage the Firebase user object
   - `set-id-token`/`remove-id-token`/`get-id-token` - to manage the Firebase ID token

3. This abstraction is used by `firebase_client.cljs` to keep track of authentication state changes and ID token updates from Firebase, and by `main.cljc` to access the current authentication state for rendering the UI appropriately.

The file is intentionally simple, just providing a centralized place to store and access authentication state, rather than having it scattered throughout the application. This makes it easier to:

- Track authentication state changes
- Access the current user's information  
- Access the current ID token for making authenticated requests to the server
- Modify how authentication state is stored if needed in the future

## firebase_client.cljs
The `firebase_client.cljs` file serves as the client-side Firebase authentication interface. Here are its main purposes:

1. **Firebase Initialization**
   - Configures and initializes the Firebase app with credentials
   - Sets up the Google authentication provider

2. **Authentication Functions**
   - Provides functions for signing in with Google (`sign-in-with-google`)
   - Handles signing out (`sign-out`)
   - Contains commented examples for email/password authentication

3. **Token Management**
   - Manages Firebase ID tokens, which are used for authenticated requests
   - Automatically refreshes ID tokens:
     - When the browser tab becomes visible
     - Every 5 minutes as a fallback
     - When authentication state changes

4. **State Tracking**
   - Maintains authentication state in the client database (`!client-db`)
   - Tracks both the user object and ID token
   - Uses Firebase's `onAuthStateChanged` and `onIdTokenChanged` listeners to keep the client state in sync

The file acts as a bridge between Firebase's JavaScript SDK and the rest of the Electric application, handling all client-side authentication concerns and maintaining the authentication state that can be used by other parts of the application.

## main.cljc
The `main.cljc` file demonstrates a complete Firebase authentication flow in an Electric application. Here's a breakdown:

1. **Main Components**
   - `LogoutButton`: Simple button that triggers Firebase sign-out
   - `SignInWithGoogleButton`: Button to initiate Google OAuth login
   - `DebugInfo`: Displays authentication state and user data for debugging
   - `LoggedIn`: UI component shown to authenticated users
   - `LoggedOut`: UI component shown to unauthenticated users

2. **Authentication Flow**
   ```clojure
   (e/defn Main [ring-request]
     (e/client
       ;; ... 
       (let [client-db  (e/watch !client-db)
             id-token   (db/get-id-token client-db)]
         (if (e/server (:verified (fbs/verify-id-token id-token)))
           (LoggedIn client-db id-token)
           (LoggedOut client-db)))))
   ```
   - Watches the client-side database for auth state changes
   - Verifies the ID token on the server
   - Shows different UI based on auth state

3. **Protected Features Demo**
   ```clojure
   (e/defn LoggedIn [client-db id-token]
     (let [user-note (e/server (when-verified id-token restricted/get-note))]
       ;; ... textarea for editing note ...
       (dom/button 
         ;; ... save note with stricter verification ...
         (when spend
           (spend (e/server (when-verified-strict id-token restricted/set-note! s)))))))
   ```
   - Demonstrates secure server calls using `when-verified` and `when-verified-strict`
   - Shows a simple "notes" feature where each user can save private text
   - Illustrates different security levels (basic vs strict verification)

The demo serves as both a reference implementation and a starting point for adding Firebase authentication to Electric applications. It shows best practices for secure client-server communication and how to protect server-side resources.

## firebase_server.clj
The `firebase_server.clj` file serves as the server-side security layer for Firebase authentication. Here are its main purposes:

1. **Firebase SDK Initialization**
   - Initializes the Firebase Admin SDK using service account credentials
   - For development, uses a secret JSON file for authentication
   - In production, credentials should be read from environment variables instead of JSON files

2. **Token Verification**
   - Provides functions to verify Firebase ID tokens:
     - `verify-id-token`: Core verification function that checks if a token is valid
     - `when-verified`: Helper that runs a function only if token is valid (fast check)
     - `when-verified-strict`: Stricter version that also checks for token revocation (slower but more secure)

3. **Claims Management**
   - `token->claims-map`: Converts Firebase token claims into a Clojure map
   - Handles conversion of Java/Guava maps to proper Clojure maps
   - Extracts user information like user ID, email, etc.

4. **Session Management**
   - `revoke-refresh-tokens`: Allows revoking a user's refresh tokens for security purposes
   - Tracks token revocation timestamps

The file ensures that only authenticated users with valid tokens can access protected resources. It's used in conjunction with `restricted.clj` to implement secure endpoints and data access.

Example usage from `main.cljc`:
```clojure
(e/server (when-verified id-token restricted/get-note))
```
This verifies the user's token before allowing access to restricted functionality.

## restricted.clj
The `restricted.clj` file demonstrates how to implement functions that access private data in an Electric application. **Important**: These functions must always be wrapped with authentication checks (via `when-verified` or `when-verified-strict`) to ensure data security.

Here are its main purposes:

1. **User Data Storage**
   - Maintains a simple in-memory store of user notes using an atom `!user-notes`
   - Maps user IDs to their personal notes: `{user-id -> string}`

2. **Protected Operations**
   - `get-note`: Retrieves a user's personal note based on their Firebase user ID
   - `set-note!`: Updates a user's personal note with new text
   - Both functions expect Firebase claims as input (containing `:user_id`)

3. **Security Integration**
   The file works in conjunction with `firebase_server.clj` - its functions must be called through verification wrappers:

   ```clojure
   ;; Example from main.cljc
   (e/server (when-verified id-token restricted/get-note))
   (e/server (when-verified-strict id-token restricted/set-note! s))
   ```

   This ensures that:
   - Only authenticated users can access their notes
   - Users can only access their own notes (via their user ID in claims)
   - More sensitive operations (like `set-note!`) can optionally use stricter verification

The file demonstrates a pattern for implementing protected resources in an Electric application using Firebase authentication. While this example uses simple in-memory storage, the same pattern applies when accessing databases or other sensitive resources.


# Electric v3 Starter App

## Links

* Electric github with source code: https://github.com/hyperfiddle/electric
* Tutorial: https://electric.hyperfiddle.net/ (we'll be fleshing out this as a full docs site asap)

## Getting started

* Shell: `clj -A:dev -X dev/-main`. 
* Login instructions will be printed
* REPL: `:dev` deps alias, `(dev/-main)` at the REPL to start dev build
* App will start on http://localhost:8080
* Electric root function: [src/electric_starter_app/main.cljc](src/electric_starter_app/main.cljc)
* Hot code reloading works: edit -> save -> see app reload in browser

```shell
# Prod build
clj -X:build:prod build-client
clj -M:prod -m prod

# Uberjar (optional)
clj -X:build:prod uberjar :build/jar-name "app.jar"
java -cp target/app.jar clojure.main -m prod

# Docker
docker build -t electric3-starter-app:latest .
docker run --rm -it -p 8080:8080 electric3-starter-app:latest
```

## License
Electric v3 is **free for bootstrappers and non-commercial use,** and otherwise available commercially under a business source available license, see: [Electric v3 license change](https://tana.pub/lQwRvGRaQ7hM/electric-v3-license-change) (2024 Oct). License activation is experimentally implemented through the Electric compiler, requiring **compile-time** login for **dev builds only**. That means: no license check at runtime, no login in prod builds, CI/CD etc, no licensing code even on the classpath at runtime. This is experimental, but seems to be working great so far. We do not currently require any account approval steps, just log in. There will be a EULA at some point once we finalize the non-commercial license, for now we are focused on enterprise deals which are different.
