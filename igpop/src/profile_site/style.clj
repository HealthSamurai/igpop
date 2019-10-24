(ns profile-site.style
  (:require [garden.core :as gc]))

(defn style [data]
  (gc/css data))

(defn set-page-style [style & styles]
  (conj styles style))

(def profile-style
  [[:.line-item
    {:vertical-align "top"
     :text-align "left"
     :padding "0px 4px 0px 4px"
     :background-color "white"}]

   [:.line-item-resource-type
    {:vertical-align "top"
     :text-align "left"
     :padding "0px 4px 0px 4px"}]

   [:.line-inner-item
    {:vertical-align "top"
     :text-align "left"
     :padding "0px 4px 0px 4px"
     :background-image "url(/assets/tbl_bck110.png)"}]

   [:table
    {:font-size "13px"
     :font-family "verdana"
     :max-width "100%"
     :border-spacing "0px"
     :border-collapse "collapse"}]

   [:a#list-item
    {:color "#666"}]

   [:a#list-item:hover
    {:text-decoration "underline"}]

   [:a
    {:text-decoration "none"}]

   [:td
    {:line-height "1em"}]

   [:tbody
    {:display "table-row-group"}]

   [:.flag-item
    {:padding-left "3px"
     :padding-right "3px"
     :color "white"
     :background-color "red"}]

   [:th
    {:vertical-align "top"
     :text-align "left"
     :background-color "white"
     :border "0px #F0F0F0 solid"
     :padding "0px 4px 0px 4px"}]

   [:.table-icon
    {:vertical-align "top"
     :margin "0px 2px 0px 0px"
     :background-color "white"}]

   [:body
    {:display "flex"
     :flex-direction "column"
     :font-family "'Roboto', sans-serif"
     :margin "0"
     :color "#212529"
     :line-height "1.5"
     :text-align "left"}]

   [:.logo :.img-logo
    {:height "30px"}]

   [:.menu :.logo
    {:padding "10px 50px"}]

   [:.profile
    {:margin "20px 88px"}]

   [:.wrap
    {:flex-direction "row"
     :display "flex"
     :box-sizing "border-box"}]])

(def navigation-menu-style
  [
  ;; [:.heading-segment
  ;;   {:display "flex"
  ;;    :border-bottom "1px solid #d4dadf"}]

   [:.heading
    {:height "59px"
     :display "block"
     :border-bottom "1px solid #d4dadf"}]

   [:.heading-logo
    {:margin "19px 0px"
     :height "40px"
     ;;:box-shadow "0 3px 8px 0 rgba(116, 129, 141, 0.1)"
     :border-right "1px solid #E6ECF1"
     ;;:border-bottom "1px solid #d4dadf"
     :background-color "#FFFFFF"}]

   [:.fhir-image
    {;;:padding "20px 0px 20px 92px"
     :padding-left "92px"
     :height "40px"}]

   ;; [:.heading-logo
   ;;  {;;:border-right "1px solid #e6ecf1"
   ;;   }]

   [:.left-side
    {:display "block"}]

   ;; [:.logo-border
   ;;  {:margin-top "20px"
   ;;   :width "0"
   ;;   :height "40px"
   ;;   :border-right "1px solid #e6ecf1"}]

   [:.whole-content-body
    {:font-family "Roboto sans-serif"
     :display "flex"}]

   [:.left-menu
    {:padding-top "32px"
     :padding-left "68px";;"calc((100% - 1448px) / 2)"
     :min-width "298px"
     :height "100%"
     ;;:width "calc((100% - 1448px) / 2 + 298px)"
     :border-right "1px solid #E6ECF1"
     :background-color "#f5f7f9"}]

   ;; Style the menu links and the dropdown divs */
   [:.lmenu-item
    {:margin-left "24px"
     :padding "7px 24px 7px 16px"
     :box-sizing "content-box"
     :font-size "14px"
     :color "inherit";;"#5C6975"
     :display "block"
     :word-break "break-word"
     :background "none"
     :border "1px solid transparent"
     :cursor "pointer"
     :font-weight "500"
     :line-height "1.5"}]

   [:.lmenu-item [:a
                  {:width "100%"
                   :display "block"
                   :color "#3B454E"
                   :flex "1"
                   :font-weight "500"}]]

   [:.dropdown-btn [:svg
    {:align-items "center"
     :color "#9DAAB6"
     :font-size "18px"}]]

   ;; On mouse-over */
   [:.lmenu-item:hover {:background-color "#E6ECF1"}]

   [:.lmenu-add-items
    {:margin-left "45px"
     :padding-left "10px"
     :border-left "1px solid #E6ECF1"}]

   [:.lmenu-add-items [:a {:color "#9DAAB6"}]]

   ;;/* Add an active class to the active dropdown div */
   [:.active
    {:background-color "white"
     :border "1px solid #e6ecf1"
     :border-right "none"
     :color "#d95640"}]

   [:.active
    [:a
     {:color "#d95640"}]]

   [:.dropdown-btn
    {:display "flex"}]

   ;; Dropdown container (hidden by default).
   [:.dropdown-container
    {:display "none"
     :color "#9DAAB6"
     :border-left-color "rgb(230, 236, 241)"}]

   [:.body-container
    {:margin "0px 88px"
     :width "50%"         ;;$$
     :max-width "750px"
     :min-width "0"}]

   [:h1
    {:font-size "32px"
     :margin "0"}]

   [:.body-header
    {:margin-bottom "32px"
     :padding "40px 0px"
     :color "#242A31"
     :font-weight "500"
     :line-height "1.5"
     :border-bottom "2px solid #E6ECF1"}]

   [:.body-content
    {:margin "0"
     :padding "0"}]
])
