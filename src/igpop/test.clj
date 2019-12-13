(ns test
  (:require [flatland.ordered.map :refer :all]))

(defn myfunc [collections]
  (loop [acc '()
         prod collections]
    (if (= (first prod) nil)
      acc
      (let [inprod (first prod)]
        (if (= (first inprod) nil)
          (recur acc (rest prod))
          (recur (conj (first inprod) acc) (concat (rest (first prod)) (rest prod))))))))

(myfunc2 '((1 2) 3 [4 #{5 6}]))



(defn myfunc
  [x]
  (filter (complement sequential?)
          (rest (tree-seq sequential? seq x))))

(defn myfunc2 [x]
  (tree-seq sequential? seq x))

(defn myfunc2 [x]
  (first (rest (tree-seq sequential? seq x))))




(defn myfunc [input]
  (loop [ans '()
         cur (list (first input))
         prod input]
    (println cur)
    (println ans)
    (if (empty? prod)
      (if (empty? cur)
        ans
        (conj (list cur) ans))
      (if (= (last cur) (first prod))
        (recur ans
               (conj cur (first prod))
               (rest prod))
        (recur (conj (list cur) ans)
               '()
               (rest prod))))))

        ;; (if (empty? cur)
        ;;   (recur ans
        ;;        (list (first prod))
        ;;        (rest prod))
        ;;   (recur ans
        ;;          (concat cur (list (first prod)))
        ;;          (rest prod))
        ;;   )))))

(defn myfunc [input]
  (loop [ans (list (list (first input)))
         prod (rest input)]
    (if (empty? prod)
      ans
      (if (= (last (last ans)) (first prod))
        (if (= nil (butlast ans))
          (recur (list (conj (last ans) (first prod)))
                 (rest prod))
          (recur (concat (butlast ans) (list (conj (last ans) (first prod))))
                 (rest prod)))
        (recur (concat ans (list (list (first prod))))
               (rest prod))))))

(myfunc [1 1 2 4 4 4 4 4 3 3])

(myfunc [[1 2] [1 2] [3 4]])

(butlast '(1))

(def x [5 6])

(repeat 3 [1 2 3 4])

((fn [x n](apply concat (map #(repeat n %) x))) [1 2 3] 4)

(map + [1 2 3 4 5] (iterate inc 3))


(map + [1 2 3])

(#(loop [n1 %1 n2 %2 acc '()]
   (if (= n1 n2)
     acc
     (recur (inc n1) n2 (concat acc (list n1))))) 1 5)


(println ((fn [arr1 arr2]
  (loop [acc '() x arr1 y arr2]
    (println acc)
    (if (or (empty? arr1) (empty? arr2))
      acc
      (recur (conj '(first y) (conj '(first x) acc)) (rest x) (rest y))))) [1 2] [:a :b :c :d]))

(defn flatten-map [m]
  (->> [nil m]
       (tree-seq sequential? second)
       (drop 1)
       (map first)))

(flatten-map {"1" { {"1.1.1" {}
                          "1.1.2" {}}
                   "1.2" {}}
              "2" {"2.1" {}
                   "2.2" {}
                   "2.3" {}}})

(-> {}
    (assoc :id "id")
    (if-let [rqrd (:required coll)]
      (:required rqrd)))

(def coll {:required "true" :not-required "false"})

(defn detector [] (if-let [rqrd (:required coll)]
                    (assoc {} :required rqrd)))

(assoc {:id "id123"} (first (flatten (apply concat (detector)))) (second (flatten (apply concat (detector)))))

(apply concat (detector))





(reduce
 (fn [primes number]
   (if (some zero? (map (partial mod number) primes))
     primes
     (conj primes number)))
 [2]
 (take 10 (iterate inc 3)))


(reduce conj [1 2 3] [4 5 6])


(reduce into {} [{:dog :food} {:cat :chow}])

(reduce
 (fn [a b] (-> (conj a (+' (last a) (last (butlast a))))))
 [2 3]
 (range 7))


(def rt :resource-type)

(def props {:elements
            {:el "halo" :elements {:rt1 "tr" :rt2 "halo4"}}})

  (defn elements-traversal [els parent-name]
    (reduce (fn [acc el] (if (= :element (el :el2))
                           ()
                           ({(first el) (second el)})))

                          ;;   (assoc :id (str (vals el) "."))))
            []
            els))

    (defn func []
      (map (fn [[k v]] {k v})
         {:a 1 :b 2 :r {:c 3}}))


(elements-traversal (:elements props) (name rt))



    (defn get-key
      [prefix key]
      (if (nil? prefix)
        key
        (do
          (println "    point key    " (str prefix "." key))
          (str prefix "." key))))

        (defn flatten-map-kvs
          ([map] (flatten-map-kvs map nil))
          ([map prefix]
           (println map)
           (reduce
            (fn [memo [k v]]
              (do (println "k is " k)
                  (println "v is " v)
                  (if (map? v)
              ;;(if (and (map? v) (not (= :h k)))
              ;;(if not (= (keyword "a") (first (keys v)))
                ;;(if (= "a" "a")
                (if (contains? v :k)
                  (concat (conj memo [(get-key prefix (name k)) (dissoc v :k)]) (flatten-map-kvs (:k v) (get-key prefix (name k))))
                  (conj memo [(get-key prefix (name k)) v]))
                ;; (do (println "point 1  " "memo  " memo "  k  " k "  v  " v)
                ;;     (concat memo (flatten-map-kvs v (get-key prefix (name k))))) ;;(println prefix " ++ " v));;(first (keys v))))
                ;;(conj memo [(get-key prefix (name k)) v]))
                (do (println "point 2  " "memo  " memo "  k  " k "  v  " v)
                    (conj memo [(get-key prefix (name k)) v])))))
            [] map)))

    (defn flatten-map
      [m]
      (into {} (flatten-map-kvs m)))

    (def mapa {:tipo {:hero "bono" :k {:ro "tut" :d {:a {:t "hi" :b "hey" :c "yo" :d "hiya"} :k {:t "zz" :e {:z "hola" :x "bonjour"}}}}}})

    (def arr (ordered-map :a "5" :b "3" :c (ordered-map :d "4" :e (ordered-map :f "54")))) ;;[:a [:t "hi" :b "hey"]])

(flatten-map mapa)

(flatten-map arr)

(map? arr)

(type arr)

(type (second (second (rest (flatten-map arr)))))

(println (first arr))

(println "\n \n \n \n \n \n")

    (flatten-map {:d {:a {:t "hi" :b "hey" :c "yo" :d "hiya"} :k {[:t1 "yes" :t2 "no"]}}})

    (flatten-map {:a 12 :url { :id 12 :url {:newkey "something"}}})

(conj '(:a :b) :c (println "hello"))


(merge (ordered-map :a 1 :v 5 :k 7) (ordered-map :b 4 :t 6))
