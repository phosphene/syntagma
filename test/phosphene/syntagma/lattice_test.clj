(ns phosphene.syntagma.lattice-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.lattice :as lattice]))

;; --- Specs for lattice ---

(s/fdef lattice/longest-common-factor
  :args (s/cat :a ::core/sequence :b ::core/sequence)
  :ret ::core/sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [a b]} args]
          ;; Meet invariant: ret must be a strict factor of both a and b
          (and (core/strict-subsequence? ret a)
               (core/strict-subsequence? ret b)))))

(s/fdef lattice/shortest-common-supersequence
  :args (s/cat :a ::core/sequence :b ::core/sequence)
  :ret ::core/sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [a b]} args]
          ;; Join invariant: ret must contain both a and b as loose monotonic subsequences
          (and (core/loose-subsequence? a ret)
               (core/loose-subsequence? b ret)))))

(deftest unit-lattice-test
  (testing "Longest common factor (meet)"
    (let [s1 [:faraday :observed :magnetic :lines :of :force]
          s2 [:maxwell :verified :magnetic :lines :in :field]]
      (is (= [:magnetic :lines] (lattice/longest-common-factor s1 s2))))
    (is (= [] (lattice/longest-common-factor [:a :b] [:c :d]))))

  (testing "Prefix and suffix order relations"
    (let [target [:faraday :magnetic]
          longer [:faraday :magnetic :curves :observed]]
      (is (true? (lattice/prefix? target longer)))
      (is (false? (lattice/suffix? target longer)))
      (is (true? (lattice/suffix? [:curves :observed] longer)))))

  (testing "Shortest common supersequence (join)"
    (let [s1 [:a :b :d]
          s2 [:b :c :d]
          scs (lattice/shortest-common-supersequence s1 s2)]
      (is (core/loose-subsequence? s1 scs))
      (is (core/loose-subsequence? s2 scs))
      (is (= [:a :b :c :d] scs))))

  (testing "Subword poset DAG generation"
    (let [seqs [[:faraday]
                [:faraday :magnetic]
                [:faraday :magnetic :curve]
                [:unrelated]]
          edges (lattice/subword-poset-edges seqs)]
      (is (contains? edges [[:faraday] [:faraday :magnetic]]))
      (is (contains? edges [[:faraday :magnetic] [:faraday :magnetic :curve]]))
      (is (contains? edges [[:faraday] [:faraday :magnetic :curve]]))
      (is (not (contains? edges [[:unrelated] [:faraday]]))))))

(deftest generative-lattice-check-test
  (testing "Generative property-based testing of lattice meets and joins"
    (let [res (stest/check [`lattice/longest-common-factor
                            `lattice/shortest-common-supersequence]
                           {:clojure.spec.test.check/opts {:num-tests 50}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
