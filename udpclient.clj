(ns udpclient
  (:import (java.net DatagramSocket InetSocketAddress DatagramPacket Socket ServerSocket))
  (:import (java.lang Integer String))
  (:import (java.util Arrays))
  (:import (java.io InputStream OutputStream))
  (:use [clojure.java.io]))

(defn concatarr [first second]
  (let [result (ref nil)]
    (dosync (ref-set result (. Arrays copyOf first (+ (alength first) (alength second)))))
    (System/arraycopy second 0 @result (alength first) (alength second))
    @result
    ))

(defmacro timecust
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (/ (double (- (. System (nanoTime)) start#)) 1000000.0))
     ret#))


(def bufsize 1024)
;;(def joinedRouter (ref true))


(defn initserv [rtaddr rtport setport]
  (let [sendData (ref nil)
	sendPacket (ref nil)
	sendBuf (ref nil)
	udpSocket (ref nil)
	servSocket (ref nil)
	dataSocket (ref nil)
	rtmsg (ref nil)]
    (dosync (ref-set udpSocket (DatagramSocket. setport)))
    (dosync (ref-set sendData  (byte-array bufsize)))
    ;; Join router network
    (dosync (ref-set rtmsg "setting up node"))
     (dosync (ref-set sendBuf (byte-array (map byte (. @rtmsg getBytes)))))
     (dosync (ref-set sendPacket (DatagramPacket. @sendBuf (alength @sendBuf)
                                              (InetSocketAddress. rtaddr rtport))))
     (. @udpSocket send @sendPacket)
     (.close @udpSocket)))
     
      

(defn udpclient [dstaddr dstport rtaddr rtport setport file runtimes]
  (let [fileloc (ref nil)
	istream (ref nil)
        sendData (ref nil)
	sendPacket (ref nil)
        receiveData (ref nil)
	receivePacket (ref nil)
	sendBuf (ref nil)
	udpSocket (ref nil)
	servSocket (ref nil)
	dataSocket (ref nil)
	isstream (ref nil)
	ostream (ref nil)
        ipinfo (ref nil)
	rtmsg (ref nil)]
    
    (dosync (ref-set udpSocket (DatagramSocket. setport)))
    (dosync (ref-set receiveData (byte-array bufsize)))
    (dosync (ref-set sendData  (byte-array bufsize)))
    
    
    

     ;;  Send connection request to router
     ;;  Setup multi-threaded tcp connection socket
     ;;  listen for set number of messages
     ;;
     


    (dosync (ref-set fileloc file))
    (dosync (ref-set istream (input-stream @fileloc)))
    (. @istream read @sendData)

    (dosync (ref-set ipinfo (.concat dstaddr (.concat "/" (Integer/toString dstport)))))
    (dosync (ref-set sendBuf (byte-array (map byte (.getBytes @ipinfo)))))
    
    
    ;; pack data

    (dosync (ref-set sendPacket
		     (DatagramPacket. @sendBuf (alength @sendBuf)
				      (InetSocketAddress. rtaddr rtport))))
    
   
;;    (println "packet successfully sent")
    
     
     ;; we use UDP here to send the initial connection packet
       (. @udpSocket send @sendPacket)

       ;; close udp connection
       (. @udpSocket close)
       
       ;; setup TCP connection
       ;; ServerSocket
       (dosync (ref-set servSocket (ServerSocket. setport)))
       (dosync (ref-set dataSocket (.accept @servSocket)))
       ;; (prn "connection accepted")
       ;; setup input and output streams here
       
       (dosync (ref-set isstream (.getInputStream @dataSocket)))
       (dosync (ref-set ostream (.getOutputStream @dataSocket)))
       
	
	  ;; send data using output stream
	  (let [i (atom 0)]
	    (while (< @i runtimes)
		(do
		  (timecust
		    (do 
		
		     (.write @ostream @sendData)
    		     (.flush @ostream)
                 (.read @isstream @receiveData)))
		;;(println "FROM SERVER:" (trim (String. @receiveData 0 bufsize)))
		
		(swap! i inc))))

       ;; close streams and sockets
	 (.close @isstream)
	 (.close @ostream)
	 (.close @dataSocket)
	 (.close @servSocket)))
