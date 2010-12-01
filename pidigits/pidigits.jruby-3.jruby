# The Computer Language Benchmarks Game
# http://shootout.alioth.debian.org/

# transliterated from Mario Pernici's Python program
# contributed by Rick Branson

N = (ARGV[0] || 100).to_i

i = k = ns = 0
k1 = 1
n,a,d,t,u = [1,0,1,0,0]

loop do
  k += 1
  t = n<<1
  n *= k
  a += t
  k1 += 2
  a *= k1
  d *= k1
  if a >= n
    t,u = (n*3 +a).divmod(d)
    u += n
    if d > u
      ns = ns*10 + t
      i += 1
      if i % 10 == 0
        puts "#{ns.to_s.rjust(10, '0')}\t:#{i.to_s}"
        ns = 0
      end
      break if i >= N
   
      a -= d*t
      a *= 10
      n *= 10
    end
  end
end
