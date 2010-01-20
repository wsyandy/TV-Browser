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
package mediathekplugin;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import mediathekplugin.parser.ARDParser;
import mediathekplugin.parser.AbstractParser;
import mediathekplugin.parser.IParser;
import mediathekplugin.parser.NRKParser;
import mediathekplugin.parser.ZDFParser;
import util.browserlauncher.Launch;
import util.io.IOUtilities;
import util.ui.UiUtilities;
import util.ui.html.HTMLTextHelper;
import devplugin.ActionMenu;
import devplugin.Channel;
import devplugin.ContextMenuAction;
import devplugin.Date;
import devplugin.Plugin;
import devplugin.PluginInfo;
import devplugin.PluginTreeNode;
import devplugin.Program;
import devplugin.ProgramFilter;
import devplugin.SettingsTab;
import devplugin.ThemeIcon;
import devplugin.Version;

/**
 * @author Bananeweizen
 * 
 */
public class MediathekPlugin extends Plugin {

  private static final boolean IS_STABLE = false;

  private static final Version PLUGIN_VERSION = new Version(2, 72, IS_STABLE);

  /** The localizer used by this class. */
  private static final util.ui.Localizer mLocalizer = util.ui.Localizer
      .getLocalizerFor(MediathekPlugin.class);

  private HashMap<String, List<MediathekProgram>> mPrograms;

  // cached ordered copy of the programs map
  private MediathekProgram[] mSortedPrograms;

  private boolean mSorted = false;

  private Icon markIcon, contextIcon, pluginIconSmall, pluginIconLarge,
      mIconWeb;

  /** location of the dialog */
  private Point mLocation = null;

  /** size of Dialog */
  private Dimension mSize = null;

  private MediathekSettings mSettings;

  private PluginTreeNode rootNode = new PluginTreeNode(this, false);

  private static MediathekPlugin instance = null;

  /** The logger for this class */
  private static final Logger logger = Logger
      .getLogger(MediathekPlugin.class.getName());

  public static Version getVersion() {
    return PLUGIN_VERSION;
  }

  private AbstractParser[] mParsers = new AbstractParser[0];

  private static final Icon[] EMPTY_ICON_LIST = {};

  @Override
  public PluginInfo getInfo() {
    final String name = mLocalizer.msg("name", "Mediathek");
    final String description = mLocalizer.msg("description",
        "Shows video information for the ZDF Mediathek.");
    return new PluginInfo(MediathekPlugin.class, name, description,
        "Michael Keppler", "GPL 3");
  }

  public MediathekPlugin() {
    rememberInstance(this);
    mPrograms = new HashMap<String, List<MediathekProgram>>();
    pluginIconSmall = createImageIcon("actions", "web-search", 16);
    pluginIconLarge = createImageIcon("actions", "web-search", 22);
    contextIcon = createImageIcon("actions", "web-search", 16);
    markIcon = contextIcon;
    mIconWeb = createImageIcon("apps", "internet-web-browser", 16);
    rootNode.setGroupingByDateEnabled(false);
  }

  private static void rememberInstance(final MediathekPlugin plugin) {
    instance = plugin;
  }

  @Override
  public ActionMenu getContextMenuActions(final Program program) {
    // pseudo action for example program
    if (program.equals(getPluginManager().getExampleProgram())) {
      return new ActionMenu(new AbstractAction(mLocalizer.msg("name",
          "Mediathek"), getContextMenuIcon()) {
        public void actionPerformed(final ActionEvent e) {
          // empty
        }
      });
    }
    // do we support the channel at all?
    if (!isSupportedChannel(program.getChannel())) {
      return null;
    }
    // get mediathek contents, if not yet loaded
    if (mPrograms.isEmpty()) {
      return actionMenuReadMediathekContents();
    }
    // do we have any media?
    final MediathekProgram mediaProgram = findProgram(program);
    if (mediaProgram == null) {
      return null;
    }
    // now create a menu
    if (mediaProgram.canReadEpisodes()) {
      if (mediaProgram.getItemCount() == -1) {
        return actionMenuReadEpisodes(mediaProgram);
      } else {
        return mediaProgram.actionMenuShowEpisodes();
      }
    } else {
      return new ActionMenu(new LaunchBrowserAction(mediaProgram.getUrl(),
          mLocalizer.msg("action.browseProgram", "Show Mediathek")));
    }
  }

