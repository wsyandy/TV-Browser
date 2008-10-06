/*
 * IDontWant2See - Plugin for TV-Browser
 * Copyright (C) 2008 Ren� Mach
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
 * SVN information:
 *     $Date$
 *   $Author$
 * $Revision$
 */
package idontwant2see;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The renderer class for the settings table.
 * 
 * @author Ren� Mach
 */
public class IDontWant2SeeSettingsTableRenderer extends
    DefaultTableCellRenderer {
  private final static Color NOT_VALID_COLOR = new Color(220,0,0,60);
  
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)  {
    Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
    
    if(column == 0) {
      if(!((IDontWant2SeeSettingsTableModel)table.getModel()).rowIsValid(row) && !isSelected) {
        ((JLabel)c).setOpaque(true);
        c.setBackground(NOT_VALID_COLOR);
      }
      else if (isSelected){
        c.setBackground(table.getSelectionBackground());
      }
      else {
        c.setBackground(table.getBackground());
      }
      
      return c;
    }
    else {
      JPanel background = new JPanel(new FormLayout("0dlu:grow,default,0dlu:grow","default"));
      background.setOpaque(true);
      
      if(!isSelected) {
        background.setBackground(table.getBackground());
      }
      else {
        background.setBackground(table.getSelectionBackground());
      }
      
      JCheckBox checkBox = new JCheckBox();
      checkBox.setSelected((Boolean)value);
      checkBox.setOpaque(false);
      checkBox.setContentAreaFilled(false); 

      if(!((IDontWant2SeeSettingsTableModel)table.getModel()).rowIsValid(row) && !isSelected) {
        background.setBackground(NOT_VALID_COLOR);
      }
      
      background.add(checkBox, new CellConstraints().xy(2,1));
      
      return background;
    }
  }
}