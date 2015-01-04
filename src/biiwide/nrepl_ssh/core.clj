(ns biiwide.nrepl-ssh.core
  (:require [clj-ssh.cli :as ssh-cli :refer [session]]
            [clj-ssh.ssh   :as ssh]
            [clojure.string :as str]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.transport :as transport])
  (:import  (clojure.tools.nrepl.transport FnTransport)
            (com.jcraft.jsch Channel Session)))


(defn- parse-int-param [param str]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException nfe
      (throw (NumberFormatException.
               (format "URI parameter %s: \"%s\" is not an integer" (name param) str))))))

(defn- split-pairs [s]
  (when s
    (remove str/blank?
      (str/split s #"\&"))))

(defn- kv-parser [transformations]
  (fn [m s]
    (let [[k v] (str/split s #"=" 2)
          k (keyword k)
          v (or v "")]
      (assoc m k
        (if (str/blank? v) nil
          (if-some [xform (transformations k)]
            (xform k v) v))))))

(let [parse-pair (kv-parser {:ssh-port parse-int-param
                             :transport-port parse-int-param})]
  (defn- parse-opts [^String s]
    (->> (split-pairs s)
         (reduce parse-pair nil))))

(let [defaults {:transport :nrepl
                :username (System/getProperty "user.name")
                :ssh-port 22
                :transport-host "localhost"
                :transport-port 7888}]
  (defn parse-uri
    "Parses URIs for nREPL connections over SSH
 ssh://[ssh-host]:[transport-port#]
 (nrepl|telnet)+ssh://[ssh-host]:[transport-port#]
 [transport]+ssh://[username]@[ssh-host]:[transport-port#]
 [transport]+ssh://[ssh-host]:[transport-port#]?ssh-port=[ssh-port#]&transport-host=[transport-host]"
    [^String uri]
    {:pre [(string? uri)]}
    (let [lc-uri (.toLowerCase uri)
          [match _ transport _ username host _ port _ opts]
          (re-find #"^((nrepl|telnet)\+)?ssh://(([^@]*)@)?([^:/?]+)(:([^:/?]*))?(\?(.*))?" lc-uri)]
      (if-not match
        (throw (RuntimeException.
                 (format "Invalid URI: \"%s\"" uri)))
        (-> (merge defaults (parse-opts opts))
            (assoc :ssh-host host)
            (cond-> 
              transport (assoc :transport (keyword transport))
              username  (assoc :username username)
              port      (assoc :transport-port (parse-int :transport-port port))
              ))))))

(defn direct-tcpip-channel
  "Open a direct TCP/IP connection from the remote host
   to the host and port provided"
  [^Session session host port]
  {:pre [(string? host) (integer? port)]}
  (let [^Channel channel (ssh/open-channel session :direct-tcpip)]
    (doto channel
      (.setHost host)
      (.setPort port))))

(defn- close-fn
  "Returns a function that will close the provided session and channel"
  [session channel]
  (fn []
    (ssh/disconnect-channel channel)
    (ssh/disconnect session)))

(defn wrapped-transport [^FnTransport transport close]
  (let [recv-fn (.recv_fn transport)
        send-fn (.send_fn transport)]
    (transport/->FnTransport recv-fn send-fn close)))

(let [transport-fns {:nrepl transport/bencode
                     :telnet transport/tty}]
  (defn ssh-transport
    "Establishes an SSH connection and wraps it in an FnTransport"
    [{:keys [transport transport-fn username ssh-host ssh-port transport-host transport-port]}]
    {:pre [(get transport-fns transport)
           (string? username)
           (string? ssh-host)
           (integer? ssh-port) (pos? ssh-port)
           (string? transport-host)
           (integer? transport-port) (pos? transport-port)]}
    (let [transport-fn (or transport-fn (get transport-fns transport))
          session (session ssh-host :port ssh-port :username username)
          _ (ssh/connect session)
          ^Channel channel (direct-tcpip-channel session transport-host transport-port)
          _ (ssh/connect-channel channel)]
      (-> (transport-fn (.getInputStream channel) (.getOutputStream channel))
          (wrapped-transport (close-fn session channel))))))


(defn- add-url-scheme! [scheme]
  (defmethod nrepl/url-connect scheme
    [uri]
    (ssh-transport (parse-uri uri))))

(add-url-scheme! "ssh")
(add-url-scheme! "nrepl+ssh")
(add-url-scheme! "telnet+ssh")
