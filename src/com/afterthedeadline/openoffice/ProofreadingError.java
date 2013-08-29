package com.afterthedeadline.openoffice;

import java.util.*;

/* a proofreading error */
public class ProofreadingError {
	public String precontext    = "";
	public int    locationStart = 0;
	public String errorString   = "";
	public List<String> suggestions = null;
	public String description   = "";
	public String type          = "";
	
	public String toString() {
		return precontext + "::" + errorString + "@" + locationStart + "::" + type + "(" + description + ") => " + suggestions;
	}
}
