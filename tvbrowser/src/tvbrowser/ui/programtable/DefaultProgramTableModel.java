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
package tvbrowser.ui.programtable;

import java.util.ArrayList;
import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tvbrowser.core.TvDataBase;
import util.io.IOUtilities;
import util.ui.ProgramPanel;

import devplugin.Channel;
import devplugin.ChannelDayProgram;
import devplugin.Program;
import devplugin.Date;
import devplugin.ProgressMonitor;
import devplugin.ProgramFilter;

/**
 *
 * @author  Til
 */
public class DefaultProgramTableModel implements ProgramTableModel, ChangeListener {
  
  private int mTomorrowLatestTime;
  private int mTodayEarliestTime;

  private ArrayList mListenerList;
  
  private Channel[] mChannelArr, mShownChannelArr;
  private Date mMainDay, mNextDay;
  
  private ArrayList[] mProgramColumn, mShownProgramColumn;
  
  private int mLastTimerMinutesAfterMidnight;
  private Timer mTimer;
  
  private ProgramFilter mProgramFilter=null;



  /**
   * Creates a new instance of DefaultProgramTableModel.
   */
  public DefaultProgramTableModel(Channel[] channelArr,
    int todayEarliestTime, int tomorrowLatestTime)
  {
    mListenerList = new ArrayList();
    mTodayEarliestTime=todayEarliestTime;
    mTomorrowLatestTime=tomorrowLatestTime;

    mMainDay = new Date();
    mNextDay = mMainDay.addDays(1);
    
	  setChannels(channelArr);
    
    mTimer = new Timer(10000, new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        handleTimerEvent();
      }
    });
    mTimer.start();
  }

  public void setTimeRange(int todayEarliestTime, int tomorrowLatestTime) {
    mTodayEarliestTime=todayEarliestTime;
    mTomorrowLatestTime=tomorrowLatestTime;
    fireTableDataChanged();
  }
  
  
  
  
  public synchronized void setChannels(Channel[] channelArr) {
    if (channelArr == null) {
      throw new NullPointerException("shownChannelArr is null!");
    }
    mChannelArr = channelArr;
    
	  mProgramColumn=new ArrayList[mChannelArr.length];
	  for (int i=0;i<mProgramColumn.length;i++) {
		  mProgramColumn[i]=new ArrayList();
	  }
    
    updateTableContent();
  }
  
  
  public void setProgramFilter(ProgramFilter filter) {
    mProgramFilter=filter;
    fireTableDataChanged();
    updateTableContent();
  }
  
  
  private int compareDateTime(Date d1, int m1, Date d2, int m2) {


    if (d1.compareTo(d2)<0) { // (d1<d2)
      return -1;
    }
    else if (d1.compareTo(d2)>0) { //(d1>d2)
      return 1;
    }
    else { // d1 == d2
      if (m1<m2) {
        return -1;
      }
      else if (m1>m2) {
        return 1;
      }
      else {
          return 0;
      }
    }

  } 
  
  private synchronized void addChannelDayProgram(int col, ChannelDayProgram cdp,
    int startMinutes, Date startDate, int endMinutes, Date endDate )
  {
    if (cdp==null) return;
    Iterator it=cdp.getPrograms();  
    if (it!=null) {
      while (it.hasNext()) {
        Program prog=(Program)it.next();
        int time=prog.getHours()*60+prog.getMinutes();
	    if (compareDateTime(prog.getDate(), time, startDate, startMinutes) >=0 && compareDateTime(prog.getDate(), time, endDate, endMinutes)<=0) {
		  if (mProgramFilter==null || mProgramFilter.accept(prog)) {
            ProgramPanel panel = new ProgramPanel(prog);
            mProgramColumn[col].add(panel);
          }
        }
      }
    }
  }
  
 

  public synchronized void setDate(Date date, ProgressMonitor monitor,
    Runnable callback)
  {
    mMainDay = date;
    mNextDay = date.addDays(1);
    
    updateTableContent(monitor, callback);
  }
  
  
  public Date getDate() {
    return mMainDay;
  }
  
  
  private void updateTableContent() {
    updateTableContent(null, null);
  }


  private synchronized void updateTableContent(ProgressMonitor monitor,
    final Runnable callback)
  {  
    deregisterFromPrograms(mProgramColumn);
    
    TvDataBase db = TvDataBase.getInstance();
    
    if (monitor!=null) {    
      monitor.setMaximum(mChannelArr.length-1);
      monitor.setValue(0);
    }
    
    for (int i = 0; i < mChannelArr.length; i++) {
     
	  mProgramColumn[i].clear();
      ChannelDayProgram cdpThisDay = db.getDayProgram(mMainDay, mChannelArr[i]);

      ChannelDayProgram cdpNextDay = db.getDayProgram(mNextDay, mChannelArr[i]);



      if (cdpThisDay != null) {
        addChannelDayProgram(i, cdpThisDay, mTodayEarliestTime, mMainDay,
                mTomorrowLatestTime, mNextDay);
      }


      if (cdpNextDay != null) {
        addChannelDayProgram(i, cdpNextDay, mTodayEarliestTime, mMainDay,
                mTomorrowLatestTime, mNextDay);
      }
	  
	  
      if (monitor!=null) {
        monitor.setValue(i);
      }       
      
    }
	
	
    boolean showEmptyColumns = mProgramFilter instanceof tvbrowser.core.filters.ShowAllFilter;
    
    ArrayList newShownColumns = new ArrayList();
    ArrayList newShownChannels = new ArrayList();
    for (int i = 0; i < mProgramColumn.length; i++) {
      if (showEmptyColumns || mProgramColumn[i].size() > 0) {
        newShownColumns.add(mProgramColumn[i]);
        newShownChannels.add(mChannelArr[i]);
      }
    }
    mShownProgramColumn = new ArrayList[newShownColumns.size()];
    mShownChannelArr = new Channel[newShownChannels.size()];

    newShownColumns.toArray(mShownProgramColumn);
    newShownChannels.toArray(mShownChannelArr);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        handleTimerEvent();

        registerAtPrograms(mProgramColumn);

        // Update the programs on air
        updateProgramsOnAir();

        fireTableDataChanged();

        if (callback != null) {
          callback.run();
        }
      }
    });
  }

  
  
  public void addProgramTableModelListener(ProgramTableModelListener listener) {
    mListenerList.add(listener);
  }

  
  
  public Channel[] getShownChannels() {
    return mShownChannelArr;
  }
  
  

  public int getColumnCount() {
    return mShownChannelArr.length;
  }



  public synchronized int getRowCount(int col) {
    return mShownProgramColumn[col].size();   
  }



  public synchronized ProgramPanel getProgramPanel(int col, int row) {
      
    //  ArrayList list=mProgramColumn[col];
    //  if (list.size()<=row) return null;
    //  return (ProgramPanel)list.get(row);
      
    ArrayList list=mShownProgramColumn[col];
    if (list.size()<=row) return null;
    return (ProgramPanel)list.get(row);
 
  }

  
 

  private synchronized void deregisterFromPrograms(ArrayList[] columns) {
    for (int i=0;i<columns.length;i++) {
      Iterator it=columns[i].iterator();
      while (it.hasNext()) {
        ProgramPanel panel = (ProgramPanel) it.next();
        Program prog = panel.getProgram();
        prog.removeChangeListener(this);
      }
    }          
  }


  private synchronized void registerAtPrograms(ArrayList[] columns) {
    for (int i=0;i<columns.length;i++) {
      Iterator it=columns[i].iterator();
      while (it.hasNext()) {
        ProgramPanel panel = (ProgramPanel) it.next();
        Program prog = panel.getProgram();
        prog.addChangeListener(this);
      }
    }
  }
  
  
 

  protected synchronized void fireTableDataChanged() {
    for (int i = 0; i < mListenerList.size(); i++) {
      ProgramTableModelListener lst = (ProgramTableModelListener) mListenerList.get(i);
      lst.tableDataChanged();
    }
  }

  
  
  protected synchronized void fireTableCellUpdated(int col, int row) {
    for (int i = 0; i < mListenerList.size(); i++) {
      ProgramTableModelListener lst = (ProgramTableModelListener) mListenerList.get(i);
      lst.tableCellUpdated(col, row);
    }
  }
  
  
  
  protected synchronized int getColumnOfChannel(Channel channel) {
    for (int col = 0; col < mShownChannelArr.length; col++) {
    //for (int col = 0; col < mChannelArr.length; col++) {
      //if (channel.equals(mChannelArr[col])) {
      if (channel.equals(mShownChannelArr[col])) {
        return col;
      }
    }
    
    // No such column found
    return -1;
  }



  private synchronized void handleTimerEvent() {
    // Avoid a repaint 6 times a minute (Once a minute is enough)
    int minutesAfterMidnight = IOUtilities.getMinutesAfterMidnight();
    if (minutesAfterMidnight == mLastTimerMinutesAfterMidnight) {
      return;
    }

    mLastTimerMinutesAfterMidnight = minutesAfterMidnight;
        
    // Update the programs on air
    updateProgramsOnAir();
    
    // Force a repaint of all programs on air
    // (so the progress background will be updated)
    for (int col = 0; col < getColumnCount(); col++) {
      for (int row = 0; row < getRowCount(col); row++) {
        ProgramPanel panel = getProgramPanel(col, row);
        if (panel.getProgram().isOnAir()) {
          fireTableCellUpdated(col, row);
        }
      }
    }
  }
  
  
  private synchronized void updateProgramsOnAir() {
    TvDataBase db = TvDataBase.getInstance();
    for (int i = 0; i < mChannelArr.length; i++) {
      Channel channel = mChannelArr[i];

      ChannelDayProgram dayProg = db.getDayProgram(mMainDay, channel);
      if (dayProg != null) {
        dayProg.markProgramOnAir();
      }

      dayProg = db.getDayProgram(mNextDay, channel);
      if (dayProg != null) {
        dayProg.markProgramOnAir();
      }
    }
  }


  public synchronized void stateChanged(ChangeEvent evt) {
    // A program has changed
    Program program = (Program) evt.getSource();

    // Get the column of this program
    int col = getColumnOfChannel(program.getChannel());
    if (col == -1) {
      // This program is not shown in this table
      return;
    }
    
    // Get the row of this program
    for (int row = 0; row < getRowCount(col); row++) {
      ProgramPanel panel = getProgramPanel(col, row);
      if (program == panel.getProgram()) {
        // Tell the panel that its program has changed
        panel.programHasChanged();
        
        // Fire the event
        fireTableCellUpdated(col, row);
        return;
      }
    }
  }

}
