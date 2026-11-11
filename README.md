# syntagma

> *structural abstraction over notation.*  
> *formal hierarchies of relational constraints.*

A Clojure library modeling order relations of notation sequences as a space of abstraction, coupled with classical distributional frequency primitives.

---

## The Spectrum of Structural Abstraction

`syntagma` organizes relations over sequences into an explicit lattice of constraints:

| Tier | Function | Description |
|---|---|---|
| **1. Strict Sequence** | `strict-subsequence?` | Exact contiguous factor inclusion ($\sigma_1 \sqsubseteq \sigma_2$) |
| **2. Equivalence Sequence** | `equiv-subsequence?` | Contiguous factor after quotient projection ($\langle [t_1]_\sim, \dots \rangle$) |
| **3. Loose Sequence** | `loose-subsequence?` | Monotonic poset order ($t_1 \prec t_2 \prec t_3$) allowing arbitrary intervening tokens |
| **4. Framed Conjunction** | `framed-conjunction?` | Unordered set containment ($\{t_1, t_2\} \subseteq F$) within a bounded frame |

The highest satisfied level of constraint is deterministically evaluated by `relational-tier`.

---

## Specification & Generative Testing

All functions are formally specified with `clojure.spec.alpha` (`phosphene.syntagma.spec`) and verified with generative property-based testing via `clojure.test.check` (`clojure.spec.test.alpha/check`):

- **Invariant 1:** $\text{strict} \implies \text{loose} \implies \text{conjunction}$.
- **Invariant 2:** Mutually exclusive partition of `relational-tier` across `#{:strict :loose :conjunction :none}`.

---

## Distributional Primitives (S-Space / Harris / Firth)

In addition to combinatorial order relations, `phosphene.syntagma.distribution` provides empirical frequency counting:

- `sliding-windows`: Partitions notation vectors into sliding frames.
- `co-occurrence-matrix`: Symmetric sliding window co-occurrence counts.
- `framed-conjunction-frequency`: Counts frames satisfying set co-presence.
- `loose-sequence-frequency`: Counts frames satisfying monotonic poset order.

---

## Running the Test Suite

```bash
# Run unit tests and clojure.spec/test.check generative checks:
clojure -M:runner
```
