/*
 * TV-Pearl by Reinhard Lehrbaum
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
 */
package tvpearlplugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

public class TVPearlListCellRenderer extends DefaultListCellRenderer
{
    private static final long serialVersionUID = 1L;

    private static final Color SECOND_ROW_COLOR = new Color(230, 230, 230);

    public Component getListCellRendererComponent(final JList list,
      final Object value, final int index, final boolean isSelected,
      final boolean cellHasFocus)
    {
      final JLabel label = (JLabel) super.getListCellRendererComponent(list,
        value, index, isSelected, cellHasFocus);

        if (value instanceof TVPProgram)
        {
          final TVPProgram p = (TVPProgram) value;

          final TVPearlProgramPanel prog = new TVPearlProgramPanel(p);
            prog.setTextColor(label.getForeground());
            final JPanel pan = new JPanel(new BorderLayout());
            pan.add(prog, BorderLayout.CENTER);
            pan.setBackground(label.getBackground());
            if ((index % 2 != 0) && (!isSelected))
            {
                pan.setBackground(SECOND_ROW_COLOR);
            }

            return pan;
        }

        return label;
    }
}
