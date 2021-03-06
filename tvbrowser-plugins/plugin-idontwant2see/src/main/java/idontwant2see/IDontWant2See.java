/*
 * IDontWant2See - Plugin for TV-Browser
 * Copyright (C) 2008 René Mach
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

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.apache.commons.lang.StringUtils;

import util.ui.Localizer;
import util.ui.UiUtilities;
import util.ui.WindowClosingIf;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import devplugin.ActionMenu;
import devplugin.ContextMenuAction;
import devplugin.Date;
import devplugin.ImportanceValue;
import devplugin.Plugin;
import devplugin.PluginInfo;
import devplugin.PluginsFilterComponent;
import devplugin.PluginsProgramFilter;
import devplugin.Program;
import devplugin.ProgramReceiveTarget;
import devplugin.SettingsTab;
import devplugin.Version;

/**
 * A very simple filter plugin to easily get rid of stupid programs in the
 * program table.
 *
 * @author René Mach
 */
public final class IDontWant2See extends Plugin implements AWTEventListener {

  private static final boolean PLUGIN_IS_STABLE = true;
  private static final Version PLUGIN_VERSION = new Version(0, 12, 1, PLUGIN_IS_STABLE);

  private static final String RECEIVE_TARGET_EXCLUDE_EXACT = "target_exclude_exact";

  static final Localizer mLocalizer = Localizer
      .getLocalizerFor(IDontWant2See.class);

  private static Date mCurrentDate = Date.getCurrentDate();
  private PluginsProgramFilter mFilter;
  private static IDontWant2See mInstance;
  private boolean mDateWasSet;
  private IDontWant2SeeSettings mSettings;

  private boolean mCtrlPressed;

  private HashMap<Program, Boolean> mMatchCache = new HashMap<Program, Boolean>(
			1000);

  private static final Pattern PATTERN_TITLE_PART = Pattern.compile(
      "(?i)" // case insensitive matching
      + "(.*)" // ignore everything at the beginning
      + "((" // have two alternatives: optional and mandatory brackets
      + "\\(?(" // optional brackets begin
      + "(Teil \\d+)" + "|"
      + "(Teil \\d+/\\d+)" + "|"
      + "(Teil \\d+ von \\d+)" + "|"
      + "(Folge \\d+)" + "|"
      + "(Folge \\d+/\\d+)" + "|"
      + "(Folge \\d+ von \\d+)" + "|"
      + "(Best of)" + "|"
      + "(\\d+/\\d+)" + "|"
      + "(\\sI)" + "|" // 1
      + "(\\sII)" + "|" // 2
      + "(\\sIII)" + "|" // 3
      + "(\\sIV)" + "|" // 4
      + "(\\sV)" // 5
      + ")\\)?" // optional brackets end
      + ")|(" // 2. alternative: mandatory brackets
      + "\\((" // mandatory brackets begin
      + "(Fortsetzung)" + "|"
      + "(\\d+)"
      + ")\\)" // mandatory brackets end
      + "))"
      + "$"); // at the end only

  public static Version getVersion() {
    return PLUGIN_VERSION;
  }

  /**
   * Creates an instance of this plugin.
   */
  public IDontWant2See() {
    mInstance = this;
    mSettings = new IDontWant2SeeSettings();
    mDateWasSet = false;
  }

  static IDontWant2See getInstance() {
    return mInstance;
  }

  public void handleTvDataUpdateFinished() {
    clearCache();
    setCurrentDate();
    mDateWasSet = false;

    for(IDontWant2SeeListEntry entry : mSettings.getSearchList()) {
      entry.resetDateWasSetFlag();
    }
  }

  void clearCache() {
    mMatchCache = new HashMap<Program, Boolean>(1000);
  }

  private static void setCurrentDate() {
    mCurrentDate = Date.getCurrentDate();
  }
  
