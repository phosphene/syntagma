(ns phosphene.syntagma.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.spec :as syntagma-spec]))

(deftest unit-relational-tiers-test
  (testing "Strict contiguous factor"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/strict-subsequence? [:magnetic :curve] frame)))
      (is (false? (core/strict-subsequence? [:magnetic :observed] frame)))
      (is (= :strict (core/relational-tier [:magnetic :curve] frame)))))

  (testing "Loose monotonic subsequence (allowing intervening tokens)"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/loose-subsequence? [:magnetic :observed] frame)))
      (is (true? (core/loose-subsequence? [:faraday :needle] frame)))
      ;; Inverted order must fail loose
      (is (false? (core/loose-subsequence? [:needle :faraday] frame)))
      (is (= :loose (core/relational-tier [:magnetic :observed] frame)))))

  (testing "Framed conjunction (unordered co-presence)"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/framed-conjunction? [:needle :faraday] frame)))
      (is (= :conjunction (core/relational-tier [:needle :faraday] frame)))
      (is (false? (core/framed-conjunction? [:needle :maxwell] frame)))
      (is (= :none (core/relational-tier [:needle :maxwell] frame)))))

  (testing "Equivalence-class projection"
    (let [equiv-map {:curves :curve
                     :lines :curve
                     :observed :seen}
          equiv-fn (fn [tok] (get equiv-map tok tok))
          frame [:magnetic :lines :observed]]
      (is (true? (core/equiv-subsequence? [:magnetic :curves] frame equiv-fn)))
      (is (false? (core/strict-subsequence? [:magnetic :curves] frame))))))

(deftest generative-spec-check-test
  (testing "clojure.spec.test.alpha generative check on relational invariants"
    (let [check-results (stest/check [`core/strict-subsequence?
                                      `core/loose-subsequence?
                                      `core/framed-conjunction?
                                      `core/relational-tier]
                                     {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [res check-results]
        (let [sym (:sym res)
              check-ret (:clojure.spec.test.check/ret res)
              failure (:error check-ret)]
          (is (nil? failure) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret))
              (str "Check result was not true for " sym ": " (pr-str check-ret))))))))
