(ns todo.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [goog.string :as gstring]
              [goog.string.format]
              [cljsjs.react :as react])
    (:import goog.History))

;; debug:
;; (.log js/console (pr-str [val]))

;; -------------------------
;; Views

(defonce pace (atom {:minutes 5 :seconds 25 :paceInMetersPerSecond 3.076923076923077}))
(defonce calcs (atom 0))

(defn recalculate-chart [minutes seconds]
  (let [id calcs]
    (swap! pace assoc
           :minutes minutes :seconds seconds
           :paceInMetersPerSecond (/ 1000 (+ (* 60 (js/parseFloat minutes)) (js/parseFloat seconds))))
    )
  )

(defn pace-input [{:keys [minutes seconds on-save on-stop]}]
  (let [minutes (atom minutes)
        seconds (atom seconds)
        stop #(do 
                  (if on-stop (on-stop)))
        save #(let [m (-> @minutes str clojure.string/trim)
                    s (-> @seconds str clojure.string/trim)]               
                (if-not (empty? s) (on-save m s))
                )]
    (fn [props]
      [:div
       [:input {:type "text" :value @minutes :on-blur save
                :placeholder "minutes"
                :on-change #(reset! minutes (-> % .-target .-value))
                :on-key-down #(case (.-which %)
                                13 (save)
                                27 (stop)
                                nil)
                }]
       [:input {:type "text" :value @seconds :on-blur save
                :placeholder "seconds"
                :on-change #(reset! seconds (-> % .-target .-value))
                :on-key-down #(case (.-which %)
                                13 (save)
                                27 (stop)
                                nil)
                }]])))

(defn toHMS [timeInSeconds]
  (let [round (.round js/Math timeInSeconds)
        hours (.floor js/Math (/ timeInSeconds 3600))
        tISmins (mod timeInSeconds 3600)
        minutes (.floor js/Math (/ tISmins 60))
        tISsecs (mod tISmins 60)
        seconds (.floor js/Math tISsecs)]
    (gstring/format "%02f:%02f:%02f" hours minutes seconds)
    )
  )

(defn home-page []
  (let [p (:paceInMetersPerSecond @pace)]
    [:div
     [:div.page-header [:h1 "Pace Chart - Work in Progress...."]]
     [:div.row
      [:div.col-md-6
       [:div.panel.panel-default
        [:div.panel-heading "Set your pace"]
        [:div.panel-body
         [:div [pace-input {:minutes 5 :seconds 25 :on-save recalculate-chart}]]]]]
      [:div.col-md-6
       [:div.panel.panel-default
        [:div.panel-heading "Pace in Meters/Second"]
        [:div.panel-body
         [:div (:paceInMetersPerSecond @pace)]]]]
      ]

     [:div.row
      [:div.col-md-12
       [:div
        [:div.panel.panel-default
         [:div.panel-heading "Pace chart"]
         [:div.panel-body
          [:table.table
           [:thead
            [:tr
             [:td [:b "pace"]]
             [:td [:b "1 km"]]
             [:td [:b "5 km"]]
             [:td [:b "10 km"]]
             [:td [:b "half"]]
             [:td [:b "full"]]]]
           [:tbody
              [:tr
               [:td (:minutes @pace) ":" (:seconds @pace)]
               [:td (toHMS (/ 1000 p))]
               [:td (toHMS (/ 5000 p))]
               [:td (toHMS (/ 10000 p))]
               [:td (toHMS (/ 21100 p))]
               [:td (toHMS (/ 42200 p))]]
              ]
            ]]]]]
      
      [:div.row
       [:div [:a {:href "#/about"} "Read more about this toy"]]]]
     ]
    )
  )

(defn about-page []
  [:div [:h2 "About todo"]
   [:p "This is a small toy using ClojureScript / Reagent to dynamically create a pace chart"]
   [:div [:a {:href "#/"} "Check out the pace chart"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render
   [current-page]
   (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
