package org.iucn.sis.shared.acl;

/**
 * A class that enumerates what a User may have a preference over and what that
 * preference may be.
 * 
 * @author adam
 */
public interface UserPreferences {
	public static final String PROMPT = "PROMPT";
	public static final String DO_ACTION = "DO_ACTION";
	public static final String IGNORE = "IGNORE";

	public static final String AUTO_SAVE = "autosave";
	public static final String VIEW_CHOICES = "viewPreference";
}
