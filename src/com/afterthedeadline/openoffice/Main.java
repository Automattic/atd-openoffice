/* After the Deadline, a proofreading tool
 * Copyright (C) 2009-2010 Automattic (http://www.automattic.com)
 *
 * AtDdev: Raphael Mudge
 *
 * This code is derived from:
 *
 * LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package com.afterthedeadline.openoffice;

import java.io.File;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.afterthedeadline.client.*;

import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceDisplayName;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.linguistic2.ProofreadingResult;
import com.sun.star.linguistic2.SingleProofreadingError;
import com.sun.star.linguistic2.XLinguServiceEventBroadcaster;
import com.sun.star.linguistic2.XLinguServiceEventListener;
import com.sun.star.linguistic2.XProofreader;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XJobExecutor;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class Main extends WeakBase implements XJobExecutor, XServiceDisplayName, XServiceInfo, XProofreader, XLinguServiceEventBroadcaster, ChangeListener {
	private String docID;

	private Set<String> disabledRules = new HashSet();
	private List<XLinguServiceEventListener> xEventListeners;

	private int position;

	/**
	 * Service name required by the OOo API && our own name.
	 */
	private static final String[] SERVICE_NAMES = { "com.sun.star.linguistic2.Proofreader", "com.afterthedeadline.openoffice.Main" };

	private XComponentContext xContext;
	private Client atdClient = null;

	public Main(final XComponentContext xCompContext) {
		try {
			changeContext(xCompContext);
			disabledRules = new HashSet<String>();
			xEventListeners = new ArrayList<XLinguServiceEventListener>();
			atdClient = new Client();
			Configuration.getConfiguration().addChangeListener(this);
		} 
		catch (final Throwable t) {
			showError(t);
		}
	}

	public final void changeContext(final XComponentContext xCompContext) {
		xContext = xCompContext;
	}

	private XComponent getxComponent() {
		try {
			final XMultiComponentFactory xMCF = xContext.getServiceManager();
			final Object desktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
			final XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktop);
			final XComponent xComponent = xDesktop.getCurrentComponent();
			return xComponent;
		} 
		catch (final Throwable t) {
			showError(t);
			return null;
		}
	}

	/**
	 * Runs the grammar checker on paragraph text.
	 * 
	 * @param String
	 *          docID - document ID
	 * @param String
	 *          paraText - paragraph text
	 * @param locale
	 *          Locale - the text Locale
	 * @param startOfSentencePos start of sentence position
	 * @param nSuggestedBehindEndOfSentencePosition end of sentence position
	 * @param PropertyValue
	 *          [] props - properties
	 * @return ProofreadingResult containing the results of the check. *
	 * @throws IllegalArgumentException
	 *           (not really, LT simply returns the ProofreadingResult with the
	 *           values supplied)
	 */
	public final ProofreadingResult doProofreading(final String docID, final String paraText, final Locale locale, final int startOfSentencePos, final int nSuggestedBehindEndOfSentencePosition, final PropertyValue[] props) {
		final ProofreadingResult paRes = new ProofreadingResult();
		try {
			paRes.nStartOfSentencePosition = startOfSentencePos;
			paRes.xProofreader = this;
			paRes.aLocale = locale;
			paRes.aDocumentIdentifier = docID;
			paRes.aText = paraText;
			paRes.aProperties = props;
			return doGrammarCheckingInternal(paraText, locale, paRes);
		} 
		catch (final Throwable t) {
			showError(t);
			return paRes;
		}
	}

	synchronized private final ProofreadingResult doGrammarCheckingInternal(final String paraText, final Locale locale, final ProofreadingResult paRes) {
		// process enabled rules, disabled rules, and categories here
        
		try {
			String sentence = paraText; 
			paRes.nStartOfSentencePosition = position;
			paRes.nStartOfNextSentencePosition = position + sentence.length();
			paRes.nBehindEndOfSentencePosition = paRes.nStartOfNextSentencePosition;

			ProofreadingError[] errors = atdClient.proofreadText(paraText);
			
			List errorsList = new LinkedList();
			for (int x = 0; x < errors.length; x++) {
				SingleProofreadingError aError = createOOoError(errors[x]);
				if (!disabledRules.contains(aError.aRuleIdentifier))
					errorsList.add(aError);
			}

			SingleProofreadingError[] errorArray = new SingleProofreadingError[errorsList.size()];
			Iterator i = errorsList.iterator();
			for (int x = 0; i.hasNext(); x++) {
				errorArray[x] = (SingleProofreadingError)i.next();
			}

			paRes.aErrors = errorArray;
		} 
		catch (final Throwable t) {
			showError(t);
			paRes.nBehindEndOfSentencePosition = paraText.length();
		}      
		return paRes;
	}

	/**
	 * Creates a SingleGrammarError object for use in OOo.
	 * @param myMatch
	 *          ruleMatch - LT rule match
	 * 
	 * @return SingleGrammarError - object for OOo checker integration
	 */
	private SingleProofreadingError createOOoError(ProofreadingError error) {
		final SingleProofreadingError aError = new SingleProofreadingError();
		
		if ("spelling".equals(error.type) || "Hyphen Required".equals(error.description)) {
			/* OOo ignores this value setting it to PROOFREADING, thanks OOo, not really */
                        aError.nErrorType = com.sun.star.text.TextMarkupType.SPELLCHECK;
		}
		else {
			aError.nErrorType = com.sun.star.text.TextMarkupType.PROOFREADING;
		}

		// the API currently has no support for formatting text in comments

		aError.aFullComment = error.description;
		aError.aShortComment = error.description;

		if (error.suggestions != null) {
			aError.aSuggestions = new String[error.suggestions.size()];

			Iterator i = error.suggestions.iterator();
			for (int y = 0; i.hasNext(); y++) {
				aError.aSuggestions[y] = i.next().toString();
			}
		}
		else {
			aError.aSuggestions = new String[0];
		}

		aError.nErrorStart = error.locationStart;
		aError.nErrorLength = error.errorString.length();
		aError.aRuleIdentifier = error.description;
		aError.aProperties = new PropertyValue[0];

		return aError;
	}

	public boolean hasLocale(Locale locale) {
		return "en".equals( locale.Language );
	}

	public final Locale[] getLocales() {
		return new Locale[] { 
			new Locale("en", "us", "en_US"), 
			new Locale("en", "ca", "en_CA"), 
			new Locale("en", "uk", "en_UK"), 
			new Locale("en", "gb", "en_GB"), 
			new Locale("en", "in", "en_IN"), 
			new Locale("en", "au", "en_AU"), 
			new Locale("en", "nz", "en_NZ"), 
			new Locale("en", "za", "en_ZA"), 
			new Locale("en", "zw", "en_ZW"), 
			new Locale("en", "ph", "en_PH"), 
			new Locale("en", "be", "en_BE"), 
			new Locale("en", "bw", "en_BW"), 
			new Locale("en", "bz", "en_BZ"), 
			new Locale("en", "jm", "en_JM"), 
			new Locale("en", "be", "en_BE"), 
			new Locale("en", "gu", "en_GU"), 
			new Locale("en", "hk", "en_HK"), 
			new Locale("en", "mh", "en_MH"), 
			new Locale("en", "mp", "en_MP"), 
			new Locale("en", "pk", "en_PK"), 
			new Locale("en", "sg", "en_SG"), 
			new Locale("en", "tt", "en_TT"), 
			new Locale("en", "um", "en_UM"), 
			new Locale("en", "vi", "en_VI")
		};
	}

	public final boolean isSpellChecker() {
		return false;
	}

	public final boolean hasOptionsDialog() {
		return false;
	}

	/**
	 * Add a listener that allow re-checking the document after changing the
	 * options in the configuration dialog box.
	 * 
	 * @param xLinEvLis
	 *          - the listener to be added
	 * @return true if listener is non-null and has been added, false otherwise.
	 */
	public final boolean addLinguServiceEventListener(final XLinguServiceEventListener xLinEvLis) {
		if (xLinEvLis == null) {
			return false;
		}
		xEventListeners.add(xLinEvLis);
		return true;
	}

	/**
	 * Remove a listener from the event listeners list.
	 * 
	 * @param xLinEvLis
	 *          - the listener to be removed
	 * @return true if listener is non-null and has been removed, false otherwise.
	 */
	public final boolean removeLinguServiceEventListener(final XLinguServiceEventListener xLinEvLis) {
		if (xLinEvLis == null) {
			return false;
		}
    
		if (xEventListeners.contains(xLinEvLis)) {
			xEventListeners.remove(xLinEvLis);
			return true;
		}
		return false;
	}

	public final void recheckDocument() {
		if (!xEventListeners.isEmpty()) {
			for (final XLinguServiceEventListener xEvLis : xEventListeners) {
				if (xEvLis != null) {
					final com.sun.star.linguistic2.LinguServiceEvent xEvent = new com.sun.star.linguistic2.LinguServiceEvent();
					xEvent.nEvent = com.sun.star.linguistic2.LinguServiceEventFlags.PROOFREAD_AGAIN;
					xEvLis.processLinguServiceEvent(xEvent);
 				}
			}
		}
	}

	public final void resetDocument() {
		disabledRules = new HashSet<String>();
		recheckDocument();
	}

	public String[] getSupportedServiceNames() {
		return getServiceNames();
	}

	public static String[] getServiceNames() {
		return SERVICE_NAMES;
	}

	public boolean supportsService(final String sServiceName) {
		for (final String sName : SERVICE_NAMES) {
			if (sServiceName.equals(sName)) {
				return true;
			}
		}
		return false;
	}

	public String getImplementationName() {
		return Main.class.getName();
	}

	public static XSingleComponentFactory __getComponentFactory(final String sImplName) {
		SingletonFactory xFactory = null;
		if (sImplName.equals(Main.class.getName())) {
			xFactory = new SingletonFactory();
		}
		return xFactory;
	}

	public static boolean __writeRegistryServiceInfo(final XRegistryKey regKey) {
		return Factory.writeRegistryServiceInfo(Main.class.getName(), Main.getServiceNames(), regKey);
	}

	public void trigger(final String sEvent) {
		if (!javaVersionOkay()) {
			return;
		}

		try {
			if (sEvent.equals("reset")) {
				resetDocument();	
			}
			else {
				System.err.println("Sorry, don't know what to do, sEvent = " + sEvent);
			}
		} 
		catch (final Throwable e) {
			showError(e);
		}
	}

	public void settingsChanged() {
		resetDocument();
	}

	private boolean javaVersionOkay() {
		final String version = System.getProperty("java.version");

		if (version != null && (version.startsWith("1.0") || version.startsWith("1.1") || version.startsWith("1.2") || version.startsWith("1.3") || version.startsWith("1.4"))) {
			final DialogThread dt = new DialogThread("Error: LanguageTool requires Java 1.5 or later. Current version: " + version);
			dt.start();
			return false;
		}

		return true;
	}

	static void showError(final Throwable e) {
		String metaInfo = "OS: " + System.getProperty("os.name") + " on " + System.getProperty("os.arch") + ", Java version " + System.getProperty("java.vm.version") + " from " + System.getProperty("java.vm.vendor");
		String msg = "An error has occurred in After the Deadline:\n" + e.toString() + "\nStacktrace:\n";
    
		final StackTraceElement[] elem = e.getStackTrace();
		for (final StackTraceElement element : elem) {
			msg += element.toString() + "\n";
		}
		msg += metaInfo;
		final DialogThread dt = new DialogThread(msg);
		dt.start();
	}

	public static void showMessage(String msg) {
		final DialogThread dt = new DialogThread(msg);
		dt.start();
	}

	public void ignoreRule(final String ruleId, final Locale locale) throws IllegalArgumentException {
		try {
			disabledRules.add(ruleId);
			recheckDocument();
		} 
		catch (final Throwable t) {
			showError(t);
		}
  	}

	public void resetIgnoreRules() {
		try {
			disabledRules = new HashSet<String>();
		} 
		catch (final Throwable t) {
			showError(t);
		}
	}
  
	public String getServiceDisplayName(Locale locale) {
		return "After the Deadline";
	}
}

class DialogThread extends Thread {
	final private String text;

	DialogThread(final String text) {
		this.text = text;
	}

	@Override
	public void run() {
		JOptionPane.showMessageDialog(null, text);
	}
}
