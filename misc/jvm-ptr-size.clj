;; Show all JVM properties, but especially at the end the two that I
;; believe have the most to do in determining whether the JVM is
;; 32-bit or 64-bit.  In particular sun.arch.data.model seems to be
;; the one introduced by Sun for this purpose.

(let [props (. System getProperties)
      keys (. props keys)
      s (enumeration-seq keys)
      ]
  ;(printf "(class props)=%s\n" (class props))
  ;(printf "(class *out*)=%s\n" (class *out*))
  ;(printf "(class keys)=%s\n" (class keys))
  ;(printf "(class s)=%s\n" (class s))
  ;(printf "(count s)=%s\n" (count s))
  (doseq [prop-name (sort s)]
    (printf "%s=%s\n" prop-name (. System getProperty prop-name)))
  (printf "\n")
  (printf "number of properties=%d\n" (count s)))

(printf "\n")
(printf "sun.arch.data.model='%s'\n"
        (. System getProperty "sun.arch.data.model"))
(printf "os.arch='%s'\n"
        (. System getProperty "os.arch"))
(flush)

;; Source:
;; http://forums.java.net/node/678245?#comment-721195

;; public static final boolean is64bit = (System.getProperty("sun.arch.data.model").indexOf("64") != -1)

;; example (core2duo 64bit windows):
;; 32bit VM: os.arch=x86
;; 64bit VM: os.arch=amd64

;; Source:
;; http://stackoverflow.com/questions/2062020/how-can-i-tell-if-im-running-in-64-bit-jvm-or-32-bit-jvm

;; sun.arch.data.model=32 // 32 bit JVM
;; sun.arch.data.model=64 // 64 bit JVM
;; System.getProperty("sun.arch.data.model")

;; http://www.oracle.com/technetwork/java/hotspotfaq-138619.html#64bit_detection

;; (Note: The full web page above has many other useful tidbits of
;; information besides the answer to this one question.)

;; When writing Java code, how do I distinguish between 32 and
;; 64-bit operation?

;; There's no public API that allows you to distinguish between 32
;; and 64-bit operation.  Think of 64-bit as just another platform
;; in the write once, run anywhere tradition.  However, if you'd
;; like to write code which is platform specific (shame on you),
;; the system property sun.arch.data.model has the value "32",
;; "64", or "unknown".


;; Mac OS X 10.5.8 with all updates from Apple as of Nov 19 2010:

;; % java -d32 -client -version
;; Cannot run Java in 32 bit mode. Continuing in 64 bit mode.
;; java version "1.6.0_22"
;; Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-9M3263)
;; Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)
;; % java -d32 -server -version
;; Cannot run Java in 32 bit mode. Continuing in 64 bit mode.
;; java version "1.6.0_22"
;; Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-9M3263)
;; Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)
;; % java -d64 -client -version
;; java version "1.6.0_22"
;; Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-9M3263)
;; Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)
;; % java -d64 -server -version
;; java version "1.6.0_22"
;; Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-9M3263)
;; Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)
