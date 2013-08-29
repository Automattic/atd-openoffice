### 0.4 - 11 Jan 12

* AtD/OpenOffice now respects the rate limit on the free hosted server
  at service.afterthedeadline.com by spacing proofreading requests out
  to no more than 1 per second.  Proofreading still works as the current 
  line is sent repeatedly to the server and errors will be flagged on
  the next request.  Also removes the error dialog from the rate limiter 
  when using the free hosted server (response code 302).

### 0.3 - 9 Aug 10

* After the Deadline now checks documents set to English (UK). My apologies for
  this. I had the locale wrong (en_UK instead of en_GB). Thanks to Chalkie for
  providing the necessary clue. I've added every English related locale I could 
  find to the code. Hopefully this issue is put to rest now.
* AtD now trims the trailing slash out of the AtD service URL option, if it's
  present. Trailing slashes in the service URL would cause the request to fail
* AtD now resets the AtD service URL to the default if the value is invalid. 
* Added instructions on how to reset the AtD service URL to the AtD options
  dialog (simply clear the URL and hit Save, this will reset it).

### 0.2 - 15 Jul 10

* AtD/OpenOffice.org will now show a dialog with an error when it can't connect
  to the AtD service.
* Added more en locales to the getLocales() call in Main. This may allow AtD to
  check documents with their language set to en_UK 

### 0.1 - 10 Jun 10

* Initial (beta) release.
