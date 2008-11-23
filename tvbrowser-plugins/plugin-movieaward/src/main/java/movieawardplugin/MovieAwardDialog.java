package movieawardplugin;

import devplugin.Program;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import util.ui.WindowClosingIf;
import util.ui.Localizer;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import static movieawardplugin.Award.Status.NOMINATED;

public class MovieAwardDialog extends JDialog implements WindowClosingIf {
  /**
   * Translator
   */
  private static final Localizer mLocalizer = Localizer.getLocalizerFor(MovieAwardDialog.class);

  public MovieAwardDialog(JFrame frame, ArrayList<MovieAward> mMovieAwards, Program program) {
    super(frame, true);
    createDialog(mMovieAwards, program);
  }

  public MovieAwardDialog(JDialog dialog, ArrayList<MovieAward> mMovieAwards, Program program) {
    super(dialog, true);
    createDialog(mMovieAwards, program);
  }

  private void createDialog(ArrayList<MovieAward> mMovieAwards, Program program) {
    setTitle(mLocalizer.msg("title", "Movie Awards"));

    JPanel panel = (JPanel) getContentPane();
    panel.setBorder(Borders.DLU4_BORDER);
    panel.setLayout(new FormLayout("fill:min:grow", "fill:min:grow, 3dlu, pref"));

    CellConstraints cc = new CellConstraints();

    StringBuilder text = new StringBuilder();

    text.append("<html><body><h1>").append(mLocalizer.msg("movieAwardFor","Movie Awards for")).append(" <i>").append(program.getTitle()).append("</i>:</h1><br>");
    text.append("<ul>");

    for (MovieAward maward : mMovieAwards) {
      for (Award award : maward.getAwardsFor(program)) {
        text.append("<li>").append(maward.getName()).append(" ").append(award.getAwardYear()).append(" - ");
        text.append(maward.getCategoryName(award.getCategorie()));

        switch(award.getStatus()) {
          case NOMINATED: text.append(" (").append(mLocalizer.msg("nominated", "nominated")).append(") "); break;
          case WINNER: text.append(" (").append(mLocalizer.msg("winner", "winner")).append(") "); break;
        }

        if (award.getRecipient() != null) {
          text.append(" ").append(mLocalizer.msg("for", "for")).append(" ").append(award.getRecipient());
        }
        text.append("</li>");
      }
    }

    text.append("</ul></body></html>");

    JEditorPane pane = new JEditorPane("text/html", text.toString());
    pane.setEditable(false);
    panel.add(new JScrollPane(pane), cc.xy(1,1));

    ButtonBarBuilder builder = new ButtonBarBuilder();

    JButton ok = new JButton(Localizer.getLocalization(Localizer.I18N_OK));

    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        close();
      }
    });

    builder.addGlue();
    builder.addGridded(ok);

    panel.add(builder.getPanel(), cc.xy(1,3));

    setSize(Sizes.dialogUnitXAsPixel(300, this),
            Sizes.dialogUnitYAsPixel(200, this));

    getRootPane().setDefaultButton(ok);
  }

  public void close() {
    setVisible(false);
  }
}