(ns phosphene.syntagma.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.morphism :as morphism]
            [phosphene.syntagma.textuality :as text]
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
                                      `morphism/relational-fidelity
                                      `text/tmesis-index
                                      `text/factor-meet-ratio
                                      `text/supersequence-join-ratio
                                      `text/textuality-profile]
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

(deftest textuality-and-tmesis-diagnostics-test
  (testing "Tmesis index (skipping/dispersion)"
    (let [frame [:the :magnetic :lines :curve :around :the :conducting :wire]]
      ;; Contiguous factor -> zero tmesis
      (is (== 0.0 (text/tmesis-index [:magnetic :lines :curve] frame)))
      ;; Loose subsequence -> positive tmesis
      (let [idx (text/tmesis-index [:magnetic :curve :wire] frame)]
        (is (pos? idx))
        (is (<= idx 1.0)))
      ;; Unordered conjunction -> max tmesis (1.0)
      (is (== 1.0 (text/tmesis-index [:wire :magnetic] frame)))
      ;; Not present -> nil
      (is (nil? (text/tmesis-index [:galvanometer] frame)))))

  (testing "Factor meet and supersequence join ratios"
    (let [seq-a [:a :b :c :d]
          seq-b [:b :c :e :f]]
      (is (== 0.5 (text/factor-meet-ratio seq-a seq-b)))
      (is (<= 0.0 (text/supersequence-join-ratio seq-a seq-b) 1.0))))

  (testing "Comparative textuality profiling: readerly vs writerly"
    (let [readerly-corpus [[:once :upon :a :time :in :the :kingdom]
                           [:once :upon :a :time :in :the :castle]
                           [:once :upon :a :time :in :the :forest]]
          writerly-corpus [[:a :b :c :d :e]
                           [:z :y :x :w :v]
                           [:m :n :o :p :q :r]]
          r-profile (text/textuality-profile readerly-corpus)
          w-profile (text/textuality-profile writerly-corpus)]
      ;; Readerly has higher lisible index and higher factor meet overlap
      (is (pos? (:profile/lisible-index r-profile)))
      (is (>= (:profile/meet-ratio-mean r-profile) 0.5))
      (is (= :readerly (:profile/classification r-profile)))

      ;; Writerly has divergence and lower factor meet overlap
      (is (<= (:profile/meet-ratio-mean w-profile) 0.1))
      (is (<= 0.0 (:profile/scriptible-index w-profile) 1.0)))))
