(ns server.udp
  (:import (java.net InetAddress InetSocketAddress DatagramPacket DatagramSocket SocketException Socket))
  (:import (java.util Arrays Date))
  (:import (java.io InputStream OutputStream))
  (:use clojure.string))

(defn concatarr [first second]
  (let [result (ref nil)]
    (dosync (ref-set result (. Arrays copyOf first (+ (alength first) (alength second)))))
    (System/arraycopy second 0 @result (alength first) (alength second))
    @result
    ))

(def bufsize 1024)

(def sendData
     (ref nil))

(def receiveData
     (ref nil))

(def serverSocket (ref nil))

(defn udpserver [rtaddr rtport setport]
  (let [port (ref nil)
	sendPacket (ref nil)
	receivePacket (ref nil)
	
	
	msg (ref nil)]
    (try
      (dosync (ref-set serverSocket (DatagramSocket. setport)))
      (catch SocketException e (println (.getMessage e))))
    (dosync (ref-set receiveData (byte-array bufsize)))
    ;; Join router network
    (dosync (ref-set msg (String. "setting up node")))
     (dosync (ref-set sendData (byte-array (map byte (.getBytes @msg)))))
     (dosync (ref-set sendPacket (DatagramPacket. @sendData (alength @sendData)
                                              (InetSocketAddress. rtaddr rtport))))

     (. @serverSocket send @sendPacket)

     ;; listen for connection requests (that is, udp packets from router)
     ;; get client ip addr,port from udp packet
     ;; connect to client
     ;; parse data while connection is open
     
     (while true

       (dosync (ref-set receivePacket (DatagramPacket. @receiveData (alength @receiveData))))
       (prn "waiting for packet in loop")
       (. @serverSocket receive @receivePacket)
       (prn "received packet from " (.getPort @receivePacket) " and contains " @receiveData)
      ;; (prn (.toString (Date. )))
       ;; simple threading 
       (future
	 (let [tcprcvData (ref nil)
	       tcpSocket (ref nil)
	       isstream (ref nil)
	       ostream (ref nil)
	       sendBuf (ref nil)
	       modsentence (ref nil)
	       ipinfo (ref nil)
	       unpacked (ref nil)
	       dstaddr (ref nil)
	       ]
	   (dosync (ref-set dstaddr (String. @receiveData)))
	   (dosync (ref-set ipinfo (.split  (.trim @dstaddr) "/")))
	   (dosync (ref-set tcprcvData (byte-array bufsize)))
	;;   (prn (alength @ipinfo) " and contains " @dstaddr)
	   ;; connect to the client, setup input/output streams
	;;   (prn "alength of ipinfo is " (alength @ipinfo))
	   (try 
	     (dosync (ref-set tcpSocket (Socket. (InetAddress/getByName (aget @ipinfo 2)) (Integer/parseInt (aget @ipinfo 3)))))
	     (dosync (ref-set isstream (.getInputStream @tcpSocket)))
	     (dosync (ref-set ostream (.getOutputStream @tcpSocket)))
	     
	     ;; if the server is still connected to client, get and process data.
	     (while (.isConnected @tcpSocket)
	       
	       ;; get data from client
	       (.read @isstream @tcprcvData)
	       
	       (dosync (ref-set modsentence (upper-case (String. @tcprcvData 0 bufsize))))
	       
	       ;;(println "receiveData contains: " (String. @receiveData))
	       (dosync (ref-set sendBuf (byte-array (map byte (.getBytes @modsentence)))))
	       (.write @ostream @sendBuf)
	       (.flush @ostream))
	     
	     (.close @isstream)
	     (.close @ostream)
	     (.close @tcpSocket)
	     (catch SocketException e (println " in catch with error: " (.getMessage e))))))
       (prn "caught exception keep running")
       (prn "end of loop, testing receiveData: " @receiveData)
       )))
