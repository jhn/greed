(ns greed.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as str]
            [clojure.set :as set]))

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

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn split-on-space [word]
  (str/split word #"\s+"))

(defn squish [line]
  (str/triml (str/join " "
     (split-on-space (str/replace line #"\n" " ")))))

(defn extract [node]
  (let [designer  (first (html/select [node] (:designer  *selectors*)))
        title     (first (html/select [node] (:title     *selectors*)))
        pre-sale  (first (html/select [node] (:pre-sale  *selectors*)))
        post-sale (first (html/select [node] (:post-sale *selectors*)))
        percent   (first (html/select [node] (:percent   *selectors*)))
        url       (first (html/select [node] (:url       *selectors*)))
        result (conj
                 (map html/text [designer title pre-sale post-sale percent])
                 (str *base-url* (:href (:attrs url))))]
    (zipmap [:url :designer :title :pre-sale :post-sale :percent] (map squish result))))

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
  (html/select (fetch-url (str *base-url* *common-projects*)) (:products *selectors*)))

(defn go []
  (let [new-products (set (map extract (products)))
        diff (set/difference new-products @last-products)]
    (when-not (empty? diff)
      (print-products diff)
      (println (apply str (repeat 30 "*")))
      (reset! last-products new-products))))

(defn schedule [f ms]
  (future (while true (do (f) (Thread/sleep ms)))))

(defn minutes-to-ms [m]
  (* 1000 60 m))

(defn -main [& args]
  (schedule go (minutes-to-ms 10)))
