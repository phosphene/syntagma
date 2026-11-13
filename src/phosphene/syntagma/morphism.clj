(ns phosphene.syntagma.morphism
  "Relational Morphisms Between Notation Vocabularies (T-SYN-007).
   Maps notation sequences from alphabet Sigma_1 to Sigma_2 while preserving or
   reflecting order relations across the constraint hierarchy:
     - Strict sequence (factor inclusion)
     - Loose sequence (monotonic subsequence)
     - Framed conjunction (set containment)
   Provides verification of order-preservation, relational fidelity,
   and monotonic translation without hermeneutic inflation."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]))

;; --- Vocabulary Morphism Definition ---

(defn vocabulary-morphism
  "Constructs a vocabulary morphism from mapping dictionary or function.
   mapping: map of {token-sigma-1 token-sigma-2} or function (token-sigma-1 -> token-sigma-2).
   opts:
     :id - identifier keyword for this morphism (default :morphism/anonymous)
     :domain-alphabet - optional set of recognized input tokens
     :codomain-alphabet - optional set of valid output tokens
     :fallback - fallback token or function for unmapped items (default identity)"
  ([mapping]
   (vocabulary-morphism :morphism/anonymous mapping {}))
  ([id mapping]
   (vocabulary-morphism id mapping {}))
  ([id mapping {:keys [domain-alphabet codomain-alphabet fallback]
                :or {fallback identity}}]
   {:morphism/id id
    :morphism/mapping mapping
    :morphism/domain domain-alphabet
    :morphism/codomain codomain-alphabet
    :morphism/fallback fallback}))

(defn morphism?
  "Returns true if x is a valid vocabulary morphism record."
  [x]
  (and (map? x)
       (keyword? (:morphism/id x))
       (or (map? (:morphism/mapping x))
           (fn? (:morphism/mapping x)))))

;; --- Sequence Application ---

(defn apply-token
  "Maps an individual token across a vocabulary morphism."
  [morphism tok]
  (let [m (:morphism/mapping morphism)
        fb (:morphism/fallback morphism)]
    (cond
      (map? m) (get m tok (if (fn? fb) (fb tok) fb))
      (fn? m)  (let [res (m tok)]
                 (if (nil? res)
                   (if (fn? fb) (fb tok) fb)
                   res))
      :else tok)))

(defn map-sequence
  "Applies a vocabulary morphism element-wise to a sequence vector in Sigma_1,
   yielding a conformed sequence vector in Sigma_2."
  [morphism seq-tokens]
  (mapv #(apply-token morphism %) seq-tokens))

;; --- Order-Preservation Verifiers ---

(defn factor-preserving?
  "Checks whether a morphism preserves factor inclusion between target and frame:
   target <=_factor frame => (map-sequence m target) <=_factor (map-sequence m frame)."
  [morphism target frame]
  (if (core/factor? target frame)
    (core/factor? (map-sequence morphism target) (map-sequence morphism frame))
    true))

(defn subsequence-preserving?
  "Checks whether a morphism preserves monotonic subsequence precedence:
   target <=_subseq frame => (map-sequence m target) <=_subseq (map-sequence m frame)."
  [morphism target frame]
  (if (core/subsequence? target frame)
    (core/subsequence? (map-sequence morphism target) (map-sequence morphism frame))
    true))

(defn conjunction-preserving?
  "Checks whether a morphism preserves framed conjunction (unordered co-presence):
   target <=_conj frame => (map-sequence m target) <=_conj (map-sequence m frame)."
  [morphism target frame]
  (if (core/conjunction? target frame)
    (core/conjunction? (map-sequence morphism target) (map-sequence morphism frame))
    true))

(defn order-preserving?
  "Verifies order-preservation across all relational tiers:
   factor, monotonic subsequence, and conjunction."
  [morphism target frame]
  (and (factor-preserving? morphism target frame)
       (subsequence-preserving? morphism target frame)
       (conjunction-preserving? morphism target frame)))

;; --- Relational Fidelity Analysis ---

(defn relational-fidelity
  "Evaluates the relational fidelity of a morphism over target and frame.
   Compares the relational-tier before and after mapping.
   Returns:
     {:source-tier  keyword (:strict, :loose, :conjunction, :none)
      :target-tier  keyword (:strict, :loose, :conjunction, :none)
      :preserved?   boolean (true if target-tier is at least as restrictive as source-tier)
      :degraded?    boolean (true if source constraint held but target failed or degraded)
      :amplified?   boolean (true if target satisfies a strictly higher tier due to token collapse)}."
  [morphism target frame]
  (let [src-tier (core/relational-tier target frame)
        mapped-target (map-sequence morphism target)
        mapped-frame (map-sequence morphism frame)
        tgt-tier (core/relational-tier mapped-target mapped-frame)
        tier-rank {:none 0 :conjunction 1 :loose 2 :strict 3}
        src-rank (get tier-rank src-tier 0)
        tgt-rank (get tier-rank tgt-tier 0)]
    {:source-tier src-tier
     :target-tier tgt-tier
     :preserved? (or (= src-tier :none) (>= tgt-rank src-rank))
     :degraded? (and (not= src-tier :none) (< tgt-rank src-rank))
     :amplified? (> tgt-rank src-rank)}))

;; --- Invertibility & Reflection ---

(defn invert-morphism
  "Constructs an inverted morphism if mapping is injective/bijective.
   If mapping has collisions, last mapped value wins."
  ([morphism]
   (invert-morphism morphism (keyword (str (name (:morphism/id morphism)) "-inverse"))))
  ([morphism inverse-id]
   (let [m (:morphism/mapping morphism)]
     (if (map? m)
       (let [inv-map (reduce-kv (fn [acc k v] (assoc acc v k)) {} m)]
         (vocabulary-morphism inverse-id inv-map
                              {:domain-alphabet (:morphism/codomain morphism)
                               :codomain-alphabet (:morphism/domain morphism)
                               :fallback (:morphism/fallback morphism)}))
       (throw (ex-info "Cannot invert function-based morphism automatically; requires explicit map"
                       {:morphism-id (:morphism/id morphism)}))))))