  public boolean isSupportedChannel(final Channel channel) {
    for (IParser parser : mParsers) {
      if (parser.isSupportedChannel(channel)) {
        return true;
      }
    }
    return false;
  }

  protected Icon getContextMenuIcon() {
    return contextIcon;
  }

  private ActionMenu actionMenuReadEpisodes(final MediathekProgram mediaProgram) {
    final AbstractAction actionSeries = new AbstractAction(mLocalizer.msg(
        "action.readEpisodes", "Search items in the Mediathek"),
        getContextMenuIcon()) {

      public void actionPerformed(final ActionEvent event) {
        mediaProgram.readEpisodes();
      }
    };
    return new ActionMenu(actionSeries);
  }

  private ActionMenu actionMenuReadMediathekContents() {
    final AbstractAction searchMedia = new AbstractAction(mLocalizer.msg(
        "action.readContents", "Read all Mediathek programs"),
        getContextMenuIcon()) {

      public void actionPerformed(final ActionEvent event) {
        readMediathekContents();
      }
    };
    return new ActionMenu(searchMedia);
  }

  public void addProgram(final IParser parser, final String title,
      final String url) {
    final String key = title.toLowerCase();
    final List<MediathekProgram> list = mPrograms.get(key);
    if (list != null) {
      for (MediathekProgram mediathekProgram : list) {
        if (mediathekProgram.getParser() == parser) {
          return;
        }
      }
    }
    addProgram(new MediathekProgram(parser, title, url));
  }

  private void addProgram(final MediathekProgram program) {
    final String key = getProgramKey(program);
    List<MediathekProgram> list = mPrograms.get(key);
    if (list == null) {
      list = new ArrayList<MediathekProgram>(4);
      mPrograms.put(key, list);
    }
    list.add(program);
    mSorted = false;
  }

  private String getProgramKey(final MediathekProgram program) {
    return program.getLowerCaseTitle();
  }

  @Override
  public ThemeIcon getMarkIconFromTheme() {
    return new ThemeIcon("apps", "internet-web-browser", 16);
  }

  @Override
  public String getProgramTableIconText() {
    return mLocalizer.msg("programTableIconText", "Mediathek");
  }

  @Override
  public Icon[] getProgramTableIcons(final Program program) {
    if (mPrograms.isEmpty()) {
      return EMPTY_ICON_LIST;
    }
    if (!isSupportedChannel(program.getChannel())) {
      return EMPTY_ICON_LIST;
    }
    final MediathekProgram mediaProgram = findProgram(program);
    if (mediaProgram != null) {
      mediaProgram.readEpisodes();
      return new Icon[] { markIcon };
    }
    return EMPTY_ICON_LIST;
  }

