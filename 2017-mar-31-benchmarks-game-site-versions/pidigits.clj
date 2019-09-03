;; The Computer Language Benchmarks Game
;; http://benchmarksgame.alioth.debian.org/
;; contributed by Mike Pall
;; java port by Stefan Krause
;; clojure port by Koji Yusa
;; this code uses jgmplib library

(ns pidigits
  (:gen-class)
  (:import (clojure.asm Opcodes Type Label ClassWriter)))

;; Compose matrix with numbers on the right.
(defn compose_r [bq br bs bt GI]
  (let [q (GI 0) r (GI 1) s (GI 2) t (GI 3) u (GI 4) v (GI 5) w (GI 6)] 
    (.mul u r bs)
    (.mul r r bq)
    (.mul v t br)
    (.add r r v)
    (.mul t t bt)
    (.add t t u)
    (.mul s s bt)
    (.mul u q bs)
    (.add s s u)
    (.mul q q bq)))

;; Compose matrix with numbers on the left.
(defn compose_l [bq br bs bt GI]
  (let [q (GI 0) r (GI 1) s (GI 2) t (GI 3) u (GI 4) v (GI 5) w (GI 6)] 
    (.mul r r bt)
    (.mul u q br)
    (.add r r u)
    (.mul u t bs)
    (.mul t t bt)
    (.mul v s br)
    (.add t t v)
    (.mul s s bq)
    (.add s s u)
    (.mul q q bq)))

;; Extract one digit.
(defn extract [j GI]
  (let [q (GI 0) r (GI 1) s (GI 2) t (GI 3) u (GI 4) v (GI 5) w (GI 6)] 
    (.mul u q j)
    (.add u u r)
    (.mul v s j)
    (.add v v t)
    (.div w u v)
    (.intValue w)))

;; Print one digit. Returns 1 for the last digit.
(defn prdigit [y i n]
  (printf "%s" y) 
  (if (or (= (mod i 10) 0) (= i n))
    (do
      (if (not= (mod i 10) 0)
        (printf "%s" (apply str (repeat (- 10 (mod i 10)) " "))))
      (printf "\t:%s\n" i)))
  (= i n)) 

;; Generate successive digits of PI.
(defn digits [^long n GI]
  (let [q (GI 0) r (GI 1) s (GI 2) t (GI 3) u (GI 4) v (GI 5) w (GI 6)] 
    (do
      (.set q 1)
      (.set r 0)
      (.set s 0)
      (.set t 1)
      (.set u 0)
      (.set v 0)
      (.set w 0)
      (loop [k (int 1) i (int 1)]
        (let [y (extract 3 GI)]
          (if (= y (extract 4 GI))
            (if (prdigit y i n)
              k
              (do
                (compose_r 10 (* -10 y) 0 1 GI)
                (recur k (inc i))))
            (do
              (compose_l k (+ (* 4 k) 2) 0 (+ (* 2 k) 1) GI)
              (recur (inc k) i))))))))

