# The Computer Language Benchmarks Game
# http://shootout.alioth.debian.org/
# implemented by Greg Buchholz
# streamlined by Kalev Soikonen
# parallelised by Philip Boulain
# modified by Jerry D. Hedden
use warnings; use strict; use threads;

use constant ITER     => 50;
use constant LIMITSQR => 2.0 ** 2;
use constant MAXPIXEL => 524288; # Maximum pixel buffer per thread

my ($w, $h);
$w = $h = shift || 80;
my $threads = 6; # Workers; ideally slightly overshoots number of processors

# Generate pixel data for a single dot
sub dot($$) {
   my ($Zr, $Zi, $Tr, $Ti) = (0.0,0.0,0.0,0.0);
   my $i = ITER;
   my $Cr = 2 * $_[0] / $w - 1.5;
   my $Ci = 2 * $_[1] / $h - 1.0;
   (
      $Zi = 2.0 * $Zr * $Zi + $Ci,
      $Zr = $Tr - $Ti + $Cr,
      $Ti = $Zi * $Zi,
      $Tr = $Zr * $Zr
   ) until ($Tr + $Ti > LIMITSQR || !$i--);
   return ($i == -1);
}

# Generate pixel data for range of lines, inclusive
sub lines($$) {
   map { my $y = $_;
      pack 'B*', pack 'C*', map dot($_, $y), 0..$w-1;
   } $_[0]..$_[1]
}

# Decide upon roughly equal batching of workload, within buffer limits
$threads = $h if $threads > $h;
my $each = int($h / $threads);
$each = int(MAXPIXEL / $w) if ($each * $w) > MAXPIXEL;
$each = 1 if $each < 1;

# Work as long as we have lines to spawn for or threads to collect from
$| = 1;
print "P4\n$w $h\n";
my $y = 0;
my @workers;
while(@workers or ($y < $h)) {
   # Create workers up to requirement
   while((@workers < $threads) and ($y < $h)) {
      my $y2 = $y + $each;
      $y2 = $h if $y2 > $h;
      push(@workers, threads->create('lines', $y, $y2 - 1));
      $y = $y2;
   }
   # Block for result from the leading thread (to keep output in order)
   my $next = shift @workers;
   print $next->join();
}
