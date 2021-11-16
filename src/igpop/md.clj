(ns igpop.md
  (:import
   org.commonmark.parser.Parser
   java.util.Arrays
   org.commonmark.renderer.html.HtmlRenderer
   org.commonmark.ext.gfm.tables.TablesExtension))

(defn parse-markdown [s]
  (when s
    (let [exts (Arrays/asList (to-array [(TablesExtension/create)]))
          parser (-> (Parser/builder)
                     (.extensions exts)
                     .build)
          renderer (-> (HtmlRenderer/builder)
                       (.extensions exts)
                       .build)]
      (.render renderer (.parse parser s)))))
