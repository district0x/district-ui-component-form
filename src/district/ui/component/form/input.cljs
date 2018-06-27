(ns district.ui.component.form.input
  (:require [clojure.set :refer [rename-keys]]
            [reagent.core :as r]
            [clojure.string :as str])
  (:require-macros [reagent.ratom :refer [reaction]]))

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

(defn autocomplete-input [{:keys [form-data id ac-options on-option-selected on-empty-backspace]}]
  (let [selected-idx (r/atom 0)]
    (fn [{:keys [form-data id ac-options on-option-selected on-empty-backspace]}]
      (let [select-opt (fn [o]
                         (swap! form-data assoc id "") 
                         (reset! selected-idx 0)
                         (on-option-selected o))
            selectable-opts (let [input (get @form-data id)]
                              (when (not-empty input)
                                (filter #(str/starts-with? % input) ac-options)))
            key-down-handler (fn [e]
                               (let [key-code (-> e .-keyCode)
                                     input (get @form-data id)]
                                 (cond
                                   (and (= key-code 8) ;; backspace key
                                        (empty? input))
                                   (on-empty-backspace)

                                   (= key-code 13) ;; return key
                                   (select-opt (nth selectable-opts @selected-idx))

                                   (= key-code 40) ;; down key
                                   (swap! selected-idx #(min (inc %) (dec (count selectable-opts))))

                                   (= key-code 38) ;; up key
                                   (swap! selected-idx #(max (dec %) 0)))))]
        [:div.autocomplete-input
         [text-input {:form-data form-data
                      :id id
                      :on-key-down key-down-handler}]
         (when (not-empty selectable-opts)
           [:ol.options
            (doall
             (map-indexed
              (fn [idx opt]
                ^{:key opt}
                [:li.option {:class (when (= idx @selected-idx) "selected")
                             :style (when (= idx @selected-idx) {:background-color "red"}) ;; for testing only
                             :on-click #(select-opt opt)} 
                 opt])
              selectable-opts))])]))))

(defn chip-input [{:keys [form-data chip-set-path ac-options chip-render-fn on-change]}]
  [:div.chip-input
   [:ol.chip-input
    (for [c (get-in @form-data chip-set-path)]
      ^{:key c}
      [:li.chip
       (chip-render-fn c)
       [:span {:on-click #(swap! form-data update-in chip-set-path (fn [cs] (remove #{c} cs)))}
        "X"]])]
   [autocomplete-input {:form-data form-data
                        :id :text
                        :ac-options (->> ac-options
                                         (remove (set (get-in @form-data chip-set-path)))
                                         (filter #(not= % nil))
                                         (into []))
                        :on-option-selected #(do (swap! form-data update-in chip-set-path (fn [cs] (conj cs %)))
                                                 (on-change))
                        :on-empty-backspace #(swap! form-data update-in chip-set-path butlast)}]])

(defn file-drag-input [{:keys [form-data id file-accept-pred on-file-accepted on-file-rejected]}]
  (let [allow-drop #(.preventDefault %)
        handle-files-select (fn [files]
                              (let [f (aget files 0)
                                    fprops {:name (.-name f)
                                            :type (.-type f)
                                            :size (.-size f)}
                                    reader (js/FileReader.)]
                                (if (file-accept-pred fprops)
                                  (do
                                    (set! (.-onload reader) (fn [e]
                                                              (let [img-data (-> e .-target .-result)
                                                                    fmap (assoc fprops :url-data img-data)] 
                                                                (swap! form-data assoc id fmap)
                                                                (on-file-accepted (assoc fmap :file f)))))
                                    (.readAsDataURL reader f))
                                  (on-file-rejected fprops))))]
    (fn []
      (let [{:keys [name url-data]} (get @form-data id)]
        [:div.dropzone 
         {:on-drag-over allow-drop
          :on-drop #(do
                      (.preventDefault %)
                      (handle-files-select (.. % -dataTransfer -files)))
          :on-drag-enter allow-drop}
         [:img {:src url-data}]
         [:span.file-name name]
         [:input {:type :file
                  :on-change (fn [e]
                               (handle-files-select (-> e .-target .-files)))}]]))))
