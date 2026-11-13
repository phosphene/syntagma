(ns phosphene.syntagma.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.morphism :as morphism]
            [phosphene.syntagma.spec :as syntagma-spec]))

(deftest unit-relational-tiers-test
  (testing "Strict contiguous factor"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/factor? [:magnetic :curve] frame)))
      (is (true? (core/strict-subsequence? [:magnetic :curve] frame)))
      (is (false? (core/factor? [:magnetic :observed] frame)))
      (is (= :strict (core/relational-tier [:magnetic :curve] frame)))))

  (testing "Loose monotonic subsequence (allowing intervening tokens)"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/subsequence? [:magnetic :observed] frame)))
      (is (true? (core/loose-subsequence? [:magnetic :observed] frame)))
      (is (true? (core/subsequence? [:faraday :needle] frame)))
      ;; Inverted order must fail subsequence
      (is (false? (core/subsequence? [:needle :faraday] frame)))
      (is (= :loose (core/relational-tier [:magnetic :observed] frame)))))

  (testing "Framed conjunction (unordered co-presence)"
    (let [frame [:faraday :magnetic :curve :observed :needle]]
      (is (true? (core/conjunction? [:needle :faraday] frame)))
      (is (true? (core/framed-conjunction? [:needle :faraday] frame)))
      (is (= :conjunction (core/relational-tier [:needle :faraday] frame)))
      (is (false? (core/conjunction? [:needle :maxwell] frame)))
      (is (= :none (core/relational-tier [:needle :maxwell] frame)))))

  (testing "Equivalence-class projection"
    (let [equiv-map {:curves :curve
                     :lines :curve
                     :observed :seen}
          equiv-fn (fn [tok] (get equiv-map tok tok))
          frame [:magnetic :lines :observed]]
      (is (true? (core/equiv-factor? [:magnetic :curves] frame equiv-fn)))
      (is (true? (core/equiv-subsequence? [:magnetic :curves] frame equiv-fn)))
      (is (false? (core/factor? [:magnetic :curves] frame))))))

(deftest generative-spec-check-test
  (testing "clojure.spec.test.alpha generative check on relational invariants with dense alphabet"
    (let [check-results (stest/check [`core/strict-subsequence?
                                      `core/loose-subsequence?
                                      `core/framed-conjunction?
                                      `core/relational-tier
                                      `morphism/map-sequence
                                      `morphism/factor-preserving?
                                      `morphism/subsequence-preserving?
                                      `morphism/conjunction-preserving?
                                      `morphism/order-preserving?
                                      `morphism/relational-fidelity]
                                     {:clojure.spec.test.check/opts {:num-tests 150}})]
      (doseq [res check-results]
        (let [sym (:sym res)
              check-ret (:clojure.spec.test.check/ret res)
              failure (:error check-ret)]
          (is (nil? failure) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret))
              (str "Check result was not true for " sym ": " (pr-str check-ret))))))))

(deftest coupled-generator-tier-density-test
  (testing "Coupled generator hits all tiers (:strict, :loose, :conjunction, :none) across 200 trials"
    (let [observed-tiers (atom #{})
          prop (prop/for-all [[target frame] (syntagma-spec/gen-target-and-frame)]
                 (let [tier (core/relational-tier target frame)]
                   (swap! observed-tiers conj tier)
                   (case tier
                     :strict (and (core/strict-subsequence? target frame)
                                  (core/loose-subsequence? target frame)
                                  (core/framed-conjunction? target frame))
                     :loose (and (not (core/strict-subsequence? target frame))
                                 (core/loose-subsequence? target frame)
                                 (core/framed-conjunction? target frame))
                     :conjunction (and (not (core/loose-subsequence? target frame))
                                       (core/framed-conjunction? target frame))
                     :none (not (core/framed-conjunction? target frame)))))
          result (tc/quick-check 200 prop)]
      (is (true? (:result result)) (str "Coupled property check failed: " (pr-str result)))
      ;; Verify that dense coupled generation successfully explored all 4 mutually exclusive tiers!
      (is (= #{:strict :loose :conjunction :none} @observed-tiers)
          (str "Expected all 4 tiers to be generated, but saw: " @observed-tiers)))))
