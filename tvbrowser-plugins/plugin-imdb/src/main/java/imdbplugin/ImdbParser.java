package imdbplugin;

import devplugin.ProgressMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.net.URL;

import util.io.IOUtilities;
import util.ui.Localizer;
import util.ui.progress.ProgressInputStream;


public class ImdbParser {
  private static final Localizer mLocalizer = Localizer.getLocalizerFor(ImdbParser.class);

  private static final Pattern epsiodePattern = Pattern.compile("^(.*?)(?:\\W\\(\\#.*\\))?$");
  private static final Pattern moviePrefixPattern = Pattern.compile("(.*), ([A-Z][a-z']{0,2})");

  private ImdbDatabase mDatabase;
  private String mServer;
  private boolean mRunParser = true;

  public ImdbParser(ImdbDatabase db, String server) {
    mDatabase = db;
    mServer = server;
  }

  public void startParsing(ProgressMonitor monitor) throws IOException {
    mDatabase.deleteDatabase();

    monitor.setMaximum(getFileSize(mServer));
    monitor.setMessage(mLocalizer.msg("download", "Downloading files"));

    mRunParser = true;

    ProgressInputStream progressInputStream = new ProgressInputStream(new URL(mServer + "aka-titles.list.gz").openStream(), monitor);

    System.out.println("AKA TITLES");

    parseAkaTitles(new GZIPInputStream(progressInputStream), monitor);

    System.out.println("AKA TITLES DONE");

    optimizeDatabase(monitor);
    System.out.println("RATINGS");
    if (mRunParser) {
      progressInputStream = new ProgressInputStream(new URL(mServer + "ratings.list.gz").openStream(), monitor, progressInputStream.getCurrentPosition());
      parseRatings(new GZIPInputStream(progressInputStream), monitor);
    }
    System.out.println("RATINGS DONE");

    if (!mRunParser) {
      // Cancel was pressed, all Files have to be deleted
      mDatabase.deleteDatabase();
    } else {
      optimizeDatabase(monitor);
    }

    System.out.println("PARSING DONE");
  }

  private void optimizeDatabase(ProgressMonitor monitor) throws IOException {
    monitor.setMessage(mLocalizer.msg("optimize", "Optimizing database"));
    mDatabase.close();
    mDatabase.reOpen();
    mDatabase.optimizeIndex();
  }

