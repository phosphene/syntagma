(ns phosphene.syntagma.distribution
  "Classical distributional frequency primitives over notation sequences.
   Provides observable co-occurrence, sliding-frame, HAL ramped matrices,
   and second-order context vector aggregation as deterministic empirical
   measurements (S-Space / Harris / Firth / Lund & Burgess 1996)."
  (:require [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]))

;; --- Specs ---

(s/def ::window-size pos-int?)
(s/def ::radius pos-int?)
(s/def ::co-occurrence-map (s/map-of any? (s/map-of any? pos-int?)))
(s/def ::context-vector (s/map-of any? number?))
(s/def ::hal-matrix
  (s/keys :req-un [::radius ::forward ::backward]))

;; --- 1. Basic Sliding Windows & Co-occurrence ---

(defn sliding-windows
  "Partitions a vector of notation tokens into contiguous sliding windows of size n with step 1."
  [tokens n]
  (let [c (count tokens)]
    (if (< c n)
      []
      (mapv #(subvec tokens % (+ % n)) (range (inc (- c n)))))))

(defn co-occurrence-matrix
  "Computes unweighted co-occurrence counts within a symmetric sliding window of radius w.
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

;; --- 2. Hyperspace Analogue to Language (HAL) Matrix ---

(defn hal-matrix
  "Computes an asymmetric Hyperspace Analogue to Language (HAL; Lund & Burgess 1996) matrix.
   Within a sliding window of radius K, co-occurrences are weighted inversely by distance:
     w = K - d + 1
   Tracks preceding (backward) and succeeding (forward) context separately to preserve
   asymmetric syntagmatic ordering information.
   Returns {:radius K :forward {focus {succ-token weight}} :backward {focus {prec-token weight}}}."
  [tokens radius]
  (let [n (count tokens)]
    (reduce
      (fn [acc i]
        (let [focus (nth tokens i)]
          ;; Backward context: preceding tokens j < i at distance d = (- i j)
          (let [backward-acc
                (reduce
                  (fn [m d]
                    (let [j (- i d)]
                      (if (>= j 0)
                        (let [prec (nth tokens j)
                              weight (inc (- radius d))]
                          (update-in m [:backward focus prec] (fnil + 0) weight))
                        m)))
                  acc
                  (range 1 (inc radius)))]
            ;; Forward context: succeeding tokens j > i at distance d = (- j i)
            (reduce
              (fn [m d]
                (let [j (+ i d)]
                  (if (< j n)
                    (let [succ (nth tokens j)
                          weight (inc (- radius d))]
                      (update-in m [:forward focus succ] (fnil + 0) weight))
                    m)))
              backward-acc
              (range 1 (inc radius))))))
      {:radius radius
       :forward {}
       :backward {}}
      (range n))))

(defn hal-vector
  "Extracts the full HAL profile for a token by concatenating backward and forward context.
   Backward keys are prefixed with :before/ and forward keys with :after/."
  [hal token]
  (let [b-map (get-in hal [:backward token] {})
        f-map (get-in hal [:forward token] {})
        b-profile (reduce-kv (fn [m k v] (assoc m (keyword (str "before/" (name k))) v)) {} b-map)
        f-profile (reduce-kv (fn [m k v] (assoc m (keyword (str "after/" (name k))) v)) {} f-map)]
    (merge b-profile f-profile)))

;; --- 3. Second-Order Context Vector Aggregation ---

(defn aggregate-context-vectors
  "Aggregates first-order co-occurrence or HAL vectors across discrete coordinate frames
   (e.g. AST paragraph or section blocks).
   Given a collection of frames (vectors of tokens) and a vector-extractor-fn
   (e.g. #(hal-vector (hal-matrix % radius) term)), computes the centroid second-order
   representation for target term across all frames where it appears."
  [frames term radius]
  (let [matching-frames (filter (fn [frame] (some #(= term %) frame)) frames)]
    (if (empty? matching-frames)
      {}
      (let [frame-vectors (map (fn [frame]
                                 (hal-vector (hal-matrix frame radius) term))
                               matching-frames)
            total-count (count frame-vectors)]
        (reduce
          (fn [acc f-vec]
            (merge-with + acc f-vec))
          {}
          frame-vectors)))))

(defn cosine-similarity
  "Calculates cosine similarity between two sparse numerical context vectors."
  [vec-a vec-b]
  (let [common-keys (filter #(contains? vec-b %) (keys vec-a))]
    (if (empty? common-keys)
      0.0
      (let [dot-prod (reduce + (map (fn [k] (* (get vec-a k) (get vec-b k))) common-keys))
            norm-a (Math/sqrt (reduce + (map (fn [v] (* v v)) (vals vec-a))))
            norm-b (Math/sqrt (reduce + (map (fn [v] (* v v)) (vals vec-b))))]
        (if (or (zero? norm-a) (zero? norm-b))
          0.0
          (/ dot-prod (* norm-a norm-b)))))))

;; --- 4. Frame Query Frequencies ---

(defn framed-conjunction-frequency
  "Counts how many frames (AST blocks or windows) contain all tokens in query-set."
  [query-set frames]
  (let [q (set query-set)]
    (count (filter #(core/framed-conjunction? q %) frames))))

(defn loose-sequence-frequency
  "Counts how many frames satisfy the loose monotonic sequence target."
  [target frames]
  (count (filter #(core/loose-subsequence? target %) frames)))
