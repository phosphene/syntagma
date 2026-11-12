(ns phosphene.syntagma.core
  "Order relations of notation sequences as a space of abstraction.
   Formal hierarchies of relational constraints:
     - Strict sequence (contiguous factor inclusion)
     - Equivalence-class sequence (quotiented mapping over similarity)
     - Loose sequence (monotonic partial order / subsequence within frame)
     - Framed conjunction (unordered set containment within frame)"
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]))

;; --- Specs ---

(s/def ::token any?)
(s/def ::sequence (s/coll-of ::token :kind vector?))
(s/def ::frame (s/coll-of ::token :kind vector?))

(s/def ::equiv-fn ifn?)

;; --- 1. Strict Sequence (Contiguous Factor Inclusion) ---

(defn factor?
  "Returns true if target vector appears as a contiguous factor (subword) of frame."
  [target frame]
  (let [m (count target)
        n (count frame)]
    (cond
      (zero? m) true
      (> m n) false
      :else (boolean
              (some (fn [i]
                      (= target (subvec frame i (+ i m))))
                    (range (inc (- n m))))))))

(def ^{:doc "Alias for factor? honoring classical subsequence terminology."}
  strict-subsequence? factor?)

;; --- 2. Equivalence-Class Sequence (Quotiented Mapping) ---

(defn equiv-factor?
  "Returns true if the projection of target under equiv-fn appears
   as a contiguous factor in the projection of frame under equiv-fn."
  [target frame equiv-fn]
  (let [proj-target (mapv equiv-fn target)
        proj-frame (mapv equiv-fn frame)]
    (factor? proj-target proj-frame)))

(def ^{:doc "Alias for equiv-factor?."}
  equiv-subsequence? equiv-factor?)

;; --- 3. Loose Sequence (Monotonic Poset Order / Subsequence) ---

(defn subsequence?
  "Returns true if elements of target appear in frame in strictly ascending index order
   (monotonic precedence), allowing arbitrary intervening tokens."
  [target frame]
  (let [n (count frame)]
    (loop [t-idx 0
           f-idx 0]
      (cond
        (= t-idx (count target)) true
        (>= f-idx n) false
        (= (nth target t-idx) (nth frame f-idx))
        (recur (inc t-idx) (inc f-idx))
        :else
        (recur t-idx (inc f-idx))))))

(def ^{:doc "Alias for subsequence?."}
  loose-subsequence? subsequence?)

;; --- 4. Framed Conjunction (Unordered Co-presence) ---

(defn conjunction?
  "Returns true if all distinct elements of target exist within frame,
   disregarding order and multiplicity."
  [target frame]
  (set/subset? (set target) (set frame)))

(def ^{:doc "Alias for conjunction?."}
  framed-conjunction? conjunction?)

;; --- Hierarchy of Constraint Check ---

(defn relational-tier
  "Evaluates the most specific relational tier satisfied by target within frame:
     :strict       - contiguous factor
     :loose        - monotonic subsequence
     :conjunction  - unordered co-presence
     :none         - not satisfied"
  [target frame]
  (cond
    (factor? target frame)      :strict
    (subsequence? target frame) :loose
    (conjunction? target frame) :conjunction
    :else                       :none))
