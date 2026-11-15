(ns phosphene.syntagma.textuality
  "Roland Barthes' Textuality Diagnostics: Readerly (Lisible) vs. Writerly (Scriptible) & Tmesis.
   Formalizes:
     - The distinction between linear deterministic consumption (lisible) and plural open networks (scriptible)
       (S/Z 1970 §I)
     - Tmesis: selective non-contiguous skipping across notation sequences (The Pleasure of the Text 1973)
     - Lattice-based structural diagnostics:
         * Relational tier entropy and distribution (:strict vs :conjunction)
         * Factor Meet Convergence (longest-common-factor ratio)
         * Supersequence Join Dispersion (shortest-common-supersequence ratio)
         * Tmesis index: ratio of conjunction/loose hops over strict factor spans
         * Scriptibility profile: normalized composite index in [0.0, 1.0]."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.lattice :as lattice]))

;; --- 1. Tmesis Measurement (Non-contiguous Reading Dispersion) ---

(defn tmesis-index
  "Measures the degree of tmesis (skipping/dispersion) when matching a target
   sequence within an empirical frame:
     - Returns 0.0 if target occurs as a strict contiguous factor (no skipping).
     - Returns a value in (0.0, 1.0] representing the ratio of skipped intervening
       tokens when target matches as a loose monotonic subsequence or conjunction.
     - Returns 1.0 if only unordered conjunction is satisfied or max dispersion occurs.
     - Returns nil if target is not satisfied (:none)."
  [target frame]
  (let [tier (core/relational-tier target frame)]
    (case tier
      :strict 0.0
      :loose
      (let [m (count target)
            n (count frame)]
        (if (<= n m)
          0.0
          ;; Find span from first matching token to last matching token
          (let [first-tok (first target)
                last-tok (last target)
                start-idx (.indexOf frame first-tok)
                end-idx (.lastIndexOf frame last-tok)]
            (if (and (>= start-idx 0) (>= end-idx start-idx))
              (let [span-len (inc (- end-idx start-idx))
                    intervening (- span-len m)]
                (double (min 1.0 (/ (max 0 intervening) (max 1 (- n m))))))
              0.5))))
      :conjunction 1.0
      :none nil)))

;; --- 2. Lattice Meet & Join Ratios ---

(defn factor-meet-ratio
  "Calculates the factor meet ratio (longest-common-factor length divided by
   the shorter sequence length). High ratio indicates readerly formulaic convergence."
  [seq-a seq-b]
  (let [lcf (lattice/longest-common-factor seq-a seq-b)
        min-len (min (count seq-a) (count seq-b))]
    (if (zero? min-len)
      0.0
      (double (/ (count lcf) min-len)))))

(defn supersequence-join-ratio
  "Calculates the supersequence join expansion ratio:
   (scs-len - max(len_a, len_b)) / min(len_a, len_b).
   Bounded in [0.0, 1.0]. High ratio indicates writerly divergence/plurality."
  [seq-a seq-b]
  (let [scs (lattice/shortest-common-supersequence seq-a seq-b)
        len-a (count seq-a)
        len-b (count seq-b)
        max-len (max len-a len-b)
        min-len (min len-a len-b)]
    (if (zero? min-len)
      0.0
      (double (min 1.0 (/ (- (count scs) max-len) min-len))))))

;; --- 3. Textuality Profile (Lisible vs. Scriptible) ---

(defn textuality-profile
  "Computes a quantitative diagnostic profile comparing readerly (lisible) vs.
   writerly (scriptible) textuality over a collection of token sequences (e.g. paragraphs or stanzas).
   Parameters:
     - sequences: Collection of token vectors (at least 2 required for comparative diagnostics).
   Returns:
     {:profile/lisible-index double  ; [0.0, 1.0] High = rigid, formulaic, strict
      :profile/scriptible-index double ; [0.0, 1.0] High = plural, open, divergent
      :profile/tmesis-mean double      ; [0.0, 1.0] Average skipping dispersion
      :profile/meet-ratio-mean double  ; [0.0, 1.0] Average factor overlap
      :profile/join-ratio-mean double  ; [0.0, 1.0] Average supersequence divergence
      :profile/tier-distribution map   ; Frequencies of pairwise relational tiers
      :profile/classification #{:readerly :writerly :balanced :indeterminate}}"
  [sequences]
  (let [seqs (vec (filter #(and (sequential? %) (seq %)) sequences))
        n (count seqs)]
    (if (< n 2)
      {:profile/lisible-index 0.5
       :profile/scriptible-index 0.5
       :profile/tmesis-mean 0.0
       :profile/meet-ratio-mean 1.0
       :profile/join-ratio-mean 0.0
       :profile/tier-distribution {}
       :profile/classification :indeterminate}
      (let [pairs (for [i (range n)
                        j (range (inc i) n)]
                    [(nth seqs i) (nth seqs j)])
            total-pairs (count pairs)
            tier-counts (reduce
                          (fn [acc [a b]]
                            (let [t-ab (core/relational-tier a b)
                                  t-ba (core/relational-tier b a)
                                  best-t (cond
                                           (= :strict t-ab) :strict
                                           (= :strict t-ba) :strict
                                           (= :loose t-ab) :loose
                                           (= :loose t-ba) :loose
                                           (= :conjunction t-ab) :conjunction
                                           (= :conjunction t-ba) :conjunction
                                           :else :none)]
                              (update acc best-t (fnil inc 0))))
                          {}
                          pairs)
            meet-ratios (mapv (fn [[a b]] (factor-meet-ratio a b)) pairs)
            join-ratios (mapv (fn [[a b]] (supersequence-join-ratio a b)) pairs)
            tmesis-vals (keep (fn [[a b]] (or (tmesis-index a b) (tmesis-index b a))) pairs)

            mean-meet (if (seq meet-ratios) (/ (reduce + meet-ratios) (count meet-ratios)) 0.0)
            mean-join (if (seq join-ratios) (/ (reduce + join-ratios) (count join-ratios)) 0.0)
            mean-tmesis (if (seq tmesis-vals) (/ (reduce + tmesis-vals) (count tmesis-vals)) 0.5)

            strict-share (double (/ (get tier-counts :strict 0) total-pairs))
            divergent-share (double (/ (+ (get tier-counts :conjunction 0) (get tier-counts :none 0)) total-pairs))

            ;; Composite lisible index: high factor meet overlap + strict share - join expansion
            lisible (max 0.0 (min 1.0 (* 0.5 (+ mean-meet (- 1.0 mean-join)))))
            ;; Composite scriptible index: high join expansion + tmesis - meet overlap
            scriptible (max 0.0 (min 1.0 (* 0.5 (+ mean-join (- 1.0 mean-meet)))))

            classification (cond
                             (> lisible (+ scriptible 0.15)) :readerly
                             (> scriptible (+ lisible 0.15)) :writerly
                             :else :balanced)]
        {:profile/lisible-index (double lisible)
         :profile/scriptible-index (double scriptible)
         :profile/tmesis-mean (double mean-tmesis)
         :profile/meet-ratio-mean (double mean-meet)
         :profile/join-ratio-mean (double mean-join)
         :profile/tier-distribution tier-counts
         :profile/classification classification}))))
