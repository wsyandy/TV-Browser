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
package tvbrowser.core.search.booleansearch;

/**
 * Implementiert logische oder-Verkn�pfung zwischen 2 Blocks
 * 
 * @author Gilson Laurent, pumpkin@gmx.de
 */
public class Or implements Block {

  Block block1;

  Block block2;


  public Or(Block b1, Block b2) {
    block1 = b1;
    block2 = b2;
  }


  public boolean test(String s) {
    return block1.test(s) || block2.test(s);
  }


  public Block finish() {
    if (block1 instanceof Or) {
      MultiOr MO = new MultiOr((Or) block1, block2);
      return MO.finish();
    }
    if (block2 instanceof Or) {
      MultiOr MO = new MultiOr((Or) block2, block1);
      return MO.finish();
    }
    block1 = block1.finish();
    block2 = block2.finish();
    if ((block2 instanceof Matcher) && (!(block1 instanceof Matcher))) {
      Block b = block1;
      block1 = block2;
      block2 = b;
    }
    if ((block2 instanceof Matcher) && (block1 instanceof Matcher)) {
      Matcher m1 = (Matcher) block1;
      Matcher m2 = (Matcher) block2;
      if (m1.size() > m2.size()) {
        Block b = block1;
        block1 = block2;
        block2 = b;
      }
    }
    return this;
  }


  public String toString() {
    return "(" + block1.toString() + " OR " + block2.toString() + ")";
  }
}