  /**
   * finds a program in the list of online programs which matches the given
   * TV-Browser program
   * 
   * @param program
   * @return
   */
  private MediathekProgram findProgram(final Program program) {
    if (mPrograms == null) {
      return null;
    }
    String title = program.getTitle().toLowerCase();
    final Channel channel = program.getChannel();
    MediathekProgram mediathekProgram = findProgram(channel, title);
    if (mediathekProgram == null && title.endsWith(")") && title.contains("(")) {
      title = title.substring(0, title.lastIndexOf('(') - 1);
      mediathekProgram = findProgram(channel, title);
    }
    if (mediathekProgram == null && title.endsWith("...")) {
      title = title.substring(0, title.length() - 3).trim();
      mediathekProgram = findProgram(channel, title);
    }
    // now check also for partial title matches. partial titles are constructed
    // by adding each word of the title until a match is found or 3 words are
    // reached
    if (mediathekProgram == null && title.indexOf(' ') >= 0) {
      final String[] parts = title.split(" ");
      final StringBuilder builder = new StringBuilder();
      for (int i = 0; i <= Math.min(parts.length - 1, 2); i++) {
        if (i > 0) {
          builder.append(' ');
        }
        builder.append(parts[i]);
        if (builder.length() > 5) {
          final String partTitle = builder.toString();
          mediathekProgram = findProgram(channel, partTitle);
          if (mediathekProgram != null) {
            return mediathekProgram;
          }
        }
      }
    }
    return mediathekProgram;
  }

  private MediathekProgram findProgram(final Channel channel, final String title) {
    final List<MediathekProgram> list = mPrograms.get(title);
    if (list == null) {
      return null;
    }
    for (MediathekProgram mediathekProgram : list) {
      if (mediathekProgram.supportsChannel(channel)) {
        return mediathekProgram;
      }
    }
    return null;
  }

