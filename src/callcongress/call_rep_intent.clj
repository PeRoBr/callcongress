(ns callcongress.call-rep-intent
  (:require [com.climate.boomhauer.intent-handler :refer [defintent]]
            [callcongress.sunlight :as sunlight]
            [callcongress.reps :as reps]
            [callcongress.dynfar :as dyn]
            )
  (:import [com.amazon.speech.speechlet SpeechletResponse]
           [com.amazon.speech.ui PlainTextOutputSpeech Reprompt]))

;;; This stuff should be in util library
(defn- mk-plain-speech [text]
  (doto (PlainTextOutputSpeech.) (.setText text)))

(defn- mk-plain-reprompt [text]
  (let [reprompt (Reprompt.)]
    (.setOutputSpeech reprompt (mk-plain-speech text))
    reprompt))

(defn set-session-slot [session slot value]
  (.setAttribute session slot value))

(defn user-id [session]
  (.getUserId (.getUser session)))

(def last-bill-slot "LAST_BILL")

(defn user-zip [user]
  (dyn/read-zip user))

(defn representative-text [user chamber]
  (let [zip (user-zip user)
        legislators (filter #(= (:chamber %) chamber)
                            (sunlight/get-legislators zip))
        legislator (first legislators)]
    (format "You can call %s %s at %s"
            (or (:nickname legislator)
                (:first_name legislator))
            (:last_name legislator)
            (:phone legislator))))

(defn call-rep [session session-map]
  (let [bill (sunlight/get-bill (get session-map (keyword last-bill-slot)))
        chamber (or (get session-map :Chamber)
                    (keyword (:chamber bill))
                    "house")
        user (user-id session)
        text (representative-text user chamber)
        speech (mk-plain-speech text)]
    (SpeechletResponse/newTellResponse speech)))

(defn call-senator [session session-map]
  (call-rep session (assoc session-map :Chamber "senate")))

(defintent :CallRepIntent call-rep)
(defintent :CallSenatorIntent call-senator)

