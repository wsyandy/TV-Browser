/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin_oat@yahoo.de)
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


 /**
  * TV-Browser
  * @author Martin Oberhauser
  */

package tvbrowser.ui.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import tvbrowser.core.*;

public class SettingsDlg extends JDialog implements ActionListener {

  private JButton cancelBtn, okBtn;
  private JTabbedPane tabPane;

  public SettingsDlg(java.awt.Frame parent) {
    super(parent,"Settings");
    setModal(true);
    JPanel contentPane=(JPanel)getContentPane();
    contentPane.setLayout(new BorderLayout());
    contentPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

    tabPane=new JTabbedPane(JTabbedPane.LEFT);

    devplugin.SettingsTab tab=new ChannelsSettingsTab();
    tab.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    tabPane.addTab(tab.getName(),tab);

    tab=new AppearanceSettingsTab();
    tab.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    tabPane.addTab(tab.getName(),tab);


    tab=new PluginSettingsTab();
    tab.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    tabPane.addTab(tab.getName(),tab);


    Object[] plugins=PluginManager.getInstalledPlugins();

    for (int i=0;i<plugins.length;i++) {
      tab=((devplugin.Plugin)plugins[i]).getSettingsTab();
      if (tab!=null) {
        tab.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        tabPane.addTab(tab.getName(),tab);
      }
    }

    JPanel pushButtonPanel=new JPanel();

    cancelBtn=new JButton("Cancel");
    okBtn=new JButton("OK");

    pushButtonPanel.add(cancelBtn);
    pushButtonPanel.add(okBtn);

    cancelBtn.addActionListener(this);
    okBtn.addActionListener(this);

    contentPane.add(pushButtonPanel,BorderLayout.SOUTH);
    contentPane.add(tabPane,BorderLayout.CENTER);

    this.setSize(500,350);
  }


  public void actionPerformed(ActionEvent event) {
    Object source=event.getSource();

    if (source==cancelBtn) {
      setVisible(false);
    }else if (source==okBtn) {
      devplugin.SettingsTab curTab;
      for (int i=0;i<tabPane.getTabCount();i++) {
        curTab=(devplugin.SettingsTab)tabPane.getComponentAt(i);
        curTab.ok();
      }
      try {
        Settings.storeSettings();
      }catch (java.io.IOException e) {
        e.printStackTrace();
      }
      setVisible(false);
    }


  }

}