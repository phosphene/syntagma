---
uri: phosphene/syntagma/naming-principles
owner: edphos
status: living
updated: 2026-11-12
title: Syntagma Naming Principles and Linguistic Ontology
description: "Codification of naming standards, Fregean indirection, and scope-boundary rules in phosphene.syntagma per Tellman and structural linguistics."
---

# Syntagma: Naming Principles and Linguistic Ontology

> *"A name’s sense should be timeless. While the referent (implementation) may change tomorrow, the sense remains constant."*  
> — Zachary Tellman, *Elements of Clojure*

`phosphene.syntagma` models the horizontal, linear ordering of language and formal notation: **syntagmatic chaining** ($\sigma_1 \prec \sigma_2 \prec \sigma_3$), factors, subwords, posets, and distributional co-occurrences.

This document records the naming principles, Fregean semiotic distinctions, and scope boundaries applied across `phosphene.syntagma`.

---

## 1. Domain Ontology & Synthetic Semantics

In linguistics and semiotics (Saussure, Hjelmslev, Harris, Chomsky), natural language descriptions of sequence relationships suffer from ambiguity (e.g. conflating "subsequence", "substring", and "factor"). `phosphene.syntagma` adopts exact **synthetic terms** to ensure a strict 1-to-1 parity between **Signs** and **Senses**:

| Linguistic / Formal Concept | Sign (`phosphene.syntagma`) | Formal Sense (Invariants) |
| :--- | :--- | :--- |
| **Syntagma / Chain** | `sequence` / `syntagma` | Linear ordered vector of tokens representing horizontal syntagmatic concatenation. |
| **Context Window / Syntagmatic Horizon** | `frame` | Bounded notation horizon (sentence, paragraph, or sliding span) enclosing candidate syntagmas. |
| **Contiguous Factor (Subword)** | `factor?` (alias `strict-subsequence?`) | $u$ is a factor of $w$ iff $\exists x, y$ such that $w = x \cdot u \cdot y$. Contiguous, order-preserving. |
| **Monotonic Precedence (Subsequence)** | `subsequence?` (alias `loose-subsequence?`) | Elements of target appear in frame with strictly increasing index positions ($i_1 < i_2 < \dots < i_k$). |
| **Co-presence / Parataxis** | `conjunction?` (alias `framed-conjunction?`) | Unordered set inclusion ($T \subseteq F$) disregarding linear distance and permutation. |
| **Lattice Meet ($\bigwedge$)** | `longest-common-factor` | Maximal contiguous factor common to both parent sequences. |
| **Lattice Join ($\bigvee$)** | `shortest-common-supersequence` | Minimal sequence containing both parent sequences as monotonic subsequences. |

Because consumers of this library possess domain knowledge in linguistics and formal language theory, these synthetic names provide **narrowness** without ambiguous natural language baggage.

---

## 2. Fregean Indirection: Sign, Sense, and Referent

Every public definition in `syntagma` separates what something is (**Sense**) from how it is computed (**Referent**):

1. **Sign:** The public symbol (e.g. `longest-common-factor`, `factor?`, `subword-poset-edges`).
2. **Sense:** Reified in `phosphene.syntagma.spec` via `s/def` and `s/fdef`. For example, the sense of `longest-common-factor` is:
   $$\text{ret} \sqsubseteq \text{seq-a} \quad \land \quad \text{ret} \sqsubseteq \text{seq-b} \quad \land \quad |\text{ret}| \le \min(|\text{seq-a}|, |\text{seq-b}|)$$
3. **Referent:** The dynamic programming matrix, loop-recur index sweep, or transient state table. The referent never leaks into the sign: we do not name functions `dp-longest-common-factor` or `vector-factor?`.

---

## 3. The Scope-Boundary Rule for Function Names

Tellman establishes that functions within a pure scope must **avoid verbs**, naming themselves after the value/noun produced, whereas functions crossing scope boundaries (I/O, state, disk) **must use verbs**:

### Transformations (Pure Core — No Verbs)
In `phosphene.syntagma.core`, `phosphene.syntagma.lattice`, and `phosphene.syntagma.distribution`:
* ❌ `calculate-longest-common-factor` $\to$ ✅ `longest-common-factor`
* ❌ `compute-relational-tier` $\to$ ✅ `relational-tier`
* ❌ `generate-sliding-windows` $\to$ ✅ `sliding-windows`
* ❌ `build-poset-edges` $\to$ ✅ `subword-poset-edges`

Predicates asking a pure mathematical question end in `?`:
* `factor?`
* `subsequence?`
* `conjunction?`
* `prefix?`
* `suffix?`

### Operations (Scope Crossing — Require Verbs)
In `phosphene.syntagma.io.markdown`:
* Pull (Disk to Memory): `read-markdown-file`
* Transform (Internal String Parser): `parse-markdown-blocks`, `tokenize-notation`

---

## 4. Subsumption Hierarchy

The relational hierarchy in `core` forms a strict subsumption ladder:
$$\text{factor?} \implies \text{subsequence?} \implies \text{conjunction?}$$

`relational-tier` yields the most specific relational tier satisfied: `#{:strict :loose :conjunction :none}`.
