(ns igpop.site.views
  (:require [garden.core :as gc]
            [hiccup.core :as hc]
            [clojure.string :as str]))

(def style
  (gc/css
   [:body
    [:a {:color "rgb(217, 86, 64)"}]
    [:.body
     {:padding 0
      :flex-direction "column"
      :font-family "'Roboto', sans-serif"
      :display "grid"
      :grid-template-areas (str/join "\n" ["\"header header header\" " "\"nav content ads\""])
      :grid-template-rows "60px 1fr"
      :grid-template-columns "20% 1fr 15%"
      :grid-gap "0px"
      :height "100vh"
      :margin 0}
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
            :border-right "1px solid #ddd"}]
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
            :font-weight "400"
            }]]
      ]
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
      [:hr {:border-top "2px solid #E6ECF1"}]
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
    [:.summary {:color "#74818D"}]
    [:.tp {:position "relative"
           :z-index 10
           :display "inline-block"
           :opacity 0.6
           :font-size "10px"
           :margin-right "1em"
           :background-color "white"
           :box-shadow "0px 0px 2px black"
           :color "#5b6975"
           :font-weight "800"
           :vertical-align "middle"
           :line-height "20px"
           :text-align "center"
           :width "20px" :height "20px"
           :border-radius "20px"}
     [:.fa {:padding-top "5px" :font-size "12px"}]
     [:&.complex :&.obj {:border-radius "3px"}]
     [:&.profile {:margin-left "-10px" :border-radius "3px"}]]
    ;; profile 
    (let [link-color "#b3bac0" ;;"#e6ecf0"
          link-border (str "1px solid " link-color)
          left-width 360]
      [:.el-cnt
       {:color "rgb(33,37,41)"
        :font-weight "400"}

       [:.required {:color "red" :opacity 0.7 :margin "0 0.2em"}]
       [:.coll {:color "#888"}]
       [:.desc {:color "#5b6975" :font-size "14px"}]
       [:.tp-link {:font-size "13px" :color "#909aa2"}]

       [:.el {:border-left link-border}
        [:&:last-of-type {:border-left-color "transparent"}
         [:.el-header  {:border-left-color "transparent"
                       :font-size "15px"
                       :line-height "30px"}]]]
       [:.el-header {:border-bottom "1px solid #f1f1f1"
                    :padding-left "10px"
                    :position "relative"
                    :line-height "30px"
                    :border-left link-border
                    :margin-left "-1px"}
        
        [:&:hover {}]
        [:&:last-of-type {:border-left-color "transparent"}]]
       [:.link
        {:width "10px"
         :height "22px"
         :display "inline-block"
         :position "absolute"
         :top "-5px" 
         :left "-1px" 
         ;; :margin-top "14px"
         :border-bottom link-border 
         :border-left link-border}]
       
       [:.nm {:width (str left-width "px")
              :color "rgb(59, 69, 78)"
              :display "inline-block"
              :font-size "15px"}]
       
       [:.desc {:display "inline-block"}]
       [:.el-cnt {:margin-left "20px"}
        [:.nm {:width (str (- left-width 20) "px")}]
        [:.el-cnt
         [:.nm {:width (str (- left-width 40) "px")}]]]])]))

(defn top-nav []
  [:div#header
   [:h5 "FHIR RU Core"]
   [:div#top-nav
    [:a {:href "/"} "Docs"]
    [:a {:href "/profiles"} "Profiles"]
    [:a {:href "/valuesets"} "ValueSets"]]])

(defn layout [& content]
  (hc/html [:html
            [:head
             [:meta {:charset "utf-8"}]
             [:link {:rel "stylesheet"
                     :href "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
                     :integrity "sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
                     :crossorigin "anonymous"}]

             [:link {:href "//cdnjs.cloudflare.com/ajax/libs/font-awesome/5.11.2/css/all.min.css" :rel "stylesheet"}]
             [:link {:href "//fonts.googleapis.com/css?family=Montserrat|Roboto&display=swap" :rel "stylesheet"}]
             [:style style]
             [:title "IGPOP"]]
            [:body
             (into [:div.body (top-nav)] content)
             ;; [:script {:src "/assets/listener.js"}]
             ;; [:script {:src "/assets/jquery-3.4.1.min.js"}]
             ;; [:script {:src "/assets/menu-handler.js"}]
             ]]))

