(ns phosphene.syntagma.lattice-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.test.alpha :as stest]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.lattice :as lattice]
            [phosphene.syntagma.spec :as syntagma-spec]))

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
  (testing "Generative property-based testing of lattice meets, joins, prefixes, and poset edges"
    (let [res (stest/check [`lattice/longest-common-factor
                            `lattice/prefix?
                            `lattice/suffix?
                            `lattice/shortest-common-supersequence
                            `lattice/subword-poset-edges]
                           {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
