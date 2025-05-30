# Firebase Auth With Electric Clojure

**Live demo**: https://electric-with-firebase-auth.mircloud.us/

This example demonstrates how to implement [Firebase Authentication](https://firebase.google.com/docs/auth/web/start) in an [Electric Clojure](https://github.com/hyperfiddle/electric) application. It shows a complete authentication flow including [Sign In With Google](https://firebase.google.com/docs/auth/web/google-signin), secure [server-side verification of Firebase ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens), and protected resources. The demo includes a simple notes feature where authenticated users can save private text, demonstrating how to secure server-side operations. While the example uses in-memory storage, the patterns shown here apply equally when working with databases or other protected resources.

If you'd like to learn more about Electric Clojure, it's worth joining the #hyperfiddle channel on https://clojurians.slack.com/. To find the Electric starter template, search the #hyperfiddle channel for `in:#hyperfiddle electric v3 beta url`. 

**Security Notice**: While I've tried to follow Firebase's documentation and best practices, I am not a security expert. This code is provided as a learning resource and starting point - you should thoroughly review and test any authentication implementation before using it in production. Use at your own risk.

## Authentication Approach

This example demonstrates a pattern for protecting server-side resources in Electric applications:

1. The client stores the Firebase ID token in an atom (`!client-db`) when users authenticate
2. Protected server functions expect this ID token as an argument
3. All `e/server` calls that access sensitive data or perform protected operations must:
   - Pass the ID token from the client
   - Wrap the operation in `when-verified` or `when-verified-strict` on the server
   - These verify the token is valid before allowing access
4. The server extracts the user's Firebase UID from the verified token to:
   - Ensure users can only access their own data
   - Associate data with specific users
   - Track who performed what operations

This approach ensures that every protected server operation explicitly verifies authentication, rather than relying on session state or ambient authority.

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

3. **Configure Authorized Domains for Production (optional)**
   - This step is required when deploying to production, otherwise authentication will fail on your live site. You don't need it for running the app on localhost.
   - Go to https://console.firebase.google.com
   - Select your project
   - In the left sidebar:
     - Click Authentication
     - Then go to the Settings tab
   - Scroll to Authorized Domains
     - Click "Add domain"
     - Enter your domain, e.g., myapp.example.com
4. **Get Admin SDK Credentials**
   - In Firebase Console, go to Project Settings > Service Accounts
   - Click "Generate New Private Key"
   - Create a `.secret` directory in your project root
   - Save the downloaded JSON file in the `.secret` directory
   - Update the path in `src/electric_starter_app/firebase_server.clj` to match your JSON filename
   - Note: In production, you should use an environment variable to specify path to this JSON file instead of hard-coding it.
   - Important: Both `dev.cljc` and `prod.cljc` call `init-firebase!` during application startup to initialize the Firebase Admin SDK

**Security Notes**: 
- Never commit the Firebase Admin SDK JSON file to version control
- The `.gitignore` file already excludes `.secret/` to help prevent this
- For production deployment, use an environment variable to specify the path of the Firebase secret JSON file instead of hard coding it.
- The `DebugInfo` component in `main.cljc` exposes sensitive information and should never be used in production

**Libraries Used**:
- `com.google.firebase/firebase-admin {:mvn/version "9.4.3"}` in `deps.edn`
- `"firebase": "^11.6.1"` in `package.json`
- `"process": "^0.11.10"` in `package.json`. Needed in order for the firebase client-side library to work. This is a common issue when using shadow-cljs 3.x with various npm libraries - see [discussion on Clojurians slack](https://clojurians.slack.com/archives/C6N245JGG/p1745575597950839).

**Further Reading**:
- [Firebase Web Authentication Guide](https://firebase.google.com/docs/auth/web/start) - Essential reading for understanding how Firebase authentication works in web applications
- [Firebase Token Verification Guide](https://firebase.google.com/docs/auth/admin/verify-id-tokens) - Important documentation about server-side token verification

## db.cljs
The `db.cljs` file serves as a simple client-side state management system for the Firebase authentication data. Here are its main purposes:

1. It maintains a central atom (`!client-db`) that stores authentication-related state on the client side, specifically:
   - The current user object (`:user` key)
   - The current ID token (`:id-token` key)

2. It provides getter and setter functions to manipulate this state:
   - `set-user`/`remove-user`/`get-user` - to manage the Firebase user object
   - `set-id-token`/`remove-id-token`/`get-id-token` - to manage the Firebase ID token

3. This abstraction is used by `firebase_client.cljs` to keep track of authentication state changes and ID token updates from Firebase, and by `main.cljc` to access the current authentication state for rendering the UI appropriately.

The file provides a centralized place to store and access authentication state, rather than having it scattered throughout the application. This makes it easier to:

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
The `main.cljc` file demonstrates a Firebase authentication flow in an Electric application. Here's a breakdown:

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
         (if (e/server (:verified (verify-id-token id-token)))
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

The demo serves as both a reference implementation and a starting point for adding Firebase authentication to Electric applications. It shows how to protect server-side resources and actions.

Note: Do NOT use [FirebaseUI](https://firebase.google.com/docs/auth/web/firebaseui) as it is [no longer maintained](https://github.com/firebase/firebaseui-web/issues/1084).

## firebase_server.clj
The `firebase_server.clj` file serves as the server-side security layer for Firebase authentication. Here are its main purposes:

1. **Firebase SDK Initialization**
   - Initializes the Firebase Admin SDK using service account credentials
   - Provides the `init-firebase!` function that is called during startup in both `dev.cljc` and `prod.cljc`
   - For development, uses hard-coded path to a secret JSON file for authentication
   - In production, the path to the secret JSON file should be specified in an environment variable.

2. **Token Verification**
   - Provides functions to verify Firebase ID tokens:
     - `verify-id-token`: Core verification function that checks if a token is valid, and if so returns a map of "claims" including the `:user_id`.
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
   - Both functions expect a Firebase claims map as input (containing `:user_id`)

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

## Building an Uberjar

### META-INF License File Conflict

When building an uberjar, you may encounter a `Cannot write META-INF/license/LICENSE.aix-netbsd.txt` error. This is caused by a conflict between JARs included by the Firebase Admin SDK — specifically, the grpc-netty-shaded dependency used internally by Firebase. One library includes META-INF/license as a file, while another expects it to be a directory, which causes tools.build to fail when merging the classpath. 

To resolve this, you need to add `:exclude [#"META-INF/license/LICENSE.*"]` to the b/uber call in your build.clj. This safely skips the conflicting license files while still including all required Firebase functionality.

### Creating a Standalone Executable Jar

To create an uberjar that can be run with `java -jar target/app.jar`, modifications were made to `prod.cljc` and `build.clj`. See comments in those files labelled with initials `JTL`.

When building an uberjar to be run with `java -jar target/app.jar`, you may encounter the following error when running the jar:

```
Exception in thread "main" java.lang.NoClassDefFoundError: hyperfiddle/electric3$Apply (wrong name: hyperfiddle/electric3$apply)
```

I think this error occurs due to a case-sensitivity issue with the Clojure compiler when compiling `hyperfiddle.electric3/apply` and `hyperfiddle.electric3/Apply` on case-insensitive filesystems (like the default macOS filesystem). Both functions try to compile to the same filename on a case-insensitive system, causing the conflict.

The most reliable solution is to build the uberjar inside a Docker container with a case-sensitive filesystem. The starter app includes a Dockerfile.builder for this purpose. To build the uberjar, use this one-liner, which builds the app inside Docker and extracts the resulting jar file, which will work correctly with `java -jar target/app.jar`.

```bash
VERSION=$(git describe --tags --long --always --dirty) \
&& docker build -f Dockerfile.builder --build-arg VERSION=$VERSION -t electric3-starter-app-builder . \
&& docker create --name extract-electric electric3-starter-app-builder \
&& rm -rf target && mkdir -p target \
&& docker cp extract-electric:/app/target/app.jar target/app.jar \
&& docker rm extract-electric \
&& echo "✅ Built target/app.jar with version: $VERSION"
```




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
java -jar target/app.jar

# Docker
docker build -t electric3-starter-app:latest .
docker run --rm -it -p 8080:8080 electric3-starter-app:latest
```

## License
Electric v3 is **free for bootstrappers and non-commercial use,** and otherwise available commercially under a business source available license, see: [Electric v3 license change](https://tana.pub/lQwRvGRaQ7hM/electric-v3-license-change) (2024 Oct). License activation is experimentally implemented through the Electric compiler, requiring **compile-time** login for **dev builds only**. That means: no license check at runtime, no login in prod builds, CI/CD etc, no licensing code even on the classpath at runtime. This is experimental, but seems to be working great so far. We do not currently require any account approval steps, just log in. There will be a EULA at some point once we finalize the non-commercial license, for now we are focused on enterprise deals which are different.
