#!/usr/bin/perl -wn

chomp;
unless ( /[^0-9a-zA-Z]/ || length($_) > 6 ) {
    $words{lc($_)} = 1;
}

END {
    print '<?xml version="1.0" encoding="utf-8"?><resources><string-array name="dictwords_array">';
    foreach my $word (keys %words) {
        print '<item>' . $word . '</item>';
    }
    print '</string-array></resources>';
}


