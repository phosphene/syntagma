(ns phosphene.syntagma.io.markdown
  "Ingests on-disk Markdown AST blocks into notation sequence vectors
   annotated with coordinate metadata (:uri, :coordinate, :block-type, :tokens).
   Adheres to mdrag block topology and ID-Path Parity without external databases."
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

;; --- Specs ---

(s/def ::coordinate string?)
(s/def ::block-type #{:heading :paragraph :blockquote :list-item :code :unknown})
(s/def ::token-symbol keyword?)
(s/def ::tokens (s/coll-of ::token-symbol :kind vector?))
(s/def ::ast-block
  (s/keys :req-un [::coordinate ::block-type ::tokens]
          :opt-un [::raw-text]))

;; --- Tokenizer ---

(defn tokenize-notation
  "Normalizes string text into a vector of lowercased notation keyword symbols,
   stripping structural punctuation while preserving alphanumeric tokens."
  [text]
  (if (str/blank? text)
    []
    (->> (re-seq #"[A-Za-z0-9_\-]+" text)
         (mapv #(keyword (str/lower-case %))))))

;; --- Block Parser (Lightweight AST Block Demarcation) ---

(defn parse-markdown-blocks
  "Parses raw Markdown string content into structured AST block maps with coordinates.
   Identifies headings, blockquotes, lists, and paragraphs."
  [doc-id content]
  (let [lines (str/split-lines content)]
    (loop [remaining lines
           block-idx 0
           acc []]
      (if (empty? remaining)
        acc
        (let [line (str/trim (first remaining))]
          (cond
            ;; Blank line: skip
            (str/blank? line)
            (recur (rest remaining) block-idx acc)

            ;; Heading: # Title
            (str/starts-with? line "#")
            (let [clean-heading (str/trim (str/replace-first line #"^#+\s*" ""))
                  coord (str doc-id ":h:" block-idx)
                  block {:coordinate coord
                         :block-type :heading
                         :raw-text clean-heading
                         :tokens (tokenize-notation clean-heading)}]
              (recur (rest remaining) (inc block-idx) (conj acc block)))

            ;; Blockquote: > Quote
            (str/starts-with? line ">")
            (let [quote-lines (take-while #(str/starts-with? (str/trim %) ">") remaining)
                  clean-quote (->> quote-lines
                                   (map #(str/trim (str/replace-first (str/trim %) #"^>\s*" "")))
                                   (str/join " "))
                  coord (str doc-id ":quote:" block-idx)
                  block {:coordinate coord
                         :block-type :blockquote
                         :raw-text clean-quote
                         :tokens (tokenize-notation clean-quote)}]
              (recur (drop (count quote-lines) remaining) (inc block-idx) (conj acc block)))

            ;; List item: * or -
            (re-find #"^[\*\-]\s+" line)
            (let [clean-item (str/replace-first line #"^[\*\-]\s+" "")
                  coord (str doc-id ":item:" block-idx)
                  block {:coordinate coord
                         :block-type :list-item
                         :raw-text clean-item
                         :tokens (tokenize-notation clean-item)}]
              (recur (rest remaining) (inc block-idx) (conj acc block)))

            ;; Default: Paragraph
            :else
            (let [para-lines (take-while #(and (not (str/blank? (str/trim %)))
                                               (not (str/starts-with? (str/trim %) "#"))
                                               (not (str/starts-with? (str/trim %) ">"))
                                               (not (re-find #"^[\*\-]\s+" (str/trim %))))
                                         remaining)
                  clean-para (str/join " " (map str/trim para-lines))
                  coord (str doc-id ":p:" block-idx)
                  block {:coordinate coord
                         :block-type :paragraph
                         :raw-text clean-para
                         :tokens (tokenize-notation clean-para)}]
              (recur (drop (count para-lines) remaining) (inc block-idx) (conj acc block)))))))))

(defn read-markdown-file
  "Reads a Markdown file from path and extracts tokenized notation blocks with coordinates."
  [path]
  (let [f (clojure.java.io/file path)
        doc-id (.getName f)
        content (slurp f)]
    (parse-markdown-blocks doc-id content)))
