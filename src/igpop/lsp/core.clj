(ns igpop.lsp.core)

(defmulti procedure
  (fn [{method :method}]
    (keyword method)))





