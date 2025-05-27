FROM clojure:temurin-21-tools-deps AS build
WORKDIR /app

# Install Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
  && apt-get update \
  && apt-get install -y nodejs \
  && node --version && npm --version

# Install frontend dependencies
COPY package*.json ./
RUN npm install

# Copy and cache Clojure deps separately
COPY deps.edn deps.edn
RUN clojure -A:build:prod -M -e ::ok   # preload and cache dependencies, only reruns if deps.edn changes

# Copy rest of the project
COPY shadow-cljs.edn shadow-cljs.edn
COPY src src
COPY src-build src-build
COPY src-prod src-prod
COPY resources resources

ARG VERSION
ENV VERSION=$VERSION

RUN clojure -X:build:prod uberjar :version "\"$VERSION\"" :build/jar-name "app.jar"