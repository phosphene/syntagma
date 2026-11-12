(ns phosphene.syntagma.spec
  "Formal specification, property invariants, and dense generators for phosphene.syntagma."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.lattice :as lattice]
            [phosphene.syntagma.distribution :as dist]))

;; --- 1. Dense Generators & Alphabet Invariants ---

(def ^:dynamic *alphabet*
  "Finite controlled alphabet for high-density collision and constraint exploration."
  [:a :b :c :d :e :f])

(s/def ::token
  (s/with-gen any?
    #(gen/elements *alphabet*)))

(s/def ::sequence
  (s/with-gen (s/coll-of ::token :kind vector?)
    #(gen/vector (s/gen ::token) 0 8)))

(s/def ::frame
  (s/with-gen (s/coll-of ::token :kind vector?)
    #(gen/vector (s/gen ::token) 0 12)))

(s/def ::equiv-fn ifn?)

;; Generator for coupled target & frame pairs to ensure all relational tiers are tested
(defn gen-target-and-frame
  "Generates paired [target frame] vectors with dense structural relationships:
   exact factors, monotonic subsequences, unordered permutations, and random sequences."
  []
  (gen/bind (gen/vector (s/gen ::token) 1 12)
    (fn [f]
      (gen/one-of
        [;; 1. Exact factor (:strict)
         (gen/bind (gen/choose 0 (dec (count f)))
           (fn [start]
             (gen/bind (gen/choose start (count f))
               (fn [end]
                 (gen/return [(subvec f start end) f])))))
         ;; 2. Monotonic subsequence (:loose)
         (gen/bind (gen/vector (gen/boolean) (count f))
           (fn [mask]
             (let [sub (vec (keep-indexed (fn [idx keep?] (when keep? (nth f idx))) mask))]
               (gen/return [sub f]))))
         ;; 3. Permutation / Unordered (:conjunction)
         (gen/bind (gen/vector (s/gen ::token) 1 (count f))
           (fn [sub]
             (let [avail (vec (distinct f))]
               (gen/bind (gen/vector (gen/elements avail) 1 (count avail))
                 (fn [sampled]
                   (gen/return [sampled f]))))))
         ;; 4. Independent sequence
         (gen/bind (gen/vector (s/gen ::token) 0 6)
           (fn [t]
             (gen/return [t f])))]))))

;; --- 2. Function Specs: Core Relational Ladder ---

(s/fdef core/factor?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          ;; Invariant: if factor holds, subsequence and conjunction must also hold
          (if ret
            (and (core/subsequence? target frame)
                 (core/conjunction? target frame))
            true))))

(s/fdef core/strict-subsequence?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?)

(s/fdef core/equiv-factor?
  :args (s/cat :target ::sequence :frame ::frame :equiv-fn ::equiv-fn)
  :ret boolean?)

(s/fdef core/equiv-subsequence?
  :args (s/cat :target ::sequence :frame ::frame :equiv-fn ::equiv-fn)
  :ret boolean?)

(s/fdef core/subsequence?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          ;; Invariant: if subsequence holds, conjunction must hold
          (if ret
            (core/conjunction? target frame)
            true))))

(s/fdef core/loose-subsequence?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?)

(s/fdef core/conjunction?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?)

(s/fdef core/framed-conjunction?
  :args (s/cat :target ::sequence :frame ::frame)
  :ret boolean?)

(s/fdef core/relational-tier
  :args (s/cat :target ::sequence :frame ::frame)
  :ret #{:strict :loose :conjunction :none}
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          (case ret
            :strict (and (core/factor? target frame)
                         (core/subsequence? target frame)
                         (core/conjunction? target frame))
            :loose (and (not (core/factor? target frame))
                        (core/subsequence? target frame)
                        (core/conjunction? target frame))
            :conjunction (and (not (core/subsequence? target frame))
                              (core/conjunction? target frame))
            :none (not (core/conjunction? target frame))))))

;; --- 3. Function Specs: Lattice & Poset Operations ---

(s/fdef lattice/longest-common-factor
  :args (s/cat :seq-a ::sequence :seq-b ::sequence)
  :ret ::sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [seq-a seq-b]} args]
          ;; Invariant: Result must be a contiguous factor of both sequences
          ;; and its length cannot exceed the minimum of their lengths
          (and (core/strict-subsequence? ret seq-a)
               (core/strict-subsequence? ret seq-b)
               (<= (count ret) (min (count seq-a) (count seq-b)))))))

(s/fdef lattice/prefix?
  :args (s/cat :seq-a ::sequence :seq-b ::sequence)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [seq-a seq-b]} args]
          (if ret
            (core/strict-subsequence? seq-a seq-b)
            true))))

(s/fdef lattice/suffix?
  :args (s/cat :seq-a ::sequence :seq-b ::sequence)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [seq-a seq-b]} args]
          (if ret
            (core/strict-subsequence? seq-a seq-b)
            true))))

(s/fdef lattice/shortest-common-supersequence
  :args (s/cat :seq-a ::sequence :seq-b ::sequence)
  :ret ::sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [seq-a seq-b]} args]
          ;; Invariant: Result must contain both parent sequences as loose monotonic subsequences,
          ;; and its length must be at least max(|a|, |b|) and at most |a| + |b|.
          (and (core/loose-subsequence? seq-a ret)
               (core/loose-subsequence? seq-b ret)
               (>= (count ret) (max (count seq-a) (count seq-b)))
               (<= (count ret) (+ (count seq-a) (count seq-b)))))))

(s/fdef lattice/subword-poset-edges
  :args (s/cat :sequences (s/coll-of ::sequence :kind vector?))
  :ret (s/coll-of (s/tuple ::sequence ::sequence) :kind set?)
  :fn (fn [{:keys [ret]}]
        ;; Invariant: In every directed edge [child parent], child is strictly included in parent
        (every? (fn [[child parent]]
                  (and (not= child parent)
                       (core/strict-subsequence? child parent)))
                ret)))

;; --- 4. Function Specs: Distributional Frequency Primitives ---

(s/fdef dist/sliding-windows
  :args (s/cat :tokens ::sequence :n pos-int?)
  :ret (s/coll-of ::sequence :kind vector?)
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [tokens n]} args
              expected-count (if (< (count tokens) n) 0 (inc (- (count tokens) n)))]
          (and (= (count ret) expected-count)
               (every? #(= (count %) n) ret)
               (every? #(core/strict-subsequence? % tokens) ret)))))

(s/fdef dist/framed-conjunction-frequency
  :args (s/cat :query-set (s/coll-of ::token :kind set?)
               :frames (s/coll-of ::frame :kind vector?))
  :ret nat-int?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [frames]} args]
          (<= ret (count frames)))))

(s/fdef dist/loose-sequence-frequency
  :args (s/cat :target ::sequence
               :frames (s/coll-of ::frame :kind vector?))
  :ret nat-int?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [frames]} args]
          (<= ret (count frames)))))
