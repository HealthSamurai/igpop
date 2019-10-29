(ns igpop.site.valuesets
  (:require [igpop.site.views :as views]))

(defn render-tb-vs [{concepts :concepts system :system :as vs}]
  (conj [:div.table
         [:div.row.th
          [:div.column
           [:div.th "Code"]]
          [:div.column
           [:div.th "System"]]
          [:div.column
           [:div.th "Display"]]
          [:div.column
           [:div.th "Definition"]]]] (map (fn [{code :code display :display :as cpt}]
                                    [:div.row
                                     [:div.column
                                      code]
                                     [:div.column
                                      system]
                                     [:div.column
                                      display]
                                     [:div.column
                                      "???"]]) concepts)))
