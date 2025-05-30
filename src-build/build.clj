(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.logging :as log]
            [shadow.cljs.devtools.api :as shadow-api]
            [shadow.cljs.devtools.server :as shadow-server]))

(def electric-user-version (b/git-process {:git-args "describe --tags --long --always --dirty"}))

(defn build-client "
invoke like: clj -X:build:prod build-client`
Note: do not use `clj -T`, because Electric shadow compilation requires
application classpath to be available"
  [argmap]
  (let [{:keys [optimize debug verbose]
         :or {optimize true, debug false, verbose false}
         :as config}
        (-> argmap
          (assoc :hyperfiddle.electric-ring-adapter3/electric-user-version electric-user-version))]
    (log/info 'build-client (pr-str config #_argmap))
    (b/delete {:path "resources/public/electric_starter_app/js"})
    (b/delete {:path "resources/electric-manifest.edn"})

    ; bake electric-user-version into artifact, cljs and clj
    (b/write-file {:path "resources/electric-manifest.edn" :content config})

    ; "java.lang.NoClassDefFoundError: com/google/common/collect/Streams" is fixed by
    ; adding com.google.guava/guava {:mvn/version "31.1-jre"} to deps,
    ; see https://hf-inc.slack.com/archives/C04TBSDFAM6/p1692636958361199
    (shadow-server/start!)
    (as->
        (shadow-api/release :prod
          {:debug   debug,
           :verbose verbose,
           :config-merge
           [{:compiler-options {:optimizations (if optimize :advanced :simple)}
             :closure-defines  {'hyperfiddle.electric-client3/ELECTRIC_USER_VERSION electric-user-version}}]})
        shadow-status (assert (= shadow-status :done) "shadow-api/release error")) ; fail build on error
    (shadow-server/stop!)
    (log/info "client built")))

(def class-dir "target/classes")

(defn uberjar
  [{:keys [optimize debug verbose ::jar-name, ::skip-client]
    :or {optimize true, debug false, verbose false, skip-client false}
    :as args}]
  ; careful, shell quote escaping combines poorly with clj -X arg parsing, strings read as symbols
  (log/info 'uberjar (pr-str args))
  (b/delete {:path "target"})

  (when-not skip-client
    (build-client {:optimize optimize, :debug debug, :verbose verbose}))

  (b/copy-dir {:target-dir class-dir :src-dirs ["src" "src-prod" "resources"]})
  (let [jar-name (or (some-> jar-name str) ; override for Dockerfile builds to avoid needing to reconstruct the name
                   (format "electricfiddle-%s.jar" electric-user-version))
        aliases [:prod]
        basis (b/create-basis {:project "deps.edn" :aliases aliases})] ; JTL added line
    ;; JTL added b/compile-clj form below
    (b/compile-clj {:basis basis
                    :ns-compile '[prod]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file (str "target/" jar-name)
             :basis     basis ; JTL changed line
             :main      'prod ; JTL added line
             ; JTL added exclusion to avoid conflict between JARs included by the Firebase Admin SDK. See README.md
             :exclude [#"META-INF/license/LICENSE.*"]})
    (log/info jar-name)))
