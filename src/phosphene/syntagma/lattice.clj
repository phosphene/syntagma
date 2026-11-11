(ns phosphene.syntagma.lattice
  "Poset and lattice operations over notation sequences.
   Computes factor meets (longest common contiguous factor),
   subsequence joins (minimal common supersequence),
   prefix/suffix relations, and subword poset inclusion DAGs."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]))

;; --- Meets (Greatest Lower Bounds in Factor Order) ---

(defn longest-common-factor
  "Computes the longest contiguous factor shared by seq-a and seq-b (Factor Meet).
   Returns the vector factor, or [] if none."
  [seq-a seq-b]
  (let [m (count seq-a)
        n (count seq-b)]
    (if (or (zero? m) (zero? n))
      []
      ;; Find maximum len factor present in both
      (let [candidates (for [len (range (min m n) 0 -1)
                             start-a (range (inc (- m len)))
                             :let [factor (subvec seq-a start-a (+ start-a len))]
                             :when (core/strict-subsequence? factor seq-b)]
                         factor)]
        (or (first candidates) [])))))

;; --- Prefix & Suffix Order Relations ---

(defn prefix?
  "Returns true if seq-a is a prefix of seq-b."
  [seq-a seq-b]
  (and (<= (count seq-a) (count seq-b))
       (= seq-a (subvec seq-b 0 (count seq-a)))))

(defn suffix?
  "Returns true if seq-a is a suffix of seq-b."
  [seq-a seq-b]
  (let [m (count seq-a)
        n (count seq-b)]
    (and (<= m n)
         (= seq-a (subvec seq-b (- n m) n)))))

;; --- Joins (Least Upper Bounds in Subsequence Order) ---

(defn shortest-common-supersequence
  "Computes a shortest sequence that contains both seq-a and seq-b
   as loose monotonic subsequences (Subsequence Join).
   Uses dynamic programming over sequence elements."
  [seq-a seq-b]
  (let [m (count seq-a)
        n (count seq-b)
        ;; DP table for shortest common supersequence length
        dp (vec (for [i (range (inc m))]
                  (vec (for [j (range (inc n))]
                         (cond
                           (zero? i) j
                           (zero? j) i
                           :else 0)))))
        ;; Fill DP
        dp-filled
        (reduce
          (fn [table [i j]]
            (let [val (if (= (nth seq-a (dec i)) (nth seq-b (dec j)))
                        (inc (get-in table [(dec i) (dec j)]))
                        (inc (min (get-in table [(dec i) j])
                                  (get-in table [i (dec j)]))))]
              (assoc-in table [i j] val)))
          dp
          (for [i (range 1 (inc m))
                j (range 1 (inc n))]
            [i j]))]
    ;; Reconstruct supersequence
    (loop [i m
           j n
           acc '()]
      (cond
        (and (zero? i) (zero? j))
        (vec acc)

        (zero? i)
        (recur 0 (dec j) (cons (nth seq-b (dec j)) acc))

        (zero? j)
        (recur (dec i) 0 (cons (nth seq-a (dec i)) acc))

        (= (nth seq-a (dec i)) (nth seq-b (dec j)))
        (recur (dec i) (dec j) (cons (nth seq-a (dec i)) acc))

        (< (get-in dp-filled [(dec i) j]) (get-in dp-filled [i (dec j)]))
        (recur (dec i) j (cons (nth seq-a (dec i)) acc))

        :else
        (recur i (dec j) (cons (nth seq-b (dec j)) acc))))))

;; --- Poset Directed Acyclic Graph (DAG) ---

(defn subword-poset-edges
  "Given a collection of notation sequences, returns the set of directed edges
   [child parent] where child is strictly included in parent (strict-subsequence?)."
  [sequences]
  (let [unique-seqs (distinct sequences)]
    (set
      (for [a unique-seqs
            b unique-seqs
            :when (and (not= a b)
                       (core/strict-subsequence? a b))]
        [a b]))))
