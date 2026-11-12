# syntagma

> *structural abstraction over notation.*  
> *formal hierarchies of relational constraints.*

A Clojure library modeling order relations of notation sequences as a space of abstraction, coupled with classical distributional frequency primitives, poset lattice operations, and coordinate-preserving Markdown AST block ingestion.

[![CI](https://github.com/phosphene/syntagma/actions/workflows/ci.yml/badge.svg)](https://github.com/phosphene/syntagma/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## The Spectrum of Structural Abstraction

`syntagma` organizes relations over sequences into an explicit lattice of constraints:

| Tier | Function | Description | Mathematical Form |
|---|---|---|---|
| **1. Strict Sequence** | `strict-subsequence?` | Exact contiguous factor inclusion | $\sigma_1 \sqsubseteq \sigma_2$ |
| **2. Equivalence Sequence** | `equiv-subsequence?` | Contiguous factor after quotient projection | $\langle [t_1]_\sim, \dots \rangle \sqsubseteq \langle [s_1]_\sim, \dots \rangle$ |
| **3. Loose Sequence** | `loose-subsequence?` | Monotonic poset order (intervening tokens permitted) | $t_1 \prec t_2 \prec t_3 \dots \prec t_k$ |
| **4. Framed Conjunction** | `framed-conjunction?` | Unordered co-presence / set containment | $\{t_1, \dots, t_k\} \subseteq \{s_1, \dots, s_n\}$ |

`relational-tier` partitions the relation space into mutually exclusive categories: `#{:strict :loose :conjunction :none}`.

---

## Quickstart (REPL)

```clojure
(require '[phosphene.syntagma.core :as syn]
         '[phosphene.syntagma.lattice :as lat]
         '[phosphene.syntagma.distribution :as dist])

;; 1. Relational Constraint Ladder
(def frame [:def :magnetic-lines :curve :around :wire :producing :current])

(syn/strict-subsequence? [:curve :around :wire] frame)
;; => true

(syn/loose-subsequence? [:magnetic-lines :wire :current] frame)
;; => true

(syn/strict-subsequence? [:magnetic-lines :wire :current] frame)
;; => false (intervening tokens present)

(syn/relational-tier [:magnetic-lines :wire :current] frame)
;; => :loose

(syn/relational-tier [:current :magnetic-lines] frame)
;; => :conjunction (order inverted, but both co-present)

;; 2. Poset Lattice Operations (Meets & Joins)
(lat/longest-common-factor [:a :b :c :d] [:x :b :c :y])
;; => [:b :c]  (Factor Meet)

(lat/shortest-common-supersequence [:a :b :c] [:b :c :d])
;; => [:a :b :c :d]  (Subsequence Join)

;; 3. Distributional Frequency Primitives
(dist/sliding-windows [:a :b :c :d :e] 3)
;; => [[:a :b :c] [:b :c :d] [:c :d :e]]

(dist/co-occurrence-matrix [:magnetic :field :induces :current] 1)
;; => {:magnetic {:field 1}
;;     :field    {:magnetic 1, :induces 1}
;;     :induces  {:field 1, :current 1}
;;     :current  {:induces 1}}
```

---

## Markdown AST Block Ingestion (`phosphene.syntagma.io.markdown`)

Ingests on-disk Markdown AST blocks directly into notation sequence vectors with coordinate metadata (`:uri`, `:coordinate`, `:block-type`, `:tokens`). Adheres to Mdrag block topology and ID-Path Parity without external databases:

```clojure
(require '[phosphene.syntagma.io.markdown :as syn.io])

(syn.io/parse-markdown-blocks "faraday-1832.md"
  "# Experimental Researches\n\nThe magnetic lines curve around the conducting wire.")
;; => [{:coordinate "faraday-1832.md:h:0"
;;      :block-type :heading
;;      :raw-text "Experimental Researches"
;;      :tokens [:experimental :researches]}
;;     {:coordinate "faraday-1832.md:p:1"
;;      :block-type :paragraph
;;      :raw-text "The magnetic lines curve around the conducting wire."
;;      :tokens [:the :magnetic :lines :curve :around :the :conducting :wire]}]
```

---

## Generative Specification Invariants

Formal generative properties are enforced with `clojure.spec.alpha` and `clojure.test.check`:

- **Hierarchy Monotonicity:** $\text{strict} \implies \text{loose} \implies \text{conjunction}$.
- **Partition Invariance:** `relational-tier` yields an exact, mutually exclusive partition across `#{:strict :loose :conjunction :none}`.
- **Join Minimality:** The shortest common supersequence strictly contains both parent sequences as loose monotonic subsequences.

---

## Running the Test Suite

Hermetic, offline execution with zero network or database dependencies:

```bash
clojure -M:runner
```

---

## License

MIT License. Copyright (c) 2026 Ed Phillips / Phosphene.
