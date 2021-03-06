/*
 * Copyright Michael Keppler
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package wirschauenplugin;

import java.util.Properties;

import devplugin.Program;


/**
 * container for the settings of a plugin. this general mechanism is
 * 'best practice', so we used it even if we could have saved some mem
 * otherwise. on the plus side we have now an easy to extend mechanism
 * for the plugin settings.
 */
public class WirSchauenSettings extends PropertyBasedSettings {

  /**
   * this is the key used to store the show markings option in a property
   * (see storeSettings and loadSettings).
   */
  private static final String OPTION_KEY_SHOW_MARKINGS = "showMarkings";

  /**
   * this is the key used to store the prio for the markings of linked programs.
   */
  private static final String OPTION_KEY_OMDB_MARK_PRIORITY = "omdbMarkPriority";

  /**
   * this is the key used to store the prio for the markings of linked programs
   * which were linked by the user himself.
   */
  private static final String OPTION_KEY_MY_OMDB_MARK_PRIORITY = "myOmdbMarkPriority";

  private static final String OPTION_KEY_SPELL_CHECKING = "spellCheck";




  /**
   * @param properties the underlying properties to save the settings
   */
  public WirSchauenSettings(final Properties properties) {
    super(properties);
  }


  /**
   * @return true, if the programs with OMDB link shall be marked, false otherwise
   */
  public boolean getMarkPrograms() {
    return get(OPTION_KEY_SHOW_MARKINGS, false);
  }

  /**
   * @param showMarkings true, if the programs with OMDB link shall be marked, false otherwise
   */
  public void setMarkPrograms(final boolean showMarkings) {
    //if the setting changed, we need to update the graphics and save the new setting
    if (showMarkings != getMarkPrograms())
    {
      set(OPTION_KEY_SHOW_MARKINGS, showMarkings);
      WirSchauenPlugin.getInstance().updateMarkings(showMarkings);
    }
  }

  /**
   * Sets the mark priority for the omdb link highlighting.
   *
   * @param markPriority The new mark priority.
   */
  public void setMarkPriorityForOmdbLink(final int markPriority) {
    if (getMarkPriorityForOmdbLink() != markPriority) {
      set(OPTION_KEY_OMDB_MARK_PRIORITY, markPriority);
      WirSchauenPlugin.getInstance().updateMarkingOfLinkedPrograms();
    }
  }

  /**
   * Gets the mark priority for the omdb link highlighting.
   *
   * @return The mark priority.
   */
  public int getMarkPriorityForOmdbLink() {
    return get(OPTION_KEY_OMDB_MARK_PRIORITY, Program.MIN_MARK_PRIORITY);
  }

  /**
   * Sets the mark priority for the omdb link highlighting for
   * programs which were linked by the user himself.
   *
   * @param markPriority The new mark priority.
   */
  public void setMarkPriorityForOwnOmdbLink(final int markPriority) {
    if (getMarkPriorityForOwnOmdbLink() != markPriority) {
      set(OPTION_KEY_MY_OMDB_MARK_PRIORITY, markPriority);
      WirSchauenPlugin.getInstance().updateMarkingOfProgramsInTree();
    }
  }

  /**
   * Gets the mark priority for the omdb link highlighting for
   * programs which were linked by the user himself.
   *
   * @return The mark priority.
   */
  public int getMarkPriorityForOwnOmdbLink() {
    return get(OPTION_KEY_MY_OMDB_MARK_PRIORITY, Program.LOWER_MEDIUM_MARK_PRIORITY);
  }


  public boolean getSpellChecking() {
    return get(OPTION_KEY_SPELL_CHECKING, true);
  }


  public void setSpellChecking(final boolean check) {
    set(OPTION_KEY_SPELL_CHECKING, check);
  }
}
