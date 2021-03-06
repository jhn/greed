(ns greed.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.set :as set])
  (:gen-class))

(def ^:dynamic *base-url* "http://www.mrporter.com")

(def ^:dynamic *common-projects-sale* "/mens/sale/designers/common_projects/all/")

(def ^:dynamic *common-projects* "/mens/shoes/sneakers/low_top_sneakers?designerFilter=Common_Projects")

(def ^:dynamic *selectors*
  {:products  [:div#product-list :div.description]
   :designer  [:span.product-designer]
   :title     [:span.product-title]
   :pre-sale  [:p.pre-sale-price]
   :post-sale [:p.sale-price :span.price-value]
   :percent   [[:p.sale-price (html/nth-child 3)]]
   :url       [:div.designer :a]})

(def ^:dynamic *max-price* 200)

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn squish [line]
  (as-> line l
        (string/replace l #"\n" " ")
        (string/split l #"\s+")
        (string/join " " l)
        (string/triml l)))

(defn str->double [s]
   (try (Double/parseDouble s) (catch Exception _ 0.0)))

(defn update-vals [map vals f]
  (reduce #(update-in % [%2] f) map vals))

(defn extract [node]
  (let [designer  (first (html/select [node] (:designer  *selectors*)))
        title     (first (html/select [node] (:title     *selectors*)))
        pre-sale  (first (html/select [node] (:pre-sale  *selectors*)))
        post-sale (first (html/select [node] (:post-sale *selectors*)))
        percent   (first (html/select [node] (:percent   *selectors*)))
        url       (first (html/select [node] (:url       *selectors*)))
        values    (conj (map html/text [designer title pre-sale post-sale percent])
                        (str *base-url* (:href (:attrs url))))
        str-map (zipmap [:url :designer :title :pre-sale :post-sale :percent] (map squish values))]
    (update-vals str-map [:pre-sale :post-sale :percent] str->double)))

(defn print-product [product]
  (println)
  (println (product :designer "No designer"))
  (println "\t" (product :title "No title"))
  (println "\t" (product :pre-sale "No pre-sale"))
  (println "\t" (product :post-sale "No post-sale"))
  (println "\t" (product :percent "No percent"))
  (println "\t" (product :url "No url")))

(defn print-products [products]
  (doseq [product products]
    (print-product product)))

(def last-products (atom #{}))

(defn products []
  (html/select (fetch-url (str *base-url* *common-projects*))
               (:products *selectors*)))

(defn go-crazy [products]
  (println "PARTY LIKE IT'S 1999 SON")
  (print-products products))

(defn good-product [{p :post-sale}]
  (and (> p 0) (< p *max-price*)))

(defn go []
  (when-let [new-products (set (map extract (products)))]
    (when-let [diff (set/difference new-products @last-products)]
      (when-not (empty? diff)
        (let [good-deals (set/select good-product diff)]
          (if-not (empty? good-deals)
            (go-crazy good-deals)))
        (reset! last-products new-products)))))

(defn schedule [f ms]
  (future (while true (do (f) (Thread/sleep ms)))))

(defn minutes->ms [m]
  (* 1000 60 m))

(defn -main [& args]
  (schedule go (minutes->ms 10)))