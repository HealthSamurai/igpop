(ns igpop.site.views
  (:require [garden.core :as gc]
            [hiccup.core :as hc]
            [clojure.string :as str]
            [igpop.site.utils :as u]))

(def style
  (gc/css
   [:body
    [:.table {:display "table"
              :border-collapse "collapse"
              :margin-top "24px"
              :margin-bottom "32px"}]
    [:.row {:display "table-row"
            :border-bottom "1px solid #f1f1f1"}]
    [:.column {:display "table-cell"
               :padding "8px"}]
    [:.first-line {:border-bottom "2px solid #E6ECF1"
                   :color "#5b6975"}]

    [:a {:color "rgb(217, 86, 64)"}]
    [:.body
     {:padding 0
      :flex-direction "column"
      :font-family "'Roboto', sans-serif"
      :display "grid"
      :grid-template-areas (str/join "\n" ["\"header header header\" " "\"nav content ads\""])
      :grid-template-rows "60px 1fr"
      :grid-template-columns "minmax(0, 20%) minmax(0, 1fr) minmax(0, 15%)"
      :grid-gap "0px"
      :height "100vh"
      :margin 0}
     [:#welcome {:color "inherit"}]
     [:#header
      {:grid-area "header"
       :font-family "'Montserrat', sans-serif"
       :box-shadow " 0 3px 8px 0 rgba(116, 129, 141, 0.1)"
       :z-index "100"
       :border-bottom "1px solid #d4dadf"
       :padding "10px 0"}
      [:h5 {:display "inline-block"
            :width "20%"
            :text-align "center"
            :padding "5px 0px"
            :border-right "1px solid #ddd"}
       [:.fa {:color "#e94a35"
              :text-shadow "-1px -1px 1px #000000c7"}]]
      [:#top-nav
       {:text-align "center"
        :padding-left "80px"
        :display "inline-block"}
       [:a {:display "inline-block"
            :margin-right "30px"
            :font-size "15px"
            :font-weight "600"
            :color "#d95640"
            :padding "5px"}]]]
     [:#main-menu
      {:grid-area "nav"
       :padding-top "32px"
       :padding-left "68px"
       :border-right "1px solid #E6ECF1"
       :background-color "#f5f7f9"
       :overflow-y "auto"
       :padding-bottom "100px"
       :font-weight "800"
       ;; :font-family "'Montserrat', sans-serif"
       }
      [:a {:padding "7px 24px 7px 16px"
           :box-sizing "content-box"
           ;; :opacity 0.7
           :font-size "14px"
           :color "#5C6975"
           :display "block"
           :word-break "break-word"
           :background "none"
           :border "1px solid transparent"
           :cursor "pointer"
           :text-decoration "none"
           :font-weight "1000"
           :line-height "1.5"}
       [:&:hover {:background-color "#E6ECF1"}
        ]
       [:&.active
        {:background-color "white"
         :border-left "1px solid #E6ECF1"
         :border-top "1px solid #E6ECF1"
         :border-bottom "1px solid #E6ECF1"
         :border-right "1px solid white"
         :margin-right "-1px"
         :z-index 10
         :color "#d95640"}
        ]
     ]
      [:section
       {:border-left "1px solid #ddd"
        :margin-left "18px"}
       [:a {:color "gray"
            :font-weight "400"}]]]
     [:#content
      {:background-color "white"
       :padding "40px 88px"
       :grid-area "content"
       :font-size "16px"
       :font-family "Roboto, sans-serif"
       :font-weight "400"
       :line-height "1.625"}
      [:h1 {:margin-bottom "20px"
            :font-weight "800"
            :font-family "'Montserrat', sans-serif"}]
      [:h2 :h3 {:font-weight "800"
                :font-family "'Montserrat', sans-serif"}]
      [:hr {:border-top "2px solid #E6ECF1"}]
      [:a.refbtn {:margin "0 5px"
                  ;;:background "linear-gradient(#ffffff, #f6f7f8)"
                  ;;:border "1px solid #e8eaed"
                  :color "#888888"
                  ;;:text-decoration "none"
                  :cursor "pointer"
                  :padding "6px 12px"
                  :font-size "14px"
                  :font-style "italic"
                  :line-height "1.42857143"}]
      [:span.sub {:float :right
                  :font-size "16px"
                  :color "#888" :display "inline-block"}]]
     [:#db-content
      {:background-color "white"
       :padding "40px 0px"
       :margin "0 -40px"
       :grid-area "content"
       :font-size "16px"
       :font-family "Roboto, sans-serif"
       :font-weight "400"
       :line-height "1.625"}

      [:.db-item
       {:box-shadow "0 3px 8px 0 rgba(116, 129, 141, 0.1)"
        :display "inline-block"
        :width "350px"
        :padding "16px 24px"
        :border "1px solid #E6ECF1"
        :color "#242A31"
        :background-color "white"
        :vertical-align "top"
        :border-radius "3px"
        :margin-right "1em"
        :min-height "108px"
        :margin-bottom "1em"}
       [:&:hover {:text-decoration "none"
                  :border-color "rgb(56, 132, 254)"
                  :opacity 1
                  :color "#3884FE"}]
       [:h5 {:font-size "16px"
             :font-weight "800"}]
       [:.desc {:color "#9DAAB6" :font-size "14px"}]]]]
    [:.summary {:color "#74818D"}]]))

(defn top-nav [ctx]
  [:div#header
   [:a#welcome {:href (u/href ctx "")}
    [:h5
     [:span.fa.fa-fire]
     " "
     (or (:title ctx) "ig.yaml:.title")]]
   [:div#top-nav
    [:a {:href (u/href ctx "docs")} "Docs"]
    [:a {:href (u/href ctx "profiles")} "Profiles"]
    [:a {:href (u/href ctx "valuesets")} "ValueSets"]]])


(defn current-page [uri res-url]
  ;; (println "CURP:" uri res-url)
  (str/ends-with?
   (str/replace res-url #".html$" "")
   (str/replace uri #".html$" "")))

(defn menu [itms {uri :uri}]
  [:div#main-menu
   (for [{display :display href :href items :items} (sort-by first itms)]
     (if (= (count items) 0)
       [:div
        [:a {:href href :class (when (current-page uri href) "active")} display]]
       [:div
        [:a.item {:href href :class (when (current-page uri href) "active")} display]
        (into [:section] (for [{display :display href :href} items]
                           [:a.nested {:href href :class (when (current-page uri href) "active")} display]))]))])

(defn layout [ctx & content]
  (hc/html [:html
            [:head
             [:meta {:charset "utf-8"}]
             [:link {:rel "stylesheet"
                     :href "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
                     :integrity "sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
                     :crossorigin "anonymous"}]

             [:link {:rel "icon" :href (u/href ctx "static" "fire-solid.png")}]
             [:link {:href "//cdnjs.cloudflare.com/ajax/libs/font-awesome/5.11.2/css/all.min.css" :rel "stylesheet"}]
             [:link {:href "//fonts.googleapis.com/css?family=Montserrat|Roboto&display=swap" :rel "stylesheet"}]
             [:style style]
             [:title (or (:title ctx) "ig.yaml:.title")]]
            [:body
             (into [:div.body (top-nav ctx)] content)
             [:script {:src (u/href ctx "static" "jquery-3.4.1.min.js")}]
             [:script {:src (u/href ctx "static" "collapse-structure.js")}]
             [:script {:src (u/href ctx "static" "lmenu-view.js")}]
             [:script {:src (u/href ctx "static" "tabs.js")}]
             ]]))

