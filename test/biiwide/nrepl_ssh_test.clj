(ns biiwide.nrepl-ssh-test
  (:require [clojure.test :refer :all]
            [biiwide.nrepl-ssh :refer :all]))

(let [default-params {:transport :nrepl
                      :username (System/getProperty "user.name")
                      :ssh-port 22
                      :transport-host "localhost"
                      :transport-port 7888}]
  (defn- connection-params [ssh-host & {:as more-params}]
    (-> (assoc default-params :ssh-host ssh-host)
        (merge more-params))))

(deftest parse-uri-test
  (testing "URI Parsing"
    (are [uri expected] (= expected (parse-uri uri))
      "ssh://host1"
      (connection-params "host1")

      "nrepl+ssh://host2"
      (connection-params "host2")

      "telnet+ssh://host3"
      (connection-params "host3" :transport :telnet)

      "ssh://host4:54321"
      (connection-params "host4" :transport-port 54321)

      "ssh://host5:12345?ssh-port=2222"
      (connection-params "host5" :transport-port 12345 :ssh-port 2222)

      "ssh://joe-random@host6"
      (connection-params "host6" :username "joe-random")

      "telnet+ssh://user1@relay-host7:7889?ssh-port=2222&transport-host=other-host"
      (connection-params "relay-host7" :transport :telnet
        :username "user1" :transport-port 7889
        :ssh-port 2222 :transport-host "other-host")

      "nrepl+ssh://user2@relay-host8:7889?ssh-port=2222&transport-host=other-host"
      (connection-params "relay-host8" :transport :nrepl
        :username "user2" :transport-port 7889
        :ssh-port 2222 :transport-host "other-host")

      "ssh://host9?no-value&some-param=value&whitespace=  "
      (connection-params "host9" :no-value nil :whitespace nil :some-param "value")
      )))
