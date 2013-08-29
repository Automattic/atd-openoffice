import com.afterthedeadline.openoffice.ProofreadingError;
import com.afterthedeadline.client.Configuration;
import java.util.*;

debug(debug() | 34);
global('$key $config @style_options $shown');
$key = 'atd-oo-' . ticks();
@style_options = @("Bias Language", "Cliches", "Complex Expression", "Diacritical Marks", "Double Negatives", "Hidden Verbs", "Jargon Language", "Passive voice","Phrases to Avoid", "Redundant Expression");

$config = [Configuration getConfiguration];

sub getLocationStart {
	local('$paragraph $pre $string $i $error $start $end $from $pos');
	($paragraph, $pre, $string, $from) = @_;

	$i = indexOf($paragraph, iff($pre eq "", $string, "$pre $string"), $from);

	# remove the pre-context hint if no match was found
	if ($i is $null) {
		if ($pre ne "") {
			return getLocationStart($paragraph, "", $string, $from, \@errors);
		}
		else {
			#warn("Not found :( ' $+ $pre $string $+ ' in ' $+ $paragraph $+ ' from: $from");
			return $i;
		}
	}
	else {
		$pos = $i + iff($pre eq "", 0, strlen($pre) + 1);

		# verifying this location doesn't conflict with any other highlights
		foreach $error (@errors) {
			$start = [$error locationStart];

			if ($pos >= $start) {
				$end = $start + strlen([$error errorString]);
				if ($pos < $end) {
					#warn("Looking at: " . @_);
					return getLocationStart($paragraph, $pre, $string, $end + 1, \@errors);
				}
			}
		}

		return $pos;
	}
}

sub error {
	this('$last $suggestions');

	if ($0 eq "error") {
		$last = [new ProofreadingError];
		
		local('$precontext $string $description $type $locStart');
		($precontext, $string, $description, $type) = values(childrenToHash($1), @('precontext', 'string', 'description', 'type'));
	
		# drop style errors that are not configured to be shown.
		if ($description in @style_options && ![$config isEnabled: $description]) {
			return;
		}

		# drop errors that we can't find
		$locStart = getLocationStart($paragraph, $precontext, $string, 0, \@errors);

		if ($locStart is $null) {
			return;
		}

		setField($last, precontext => $precontext, 
				errorString => $string,
				description => iff($description eq 'Cliches', "Clich\u00e9s", $description),
				type => $type,
				suggestions => $suggestions,
				locationStart => $locStart);

		push(@errors, $last);
		$suggestions = $null;
	}
	else if ($0 eq "suggestions") {
		$suggestions = [SleepUtils getListFromArray: map({ return $1['text']; }, $1['children'])];
	}
}

sub proofread {
	# save some bandwidth: ignore empty strings, numbers, and strings with only one word
	# finding errors without context is the job of a spell checker.
	if ($1 eq "" || $1 ismatch '\s*\d+(?:\.\d+){0,1}[%]{0,1}\s*' || indexOf($1, ' ') is $null) {
		return;
	} 

	# If we're using the free hosted service at service.afterthedeadline.com, 
	# check time passed since last proofread request, so we don't run into the rate limiter, 
	# allow a check every >= 1 second.
	if ( indexOf( [$config getServiceHost], "service.afterthedeadline.com" ) ) {
		if ( $ticks is $null ) {
			$ticks = ticks();
		} else {
			if ( ticks() - $ticks < 1000.0 ) {
				# skip this check, the first part of the current line will be sent again next time
				return;
			} else {
				$ticks = ticks();
			}
		}
	}

	local('$handle @errors $exception');
	try {
		$handle = post([$config getServiceHost] . "/checkGrammar?key= $+ $key", %(data => strrep($1, chr(8217), "'")));
		$data = buildDataStructure(readb($handle, -1));
		closef($handle);	
	
		walkData($data, lambda(&error, \@errors, $paragraph => $1));

		return @errors;
	}
	catch $exception {
		if ( indexOf( [$config getServiceHost], "service.afterthedeadline.com" ) && indexOf( [$exception toString], "503 Service Temporarily Unavailable" ) ) {
			# These are from the rate limiter for the free hosted service at service.afterthedeadline.com
			# We'll be able to get a result again the next time around
		} else {
			if ( $shown is $null ) {
				$shown = 1;
				[com.afterthedeadline.openoffice.Main showMessage: "Could not connect to AtD service.\n\nHost: " . [$config getServiceHost] . "\nReason: $exception $+ \n\nThe grammar checker will not be available."];
			}
		}
	}
}
