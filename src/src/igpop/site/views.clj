(ns igpop.site.views
  (:require [garden.core :as gc]
            [hiccup.core :as hc]
            [clojure.string :as str]))


(def style
  (gc/css
   [:body
    [:.body
     {:padding 0
      :flex-direction "column"
      :font-family "'Roboto', sans-serif"
      :display "grid"
      :grid-template-areas (str/join "\n"
                                     ["\"header header header\" "
                                      "\"nav content ads\""])

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
       :font-family "'Montserrat', sans-serif"
       }
      [:a {:padding "7px 24px 7px 16px"
           :box-sizing "content-box"
           :opacity 0.7
           :font-size "14px"
           :color "inherit";;"#5C6975"
           :display "block"
           :word-break "break-word"
           :background "none"
           :border "1px solid transparent"
           :cursor "pointer"
           :text-decoration "none"
           :font-weight "600"
           :line-height "1.5"}
       [:&:hover {:background-color "#E6ECF1"}]
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
       :font-family "Content-font, Roboto, sans-serif"
       :font-weight "400"
       :line-height "1.625"
       }

      [:h1 {:margin-bottom "20px" :font-family "'Montserrat', sans-serif"}]
      [:hr {:border-top "2px solid #E6ECF1"}]
      [:span.sub {:float :right
                  :font-size "16px"
                  :color "#888" :display "inline-block"}]
      ;; [:.profile {:box-shadow "1px 2px 3px #E6ECF1"}]

      [:.summary {:color "#74818D"}]
      [:.pth {:color "#6b6e71" :opacity 0.5 :font-weight "400"}]
      [:.required {:color "red"}]
      [:.tp {:color "white"
             :background-color "#61affe"
             :display "inline-block"
             :opacity 0.5
             :font-size "10px"
             :margin-right "1em"
             :font-weight "bold"
             :vertical-align "middle"
             :line-height "20px"
             :text-align "center"
             :width "20px" :height "20px" :border-radius "20px"}
       [:&.string {:background-color "#49cc90"}]
       [:&.Reference {:background-color "#fca130"}]
       [:&.code
        :&.Coding
        :&.CodeableConcept
        {:background-color "#007bff"}]]
      [:.desc {:color "#666" :font-size "14px"}]
      [:table.prof
       {:width "100%"}
       [:tr {:margin 0 :padding 0
             :border-bottom "1px solid #ddd"}]
       [:td.tree {:margin 0 :padding 0}]
       [:td {:margin 0
             :line-height "30px"
             :padding "5px 10px"}]

       ]
      [:.sps {:border-left "1px solid #aaa"
              :height "20px"
              :margin-left "20px"
              :display "inline-block"
              }]
      [:.lsps {:border-left "1px solid transparent"
              :height "20px"
              :margin-left "20px"
              :display "inline-block"
              }]
      [:.conn {:width "10px"
               :display "inline-block"
               :vertical-align "top"
               :margin-top "16px"
               :border-top "1px solid #aaa"}]
      ;; [:.tp-link {:font-size "13px"}]
      [:.nmx {:display "inline-block"
             :font-size "15px"
             :font-weight "bold"
             }]
      
      

      ]


     ]]))

(defn layout [& content]
  (hc/html [:html
            [:head
             [:meta {:charset "utf-8"}]
             [:link {:rel "stylesheet"
                     :href "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
                     :integrity "sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
                     :crossorigin "anonymous"}]

             ;; [:link {:href "//fonts.googleapis.com/css?family Montserrat" :rel "stylesheet"}]
             [:link {:href "//fonts.googleapis.com/css?family Montserrat:100,300,400,500,700,900" :rel "stylesheet"}]
             ;; [:link {:href "//fonts.googleapis.com/css?family Roboto" :rel "stylesheet"}]
             [:link {:href "//fonts.googleapis.com/css?family Roboto:100,300,400,500,700,900" :rel "stylesheet"}]
             [:style style]
             [:title "IGPOP"]]
            [:body
             (into [:div.body] content)
             [:script {:src "/assets/listener.js"}]
             [:script {:src "/assets/jquery-3.4.1.min.js"}]
             [:script {:src "/assets/menu-handler.js"}]]]))

