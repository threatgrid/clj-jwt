(ns clj-jwt.core
  (:require
    [clj-jwt.base64      :refer [url-safe-encode-str url-safe-decode-str]]
    [clj-jwt.sign        :refer [get-signature-fn get-verify-fn supported-algorithm?]]
    [clj-jwt.intdate     :refer [joda-time->intdate]]
    [clj-jwt.json-key-fn :refer [write-key read-key]]
    [clojure.string      :as str]
    [jsonista.core       :as jsonista]))

(def ^:private DEFAULT_SIGNATURE_ALGORITHM :HS256)
(def ^:private DEFAULT_KID nil)

(def ^:private jsonista-mapper
  (jsonista/object-mapper {:encode-key-fn write-key
                           :decode-key-fn read-key}))

(def ^:private map->encoded-json (comp url-safe-encode-str
                                       (fn [m]
                                         (jsonista/write-value-as-string m jsonista-mapper))))

(def ^:private encoded-json->map (comp (fn [^String s]
                                         (jsonista/read-value s jsonista-mapper))
                                       url-safe-decode-str))

(defn- update-map [m k f] (if (contains? m k) (update-in m [k] f) m))

(defrecord JWT [header claims signature encoded-data])

; ----------------------------------
; JsonWebToken
; ----------------------------------
(defprotocol JsonWebToken
  "Protocol for JsonWebToken"
  (init           [this claims] "Initialize token")
  (encoded-header [this] "Get url-safe base64 encoded header json")
  (encoded-claims [this] "Get url-safe base64 encoded claims json")
  (to-str         [this] "Generate JsonWebToken as string"))

(extend-protocol JsonWebToken
  JWT
  (init [this claims]
    (let [claims (reduce #(update-map % %2 joda-time->intdate) claims [:exp :nbf :iat])]
      (assoc this :header {:alg "none" :typ "JWT"} :claims claims :signature "")))

  (encoded-header [this]
    (-> this :header map->encoded-json))

  (encoded-claims [this]
    (-> this :claims map->encoded-json))

  (to-str [this]
    (str (encoded-header this) "." (encoded-claims this) "." (get this :signature ""))))


; ----------------------------------
; JsonWebSignature
; ----------------------------------
(defprotocol JsonWebSignature
  "Protocol for JonWebSignature"
  (set-alg [this alg] "Set algorithm name to JWS Header Parameter")
  (set-kid [this kid] "Set Key ID to JWS Header Parameter")
  (sign    [this key] [this alg key] [this alg key kid] "Set signature to this token")
  (verify  [this] [this key] [this algorithm key] "Verify this token"))

(extend-protocol JsonWebSignature
  JWT
  (set-alg [this alg]
    (assoc-in this [:header :alg] (name alg)))
  (set-kid [this kid]
    (assoc-in this [:header :kid] kid))
  (sign
    ([this key] (sign this DEFAULT_SIGNATURE_ALGORITHM key DEFAULT_KID))
    ([this alg key] (sign this alg key DEFAULT_KID))
    ([this alg key kid]
     (let [this* (cond-> (set-alg this alg)
                   (some? kid) (set-kid kid))
           sign-fn (get-signature-fn alg)
           data    (str (encoded-header this*) "." (encoded-claims this*))]
       (assoc this* :signature (sign-fn key data) :encoded-data data))))

  (verify
    ([this] (verify this ""))
    ([this key]
     (let [alg (-> this :header :alg keyword)]
       (cond
         (= :none alg) (= "" key (:signature this))

         (supported-algorithm? alg)
         (let [verify-fn (get-verify-fn alg)]
           (verify-fn key (:encoded-data this) (:signature this)))

         :else (throw (Exception. "Unkown signature")))))
    ([this algorithm key]
     (if (= algorithm (-> this :header :alg keyword))
       (verify this key)
       false))))

; =jwt
(defn jwt [claim] (init (->JWT "" "" "" "") claim))

; =str->jwt
(defn str->jwt
  [jwt-string]
  (let [[header claims signature] (str/split jwt-string #"\.")]
    (->JWT (encoded-json->map header)
           (encoded-json->map claims)
           (or signature "")
           (str header "." claims))))