  private int getFileSize(String mServer) {
    try {
      int size = 0;
      String filesizes = new String(IOUtilities.loadFileFromHttpServer(new URL(mServer + "filesizes")));

      Matcher m = Pattern.compile("^aka-titles\\.list\\W(\\d*)$", Pattern.MULTILINE).matcher(filesizes);

      if (m.find()) {
        size += Integer.parseInt(m.group(1));
      } else {
        size += 5622441;
      }

      m = Pattern.compile("^ratings\\.list\\W(\\d*)$", Pattern.MULTILINE).matcher(filesizes);

      if (m.find()) {
        size += Integer.parseInt(m.group(1));
      } else {
        size += 4512076;
      }

      return size;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 5622441 + 4512076;
  }

  public void stopParsing() {
    mRunParser = false;
  }

  private void parseAkaTitles(InputStream inputStream, ProgressMonitor monitor) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "ISO-8859-15"));
    String line = reader.readLine();

    Pattern moviePattern = Pattern.compile("^(.*)\\((?:(\\d{4,4}).*|\\?\\?\\?\\?)\\)(?:\\W*\\((.*)\\))?(?:\\W\\{(.*)\\})?.*$");
    Pattern akaPattern = Pattern.compile("^\\(aka (.*) \\((?:(\\d{4,4}).*|\\?\\?\\?\\?)\\)(?:\\W\\((.*)\\))?(?:\\W\\{(.*)\\})?\\).*$");

    String movieId = null;
    boolean startFound = false;
    int count = 0;
    while (line != null && mRunParser) {
      line = line.trim();
      if (!startFound && line.startsWith("==========")) {
        startFound = true;
      } else if (startFound) {

        if (line.length() > 0) {
          if (line.startsWith("(aka ") && movieId != null) {
            Matcher matcher = akaPattern.matcher(line);
            if (matcher.matches()) {
              String title = cleanMovieTitle(matcher.group(1).trim());
              int year = -1;
              if (matcher.group(2) != null) {
                year = Integer.parseInt(matcher.group(2));
              }
              String type = matcher.group(3);
              String episode = cleanEpsiodeTitle(matcher.group(4));

              mDatabase.addAkaTitle(movieId, title, episode, year, type);
              if (++count % 100 == 0) {
                monitor.setMessage(mLocalizer.msg("akaTitles", "Alternative title {0}",count));
              }
            }
          } else {
            Matcher matcher = moviePattern.matcher(line);
            if (matcher.matches()) {
              String movieTitle = cleanMovieTitle(matcher.group(1).trim());
              int year = -1;
              if (matcher.group(2) != null) {
                year = Integer.parseInt(matcher.group(2));
              }
              String type = matcher.group(3);                        
              String episode = cleanEpsiodeTitle(matcher.group(4));
              movieId = mDatabase.getOrCreateMovieId(movieTitle, episode, year, type);
            }
          }
        }

      }

      line = reader.readLine();
    }
    reader.close();
  }

  private void parseRatings(InputStream inputStream, ProgressMonitor monitor) throws IOException {
    Pattern ratingPattern = Pattern.compile("^(.*?)(?:\\W\\((\\d{4,4}|\\?\\?\\?\\?).*?\\))?(?:\\W\\((.*)\\))?(?:\\W\\{(.*)\\})?$");

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "ISO-8859-15"));
    String line = reader.readLine();

    boolean startFound = false;
    int count = 0;
    while (line != null && mRunParser) {
      line = line.trim();
      if (!startFound && line.startsWith("MOVIE RATINGS REPORT")) {
        startFound = true;
      } else if (startFound && line.startsWith("-------------")) {
        startFound = false;
      } else if (startFound && line.startsWith("New  Distribution  Votes  Rank  Title")) {
        // Ignore this line!
      } else if (startFound && line.length() > 0) {
        String distribution = line.substring(0, 10);
        int votes = Integer.parseInt(line.substring(11, 19).trim());
        String ratingStr = line.substring(20, 25).trim();
        int rating = Integer.parseInt(ratingStr.replaceAll("\\.", ""));
        String title = line.substring(25).trim();

        Matcher matcher = ratingPattern.matcher(title);
        if (matcher.matches()) {
          String movieTitle = cleanMovieTitle(matcher.group(1).trim());
          int year = -1;
          if (matcher.group(2) != null) {
            try {
              year = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
              // NOP
            }
          }
          String type = matcher.group(3);
          String episode = cleanEpsiodeTitle(matcher.group(4));

          mDatabase.addRating(mDatabase.getOrCreateMovieId(movieTitle, episode, year, type), rating, votes, distribution);
          if (++count % 100 == 0) {
            monitor.setMessage(mLocalizer.msg("ratings", "Rating {0}", count));
          }
        }
      }


      line = reader.readLine();
    }

    reader.close();
  }

  private String cleanEpsiodeTitle(String episode) {
    if (episode == null) {
      return null;
    }

    Matcher m = epsiodePattern.matcher(episode);
    m.find();

    return m.group(1);
  }

  private String cleanMovieTitle(String movieTitle) {
    if (movieTitle.startsWith("\"") && movieTitle.endsWith("\"")) {
      movieTitle = movieTitle.substring(1, movieTitle.length() - 1);
    }

    final Matcher matcher = moviePrefixPattern.matcher(movieTitle);
    if (matcher.matches()) {
      movieTitle = matcher.group(2) + " " + matcher.group(1);
    }

    return movieTitle;
  }
}