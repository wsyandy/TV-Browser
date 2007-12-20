/*
 * SweDBTvDataService.java
 *
 * Created on den 31 oktober 2005, 13:09
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package swedbtvdataservice;

import java.awt.Image;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.net.URL;
import java.net.HttpURLConnection;

import java.nio.channels.FileChannel;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import devplugin.ChannelGroup;
import devplugin.Channel;
import devplugin.Date;
import devplugin.Plugin;
import devplugin.PluginInfo;
import devplugin.Version;
import devplugin.ProgressMonitor;
import tvdataservice.MutableChannelDayProgram;

import tvdataservice.SettingsPanel;
import tvdataservice.TvDataUpdateManager;
import util.exc.TvBrowserException;
import util.ui.ImageUtilities;
import util.ui.Localizer;

/**
 * 
 * @author Inforama
 * 
 */

public class SweDBTvDataService extends devplugin.AbstractTvDataService {

  public static final Localizer mLocalizer = Localizer
      .getLocalizerFor(SweDBTvDataService.class);

  private Properties mProperties;

  private static SweDBTvDataService mInstance;

  private File mWorkingDirectory;

  private HashSet<SweDBChannelGroup> mChannelGroupsList;

  private SweDBChannelContainer[] mInternalChannel;

  private long mLastChannelUpdate = -1;

  private Channel[] mChannel;

  private boolean initialConversionDone = false;

  private static WeakHashMap<String, File> ICON_CACHE = new WeakHashMap<String, File>();

  private static java.util.logging.Logger mLog = java.util.logging.Logger
      .getLogger(SweDBTvDataService.class.getName());

  private boolean mHasRightToDownloadIcons;

  /** Creates a new instance of SweDBTvDataService */
  public SweDBTvDataService() {
    mLog.info("SweDBTvDataService initieras");
    mHasRightToDownloadIcons = false;
    mProperties = new Properties();

    mChannelGroupsList = new HashSet<SweDBChannelGroup>();
    mChannelGroupsList.clear();
    mChannelGroupsList.add(new SweDBChannelGroup(mLocalizer.msg(
        "ChannelGroup.descriptionName", "SweDB channelgroup"), "SweDB",
        mLocalizer.msg("ChannelGroup.description",
            "Channels from the XMLTV-site tv.swedb.se"), "SweDB"));

    mInstance = this;

  }

  public static SweDBTvDataService getInstance() {
    if (mInstance == null) {
      throw new RuntimeException(
          "No instance of SweDBTvDataService class available");
    }
    return mInstance;
  }

  public boolean supportsDynamicChannelList() {
    return true;
  }

  public boolean supportsDynamicChannelGroups() {
    return true;
  }

  public boolean hasSettingsPanel() {
    return false;
  }

  public SettingsPanel getSettingsPanel() {
    return null;
  }

  public void setWorkingDirectory(File dataDir) {
    mLog.info("SweDBTvDataService setting directory to " + dataDir.toString());
    mWorkingDirectory = dataDir;
  }

  /**
   * @return an array of the available channel groups.
   * 
   */
  public ChannelGroup[] getAvailableGroups() {
    return mChannelGroupsList.toArray(new SweDBChannelGroup[mChannelGroupsList
        .size()]);
  }

