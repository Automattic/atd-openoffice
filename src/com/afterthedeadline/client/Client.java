package com.afterthedeadline.client;

import sleep.runtime.*;
import sleep.interfaces.*;
import sleep.bridges.*;
import sleep.error.*;

import com.afterthedeadline.openoffice.ProofreadingError;

import java.util.*;
import java.io.*;

/* I'm questioning the sanity of this too, but it's a lot quicker to prototype something
   like this in Sleep vs. Java and I'd rather get this extension done. */
public class Client implements RuntimeWarningWatcher {

	public void processScriptWarning(ScriptWarning warning) { 
		System.err.println(warning);
	}

	public static void main(String args[]) {
		System.out.println("Initializing client");
		
		Client temp = new Client();
		ProofreadingError[] errors = temp.proofreadText("This iss an test, the ball was thrown.");
		for (int x = 0; x < errors.length; x++) {
			System.err.println(errors[x]);
		}

		System.out.println("You should have seen two errors");
	}

	public ProofreadingError[] proofreadText(String paragraph) {
		if (!isReady())
			return new ProofreadingError[0];

		Stack args = new Stack();
		args.push(SleepUtils.getScalar(paragraph));

		Scalar temp = script.callFunction("&proofread", args);

		if (!SleepUtils.isEmptyScalar(temp)) {
			ProofreadingError[] errors = new ProofreadingError[temp.getArray().size()];
			Iterator i = temp.getArray().scalarIterator();
			for (int x = 0; i.hasNext(); x++) {
				errors[x] = (ProofreadingError) ((Scalar)i.next()).objectValue();
			}

			return errors;
		}
		else {
			return new ProofreadingError[0];
		}
	}

	protected ScriptInstance script = null;

	public boolean isReady() {
		return script != null;
	}

	public Client() {
		ScriptLoader loader = new ScriptLoader(); 
		Hashtable environment = new Hashtable(); 
		ScriptVariables variables = new ScriptVariables(); 
	
		try { 
			ScriptInstance a = loader.loadScript("webservice.sl", this.getClass().getResourceAsStream("/scripts/webservice.sl"), environment);
			a.setScriptVariables(variables); 
			a.addWarningWatcher(this);


			ScriptInstance b = loader.loadScript("atdclient.sl", this.getClass().getResourceAsStream("/scripts/atdclient.sl"), environment); 
			b.setScriptVariables(variables); 
			b.addWarningWatcher(this);

			a.runScript();
			b.runScript();

			script = b;
		} 
		catch (YourCodeSucksException syntaxErrors) { 
			System.err.println(syntaxErrors.formatErrors()); 
		} 
		catch (Exception ex) {
			System.err.println(ex);
			ex.printStackTrace();
		}
	}	
}
