atd-openoffice
==============

This is an English grammar checker for OpenOffice.org. Checking is performed by the After the Deadline proofreading software service.

Automattic no longer supports this extension.  We're putting it on Github so that you can feel free to fork it, hack it, and release your own version.

### Requirements:

* OpenOffice.org 3.2.0 or later
* Sun Java 1.5 or later.
* An internet connection

### Installation:

1. Go to Tools -> Extension Manager 
2. Cick Add... and select the atd-openoffice.oxt file
3. Click OK.
4. Restart OpenOffice.org (necessary to intialize the updated grammar checker)

### Use:

Visit Tools -> Extension Manager -> After the Deadline -> Options. Here you may modify 
the style checker settings and change the server this extension posts to. 

This extension checks your grammar (and style when enabled). It does not check your 
spelling. This functionality is left to the built-in spell checker.

There are two ways to check your grammar, as you type or via the grammar checker dialog:

Type your text and hit enter. Errors in the last paragraph will show a blue squiggly line.
Right click on an error and select an option to change it.

Select Tools -> Spelling and Grammar to bring up the grammar and spell checker dialog. Click
Ignore Rule to ignore all errors associated with the rule. Ignored rules last until you
restart OpenOffice.org. You may also select Tools -> Reset Ignored Rules to clear the ignored 
rules.

Settings are stored in ~/.AtD-OpenOffice.org. Delete this file if you want to clear your
AtD settings.

### Commercial use and running your own server

This extension requires a running instance of an [After the Deadline Server](https://github.com/automattic/atd-server) in order to work.  Automattic operates an that you can use for personal use as long as you don't send too much traffic.  The extension is configured to use this instance by default.

For high volume and commercial uses of AtD, you must run your own server.  The code is available on Github: [After the Deadline Server](https://github.com/automattic/atd-server).  See the [After the Deadline Developer's page](http://open.afterthedeadline.com/) for more information, and check out the [AtD Developers Google Group](http://groups.google.com/group/atd-developers) for discussion and community support.  

When you run your own server, replace `service.afterthedeadline.com` with your server's hostname.

### Privacy:

Your text is sent to a remote server for processing. The system uses too much memory for you to
run it locally. The default server is run by Automattic (http://www.automattic.com) and 
AtD/OpenOffice.org is configured to communicate to this default server over SSL. Automattic does
not keep your text. Our server processes your text and report errors to you. That's it.

If you're not comfortable with this, you may download the open source proofreading server and run
it yourself. http://open.afterthedeadline.com. Configure this extension to use your server under: 
Tools -> Extension Manager -> After the Deadline -> Options

### Contribute

We (Automattic) are no longer supporting this extension.  This code has always been open source.  We're putting it on Github so that you can feel free to fork it, hack it, and release your own version.

Join the [atd-developers](http://groups.google.com/group/atd-developers) list for support.

### License

See `licenses/*` for licenses on individual pieces. The bulk of this extension is derived from the
LanguageTool Open Office extension (http://www.languagetool.org). This extension is released under
the GNU LGPL.

### Author

Raphael Mudge
