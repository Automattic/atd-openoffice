package com.afterthedeadline.client;

import java.util.*;
import java.io.*;
import java.lang.ref.*;

/* loads AtD config file */
public class Configuration {
	protected Properties config;
	protected File       file;
	protected Set        categories;
	protected Set        phrases;
	protected List       listeners = new LinkedList();

	protected static Configuration singleton = null;

	public void fireChange() {
		Iterator i = listeners.iterator();
		while (i.hasNext()) {
			WeakReference ref = (WeakReference)i.next();
			Object o = ref.get();
			if (o == null)
				i.remove();
			else
				((ChangeListener)o).settingsChanged();
		}
	}

	public void addChangeListener(ChangeListener l) {
		listeners.add(new WeakReference(l));
	}

	public synchronized static Configuration getConfiguration() {
		if (singleton == null) {
			singleton = new Configuration(new File(System.getProperty("user.home"), ".AtD-OpenOffice.org"));
			singleton.load();
		}
		return singleton;
	}

	public Configuration(File _file) {
		config = new Properties();
		file   = _file;
	}

	public void load() {
		try {
			config.load(new FileInputStream(file));
			phrases = getIgnoredPhrases();
			categories = getCategories();
		}
		catch (Exception ex) {
			phrases = new HashSet();
			categories = new HashSet();
		}
	}

	private Set<String> createSet(String[] strings) {
		Set temp = new HashSet<String>();
		for (int x = 0; x < strings.length; x++) {
			temp.add(strings[x]);
		}
		return temp;
	}

	public synchronized boolean isIgnored(String phrase) {
		return phrases.contains(phrase);
	}

	public synchronized boolean isEnabled(String category) {
		return categories.contains(category);
	}

	public synchronized void ignorePhrase(String phrase) {
		phrases.add(phrase);
		config.setProperty("ignoredPhrases", createString(phrases));
	}

	public synchronized void removePhrase(String phrase) {
		phrases.remove(phrase);
		config.setProperty("ignoredPhrases", createString(phrases));
	}

	public synchronized void showCategory(String category) {
		categories.add(category);
		config.setProperty("categories", createString(categories));
	}

	public synchronized void hideCategory(String category) {
		categories.remove(category);
		config.setProperty("categories", createString(categories));
	}

	private String createString(Set<String> strings) {
		StringBuffer temp = new StringBuffer();
		Iterator<String> i = strings.iterator();
		while (i.hasNext()) {
			String value = i.next();
			temp.append(value);

			if (i.hasNext())
				temp.append(", ");
		}

		return temp.toString();
	}

	public synchronized Set<String> getIgnoredPhrases() {
		return createSet(config.getProperty("ignoredPhrases", "").split(",\\s+"));
	}

	public synchronized Set<String> getCategories() {
		return createSet(config.getProperty("categories", "").split(",\\s+"));
	}

	public synchronized String getServiceHost() {
		if ("".equals(config.getProperty("host")) || config.getProperty("host") == null) {
			return "https://en.service.afterthedeadline.com";
		}

		String host = (config.getProperty("host") + "").trim();
		if (host.endsWith("/")) {
			host = host.substring(0, host.length() - 1);
		}

		return host;
	}

	public synchronized void setServiceHost(String name) {
		config.setProperty("host", name);
	}

	public void save() {
		try {
			config.save(new FileOutputStream(file), "AtD-OpenOffice Properties");
			fireChange();
		}
		catch (Exception ex) {
			throw new RuntimeException("Could not save properties\nLocation:" + file + "\n" + ex.getMessage());
		}
	}
}