  /**
   * Updates the TV listings provided by this data service.
   * 
   * @throws util.exc.TvBrowserException
   */
  public void updateTvData(TvDataUpdateManager updateManager,
      Channel[] channelArr, Date startDate, int dateCount,
      ProgressMonitor monitor) throws TvBrowserException {
    mHasRightToDownloadIcons = true;

    int counter = 0;
    mLog.info("Starting update for SweDBTvDataService from "
        + startDate.toString() + " for " + dateCount + " days");
    monitor.setMaximum(channelArr.length);
    devplugin.Date testStart = new devplugin.Date(startDate);
    for (Channel channel : channelArr) {
      for (int c = 0; c < mChannel.length; c++) {
        if (mChannel[c].equals(channel)) {
          ArrayList<Date> modifiedDates = new ArrayList<Date>();
          monitor.setMessage(mLocalizer.msg("updateTvData.progressmessage.10",
              "{2}: Searching for updated/new programs on {0} for {1} days",
              startDate.toString(), "" + dateCount, mChannel[c].getName()));
          for (int b = 0; b < dateCount; b++) {
            devplugin.Date testDay = testStart.addDays(b);
            String fileDate = createFileName(testDay);
            try {
              String urlString = mInternalChannel[c].getBaseUrl()
                  + mInternalChannel[c].getId() + "_" + fileDate + ".xml.gz";
              URL url = new URL(urlString);
              HttpURLConnection conn = (HttpURLConnection) url.openConnection();
              conn.setReadTimeout(Plugin.getPluginManager()
                  .getTvBrowserSettings().getDefaultNetworkConnectionTimeout());
              conn.setIfModifiedSince(mInternalChannel[c]
                  .getLastUpdate(testDay));
              conn.setRequestMethod("HEAD"); // Only make a HEAD request, to
                                              // see if the file has been
                                              // changed (only get the HTTP
                                              // Header fields).
              if (conn.getResponseCode() == 200) {
                modifiedDates.add(testDay);
              }
              mLog.info("mInternalChannel.lastModified="
                  + mInternalChannel[c].getLastUpdate(testDay));
            } catch (Exception e) {
              throw new TvBrowserException(SweDBTvDataService.class,
                  "An error occurred in updateTvData",
                  "Please report this to the developer");
            }
          } // int b
          mLog.info("Number of modified days for channel "
              + mInternalChannel[c].getName() + ":" + modifiedDates.size());
          monitor.setMessage(mLocalizer.msg("updateTvData.progressmessage.20",
              "{0}: Retrieving updated/new programs.", mChannel[c].getName()));
          /*********************************************************************
           * IF we found any modified/missing data, we have to download the
           * files before and after each date, since the XMLTV-data files are
           * split in UTC-time while provider is in a different timezone. This
           * procedure ensures that we get all of the data. (There is a small
           * risk that we miss updated data, since we do not verify if the files
           * before and after the actual date has been modified - but we do not
           * care for now)
           ********************************************************************/
          if (modifiedDates.size() > 0) {
            devplugin.Date prevDate;
            devplugin.Date currDate;

            Hashtable<String, Date> fileDates = new Hashtable<String, Date>();
            prevDate = new devplugin.Date((modifiedDates.get(0)).addDays(-1));
            fileDates.put(prevDate.getDateString(), prevDate);
            for (int j = 0; j < modifiedDates.size(); j++) {
              currDate = modifiedDates.get(j);
              if (currDate.equals(prevDate.addDays(1))) {
                if (!fileDates.containsKey(currDate.getDateString())) {
                  fileDates.put(currDate.getDateString(), currDate);
                }
              } else {
                devplugin.Date tempDate = new devplugin.Date(prevDate
                    .addDays(1));
                if (!fileDates.containsKey(tempDate.getDateString())) {
                  fileDates.put(tempDate.getDateString(), tempDate);
                }
                tempDate = new devplugin.Date(currDate.addDays(-1));
                if (!fileDates.containsKey(tempDate.getDateString())) {
                  fileDates.put(tempDate.getDateString(), tempDate);
                }
                if (!fileDates.containsKey(currDate.getDateString())) {
                  fileDates.put(currDate.getDateString(), currDate);
                }

              }
              prevDate = currDate;
            }// for j
            currDate = new devplugin.Date(prevDate.addDays(1));
            if (!fileDates.containsKey(currDate.getDateString())) {
              fileDates.put(currDate.getDateString(), currDate);
            }
            mLog.info(currDate.getDateString());

            /*******************************************************************
             * OK... So now we are ready to start parsing the selected data
             * files
             ******************************************************************/
            Hashtable<String, MutableChannelDayProgram> dataHashtable = new Hashtable<String, MutableChannelDayProgram>();
            Enumeration<Date> en = fileDates.elements();
            monitor.setMessage(mLocalizer.msg(
                "updateTvData.progressmessage.30", "{0}: Reading datafiles",
                mChannel[c].getName()));
            while (en.hasMoreElements()) {
              try {
                devplugin.Date date = (devplugin.Date) (en.nextElement());
                String strFileDate = createFileName(date);
                mLog.info("getting: " + mInternalChannel[c].getBaseUrl()
                    + mInternalChannel[c].getId() + "_" + strFileDate
                    + ".xml.gz");
                URL url = new URL(mInternalChannel[c].getBaseUrl()
                    + mInternalChannel[c].getId() + "_" + strFileDate
                    + ".xml.gz");
                HttpURLConnection con = (HttpURLConnection) url
                    .openConnection();
                con.setReadTimeout(Plugin.getPluginManager()
                    .getTvBrowserSettings()
                    .getDefaultNetworkConnectionTimeout());

                if (con.getResponseCode() == 200) {
                  SweDBDayParser.parseNew(new GZIPInputStream(con
                      .getInputStream()), channel, date, dataHashtable);
                  if (modifiedDates.contains(date)) {
                    mLog.info("Updating lastUpdate property for date "
                        + date.toString());
                    mInternalChannel[c].setLastUpdate(date, con
                        .getLastModified());
                  }
                }
              } catch (Exception E) {
                throw new TvBrowserException(SweDBTvDataService.class,
                    "An error occurred in updateTvData",
                    "Please report this to the developer");
              }
            }
            mLog.info("All of the files have been parsed");
            /*******************************************************************
             * Now all of the files has been parsed. Time to update the local
             * database with our data...
             */
            for (int modDates = 0; modDates < modifiedDates.size(); modDates++) {
              devplugin.Date date = modifiedDates.get(modDates);
              if (dataHashtable.containsKey(date.toString())) {
                mLog.info("Updating database for day " + date.toString());
                monitor.setMessage(mLocalizer.msg(
                    "updateTvData.progressmessage.40",
                    "{0}: Updating database", mChannel[c].getName()));
                updateManager.updateDayProgram(dataHashtable.get(date
                    .toString()));
              } else {
                mLog.info("Strange.... Didn't find the data for "
                    + date.toString());
              }
            }
          }
        } // if
      } // int c
      monitor.setValue(counter++);
    }// int a
    mHasRightToDownloadIcons = false;
  }

