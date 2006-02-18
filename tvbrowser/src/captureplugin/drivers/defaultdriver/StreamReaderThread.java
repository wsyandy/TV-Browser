/*
 * CapturePlugin by Andreas Hessel (Vidrec@gmx.de), Bodo Tasche
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

package captureplugin.drivers.defaultdriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A class that reads from InputStreams in a Thread.
 * 
 * @author Ren� Mach
 * 
 */
public class StreamReaderThread extends Thread {

  private InputStream mInput;
  private boolean mSaveOutput;
  private StringBuffer mOutput;

  /**
   * @param stream
   *          The InputStream to read from.
   * @param save
   *          Save the output of the stream.
   */
  public StreamReaderThread(InputStream stream, boolean save) {
    mInput = stream;
    mSaveOutput = save;
    mOutput = new StringBuffer();
  }

  public void run() {
    try {
      String line;
      BufferedReader reader = new BufferedReader(new InputStreamReader(mInput));
      while ((line = reader.readLine()) != null)
        if (mSaveOutput)
          mOutput.append(line + (File.separatorChar == '\\' ? "\r\n" : "\n"));

    } catch (IOException e) {}
  }

  /**
   * @return The output of the stream.
   */
  public String getOutput() {
    return mOutput.toString();
  }
}
