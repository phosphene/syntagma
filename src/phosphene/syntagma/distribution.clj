(ns phosphene.syntagma.distribution
  "Classical distributional frequency primitives over notation sequences.
   Provides observable co-occurrence, sliding-frame, and skip-gram frequency
   counting as deterministic empirical measurements (S-Space / Harris / Firth)."
  (:require [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]))

;; --- Specs ---

(s/def ::window-size pos-int?)
(s/def ::co-occurrence-map (s/map-of any? (s/map-of any? pos-int?)))

;; --- Functions ---

(defn sliding-windows
  "Partitions a vector of notation tokens into contiguous sliding windows of size n with step 1."
  [tokens n]
  (let [c (count tokens)]
    (if (< c n)
      []
      (mapv #(subvec tokens % (+ % n)) (range (inc (- c n)))))))

(defn co-occurrence-matrix
  "Computes co-occurrence counts within a symmetric sliding window of radius w.
   Returns a map of {token-a {token-b count}}."
  [tokens radius]
  (let [n (count tokens)]
    (reduce
      (fn [acc i]
        (let [focus (nth tokens i)
              min-idx (max 0 (- i radius))
              max-idx (min n (inc (+ i radius)))
              neighbors (keep-indexed
                          (fn [offset idx]
                            (when (not= idx i)
                              (nth tokens idx)))
                          (range min-idx max-idx))]
          (reduce
            (fn [inner-acc neighbor]
              (update-in inner-acc [focus neighbor] (fnil inc 0)))
            acc
            neighbors)))
      {}
      (range n))))

(defn framed-conjunction-frequency
  "Counts how many frames (AST blocks or windows) contain all tokens in query-set."
  [query-set frames]
  (let [q (set query-set)]
    (count (filter #(core/framed-conjunction? q %) frames))))

(defn loose-sequence-frequency
  "Counts how many frames satisfy the loose monotonic sequence target."
  [target frames]
  (count (filter #(core/loose-subsequence? target %) frames)))
