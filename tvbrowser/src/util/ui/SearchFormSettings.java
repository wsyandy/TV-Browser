/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (darras@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * CVS information:
 *  $RCSfile$
 *   $Source$
 *     $Date$
 *   $Author$
 * $Revision$
 */
package util.ui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import tvbrowser.core.search.regexsearch.RegexSearcher;
import util.exc.TvBrowserException;
import devplugin.Plugin;
import devplugin.PluginManager;
import devplugin.ProgramFieldType;
import devplugin.ProgramSearcher;

/**
 * Settings for the SearchForm
 * 
 * @see util.ui.SearchForm
 * @author Til Schneider, www.murfman.de
 */
public class SearchFormSettings {

  /**
   * Specifies, that the search term has to match exacly.
   * @deprecated Since 1.1. Use {@link PluginManager#SEARCHER_TYPE_EXACTLY} instead.
   */
  public static final int MATCH_EXACTLY = PluginManager.SEARCHER_TYPE_EXACTLY;
  /**
   * Specifies, that the search term is a keyword (= substring).
   * @deprecated Since 1.1. Use {@link PluginManager#SEARCHER_TYPE_KEYWORD} instead.
   */
  public static final int MATCH_KEYWORD = PluginManager.SEARCHER_TYPE_KEYWORD;
  /**
   * Specifies, that the search term is a regular expression.
   * @deprecated Since 1.1. Use {@link PluginManager#SEARCHER_TYPE_REGULAR_EXPRESSION} instead.
   */
  public static final int MATCH_REGULAR_EXPRESSION = PluginManager.SEARCHER_TYPE_REGULAR_EXPRESSION;
  
  /** Specifies, that only titles should be searched. */
  public static final int SEARCH_IN_TITLE = 1;
  /** Specifies, that all fields should be searched. */
  public static final int SEARCH_IN_ALL = 2;
  /**
   * Specifies, that a user-defined set of fields should be searched.
   * 
   * @see #setUserDefinedFieldTypes(ProgramFieldType[])
   */
  public static final int SEARCH_IN_USER_DEFINED = 3;

  /** The search text the user provided. */
  private String mSearchText;
  /**
   * Specifies where to search. Either {@link #SEARCH_IN_TITLE} or
   * {@link #SEARCH_IN_ALL}.
   */
  private int mSearchIn;
  /** The fields the user want to search in. */
  private ProgramFieldType[] mUserDefinedFieldTypes;
  /**
   * Specifies the searcher type.
   *
   * @see devplugin.PluginManager#createProgramSearcher(int, String, boolean) 
   */
  private int mSearcherType;
  /** Specifies whether to search case sensitive. */
  private boolean mCaseSensitive;
  /** Specifies how many days to search. */
  private int mNrDays;


  /**
   * Creates a new SearchFormSettings instance.
   * 
   * @param searchText The search text
   */
  public SearchFormSettings(String searchText) {
    mSearchText = searchText;
    mSearchIn = SEARCH_IN_TITLE;
    mSearcherType = PluginManager.SEARCHER_TYPE_KEYWORD;
    mCaseSensitive = false;
    mNrDays=14;
  }


  /**
   * Loads a SearchFormSettings instance from a stream.
   * 
   * @param in The stream to read from
   * @throws IOException If reading failed
   * @throws ClassNotFoundException If the read data has the wrong format
   * @see #writeData(ObjectOutputStream)
   */
  public SearchFormSettings(ObjectInputStream in)
    throws IOException, ClassNotFoundException
  {
    int version = in.readInt(); // version
    
    mSearchText = (String) in.readObject();
    mSearchIn = in.readInt();
    mSearcherType = in.readInt();
    mCaseSensitive = in.readBoolean();
    
    int fieldTypeCount = in.readInt();
    if (fieldTypeCount > 0) {
      mUserDefinedFieldTypes = new ProgramFieldType[fieldTypeCount];
      for (int i = 0; i < mUserDefinedFieldTypes.length; i++) {
        int typeId = in.readInt();
        mUserDefinedFieldTypes[i] = ProgramFieldType.getTypeForId(typeId);
      }
    } else {
      mUserDefinedFieldTypes = null;
    }
    
    if (version == 2) {
      mNrDays = in.readInt();
    }
  }


  /**
   * Writes the settings into a stream
   * 
   * @param out The stream to write to
   * @throws IOException If writing failed
   * @see #SearchFormSettings(ObjectInputStream)
   */
  public void writeData(ObjectOutputStream out) throws IOException {
    out.writeInt(2); // version

    out.writeObject(mSearchText);
    out.writeInt(mSearchIn);
    out.writeInt(mSearcherType);
    out.writeBoolean(mCaseSensitive);
    
    if ((mSearchIn == SEARCH_IN_USER_DEFINED) && (mUserDefinedFieldTypes != null)) {
      out.writeInt(mUserDefinedFieldTypes.length);
      for (int i = 0; i < mUserDefinedFieldTypes.length; i++) {
        out.writeInt(mUserDefinedFieldTypes[i].getTypeId());
      }
    } else {
      out.writeInt(0); // No field types
    }
    
    out.writeInt(mNrDays);
  }


  /**
   * Gets the search text.
   * 
   * @return The search text
   */
  public String toString() {
    return mSearchText;
  }


  /**
   * Gets the search text.
   * 
   * @return The search text
   */
  public String getSearchText() {
    return mSearchText;
  }
  
  
  /**
   * Sets the search text.
   * 
   * @param searchText The search text
   */
  public void setSearchText(String searchText) {
    mSearchText = searchText;
  }
  
  
  /**
   * Creates a searcher from this settings
   * 
   * @return A searcher that satisfies these settings.
   * @throws TvBrowserException If creating the searcher failed.
   */
  public ProgramSearcher createSearcher()
    throws TvBrowserException
  {
    return Plugin.getPluginManager().createProgramSearcher(mSearcherType,
        mSearchText, mCaseSensitive);
  }

  /**
   * Creates a searcher from this settings
   * 
   * @param text Search for this Text
   * 
   * @return A searcher that satisfies these settings.
   * @throws TvBrowserException If creating the searcher failed.
   */
  public ProgramSearcher createSearcher(String text)
    throws TvBrowserException
  {
    return Plugin.getPluginManager().createProgramSearcher(mSearcherType,
        text, mCaseSensitive);
  }


  /**
   * Gets the search text as regular expression.
   * 
   * @return The search text as regular expression.
   * 
   * @deprecated Since 1.1. Use {@link #createSearcher()} instead.
   */
  public String getSearchTextAsRegex() {
    boolean matchExactly = (mSearcherType == PluginManager.SEARCHER_TYPE_EXACTLY);
    boolean matchKeyword = (mSearcherType == PluginManager.SEARCHER_TYPE_KEYWORD);
    
    if (matchExactly || matchKeyword) {
      return RegexSearcher.searchTextToRegex(mSearchText, matchKeyword);
    } else {
      // The search text already is a regex
      return mSearchText;
    }
  }


  /**
   * Gets the field types to search for.
   * 
   * @return The field types to search for.
   */
  public ProgramFieldType[] getFieldTypes() {
    switch (mSearchIn) {
      case SEARCH_IN_TITLE:
        return new ProgramFieldType[] { ProgramFieldType.TITLE_TYPE };
      case SEARCH_IN_ALL: return SearchForm.getSearchableFieldTypes();
      default:
        if (mUserDefinedFieldTypes != null) {
          return mUserDefinedFieldTypes;
        } else {
          return SearchForm.getSearchableFieldTypes();
        }
    }
  }


  /**
   * Gets the field types defined by the user.
   * 
   * @return The field types defined by the user.
   */
  public ProgramFieldType[] getUserDefinedFieldTypes() {
    return mUserDefinedFieldTypes;
  }


  /**
   * Sets the user-defined field types to search for.
   * 
   * @param typeArr The field types to search for
   */
  public void setUserDefinedFieldTypes(ProgramFieldType[] typeArr) {
    mUserDefinedFieldTypes = typeArr;
  }


  /**
   * Gets where to search. Either {@link #SEARCH_IN_TITLE},
   * {@link #SEARCH_IN_ALL} or {@link #SEARCH_IN_USER_DEFINED}.
   * 
   * @return Where to search
   */
  public int getSearchIn() {
    return mSearchIn;
  }


  /**
   * Sets where to search. Must be either {@link #SEARCH_IN_TITLE},
   * {@link #SEARCH_IN_ALL} or {@link #SEARCH_IN_USER_DEFINED}.
   * 
   * @param searchIn Where to search
   */
  public void setSearchIn(int searchIn) {
    mSearchIn = searchIn;
  }

  
  /**
   * Gets the searcher type to be used.
   * 
   * @return The searcher type to be used.
   * 
   * @see PluginManager#createProgramSearcher(int, String, boolean)
   */
  public int getSearcherType() {
    return mSearcherType;
  }

  
  /**
   * Sets the searcher type to be used.
   * 
   * @param searcherType The searcher type to be used.
   * 
   * @see PluginManager#createProgramSearcher(int, String, boolean)
   */
  public void setSearcherType(int searcherType) {
    mSearcherType = searcherType;
  }

  

  /**
   * Gets what how the search text has to match.
   * 
   * @return How the search text has to match.
   * 
   * @deprecated Since 1.1. Use {@link #getSearcherType()} instead.
   */
  public int getMatch() {
    return getSearcherType();
  }


  /**
   * Sets what how the search text has to match.
   * 
   * @param match How the search text has to match.
   * 
   * @deprecated Since 1.1. Use {@link #getSearcherType()} instead.
   */
  public void setMatch(int match) {
    setSearcherType(match);
  }


  /**
   * Gets whether to search case sensitive.
   * 
   * @return Whether to search case sensitive
   */
  public boolean getCaseSensitive() {
    return mCaseSensitive;
  }


  /**
   * Sets whether to search case sensitive.
   * 
   * @param caseSensitive Whether to search case sensitive
   */
  public void setCaseSensitive(boolean caseSensitive) {
    mCaseSensitive = caseSensitive;
  }

  /**
   * Gets the Nr of Days
   * @return Nr of Days to search 
   */
  public int getNrDays() {
      return mNrDays;
  }
  
  /**
   * Sets the Nr of Days
   * @param nr
   */
  public void setNrDays(int nr) {
      mNrDays = nr;
  }
}