(ns phosphene.syntagma.distribution-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [phosphene.syntagma.distribution :as dist]
            [phosphene.syntagma.spec :as syntagma-spec]))

(deftest distribution-primitives-test
  (testing "Sliding windows partitioning"
    (let [tokens [:a :b :c :d :e]
          windows (dist/sliding-windows tokens 3)]
      (is (= [[:a :b :c] [:b :c :d] [:c :d :e]] windows))))

  (testing "Co-occurrence matrix computation"
    (let [tokens [:faraday :magnetic :curve :faraday :magnetic :force]
          matrix (dist/co-occurrence-matrix tokens 1)]
      (is (= 2 (get-in matrix [:faraday :magnetic])))
      (is (= 2 (get-in matrix [:magnetic :faraday])))))

  (testing "Framed conjunction frequency over AST frames"
    (let [frames [[:faraday :observed :magnetic :lines]
                  [:maxwell :derived :equations :electric]
                  [:faraday :curves :and :lines :magnetic]]
          freq (dist/framed-conjunction-frequency [:faraday :magnetic] frames)]
      (is (= 2 freq))))

  (testing "Loose sequence frequency over AST frames"
    (let [frames [[:faraday :observed :magnetic :curve]
                  [:magnetic :observed :faraday]
                  [:faraday :skips :other :tokens :magnetic :finally :curve]]
          freq (dist/loose-sequence-frequency [:faraday :magnetic :curve] frames)]
      (is (= 2 freq)))))

(deftest hal-matrix-test
  (testing "Hyperspace Analogue to Language (HAL) asymmetric ramped weighting"
    ;; Window radius K = 2.
    ;; Weights: d=1 -> w = 2 - 1 + 1 = 2
    ;;          d=2 -> w = 2 - 2 + 1 = 1
    (let [tokens [:the :magnetic :lines :of :force]
          hal (dist/hal-matrix tokens 2)]
      ;; Focus on :lines (index 2):
      ;; Preceding: :magnetic (d=1, w=2), :the (d=2, w=1)
      ;; Succeeding: :of (d=1, w=2), :force (d=2, w=1)
      (is (= 2 (get-in hal [:backward :lines :magnetic])))
      (is (= 1 (get-in hal [:backward :lines :the])))
      (is (= 2 (get-in hal [:forward :lines :of])))
      (is (= 1 (get-in hal [:forward :lines :force])))

      ;; Asymmetry check:
      ;; For :magnetic (index 1), :lines is forward (d=1, w=2)
      (is (= 2 (get-in hal [:forward :magnetic :lines])))
      (is (nil? (get-in hal [:backward :magnetic :lines])))))

  (testing "HAL profile vector concatenation with directional prefixes"
    (let [tokens [:magnetic :lines :of :force]
          hal (dist/hal-matrix tokens 2)
          vec-lines (dist/hal-vector hal :lines)]
      (is (= 2 (:before/magnetic vec-lines)))
      (is (= 2 (:after/of vec-lines)))
      (is (= 1 (:after/force vec-lines))))))

(deftest second-order-context-aggregation-test
  (testing "Second-order context vector aggregation across multiple AST frames"
    (let [frame1 [:faraday :observed :magnetic :lines :of :force]
          frame2 [:maxwell :formalized :magnetic :curves :mathematically]
          frame3 [:heat :and :temperature :in :thermodynamics]
          frames [frame1 frame2 frame3]
          agg-magnetic (dist/aggregate-context-vectors frames :magnetic 2)]
      ;; :magnetic appears in frame1 and frame2
      (is (pos? (:after/lines agg-magnetic)))
      (is (pos? (:after/curves agg-magnetic)))
      (is (pos? (:before/faraday agg-magnetic)))
      (is (pos? (:before/maxwell agg-magnetic)))
      ;; :thermodynamics is not in any frame with :magnetic
      (is (nil? (:after/thermodynamics agg-magnetic)))))

  (testing "Cosine similarity between distributional vectors"
    (let [v1 {:before/a 2.0 :after/b 1.0}
          v2 {:before/a 4.0 :after/b 2.0}
          v3 {:before/x 1.0 :after/y 3.0}]
      (is (> (dist/cosine-similarity v1 v2) 0.999))
      (is (zero? (dist/cosine-similarity v1 v3))))))

(deftest generative-distribution-check-test
  (testing "Generative property checks for distributional primitives"
    (let [res (stest/check [`dist/sliding-windows
                            `dist/framed-conjunction-frequency
                            `dist/loose-sequence-frequency]
                           {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))

    (testing "HAL matrix generative property"
      (let [hal-prop (prop/for-all [tokens (clojure.spec.alpha/gen :phosphene.syntagma.spec/sequence)
                                    radius (clojure.spec.alpha/gen :phosphene.syntagma.spec/radius)]
                       (let [res (dist/hal-matrix tokens radius)]
                         (and (= (:radius res) radius)
                              (map? (:forward res))
                              (map? (:backward res)))))
            qc-hal (tc/quick-check 100 hal-prop)]
        (is (true? (:result qc-hal)))))

    (testing "Cosine similarity generative property"
      (let [cos-prop (prop/for-all [v1 (gen/map (gen/elements [:a :b :c :d]) (gen/choose 1 10) {:max-elements 4})
                                    v2 (gen/map (gen/elements [:a :b :c :d]) (gen/choose 1 10) {:max-elements 4})]
                       (let [sim (dist/cosine-similarity v1 v2)]
                         (and (number? sim) (<= -1.0001 sim 1.0001))))
            qc-cos (tc/quick-check 100 cos-prop)]
        (is (true? (:result qc-cos)))))))
