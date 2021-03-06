(ns langohr.test.http-api-test
  (:require [langohr.http :as hc])
  (:use clojure.test
        [clojure.set :only [subset? superset?]]))

(hc/connect! "http://127.0.0.1:55672" "guest" "guest")

;;
;; These tests are pretty basic and make sure we don't
;; have any obvious issues. The results are returns as
;; JSON responses parsed into Clojure maps.
;;

(deftest ^{:http true} test-get-overview
  (let [r (hc/get-overview)]
    (is (get-in r [:queue_totals :messages]))
    (is (get-in r [:queue_totals :messages_ready]))
    (is (get-in r [:queue_totals :messages_unacknowledged]))
    (is (get-in r [:queue_totals :messages_details :rate]))))

(deftest ^{:http true} test-list-nodes
  (let [r (hc/list-nodes)
        n (first r)]
    (is (coll? r))
    (doseq [keys [[:proc_total]
                  [:disk_free]
                  [:sockets_total]
                  [:sockets_used]]]
      (is (get-in n keys)))))

(deftest ^{:http true} test-list-extensions
  (let [r (hc/list-extensions)
        e (first r)]
    (is (get e :javascript))))

(deftest ^{:http true} test-list-definitoins
  (let [r           (hc/list-definitions)
        vhosts      (:vhosts r)
        exchanges   (:exchanges r)
        queues      (:queues r)
        bindings    (:bindings r)
        parameters  (:parameters r)
        policies    (:policies r)
        permissions (:permissions r)]
    (is (:rabbit_version r))
    (is (:name (first vhosts)))
    (is (:name (first queues)))
    (is (:name (first exchanges)))
    (is (:source (first bindings)))
    (is (:user (first permissions)))))

(deftest ^{:http true} test-list-connections
  (let [r (hc/list-connections)]
    (is (coll? r))))

(deftest ^{:http true} test-list-channels
  (let [r (hc/list-channels)]
    (is (coll? r))))

(deftest ^{:http true} test-list-exchanges
  (let [r (hc/list-exchanges)]
    (is (coll? r)))
  (let [r (hc/list-exchanges "/")]
    (is (coll? r))))

(deftest ^{:http true} test-get-exchange
  (let [r (hc/get-exchange "/" "amq.fanout")]
    (is (= r {:name "amq.fanout" :vhost "/" :type "fanout" :durable true :auto_delete false :internal false :arguments {}}))))

(deftest ^{:http true} test-declare-and-delete-exchange
  (let [s  "langohr.http.fanout"
        r1 (hc/declare-exchange "/" s {:durable false :auto_delete true :internal false :arguments {}})
        r2 (hc/delete-exchange "/" s)]
    (is (= true r1))
    (is (= true r2))))

(deftest ^{:http true} test-list-queues
  (let [r (hc/list-queues)]
    (is (coll? r)))
  (let [r (hc/list-queues "/")]
    (is (coll? r))))

(deftest ^{:http true} test-declare-and-delete-queue
  (let [s  "langohr.http.queue"
        r1 (hc/declare-queue "/" s {:durable false :auto_delete true :arguments {}})
        r2 (hc/delete-queue "/" s)]
    (is (= true r1))
    (is (= true r2))))


(deftest ^{:http true} test-declare-and-purge-queue
  (let [s  "langohr.http.queue"
        r1 (hc/declare-queue "/" s {:durable false :auto_delete true :arguments {}})
        r2 (hc/purge-queue "/" s)
        _  (hc/delete-queue "/" s)]
    (is (= true r1))
    (is (= true r2))))

(deftest ^{:http true} test-list-bindings
  (let [q  "langohr.http.queue"
        e  "langohr.http.fanout"
        r1 (hc/bind "/" e q)
        xs (hc/list-bindings "/")
        m  (first xs)]
    (is r1)
    (is (coll? xs))
    (is (:source m))
    (is (:destination m))
    (is (:vhost m))
    (is (= "queue" (:destination_type m)))))

(deftest ^{:http true} test-list-vhosts
  (let [xs (hc/list-vhosts)]
    (is (subset? #{"/"} (set (map :name xs))))))
