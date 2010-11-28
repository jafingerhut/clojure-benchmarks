(require '[clojure.contrib.jmx :as jmx])
(require '[clojure.contrib.string :as str])

(comment
(loop [i 0
       a *command-line-args*]
  (if (seq a)
    (do
      (printf "%d: '%s'\n" i (first a))
      (recur (inc i) (rest a)))
    ))
)

(def *show* (if (>= (count *command-line-args*) 1)
              (nth *command-line-args* 0)
              "other"))

(when (= *show* "allproperties")
  (let [props (. System getProperties)
        keys (. props keys)
        s (enumeration-seq keys)]
    ;;(printf "(class props)=%s\n" (class props))
    ;;(printf "(class *out*)=%s\n" (class *out*))
    ;;(printf "(class keys)=%s\n" (class keys))
    ;;(printf "(class s)=%s\n" (class s))
    ;;(printf "(count s)=%s\n" (count s))
    (doseq [prop-name (sort s)]
      (printf "%s=%s\n" prop-name (. System getProperty prop-name)))
    (printf "\n")
    (printf "number of properties=%d\n" (count s))
    (flush))
  (. System exit 0))
  
(when (= *show* "sysinfo")
  ;; Show all JVM properties, but especially at the end the two that I
  ;; believe have the most to do in determining whether the JVM is
  ;; 32-bit or 64-bit.  In particular sun.arch.data.model seems to be
  ;; the one introduced by Sun for this purpose.

  (printf "os.name = %s\n" (. System getProperty "os.name"))
  (printf "os.version = %s\n" (. System getProperty "os.version"))
  (printf "java.runtime.version = %s\n"
          (. System getProperty "java.runtime.version"))
  (printf "sun.arch.data.model = %s\n"
          (. System getProperty "sun.arch.data.model"))
  (printf "os.arch = %s\n" (. System getProperty "os.arch"))
  (let [rt (. Runtime getRuntime)]
    (printf "availableProcessors = %d\n" (. rt availableProcessors)))
  (flush)
  (. System exit 0))

;; Source for documentation on Runtime object:
;; http://download.oracle.com/javase/1.4.2/docs/api/java/lang/Runtime.html
(let [rt (. Runtime getRuntime)
      total (. rt totalMemory)
      max (. rt maxMemory)
      free (. rt freeMemory)
      mb (* 1024.0 1024.0)]
  (printf "total=%.1f MB" (/ total mb))
  (printf " max=%.1f MB" (/ max mb))
  (printf " free=%.1f MB" (/ free mb)))

(let [gc-info (map #(subs (str %) 37)
                   (jmx/mbean-names "java.lang:type=GarbageCollector,*"))]
  (printf " GC method: %s" (str/join ", " gc-info)))

(printf "\n")


(comment
  (printf (str
"        totalMemory()\n"
"        Returns the total amount of memory in the Java virtual machine. The\n"
"        value returned by this method may vary over time, depending on the host\n"
"        environment.\n"
"\n"
"        Note that the amount of memory required to hold an object of any given\n"
"        type may be implementation-dependent.\n"
"\n"
"        Returns: the total amount of memory currently available for current and\n"
"        future objects, measured in bytes.\n"))
  (printf (str
"        maxMemory()\n"
"        Returns the maximum amount of memory that the Java virtual machine will\n"
"        attempt to use. If there is no inherent limit then the value\n"
"        Long.MAX_VALUE (%d) will be returned.\n"
"\n"
"        Returns: the maximum amount of memory that the virtual machine will attempt\n"
"        to use, measured in bytes\n")
          (. Long MAX_VALUE))

  (printf (str
"        freeMemory()\n"
"        The amount of free memory in the Java Virtual Machine.  Calling the\n"
"        gc method may result in increasing the value returned by freeMemory.\n"
"        Returns: an approximation to the total amount of memory currently\n"
"        available for future allocated objects\n"))
)


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
