/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin@tvbrowser.org)
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

package devplugin;

import java.awt.Color;

/**
 * Provides information of the current user settings.
 *  
 */
public interface TvBrowserSettings {

  /**
   * @return the directory name of the user settings (e.g linux: ~/home/.tvbrowser/);
   */
  public String getTvBrowserUserHome();

  /**
   * @return the times of the time buttons (in minutes)
   */
  public int[] getTimeButtonTimes();

  /**
   * @return the date of the previous donwload
   */
  public Date getLastDownloadDate();

  /**
   * Gets the color for a marking priority.
   * 
   * @param priority The priority to get the color for.
   * @return The color for the given priority or <code>null</code>
   *         if the given priority don't exists.
   * @since 2.6
   */
  public Color getColorForMarkingPriority(int priority);
}
