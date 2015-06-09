(ns server.router.udp
  (:import (java.net InetAddress InetSocketAddress DatagramSocket DatagramPacket SocketException))
  (:import (java.util Arrays))
  (:use clojure.string)
  (:import (java.io IOException)))

(defn concatarr [first second]
  (let [result (ref nil)]
    (dosync (ref-set result (. Arrays copyOf first (+ (alength first) (alength second)))))
    (System/arraycopy second 0 @result (alength first) (alength second))
    @result
    ))

(def ipArray (ref nil))
(def bufsize 1024)
(def serverSocket (ref nil))
(def numNodes (ref nil))
(def ortaddr (ref nil))
(def ortport (ref nil))



(defn init-server [secrtaddr secrtport port nodeNum]
  (let [receivePacket (ref nil)
	receiveData (ref nil)
	]
  (try
    (dosync (ref-set serverSocket (DatagramSocket. port)))
    (catch SocketException e (println (.getMessage e))))
  
  (dosync (ref-set ipArray (make-array Object (+ nodeNum 1) (+ nodeNum 1))))
  (dosync (ref-set receiveData (byte-array bufsize)))
  (dosync (ref-set numNodes nodeNum))
  (dosync (ref-set ortaddr secrtaddr))
  (dosync (ref-set ortport secrtport))
  (let [serv-addr (ref nil)
        serv-port (ref nil)]
    (loop [currnodenum 0]
      (when (< currnodenum @numNodes)
	(println "Current number of nodes is " currnodenum)
	(dosync (ref-set receivePacket
			 (DatagramPacket. @receiveData (alength @receiveData))))
	(. @serverSocket receive @receivePacket)
	
	(dosync (ref-set serv-addr (.getAddress @receivePacket)))
	(dosync (ref-set serv-port (.getPort @receivePacket)))
	;;(println (String. @receiveData))
	(println "currnodenum is " currnodenum)
	
	(aset @ipArray currnodenum 0 @serv-addr)
	(prn "adding " @serv-addr " to routing table")
	(aset @ipArray currnodenum 1 @serv-port)
	
	(recur (inc currnodenum)))))
  (println "finished initializing the server and setting up the nodes.")))
  

(defn sendMessage [message ipAddress port]
  (try
    (prn port)
    (let [sendPacket (DatagramPacket. message (alength message) (InetAddress/getByName ipAddress) port)]
      (println "message output in sendMessage" (String. message))
      (. @serverSocket send sendPacket))
    ;;(println "data sent to " (.toString (.getAddress sendPacket)) " at port " port))
    (catch Exception e (println (.getMessage e)))))
  
(defn Router [message ipAddress port]
  (sendMessage message ipAddress port))


(defn getAndBuildData []
  (let [receivePacket (ref nil)
	receiveData (ref nil)
	]

    (while true
      
      (dosync (ref-set receiveData (byte-array bufsize)))      
      (dosync (ref-set receivePacket
		       (DatagramPacket. @receiveData (alength @receiveData))))

      (. @serverSocket receive @receivePacket)
      (prn (String. @receiveData))
      
      
	  (let [ipinfo (ref nil)
		ippack (ref nil)
		ipAddress (ref nil)
		ipport (ref nil)
		dstaddr (ref nil)
		sendData (ref nil)
		th_receiveData (ref @receiveData)
		th_receivePacket (ref @receivePacket)
		]
       (dosync (ref-set dstaddr (String. @th_receiveData)))
       (dosync (ref-set ipinfo (.split  (.trim @dstaddr) "/")))
       (dosync (ref-set ipAddress (aget @ipinfo 0)))
       
       (if (not (and (. @ortaddr equals (aget (.split (.toString (.getAddress @th_receivePacket)) "/") 1))
		     (== @ortport (.getPort @th_receivePacket))))
	 (do
;;	   (prn "other router has ip,port " @ortaddr "," @ortport)
;;	   (prn "packet has ip,port " (.toString (.getAddress @receivePacket)) " " (.getPort @receivePacket))
	   ;; format = [dstip/dstport/srcip/srcport], appends srcip/srcport
	   (prn @dstaddr)
	   (dosync (ref-set ippack (.concat (.trim @dstaddr)
				    (.concat "/"
					     (.concat (aget (.split
							     (.toString (.getAddress @th_receivePacket)) "/") 1)
						      (.concat "/" (Integer/toString (.getPort @th_receivePacket))))))))
	   (prn @ippack)
	   (dosync (ref-set sendData (byte-array (map byte (.getBytes @ippack)))))
;;	   (prn "length of sendData is " (alength @sendData))
	   (Router @sendData  @ortaddr @ortport)
	   
	   )
	 ;; else the packet is from router
	 (do
	   (prn "packet is from router")
	   ;; keep original packet format
	   (prn (String. @th_receiveData))
	   (dosync (ref-set sendData (byte-array (map byte @th_receiveData))))
	   (let [i (atom 0)]
	     (while (< @i @numNodes)
	       (do
		 (try
		   (prn "ip in table,pkt " (aget @ipArray @i 0) " " (. InetAddress getByName @ipAddress))
		   (prn "port in table,pkt " (.intValue (Integer. (.trim (String. (aget @ipinfo 1))))) (aget @ipArray @i 1))
		   (if (and (. (aget @ipArray @i 0) equals (. InetAddress getByName @ipAddress))
			    (== (.intValue (Integer. (trim (String. (aget @ipinfo 1))))) (aget @ipArray @i 1)))
		     (do
		       (prn "routing to " @ipAddress " " (aget @ipArray @i 1))
		       (Router @sendData @ipAddress (aget @ipArray @i 1))
		       (compare-and-set! i @i @numNodes))
		     )
		   (catch NumberFormatException e (.printStackTrace e)))
		 (swap! i inc))))))))))
	     

(defn runserv [ortaddr ortport port nodeNum]
  (init-server ortaddr ortport port nodeNum)
  (getAndBuildData))




	
