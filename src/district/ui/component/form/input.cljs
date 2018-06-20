(ns district.ui.component.form.input
  (:require [clojure.set :refer [rename-keys]]))

(def arg-keys [:id :form-data :errors :on-change :attrs])

(defn get-by-path
  ([doc path]
   (get-by-path doc path nil))
  ([doc path default]
   (let [n-path (flatten
                 (if-not (seq? path)
                   [path]
                   path))]
     (get-in doc n-path default))))

(defn get-in-errvec [doc path]
  (some (fn [e]
          (when (= (:field e)
                   path)
            (:message e)))
        doc))

(defn assoc-by-path
  [doc path value]
  (let [n-path (flatten
                (if-not (seq? path)
                  [path]
                  path))]
    (assoc-in doc n-path value)))

(defn update-by-path
  [doc path fn]
  (let [n-path (flatten
                (if-not (seq? path)
                  [path]
                  path))]
    (update-in doc n-path fn)))

(defn with-label [label body]
  [:div
   [:label label]
   body])

(defn err-reported [{:keys [id form-data errors on-change] :as opts} cmp]
  (let [touched? (atom false)
        on-touched (fn [new-val]
                     (reset! touched? true)
                     (when on-change
                       (on-change new-val)) )]
    (fn [{:keys [id form-data errors on-change] :as opts}]
      (let [errors (if (satisfies? IAtom errors)
                     @errors
                     errors)
            err (if-let [e (and
                            @touched?
                            (or
                             (get-by-path errors [:local id])
                             (get-in-errvec (:local errors) id)))]
                  (apply str e)
                  (when-let [e (and (not @touched?)
                                    (get-by-path errors [:remote id]))]
                    (apply str e)))]
        [:div.input-group
         {:class (when err :has-error)}
         [cmp (assoc opts :on-change on-touched)]
         [:span.help-block (if err
                             err
                             [:div {:dangerouslySetInnerHTML {:__html "&nbsp;"}}])]]))))


(defn text-input* [{:keys [id form-data errors on-change attrs input-type] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs input-type] :as opts}]
    (let [a (if (= input-type :textarea)
              :textarea
              :input)
          other-opts (apply dissoc opts (conj arg-keys :input-type))]
      [a (merge
          {:type "text"
           :value (get-by-path @form-data id "")
           :on-change #(let [v (-> % .-target .-value)]
                         (swap! form-data assoc-by-path id v)
                         (when on-change
                           (on-change v)))}
          other-opts
          attrs)])))

(defn text-input [opts]
  [err-reported opts text-input*])

(defn textarea-input* [opts]
  [text-input* (merge opts
                      {:input-type :textarea})])

(defn textarea-input [opts]
  [err-reported opts textarea-input*])

(defn select-input* [{:keys [id form-data errors on-change attrs options] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs options] :as opts}]
    (let [other-opts (apply dissoc opts (conj arg-keys :options))]
      [:select
       (merge
        {:on-change (fn [item]
                      (let [val (.-target.value item)
                            iv (and (re-matches #"^\d*(\.|\.)?\d*$" val)
                                    (js/parseFloat val))
                            val (if
                                    (or
                                     (nil? iv)
                                     (js/isNaN iv))
                                  val
                                  iv)]
                        (swap! form-data assoc-by-path id val)
                        (when on-change
                          (on-change val))))
         :value (get-by-path @form-data id)}
        other-opts
        attrs)

       (doall
        (map (fn [option]
               ^{:key (str (:key option))}
               [:option (rename-keys option {:key :value})
                (:value option)])
             options))])))

(defn select-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts select-input*])

(defn int-input* [{:keys [id form-data errors on-change attrs] :as opts}]
  (let [fallback (atom nil)]
    (fn [{:keys [id form-data errors on-change attrs] :as opts}]
      (let [other-opts (apply dissoc opts (conj arg-keys :options))]
        [:input (merge
                 {:type "text"
                  :value (if-let [f @fallback]
                           f
                           (get-by-path @form-data id ""))
                  :on-change #(let [v (-> % .-target .-value)]
                                (when-let [iv (and (re-matches #"^\d*$" v)
                                                   (js/parseInt v))]
                                  (if-not (js/isNaN iv)
                                    (do
                                      (reset! fallback v)
                                      (when on-change
                                        (on-change iv))
                                      (swap! form-data assoc-by-path id iv))
                                    (do
                                      (swap! form-data assoc-by-path id nil)
                                      (reset! fallback v)))))}
                 other-opts
                 attrs)]))))

(defn int-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts int-input*])

(defn checkbox-input* [{:keys [id form-data errors on-change attrs] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs] :as opts}]
    (let [other-opts (apply dissoc opts (conj arg-keys :options))]
      [:input (merge
               {:type "checkbox"
                :checked (get-by-path @form-data id "")
                :on-change #(let [v (-> % .-target .-value)]
                              (swap! form-data update-by-path id not)
                              (when on-change
                                (on-change v)))}
               other-opts
               attrs)])))

(defn checkbox-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts checkbox-input*])
