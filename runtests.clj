(ns runtests
   (:use udpclient)
    (:use clojure.string)
    (:use clojure.contrib.io))

(defmacro redir [filename & body]
  `(binding [*out* (writer ~filename)] ~@body))


(defn runtests [rtaddr rtport setport testfile]
     (let [dstaddr  (ref nil)
           dstport  (ref nil)
           dstinfo  (ref nil)
	   dstarr   (ref nil)
           filearr  (ref nil)
           runtimes (ref nil)
           outfile  (ref nil)
           outpath  (ref nil)
	   msgarr   (ref nil)
           msgfile  (ref nil)]
           
	(dosync (ref-set filearr (.split (slurp testfile) "\n")))
	(dosync (ref-set runtimes (Integer/parseInt (trim (aget @filearr 0)))))
	(dosync (ref-set msgarr (.split (aget @filearr 1) ",")))
	(dosync (ref-set dstarr (.split (aget @filearr 2) ",")))
        (dosync (ref-set outpath (trim (aget @filearr 3))))
        ;;(println (alength @dstarr))
	(udpclient/initserv rtaddr rtport setport)
;;(comment        
	(let [i (atom 0)]
	  (while (< @i (alength @msgarr))
            (do
            (dosync (ref-set msgfile (trim (aget @msgarr @i))))
            (let [j (atom 0)]
                 (while (< @j (alength @dstarr))
		   (do
		    (dosync (ref-set dstinfo (.split (aget @dstarr @j) "/")))
 		    (dosync (ref-set dstaddr (aget @dstinfo 0)))
 		    (dosync (ref-set dstport (Integer/parseInt (trim (aget @dstinfo 1)))))
	            (println @dstport @dstaddr (type @dstport) (type @dstaddr))
			(prn @outpath)
                    (dosync (ref-set outfile (.concat @outpath
                                (.concat (.concat (.concat "msg" (.toString @i)) "-ip") (.toString @j)))))
			(prn @outfile)
			(prn @msgfile)
		    (redir @outfile
                      (udpclient @dstaddr @dstport rtaddr rtport setport @msgfile @runtimes)))
                    (println "finished running tests on ip" @j)
		    (swap! j inc))))
                  (swap! i inc)))))