;; deassempled GmpInteger
(defn makeGmpInteger []
  (let [cw (ClassWriter. 0)]
    (.visit cw Opcodes/V1_6 (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER) "GmpInteger" nil "java/lang/Object" nil)
    (.visitSource cw "GmpInteger.java" nil)
    (doto (.visitField cw Opcodes/ACC_PRIVATE "pointer" "J" nil nil)
      (.visitEnd))
    (let [l0 (Label.) l1 (Label.) l2 (Label.) l3 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "<init>" "()V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 12 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitMethodInsn Opcodes/INVOKESPECIAL "java/lang/Object" "<init>" "()V")
        (.visitLabel l1)
        (.visitLineNumber 13 l1)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitMethodInsn Opcodes/INVOKESPECIAL "GmpInteger" "mpz_init" "()V")
        (.visitLabel l2)
        (.visitLineNumber 14 l2)
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l3)
        (.visitLocalVariable "this" "LGmpInteger;" nil, l0 l3 0)
        (.visitMaxs 1 1)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.) l2 (Label.) l3 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "<init>" "(I)V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 17 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitMethodInsn Opcodes/INVOKESPECIAL "GmpInteger" "<init>" "()V")
        (.visitLabel l1)
        (.visitLineNumber 18 l1)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ILOAD 1)
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_set_si" "(JI)V")
        (.visitLabel l2)
        (.visitLineNumber 19 l2)
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l3)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l3 0)
        (.visitLocalVariable "value"  "I" nil l0 l3 1)
        (.visitMaxs 3 2)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "set" "(I)V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 21 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ILOAD 1)
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_set_si" "(JI)V")
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitLocalVariable "value" "I" nil l0 l1 1)
        (.visitMaxs 3 2)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "mul" "(LGmpInteger;I)V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 23 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ALOAD 1)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ILOAD 2)
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_mul_si" "(JJI)V")
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitLocalVariable "src" "LGmpInteger;" nil l0 l1 1)
        (.visitLocalVariable "val" "I" nil l0 l1 2)
        (.visitMaxs 5 3)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "add" "(LGmpInteger;LGmpInteger;)V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 25 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ALOAD 1)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ALOAD 2)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_add" "(JJJ)V")
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitLocalVariable "op1" "LGmpInteger;" nil l0 l1 1)
        (.visitLocalVariable "op2" "LGmpInteger;" nil l0 l1 2)
        (.visitMaxs 6 3)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "div" "(LGmpInteger;LGmpInteger;)V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 27 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ALOAD 1)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitVarInsn Opcodes/ALOAD 2)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_tdiv_q" "(JJJ)V")
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitLocalVariable "op1" "LGmpInteger;" nil l0 l1 1)
        (.visitLocalVariable "op2" "LGmpInteger;" nil l0 l1 2)
        (.visitMaxs 6 3)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "intValue" "()I" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 29 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_get_si" "(J)I")
        (.visitInsn Opcodes/IRETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitMaxs 2 1)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PUBLIC "doubleValue" "()D" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 31 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitMethodInsn Opcodes/INVOKESTATIC "GmpInteger" "mpz_get_d" "(J)D")
        (.visitInsn Opcodes/DRETURN)
        (.visitLabel l1)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l1 0)
        (.visitMaxs 2 1)
        (.visitEnd)))
    (let [l0 (Label.) l1 (Label.) l2 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_PROTECTED "finalize" "()V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 41 l0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitVarInsn Opcodes/ALOAD 0)
        (.visitFieldInsn Opcodes/GETFIELD "GmpInteger" "pointer" "J")
        (.visitMethodInsn Opcodes/INVOKESPECIAL "GmpInteger" "mpz_clear" "(J)V")
        (.visitLabel l1)
        (.visitLineNumber 42 l1)
        (.visitInsn Opcodes/RETURN)
        (.visitLabel l2)
        (.visitLocalVariable "this" "LGmpInteger;" nil l0 l2 0)
        (.visitMaxs 3 1)
        (.visitEnd)))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_NATIVE) "mpz_init" "()V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_NATIVE) "mpz_clear" "(J)V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_mul_si" "(JJI)V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_add" "(JJJ)V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_tdiv_q" "(JJJ)V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_set_si" "(JI)V" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_get_si" "(J)I" nil nil)
      (.visitEnd))
    (doto (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_NATIVE) "mpz_get_d" "(J)D" nil nil)
      (.visitEnd))
    (let [l0 (Label.) l1 (Label.)]
      (doto (.visitMethod cw Opcodes/ACC_STATIC "<clinit>" "()V" nil nil)
        (.visitCode)
        (.visitLabel l0)
        (.visitLineNumber 36 l0)
        (.visitLdcInsn "jgmplib")
        (.visitMethodInsn Opcodes/INVOKESTATIC "java/lang/System" "loadLibrary" "(Ljava/lang/String;)V")
        (.visitLabel l1)
        (.visitLineNumber 37 l1)
        (.visitInsn Opcodes/RETURN)
        (.visitMaxs 1 0)
        (.visitEnd)))
    (.visitEnd cw)
    (let [ba (.toByteArray cw)
          dcl (clojure.lang.DynamicClassLoader.)]
      (.defineClass dcl "GmpInteger" ba nil))))

(defn -main [& args]

  ;; load class
  (def GmpInt (makeGmpInteger)) 

  ;; use reflection familiar with compile
  (def q (.. GmpInt newInstance))
  (def r (.. GmpInt newInstance))
  (def s (.. GmpInt newInstance))
  (def t (.. GmpInt newInstance))
  (def u (.. GmpInt newInstance))
  (def v (.. GmpInt newInstance))
  (def w (.. GmpInt newInstance))

  (def GI [q r s t u v w])

  (let [n (try (Integer/parseInt (first args))
               (catch NumberFormatException e 27))]
    (digits n GI))
  (flush))
