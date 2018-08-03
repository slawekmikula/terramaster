package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import org.flightgear.terramaster.dns.WeightedUrl;
import org.slieb.formatter.HtmlExceptionFormatter;

import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ExceptionDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();
  private ArrayList<Exception> exceptions = new ArrayList<>();

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      ExceptionDialog dialog = new ExceptionDialog(null);
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
  public ExceptionDialog(Exception ex) {
    setAlwaysOnTop(true);
    setBounds(100, 100, 885, 528);
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
      jEditorPane.setText(getHTML(ex));
      jEditorPane.setCaretPosition(0);
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

  private String getHTML(Exception ex) {

    StringBuilder sb = new StringBuilder();
    sb.append("<HTML>");
    HtmlExceptionFormatter hef = new HtmlExceptionFormatter();
    hef.formatMessage(sb, ex);
    sb.append("</HTML>");
    return sb.toString();
  }

}
