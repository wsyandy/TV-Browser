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
package tvbrowser.ui.finder.calendar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import tvbrowser.core.TvDataBase;
import devplugin.Date;

public final class CalendarTableCellRenderer implements
    TableCellRenderer {
  
  private static JPanel mPanel;
  private static JLabel mLabel;
  private static Font mBoldFont;
  private static Font mPlainFont;
  private static DashedBorder mDashedBorder;

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    if (mPanel == null) {
      mPanel = new JPanel();
      mPanel.setLayout(new GridLayout(0,1));
      mPanel.setOpaque(true);
      mLabel = new JLabel();
      mLabel.setHorizontalAlignment(SwingConstants.CENTER);
      mPanel.add(mLabel);
      int fontSize = mLabel.getFont().getSize() - 2;
      mBoldFont = new Font(Font.DIALOG, Font.BOLD, fontSize + 2);
      mPlainFont = new Font(Font.DIALOG, Font.PLAIN, fontSize);
      mDashedBorder = new DashedBorder();
    }
    if (value instanceof Date) {
      mPanel.setBackground(table.getBackground());
      Date date = (Date) value;
      if (isSelected) {
        mPanel.setBorder(mDashedBorder);
      }
      else {
        mPanel.setBorder(null);
      }
      boolean enabled = TvDataBase.getInstance().dataAvailable(date);
      mLabel.setText(String.valueOf(date.getDayOfMonth()));
      mLabel.setEnabled(enabled);
      CalendarTableModel tableModel = (CalendarTableModel) table.getModel();
      if (date.equals(tableModel.getCurrentDate())) {
        mLabel.setFont(mBoldFont);
      }
      else {
        mLabel.setFont(mPlainFont);
      }
    }
    else {
      mPanel.setBackground(table.getBackground());
      mLabel.setText(value.toString());
      mLabel.setEnabled(true);
    }
    return mPanel;
  }

  class DashedBorder implements Border {
    int THICKNESS = 1;
    Color color;
    int dashWidth;
    int dashHeight;
    public DashedBorder () {
      this (Color.black, 2, 2);
    }
    public DashedBorder (Color c, int width, int height) {
      if (width < 1) {
        throw new IllegalArgumentException ("Invalid width: " + width);
      }
      if (height < 1) {
        throw new IllegalArgumentException ("Invalid height: " + height);
      }
      color = c;
      dashWidth = width;
      dashHeight = height;
    }
    public void paintBorder (Component c, Graphics g, int x, int y, int width, int height) {
      Insets insets = getBorderInsets(c);
      g.setColor (color);
      int numWide = (int)Math.round(width / dashWidth);
      int numHigh = (int)Math.round(height / dashHeight);
      int startPoint;
      for (int i=0;i<=numWide;i+=2) {
        startPoint = x + dashWidth * i;
        g.fillRect (startPoint, y, dashWidth, THICKNESS);
        g.fillRect (startPoint, y+height-insets.bottom, dashWidth, THICKNESS);
      }
      for (int i=0;i<=numHigh;i+=2) {
        startPoint = x + dashHeight * i;
        g.fillRect (x, startPoint, THICKNESS, dashHeight);
        g.fillRect (x+width-insets.right, startPoint, THICKNESS, dashHeight);
      }
    }
    public Insets getBorderInsets(Component c) {
      return new Insets (THICKNESS, THICKNESS, THICKNESS, THICKNESS);
    }
    public boolean isBorderOpaque() {
      return false;
    }
  }

}