  private String createFileName(devplugin.Date fileDate) {
    String fileName = "";
    fileName = Integer.toString(fileDate.getYear()) + "-";
    // fileName = fileName + "-";
    if (fileDate.getMonth() < 10) {
      fileName = fileName + "0";
    }
    fileName = fileName + Integer.toString(fileDate.getMonth()) + "-";
    // datum = datum + "-";

    if (fileDate.getDayOfMonth() < 10) {
      fileName = fileName + "0";
    }
    fileName = fileName + Integer.toString(fileDate.getDayOfMonth());
    return fileName;
  }

  /**
   * Called by the host-application during start-up. Implement this method to
   * load your dataservices settings from the file system.
   */
  public void loadSettings(Properties settings) {
    mLog.info("Loading settings in SweDBTvDataService");
    mProperties = settings;
    mLastChannelUpdate = Long.parseLong(mProperties.getProperty(
        "LastChannelUpdate", "0"));
    int numChannels = Integer.parseInt(mProperties.getProperty(
        "NumberOfChannels", "0"));
    mInternalChannel = new SweDBChannelContainer[numChannels];
    for (int i = 0; i < numChannels; i++) {
      mInternalChannel[i] = new SweDBChannelContainer(mProperties.getProperty(
          "ChannelId-" + i, ""), mProperties.getProperty("ChannelTitle-" + i,
          ""), mProperties.getProperty("ChannelBaseUrl-" + i, ""), mProperties
          .getProperty("ChannelIconUrl-" + i, ""), mProperties.getProperty(
          "ChannelLastUpdate-" + i, ""));
    }
    // convert();
    // mLog.info("mInternalChannel now contains " + mInternalChannel.length + "
    // channels");
    // mLog.info("mChannel now contains " + mChannel.length + "channels");
    mLog.info("Finished loading settings for SweDBTvDataService");
  }

  /**
   * Called by the host-application during shut-down. Implements this method to
   * store your dataservices settings to the file system.
   */
  public Properties storeSettings() {
    mLog.info("Storing settings for SweDBTvDataService");
    mProperties.setProperty("LastChannelUpdate", Long
        .toString(mLastChannelUpdate));
    mProperties.setProperty("NumberOfChannels", Integer
        .toString(mInternalChannel.length));
    for (int i = 0; i < mInternalChannel.length; i++) {
      mProperties.setProperty("ChannelId-" + i, mInternalChannel[i].getId());
      mProperties.setProperty("ChannelTitle-" + i, mInternalChannel[i]
          .getName());
      mProperties.setProperty("ChannelBaseUrl-" + i, mInternalChannel[i]
          .getBaseUrl());
      mProperties.setProperty("ChannelIconUrl-" + i, mInternalChannel[i]
          .getIconUrl());
      mProperties.setProperty("ChannelLastUpdate-" + i, mInternalChannel[i]
          .getLastUpdateString());
    }
    mLog
        .info("Finished storing settings for SweDBTvDataService. Returning properties...");
    return mProperties;
  }

