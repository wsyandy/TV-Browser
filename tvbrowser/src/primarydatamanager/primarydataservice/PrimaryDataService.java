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

package primarydatamanager.primarydataservice;

import devplugin.Channel;

/**
 * @author Martin Oberhauser
 */
public interface PrimaryDataService {

  public boolean execute(String dir, java.io.PrintStream err);

  /**
   * Gets the list of the channels that are available by this data service.
   */
  public Channel[] getAvailableChannels();

  /**
   * Gets the number of bytes read (= downloaded) by this data service.
   * 
   * @return The number of bytes read.
   */
  public int getReadBytesCount();

}