package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.SystemColor;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import org.flightgear.terramaster.dns.WeightedUrl;
import org.slieb.formatter.HtmlExceptionFormatter;

import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class DownloadResultDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      DownloadResultDialog dialog = new DownloadResultDialog(null);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   * 
   * @param downloadStats
   */
  public DownloadResultDialog(HashMap<WeightedUrl, TileResult> downloadStats) {
    setAlwaysOnTop(true);
    setBounds(100, 100, 450, 300);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    {
      JEditorPane jEditorPane = new JEditorPane();
      jEditorPane.setBackground(SystemColor.control);
      jEditorPane.setEditable(false);
      HTMLEditorKit kit = new HTMLEditorKit();
      jEditorPane.setEditorKit(kit);
      Document doc = kit.createDefaultDocument();
      jEditorPane.setDocument(doc);
      jEditorPane.setText(getHTML(downloadStats));
      JScrollPane scrollPane = new JScrollPane(jEditorPane);
      contentPanel.add(scrollPane, BorderLayout.CENTER);
    }
    {
      JPanel buttonPane = new JPanel();
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      buttonPane.setLayout(new BorderLayout(0, 0));
      {
        JPanel panel = new JPanel();
        buttonPane.add(panel, BorderLayout.NORTH);
        {
          JButton okButton = new JButton("OK");
          okButton.addActionListener(ae -> {
            setVisible(false);
          });

          panel.add(okButton);
          getRootPane().setDefaultButton(okButton);
        }
      }
    }
  }

  private String getHTML(HashMap<WeightedUrl, TileResult> downloadStats) {

    StringBuilder sb = new StringBuilder();
    sb.append("<HTML>");
    for (Entry<WeightedUrl, TileResult> entry : downloadStats.entrySet()) {
      sb.append("<H3>" + entry.getKey().getUrl().toExternalForm() + "</H3>");
      sb.append("404s " + entry.getValue().errors + " Downloads " + entry.getValue().actualDownloads + " Equal "
          + entry.getValue().equal + "<BR>");
      if (entry.getValue().numberBytes > 1024)
        sb.append(String.format("Dowloaded %d kBytes in %d seconds<BR>", entry.getValue().numberBytes / 1024,
            entry.getValue().time / 1000));
      else
        sb.append(String.format("Dowloaded %d Bytes in %d seconds<BR>", entry.getValue().numberBytes,
            entry.getValue().time / 1000));
      if (entry.getValue().time > 1000) {
        long seconds = entry.getValue().time / 1000;
        if (entry.getValue().numberBytes > 1024)
          sb.append(
              String.format("Dowload Speed %4.2f kB/s<BR>", ((double) entry.getValue().numberBytes / seconds) / 1024));
        else
          sb.append(String.format("Dowload Speed %4.2f B/s<BR>", ((double) entry.getValue().numberBytes / seconds)));
      }
//      if (entry.getValue().getException() != null) {
//          HtmlExceptionFormatter hef = new HtmlExceptionFormatter();
//          hef.formatMessage(sb, entry.getValue().getException());
//      }
    }
    sb.append("</HTML>");
    return sb.toString();
  }

}