  /**
   * Gets the list of the channels that are available for the given channel
   * group.
   */
  public Channel[] getAvailableChannels(ChannelGroup group) {
    if (!initialConversionDone) {
      convert();
      initialConversionDone = true;
    }
    HashSet<Channel> mTempHashSet = new HashSet<Channel>();
    for (Channel channel : mChannel) {
      if (channel.getGroup().getId().equalsIgnoreCase(group.getId())) {
        mTempHashSet.add(channel);
      }
    }
    return mTempHashSet.toArray(new Channel[mTempHashSet.size()]);
  }

  /**
   * Some TvDataServices may need to connect to the internet to know their
   * channels. If supportsDanymicChannelList() returns true, this method is
   * called to check for availabel channels.
   * 
   * @param group
   * @param monitor
   * @throws TvBrowserException
   */
  public Channel[] checkForAvailableChannels(ChannelGroup group,
      ProgressMonitor monitor) throws TvBrowserException {
    mHasRightToDownloadIcons = true;

    if (mLastChannelUpdate == -1) {
      mLog.info("mLastChannelUpdate has not been initialized yet. checkForAvailableChannels exits");
      return mChannel;
    }
    try {
      if (monitor != null) {
        monitor.setMessage(mLocalizer.msg("Progressmessage.10",
            "Getting messages"));
      }

      URL url = new URL("http://tv.swedb.se/xmltv/channels.xml.gz");

      if (monitor != null) {
        monitor.setMessage(mLocalizer.msg("Progressmessage.20",
            "Getting channel list from")
            + " " + url.toString());
      }

      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setReadTimeout(Plugin.getPluginManager().getTvBrowserSettings()
          .getDefaultNetworkConnectionTimeout());
      con.setIfModifiedSince(mLastChannelUpdate);

      int responseCode = con.getResponseCode();
      if (responseCode == 200) {
        if (monitor != null) {
          monitor.setMessage(mLocalizer.msg("Progressmessage.30",
              "Parsing channel list"));
        }
        mInternalChannel = SweDBChannelParser.parse(new GZIPInputStream(con
            .getInputStream()));
        if (monitor != null) {
          monitor.setMessage(mLocalizer.msg("Progressmessage.40",
              "Found {0} channels, downloading channel icons...", new Integer(
                  mInternalChannel.length)));
        }
        mLastChannelUpdate = con.getLastModified();
        con.disconnect();
        convert();
        if (monitor != null) {
          monitor.setMessage(mLocalizer.msg("Progressmessage.50",
              "All channels have been retrieved"));
        }
      } else if (responseCode == 304) {
        if (monitor != null) {
          monitor.setMessage(mLocalizer.msg("Progressmessage.100",
              "The list of channels was already up to date"));
        }
      }
      else {
        throw new TvBrowserException(SweDBTvDataService.class,
            "availableResponse",
            "Unknown response during check for available channels in Swedb plugin: {0}", responseCode);
      }
    } catch (Exception e) {
      throw new TvBrowserException(SweDBTvDataService.class,
          "checkAvailableError",
          "Error checking for available channels in Swedb plugin: {0}", e
              .getMessage());
    }

    mHasRightToDownloadIcons = false;

    return mChannel;
  }

  private void convert() {
    SweDBChannelGroup[] groups = (SweDBChannelGroup[]) getAvailableGroups();
    mChannel = new Channel[mInternalChannel.length];
    if (this.mWorkingDirectory != null) {
      IconLoader iconLoader = null;
      try {
        iconLoader = new IconLoader(groups[0].getId(), this.mWorkingDirectory);
      } catch (IOException e) {
        mLog.severe("Unable to initialize IconLoader for group ID "
            + groups[0].getId() + " in working directory "
            + this.mWorkingDirectory);
      }
      for (int i = 0; i < mInternalChannel.length; i++) {
        mChannel[i] = new Channel(this, mInternalChannel[i].getName(),
            mInternalChannel[i].getId(), TimeZone.getTimeZone("CET"), "SE",
            "(c) swedb", "http://tv.swedb.se", groups[0]);
        if (!mInternalChannel[i].getIconUrl().equals("")) {
          try {
            Icon icon = iconLoader.getIcon(mInternalChannel[i].getId(),
                mInternalChannel[i].getIconUrl());
            mChannel[i].setDefaultIcon(icon);
          } catch (IOException e) {
            mLog.severe("Unable to load icon for "
                + mInternalChannel[i].getId() + " on URL "
                + mInternalChannel[i].getIconUrl());
          }

        }
        // mNumChannels = mInternalChannel.length;
      }
      try {
        iconLoader.close();
      } catch (IOException e) {
        mLog.severe("Unable to close IconLoader for group ID "
            + groups[0].getId() + " in working directory "
            + this.mWorkingDirectory);
      }
    } else {
      mLog
          .info("SweDBTvDataService: Working directory has not been initialized yet. Icons not loaded");
    }
  }

