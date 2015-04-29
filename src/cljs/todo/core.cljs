(ns todo.core
  (:use [jayq.core :only [$ css html]])
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

(defonce pace (atom {}))
(defonce calcs (atom 0))

(defn minutes-to-seconds [minutes seconds]
  (+ (* 60 minutes) seconds))

(defn inc-seconds [seconds increase]
  (+ increase seconds))

(defn calc-pace-in-ms [seconds]
  (/ 1000 seconds))

(defn recalculate-chart [minutes seconds distance]
  (let [id calcs]
    (swap! pace assoc
           :minutes (js/parseFloat minutes) :seconds (js/parseFloat seconds)
           :distance distance
           :paceInMetersPerSecond (calc-pace-in-ms
                                   (minutes-to-seconds (js/parseFloat minutes)  (js/parseFloat seconds)))
    )))

(defn pace-input [{:keys [distance minutes seconds on-save on-stop]}]
  (let [minutes (atom minutes)
        seconds (atom seconds)
        distance (atom distance)
        stop #(do
                (if on-stop (on-stop)))
        save #(let [m (-> @minutes str clojure.string/trim)
                    s (-> @seconds str clojure.string/trim)
                    d (-> @distance str clojure.string/trim)]
                (if-not (empty? s) (on-save m s d))
                )]
    (fn [props]
      [:div.col-md-10.col-md-offset-1
       [:form.form-horizontal
        [:div.form-group
         [:div.input-group
          [:input.form-control {:id "i3" :type "text" :value @distance
                                :on-change #(reset! distance (-> % .-target .-value))
                                :size 2
                                }]
          [:div.input-group-addon "distance in km"]]]
        [:div.form-group
         [:div.input-group
          [:input.form-control {:id "i1" :type "text" :value @minutes
                                :on-change #(reset! minutes (-> % .-target .-value))
                                :size 2
                                }]
          [:div.input-group-addon "minutes"]
          ]
         ]
        [:div.form-group
         [:div.input-group
          [:input.form-control {:id "i2" :type "text" :value @seconds
                                :on-change #(reset! seconds (-> % .-target .-value))
                                :size 2
                                }]
          [:div.input-group-addon "seconds"]
          ]]
        [:div.form-group
         [:input.btn.btn-primary {:type "button" :value "Calculate" :on-click save}]]
        ]])))

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

(defn generic-pace-chart [paceMap]
  (let [p (:paceInMetersPerSecond paceMap)
        d (* 1000 (:distance paceMap))
        m (:minutes paceMap)
        s (:seconds paceMap)]
    [:div
     [:div.panel.panel-default
      [:div.panel-heading "Generic pace chart"]
      [:div.panel-body
       [:table.table
        [:thead
         [:tr
          [:td [:b "pace"]]
          [:td [:b (:distance paceMap) " km"]]
          [:td [:b "5 km"]]
          [:td [:b "10 km"]]
          [:td [:b "half"]]
          [:td [:b "full"]]]
         ]
        [:tbody
         (for [x (reverse (range 1 6))]
          (let [
                p1 (calc-pace-in-ms (- (minutes-to-seconds m s) x))
                ]
            [:tr.warning
             [:td (toHMS (/ 1000 p1))]
             [:td (toHMS (/ d p1))]
             [:td (toHMS (/ 5000 p1))]
             [:td (toHMS (/ 10000 p1))]
             [:td (toHMS (/ 21100 p1))]
             [:td (toHMS (/ 42200 p1))]
             ])
          )
         [:tr.info
          [:td (toHMS (/ 1000 p))]
          [:td (toHMS (/ d p))]
          [:td (toHMS (/ 5000 p))]
          [:td (toHMS (/ 10000 p))]
          [:td (toHMS (/ 21100 p))]
          [:td (toHMS (/ 42200 p))]]
         ]
        (for [x (range 1 6)]
          (let [
                p1 (calc-pace-in-ms (+ x (minutes-to-seconds m s)))
                ]
            [:tr.success
             [:td (toHMS (/ 1000 p1))]
             [:td (toHMS (/ d p1))]
             [:td (toHMS (/ 5000 p1))]
             [:td (toHMS (/ 10000 p1))]
             [:td (toHMS (/ 21100 p1))]
             [:td (toHMS (/ 42200 p1))]
             ])
          )
        ]
        ]]]))

(defn distance-at-pace [distance pace]
  [:div.panel.panel-default
   [:div.panel-heading "Distance at pace"]
   [:div.panel-body
    [:div [:b distance] "km at a pace of " [:b (toHMS (/ 1000 pace))] "/km will taken " [:b (toHMS (/ (* distance 1000) pace))]]]]
  )

(defn home-page []
  (let [p (:paceInMetersPerSecond @pace)]
    [:div

     [:div.page-header [:h1 "Pace Chart"]]

     [:div.row
      [:div.col-md-6
       [:div.panel.panel-default
        [:div.panel-heading "Set your pace"]
        [:div.panel-body
         [:div [pace-input {:distance (:distance @pace) :minutes (:minutes @pace) :seconds (:seconds @pace) :on-save recalculate-chart}]]]]]
      (when p
        [:div.col-md-6
         [:div.panel.panel-default
          [:div.panel-heading "Pace in Meters/Second"]
          [:div.panel-body
           [:div p]]]
         (distance-at-pace (:distance @pace) (:paceInMetersPerSecond @pace))
         ])      
      ]

     (when p
       [:div.row [:div.col-md-12 (generic-pace-chart @pace)]])

     [:div.row
      [:div [:a {:href "#/about"} "Read more about this toy"]]]]

    ))

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