  @Override
  public ActionMenu getButtonAction() {
    final ContextMenuAction menuAction = new ContextMenuAction("Mediathek",
        pluginIconSmall);
    final ArrayList<Action> actionList = new ArrayList<Action>(4);
    /*
     * final Action dialogAction = new AbstractAction("Mediathek",
     * pluginIconSmall) {
     * 
     * public void actionPerformed(final ActionEvent e) { showDialog(); } };
     * dialogAction.putValue(Plugin.BIG_ICON, pluginIconLarge);
     * actionList.add(dialogAction);
     * 
     * if (TVBrowser.VERSION.getMajor() >= 3) {
     * actionList.add(ContextMenuSeparatorAction.getInstance()); }
     */
    actionList.add(new AbstractAction("ARD Mediathek",
        createImageIcon("mediathekplugin/icons/ard.png")) {

      public void actionPerformed(final ActionEvent e) {
        Launch.openURL("http://www.ardmediathek.de/");
      }
    });

    actionList.add(new AbstractAction("ZDFmediathek",
        createImageIcon("mediathekplugin/icons/zdf.png")) {

      public void actionPerformed(final ActionEvent e) {
        Launch
            .openURL("http://www.zdf.de/ZDFmediathek/content/9602?inPopup=true");
      }
    });
    actionList.add(new AbstractAction("NRK Nett-TV",
        createImageIcon("mediathekplugin/icons/nrk.png")) {

      public void actionPerformed(final ActionEvent e) {
        Launch
            .openURL("http://www1.nrk.no/nett-tv");
      }
    });
    
    // set tooltip similar to name
    for (Action action : actionList) {
      action.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.NAME));
      action.putValue(Action.LONG_DESCRIPTION, action.getValue(Action.NAME));
    }

    return new ActionMenu(menuAction, actionList.toArray());
  }

  private void showDialog() {
    final ProgramsDialog dlg = new ProgramsDialog(getParentFrame());

    dlg.pack();
    dlg.addComponentListener(new java.awt.event.ComponentAdapter() {

      public void componentResized(final ComponentEvent e) {
        mSize = e.getComponent().getSize();
      }

      public void componentMoved(final ComponentEvent e) {
        e.getComponent().getLocation(mLocation);
      }
    });

    if ((mLocation != null) && (mSize != null)) {
      dlg.setLocation(mLocation);
      dlg.setSize(mSize);
      dlg.setVisible(true);
    } else {
      dlg.setSize(600, 600);
      UiUtilities.centerAndShow(dlg);
      mLocation = dlg.getLocation();
      mSize = dlg.getSize();
    }
  }

  public static MediathekPlugin getInstance() {
    return instance;
  }

  @Override
  public SettingsTab getSettingsTab() {
    return new MediathekSettingsTab(mSettings);
  }

  @Override
  public void handleTvBrowserStartFinished() {
    if (mSettings.isReadEpisodesOnStart()) {
      readMediathekContents();
    }
  }

  @Override
  public void loadSettings(final Properties properties) {
    mSettings = new MediathekSettings(properties);
  }

  @Override
  public Properties storeSettings() {
    return mSettings.storeSettings();
  }

  private void readMediathekContents() {
    final Thread contentThread = new Thread("Read Mediathek contents") {
      @Override
      public void run() {
        for (AbstractParser reader : mParsers) {
          if (reader.hasSubscribedChannels()) {
            reader.readContents();
          }
        }
        updatePluginTree();
        // update programs of current day to force their icons to show
        final ArrayList<Program> validationPrograms = new ArrayList<Program>(128);
        final ProgramFilter currentFilter = getPluginManager().getFilterManager()
            .getCurrentFilter();
        // have outer loop iterate over days so that all programs of today are loaded first
        for (int days = 0; days < 30; days++) {
          final Date date = getPluginManager().getCurrentDate().addDays(days);
          for (Channel channel : getPluginManager().getSubscribedChannels()) {
            if (isSupportedChannel(channel)) {
              final Iterator<Program> iter = Plugin.getPluginManager()
                  .getChannelDayProgram(date, channel);
              if (iter != null) {
                while (iter.hasNext()) {
                  final Program program = iter.next();
                  // first search mediathek, then filter -> typically better
                  // performance
                  final MediathekProgram mediaProgram = findProgram(program);
                  if (mediaProgram != null && currentFilter.accept(program)) {
                    mediaProgram.readEpisodes();
                    validationPrograms.add(program);
                  }
                }
              }
            }
          }
        }
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            for (Program program : validationPrograms) {
              program.validateMarking();
            }
          }});
      }
    };
    contentThread.setPriority(Thread.MIN_PRIORITY);
    contentThread.start();
  }

  public String convertHTML(final String html) {
    String result = HTMLTextHelper.convertHtmlToText(html);
    result = IOUtilities.replace(result, "&amp;", "&");
    return result;
  }

  @Override
  public boolean canUseProgramTree() {
    return true;
  }

  @Override
  public PluginTreeNode getRootNode() {
    return rootNode;
  }

  /**
   * Updates the plugin tree.
   */
  private void updatePluginTree() {
    final PluginTreeNode node = getRootNode();
    node.removeAllActions();
    node.removeAllChildren();
    node.getMutableTreeNode().setShowLeafCountEnabled(false);
    node.addAction(new AbstractAction(mLocalizer.msg("action.readAll",
        "Read all episodes")) {

      public void actionPerformed(final ActionEvent e) {
        for (MediathekProgram program : getSortedPrograms()) {
          program.readEpisodes();
        }
      }
    });
    for (MediathekProgram program : getSortedPrograms()) {
      program.updatePluginTree(false);
    }
    node.update();
  }

  protected MediathekProgram[] getSortedPrograms() {
    if (!mSorted) {
      final ArrayList<MediathekProgram> sorted = new ArrayList<MediathekProgram>(
          mPrograms.size());
      for (List<MediathekProgram> list : mPrograms.values()) {
        sorted.addAll(list);
      }
      mSortedPrograms = new MediathekProgram[sorted.size()];
      sorted.toArray(mSortedPrograms);
      Arrays.sort(mSortedPrograms);
      this.mSorted = true;
    }
    return mSortedPrograms;
  }

  public Logger getLogger() {
    return logger;
  }

  public Icon getPluginIcon() {
    return pluginIconSmall;
  }

  public Icon getWebIcon() {
    return mIconWeb;
  }

  protected Frame getFrame() {
    return this.getParentFrame();
  }

  @Override
  public void onActivation() {
    mParsers = new AbstractParser[] { new ZDFParser(), new ARDParser(), new NRKParser() };
  }

}