  public ChannelGroup[] checkForAvailableChannelGroups(ProgressMonitor monitor)
      throws TvBrowserException {
    return getAvailableGroups();
  }

  public static Version getVersion() {
    return new Version(2, 60);
  }

  public PluginInfo getInfo() {
    return new devplugin.PluginInfo(
        SweDBTvDataService.class,
        mLocalizer.msg("PluginInfo.name", "SweDB TV Data Service Plugin"),
        mLocalizer.msg("PluginInfo.description",
            "A TV Data Service plugin which uses XMLTV-data from TV.SWEDB.SE"),
        "Inforama",
        mLocalizer
            .msg("PluginInfo.support",
                "Support the SWEDB crew - Don't forget to register with http://tv.swedb.se/"));
  }

  class IconLoader {
    private File mIconDir;

    private File mIconIndexFile;

    private String mGroup;

    private Properties mProperties;

    public IconLoader(String group, File dir) throws IOException {
      mGroup = group;
      mIconDir = new File(dir + "/icons_" + mGroup);
      if (!mIconDir.exists()) {
        mIconDir.mkdirs();
      }
      mIconIndexFile = new File(mIconDir, "index.txt");
      mProperties = new Properties();
      if (mIconIndexFile.exists()) {
        mProperties.load(new BufferedInputStream(new FileInputStream(
            mIconIndexFile), 0x1000));
      } else {
        mLog.severe("index.txt not found in: " + mIconIndexFile.toString());
        // System.exit(-1);
      }
    }

    public Icon getIcon(String channelId, String url) throws IOException {
      String key = new StringBuffer("icons_").append(mGroup).append("_")
          .append(channelId).toString();
      String prevUrl = (String) mProperties.get(key);
      Icon icon = null;
      File iconFile = new File(mIconDir, channelId);

      if (url.equals(prevUrl)) {
        // the url hasn't changed; we should have the icon locally
        mLog.info("Found iconUrl in cache for channelId:" + channelId);
        icon = getIconFromFile(iconFile);
        return icon;
      } else {
        mLog.warning("iconUrl is not in cache for channelId " + channelId
            + ". prevUrl=" + prevUrl + ". currentUrl=" + url);
      }

      if (icon == null && mHasRightToDownloadIcons) {
        if (ICON_CACHE.containsKey(url)) {
          try {
            if (!ICON_CACHE.get(url).equals(iconFile)) {
              copyFile(ICON_CACHE.get(url), iconFile);
              icon = getIconFromFile(iconFile);
            }
          } catch (Exception e) {
            mLog.log(Level.SEVERE, "Problem while copying File from Cache", e);
          }

        }
      }

      if (icon == null) {
        // download the icon
        try {
          util.io.IOUtilities.download(new URL(url), iconFile);
          icon = getIconFromFile(iconFile);
          ICON_CACHE.put(url, iconFile);
        } catch (IOException e) {
          mLog.warning("channel " + channelId
              + ": could not download icon from " + url);
        } catch (Exception e) {
          mLog.severe("Could not extract icon file");
        }
      }
      if (icon != null) {
        mProperties.setProperty(key, url);
      }

      return icon;
    }

    /**
     * Fast Copy of a File
     * 
     * @param source
     *          Source File
     * @param dest
     *          Destination File
     */
    private void copyFile(File source, File dest) {
      try {
        // Create channel on the source
        FileChannel srcChannel = new FileInputStream(source).getChannel();

        // Create channel on the destination
        FileChannel dstChannel = new FileOutputStream(dest).getChannel();

        // Copy file contents from source to destination
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

        // Close the channels
        srcChannel.close();
        dstChannel.close();
      } catch (IOException e) {
      }
    }

    private Icon getIconFromFile(File file) {
      Image img = ImageUtilities.createImage(file.getAbsolutePath());
      if (img != null) {
        return new ImageIcon(img);
      }
      return null;
    }

    private void close() throws IOException {
      mProperties.store(new FileOutputStream(mIconIndexFile), null);
    }
  }

}
