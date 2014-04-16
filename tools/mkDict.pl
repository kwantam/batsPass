#!/usr/bin/perl -wn

chomp;
unless ( /[^0-9a-zA-Z]/ || length($_) > 7 ) {
    $words{lc($_)} = 1;
}

END {
    foreach my $word (keys %words) {
        print $word, "\n";
    }
}