  public ImportanceValue getImportanceValueForProgram(Program p) {
    if(!acceptInternal(p)) {
      return new ImportanceValue((byte)1,mSettings.getProgramImportance());
    }
    
    return new ImportanceValue((byte)1,Program.DEFAULT_PROGRAM_IMPORTANCE);
  }
  
  boolean acceptInternal(final Program program) {
    if(!mDateWasSet) {
      mSettings.setLastUsedDate(getCurrentDate());
      mDateWasSet = true;
    }
    final Boolean result = mMatchCache.get(program);
		if (result != null) {
			return result;
		}

    // calculate lower case title only once, not for each entry again
    final String title = program.getTitle();
    final String lowerCaseTitle = title.toLowerCase();
    for(IDontWant2SeeListEntry entry : mSettings.getSearchList()) {
      if (entry.matchesProgramTitle(title, lowerCaseTitle)) {
        return putCache(program, false);
      }
    }

    return putCache(program, true);
  }

  private boolean putCache(final Program program, final boolean matches) {
		mMatchCache.put(program, matches);
		return matches;
	}

	public PluginInfo getInfo() {
		return new PluginInfo(
        IDontWant2See.class,
        mLocalizer.msg("name", "I don't want to see!"),
        mLocalizer
            .msg(
                "desc",
                "Removes all programs with an entered search text in the title from the program table."),
        "Ren\u00e9 Mach", "GPL");
  }

  int getSearchTextIndexForProgram(final Program program) {
    if (program != null) {
      // calculate lower case title only once, not for each entry again
      final String title = program.getTitle();
      final String lowerCaseTitle = title.toLowerCase();
      for(int i = 0; i < mSettings.getSearchList().size(); i++) {
        if (mSettings.getSearchList().get(i).matchesProgramTitle(title, lowerCaseTitle)) {
          return i;
        }
      }
    }

    return -1;
  }

