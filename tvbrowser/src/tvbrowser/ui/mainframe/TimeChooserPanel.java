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
 
 
package tvbrowser.ui.mainframe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;


public class TimeChooserPanel extends JPanel implements ActionListener {
    
    /** The localizer for this class. */
      public static final util.ui.Localizer mLocalizer
        = util.ui.Localizer.getLocalizerFor(TimeChooserPanel.class);

    
    private MainFrame mParent;
    private JButton mNowBt, mEarlyBt, mMiddayBt, mAfternoonBt, mEveningBt;
    private JPanel mTimeBtnPanel;
    
    public TimeChooserPanel(MainFrame parent) {
      setOpaque(false);
      mParent=parent;
      setLayout(new BorderLayout(0,7));
      setBorder(BorderFactory.createEmptyBorder(5,3,5,3));
      mTimeBtnPanel=createTimeBtnPanel();
      add(mTimeBtnPanel,BorderLayout.NORTH);
    }
    
    public void updateButtons() {
      remove(mTimeBtnPanel);
      mTimeBtnPanel=createTimeBtnPanel();
      add(mTimeBtnPanel,BorderLayout.NORTH);
      updateUI();
    }
    
    private JPanel createTimeBtnPanel() {
      JPanel result=new JPanel(new GridLayout(0,1,0,5));
      result.setOpaque(false);
      String msg;
      msg = mLocalizer.msg("button.now", "Now");
      mNowBt=new JButton(msg);
      mNowBt.addActionListener(this);
      result.add(mNowBt); 

      if (tvbrowser.core.Settings.propShowTimeButtons.getBoolean()) {
        msg = mLocalizer.msg("button.early", "Early");
        mEarlyBt=new JButton(msg);
        mEarlyBt.addActionListener(this);
        result.add(mEarlyBt);
     
        msg = mLocalizer.msg("button.midday", "Midday");
        mMiddayBt=new JButton(msg);
        mMiddayBt.addActionListener(this);
        result.add(mMiddayBt);
        
        msg = mLocalizer.msg("button.afternoon", "Afternoon");
        mAfternoonBt=new JButton(msg);
        mAfternoonBt.addActionListener(this);
        result.add(mAfternoonBt);
      
        msg = mLocalizer.msg("button.evening", "Evening");
        mEveningBt=new JButton(msg);
        mEveningBt.addActionListener(this);
        result.add(mEveningBt);
      }
      
      return result;
    }
    
    public void actionPerformed(ActionEvent e) {
      Object o=e.getSource();
      if (o==mNowBt) {
        mParent.scrollToNow();    
      }
      else if (o==mEarlyBt) {
        mParent.onEarlyBtn();
      }
      else if (o==mMiddayBt) {
        mParent.onMiddayBtn();
      }
      else if (o==mAfternoonBt) {
            mParent.onAfternoonBtn();
          }
      else if (o==mEveningBt) {
        mParent.onEveningBtn();
      }
    
    }
    
    
  }