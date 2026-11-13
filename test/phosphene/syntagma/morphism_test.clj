(ns phosphene.syntagma.morphism-test
  (:require [clojure.test :refer [deftest is testing]]
            [phosphene.syntagma.core :as syntagma]
            [phosphene.syntagma.morphism :as morphism]))

(deftest vocabulary-morphism-basics-test
  (testing "Token mapping and fallback behavior"
    (let [m (morphism/vocabulary-morphism
              :morphism/chemistry
              {:phlogiston :oxygen
               :calx :metal-oxide}
              {:fallback (fn [x] (keyword (str "unknown-" (name x))))})]
      (is (true? (morphism/morphism? m)))
      (is (= :oxygen (morphism/apply-token m :phlogiston)))
      (is (= :metal-oxide (morphism/apply-token m :calx)))
      (is (= :unknown-heat (morphism/apply-token m :heat)))
      (is (= [:metal-oxide :oxygen]
             (morphism/map-sequence m [:calx :phlogiston]))))))

(deftest order-preservation-and-fidelity-test
  (testing "Order preservation under bijective translation"
    (let [m (morphism/vocabulary-morphism
              :morphism/trans
              {:a :A :b :B :c :C :d :D})
          target [:b :c]
          frame [:a :b :c :d]]
      ;; Target is contiguous factor in frame
      (is (= :strict (syntagma/relational-tier target frame)))
      (is (true? (morphism/factor-preserving? m target frame)))
      (is (true? (morphism/subsequence-preserving? m target frame)))
      (is (true? (morphism/conjunction-preserving? m target frame)))
      (is (true? (morphism/order-preserving? m target frame)))

      (let [fid (morphism/relational-fidelity m target frame)]
        (is (= :strict (:source-tier fid)))
        (is (= :strict (:target-tier fid)))
        (is (true? (:preserved? fid)))
        (is (false? (:degraded? fid)))
        (is (false? (:amplified? fid))))))

  (testing "Relational amplification under collapsing morphisms"
    (let [;; Collapse both :x and :y to :M
          m (morphism/vocabulary-morphism
              :morphism/collapsing
              {:x :M :y :M :a :A :b :B})
          target [:x :y]
          frame [:x :a :y :b]]
      ;; In source: target is loose subsequence (separated by :a)
      (is (= :loose (syntagma/relational-tier target frame)))
      (is (true? (morphism/subsequence-preserving? m target frame)))

      (let [fid (morphism/relational-fidelity m target frame)]
        (is (= :loose (:source-tier fid)))
        (is (= :loose (:target-tier fid)))
        (is (true? (:preserved? fid))))))

  (testing "Inversion of bijective morphisms"
    (let [m (morphism/vocabulary-morphism
              :morphism/simple
              {:p :q :r :s})
          inv (morphism/invert-morphism m)]
      (is (= :p (morphism/apply-token inv :q)))
      (is (= :r (morphism/apply-token inv :s)))
      (is (= [:p :r] (morphism/map-sequence inv [:q :s]))))))