  public ActionMenu getButtonAction() {
    final ContextMenuAction baseAction = new ContextMenuAction(mLocalizer.msg(
        "name", "I don't want to see!"), createImageIcon("apps",
        "idontwant2see", 16));

    final ContextMenuAction openExclusionList = new ContextMenuAction(
        mLocalizer.msg("editExclusionList", "Edit exclusion list"),
        createImageIcon("apps", "idontwant2see", 16));
    openExclusionList.putValue(Plugin.BIG_ICON, createImageIcon("apps","idontwant2see",22));
    openExclusionList.setActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final Window w = UiUtilities.getLastModalChildOf(getParentFrame());

        JDialog temDlg = null;

        if(w instanceof JDialog) {
          temDlg = new JDialog((JDialog)w,true);
        }
        else {
          temDlg = new JDialog((JFrame)w,true);
        }

        final JDialog exclusionListDlg = temDlg;
        exclusionListDlg.setTitle(mLocalizer
            .msg("name", "I don't want to see!")
            + " - "
            + mLocalizer.msg("editExclusionList", "Edit exclusion list"));

        UiUtilities.registerForClosing(new WindowClosingIf() {
          public void close() {
            exclusionListDlg.dispose();
          }

          public JRootPane getRootPane() {
            return exclusionListDlg.getRootPane();
          }
        });

        final ExclusionTablePanel exclusionPanel = new ExclusionTablePanel(mSettings);

        final JButton ok = new JButton(Localizer
            .getLocalization(Localizer.I18N_OK));
        ok.addActionListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            exclusionPanel.saveSettings(mSettings);
            exclusionListDlg.dispose();
          }
        });

        final JButton cancel = new JButton(Localizer
            .getLocalization(Localizer.I18N_CANCEL));
        cancel.addActionListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            exclusionListDlg.dispose();
          }
        });

        final FormLayout layout = new FormLayout(
            "0dlu:grow,default,3dlu,default",
            "fill:500px:grow,2dlu,default,5dlu,default");
        layout.setColumnGroups(new int[][] {{2,4}});

        final CellConstraints cc = new CellConstraints();
        final PanelBuilder pb = new PanelBuilder(layout,
            (JPanel) exclusionListDlg
            .getContentPane());
        pb.setDefaultDialogBorder();

        pb.add(exclusionPanel, cc.xyw(1,1,4));
        pb.addSeparator("", cc.xyw(1,3,4));
        pb.add(ok, cc.xy(2,5));
        pb.add(cancel, cc.xy(4,5));

        layoutWindow("exclusionListDlg", exclusionListDlg, new Dimension(600,
            450));
        exclusionListDlg.setVisible(true);
      }
    });

    final ContextMenuAction undo = new ContextMenuAction(mLocalizer.msg(
        "undoLastExclusion", "Undo last exclusion"), createImageIcon("actions",
        "edit-undo", 16));
    undo.putValue(Plugin.BIG_ICON, createImageIcon("actions","edit-undo",22));
    undo.setActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        String lastEnteredExclusionString = mSettings.getLastEnteredExclusionString();
        if(lastEnteredExclusionString.length() > 0) {
          for(int i = mSettings.getSearchList().size()-1; i >= 0; i--) {
            if(mSettings.getSearchList().get(i).getSearchText().equals(lastEnteredExclusionString)) {
              mSettings.getSearchList().remove(i);
            }
          }

          mSettings.setLastEnteredExclusionString("");

          updateFilter(true);
        }
      }
    });

    return new ActionMenu(baseAction,new Action[] {openExclusionList,undo});
  }

  public ActionMenu getContextMenuActions(final Program p) {
    if (p == null) {
      return null;
    }
    // check if this program is already hidden
    final int index = getSearchTextIndexForProgram(p);

    // return menu to hide the program
    if (index == -1 || p.equals(getPluginManager().getExampleProgram())) {
      AbstractAction actionDontWant = getActionDontWantToSee(p);

      if (mSettings.isSimpleMenu() && !mCtrlPressed) {
        final Matcher matcher = PATTERN_TITLE_PART.matcher(p.getTitle());
        if (matcher.matches()) {
          actionDontWant = getActionInputTitle(p, matcher.group(2));
        }
        actionDontWant.putValue(Action.NAME,mLocalizer.msg("name","I don't want to see!"));
        actionDontWant.putValue(Action.SMALL_ICON,createImageIcon("apps","idontwant2see",16));

        return new ActionMenu(actionDontWant);
      }
      else {
        final AbstractAction actionInput = getActionInputTitle(p, null);
        final ContextMenuAction baseAction = new ContextMenuAction(mLocalizer
            .msg("name", "I don't want to see!"),
            createImageIcon("apps","idontwant2see",16));

        return new ActionMenu(baseAction, new Action[] {actionDontWant,actionInput});
      }
    }

    // return menu to show the program
    return new ActionMenu(getActionShowAgain(p));
  }

  private ContextMenuAction getActionShowAgain(final Program p) {
    return new ContextMenuAction(mLocalizer
        .msg("menu.reshow", "I want to see!"), createImageIcon("actions",
        "edit-paste", 16)) {
      public void actionPerformed(final ActionEvent e) {
      	mSettings.showAgain(p);
        updateFilter(!mSettings.isSwitchToMyFilter());
      }
    };
  }

  private AbstractAction getActionInputTitle(final Program p, final String part) {
    return new AbstractAction(mLocalizer.msg("menu.userEntered",
        "User entered value")) {
      public void actionPerformed(final ActionEvent e) {
        final JCheckBox caseSensitive = new JCheckBox(mLocalizer.msg(
            "caseSensitive",
            "case sensitive"));
        String title = p.getTitle();
        ArrayList<String> items = new ArrayList<String>();
        if (!StringUtils.isEmpty(part)) {
          String shortTitle = title.trim().substring(0, title.length() - part.length()).trim();
          shortTitle = StringUtils.removeEnd(shortTitle, "-").trim();
          shortTitle = StringUtils.removeEnd(shortTitle, "(").trim();
          items.add(shortTitle + "*");
        }
        int index = title.indexOf(" - ");
        if (index > 0) {
          items.add(title.substring(0, index).trim()+"*");
        }
        items.add(title);
        index = title.lastIndexOf(':');
        if (index > 0) {
          items.add(title.substring(0, index).trim()+"*");
        }
        final JComboBox input = new JComboBox(items.toArray(new String[items.size()]));
        input.setEditable(true);

        input.addAncestorListener(new AncestorListener() {
          public void ancestorAdded(final AncestorEvent event) {
            event.getComponent().requestFocus();
          }

          public void ancestorMoved(final AncestorEvent event) {
          }

          public void ancestorRemoved(final AncestorEvent event) {
          }
        });

        if (JOptionPane.showConfirmDialog(UiUtilities
            .getLastModalChildOf(getParentFrame()), new Object[] {
            mLocalizer.msg("exclusionText",
                "What should be excluded? (You can use the wildcard *)"),
            input, caseSensitive }, mLocalizer.msg("exclusionTitle",
            "Exclusion value entering"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
          String test = "";

          String result = (String) input.getSelectedItem();
          if (result != null) {
            test = result.replaceAll("\\*+", "\\*").trim();

            if (test.length() >= 0 && !test.equals("*")) {
              mSettings.getSearchList().add(new IDontWant2SeeListEntry(result,
                  caseSensitive.isSelected()));
              mSettings.setLastEnteredExclusionString(result);
              updateFilter(!mSettings.isSwitchToMyFilter());
            }
          }

          if (test.trim().length() <= 1) {
            JOptionPane.showMessageDialog(UiUtilities
                .getLastModalChildOf(getParentFrame()), mLocalizer.msg(
                "notValid", "The entered text is not valid."), Localizer
                .getLocalization(Localizer.I18N_ERROR),
                JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    };
  }

  private AbstractAction getActionDontWantToSee(final Program p) {
    return new AbstractAction(mLocalizer.msg("menu.completeCaseSensitive",
        "Complete title case-sensitive")) {
      public void actionPerformed(final ActionEvent e) {
        mSettings.getSearchList().add(new IDontWant2SeeListEntry(p.getTitle(), true));
        mSettings.setLastEnteredExclusionString(p.getTitle());
        updateFilter(!mSettings.isSwitchToMyFilter());
      }
    };
  }

  void updateFilter(final boolean update) {
    clearCache();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if(!update) {
          getPluginManager().getFilterManager().setCurrentFilter(getFilter());
        }
        else {
          getPluginManager().getFilterManager().setCurrentFilter(getPluginManager().getFilterManager().getCurrentFilter());
        }
      }
    });

    saveMe();
  }

  public PluginsProgramFilter[] getAvailableFilter() {
    return new PluginsProgramFilter[] {getFilter()};
  }

  private PluginsProgramFilter getFilter() {
    if (mFilter == null) {
      mFilter = new PluginsProgramFilter(mInstance) {
        public String getSubName() {
          return "";
        }

        public boolean accept(final Program prog) {
          return acceptInternal(prog);
        }
      };
    }
    return mFilter;
  }

  public void readData(final ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    final int version = in.readInt(); // read Version

    int n = in.readInt();

    if(version <= 2) {
      for(int i = 0; i < n; i++) {
        final StringBuilder value = new StringBuilder("*");
        value.append(in.readUTF()).append("*");

        mSettings.getSearchList().add(new IDontWant2SeeListEntry(value.toString(),false));
      }

      if(version == 2) {
        n = in.readInt();

        for(int i = 0; i < n; i++) {
          mSettings.getSearchList().add(new IDontWant2SeeListEntry(in.readUTF(),true));
        }

        mSettings.setSimpleMenu(false);
      }
    }
    else {
      for(int i = 0; i < n; i++) {
        mSettings.getSearchList().add(new IDontWant2SeeListEntry(in, version));
      }

      mSettings.setSimpleMenu(in.readBoolean());

      if(version >= 4) {
        mSettings.setSwitchToMyFilter(in.readBoolean());
      }
      if(version >= 5) {
        mSettings.setLastEnteredExclusionString(in.readUTF());
      }
      if(version >= 6) {
        mSettings.setLastUsedDate(Date.readData(in));
      }
      if(version >= 7) {
        mSettings.setProgramImportance(in.readByte());
      }
      else {
        mSettings.setProgramImportance(Program.DEFAULT_PROGRAM_IMPORTANCE);
      }
    }
  }

  public void writeData(final ObjectOutputStream out) throws IOException {
    // sort the list so that often used entries are in the front (for faster lookup)
    Collections.sort(mSettings.getSearchList(), new Comparator<IDontWant2SeeListEntry>() {

      public int compare(IDontWant2SeeListEntry o1, IDontWant2SeeListEntry o2) {
        return o2.getLastMatchedDate().compareTo(o1.getLastMatchedDate());
      }
    });

    out.writeInt(7); //version
    out.writeInt(mSettings.getSearchList().size());

    for(IDontWant2SeeListEntry entry : mSettings.getSearchList()) {
      entry.writeData(out);
    }

    out.writeBoolean(mSettings.isSimpleMenu());
    out.writeBoolean(mSettings.isSwitchToMyFilter());

    out.writeUTF(mSettings.getLastEnteredExclusionString());

    mSettings.getLastUsedDate().writeData((DataOutput)out);

    out.writeByte(mSettings.getProgramImportance());
  }

  public SettingsTab getSettingsTab() {
    return new IDontWant2SeeSettingsTab(mSettings);
  }

  static Date getCurrentDate() {
    return mCurrentDate;
  }

  @SuppressWarnings("unchecked")
  public Class<? extends PluginsFilterComponent>[] getAvailableFilterComponentClasses() {
    return (Class<? extends PluginsFilterComponent>[]) new Class[] {IDontWant2SeeFilterComponent.class};
  }

  @Override
  public boolean canReceiveProgramsWithTarget() {
    return true;
  }

  @Override
  public ProgramReceiveTarget[] getProgramReceiveTargets() {
    return new ProgramReceiveTarget[] { new ProgramReceiveTarget(this,
        mLocalizer.msg("programTarget", "Exclude programs"),
        RECEIVE_TARGET_EXCLUDE_EXACT) };
  }

  @Override
  public boolean receivePrograms(final Program[] programArr,
      final ProgramReceiveTarget receiveTarget) {
    if (receiveTarget.getTargetId().equals(RECEIVE_TARGET_EXCLUDE_EXACT)) {
      if (programArr.length > 0) {
        for (Program program : programArr) {
          if (getSearchTextIndexForProgram(program) == -1) {
            mSettings.getSearchList()
                .add(new IDontWant2SeeListEntry(program.getTitle(), true));
            mSettings.setLastEnteredExclusionString(program.getTitle());
          }
        }
        updateFilter(!mSettings.isSwitchToMyFilter());
      }
      return true;
    }
    return false;
  }

  @Override
  public void onActivation() {
    mFilter = new PluginsProgramFilter(this) {
      public String getSubName() {
        return "";
      }

      public boolean accept(final Program prog) {
        return acceptInternal(prog);
      }
    };

    Toolkit.getDefaultToolkit().addAWTEventListener(this,
        AWTEvent.KEY_EVENT_MASK);
  }

  @Override
  public void onDeactivation() {
    Toolkit.getDefaultToolkit().removeAWTEventListener(this);
  }

  public void eventDispatched(final AWTEvent event) {
    if (event instanceof KeyEvent) {
      final KeyEvent keyEvent = (KeyEvent) event;
      mCtrlPressed = keyEvent.isControlDown();
      // System.out.println("Ctrl " + mCtrlPressed);
    }
  }
  
  public String getPluginCategory() {
    //Plugin.OTHER_CATEGORY
    return "misc";
  }

}
