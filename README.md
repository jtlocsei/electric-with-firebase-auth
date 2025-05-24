# Firebase Auth With Electric 

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
