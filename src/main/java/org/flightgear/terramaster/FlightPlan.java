package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class FlightPlan extends JDialog {

	private final class OkActionListner implements ActionListener {
    private final TerraMaster terraMaster;

    private OkActionListner(TerraMaster terraMaster) {
      this.terraMaster = terraMaster;
    }

    public void actionPerformed(ActionEvent e) {
    	Airport selectedDeparture = (Airport) txtDeparture.getSelectedItem();
    	Airport selectedArrival = (Airport) txtArrival.getSelectedItem();
    	List<TileName> tiles = CoordinateCalculation.findAllTiles(selectedDeparture.lat, selectedDeparture.lon, selectedArrival.lat, selectedArrival.lon);
    	terraMaster.frame.map.setSelection(tiles);
    	terraMaster.frame.map.repaint();
    	setVisible(false);
    }
  }

  private final JPanel contentPanel = new JPanel();
	private JComboBox<Airport> txtDeparture;
	private JComboBox<Airport> txtArrival;
  private transient TerraMaster terraMaster;

	/**
	 * Create the dialog.
	 * @param terraMaster 
	 */
	public FlightPlan(TerraMaster terraMaster) {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Flightplan");
		this.terraMaster = terraMaster; 
		setBounds(100, 100, 446, 162);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblDeparture = new JLabel("Departure : ");
			GridBagConstraints gbc_lblDeparture = new GridBagConstraints();
			gbc_lblDeparture.anchor = GridBagConstraints.EAST;
			gbc_lblDeparture.insets = new Insets(5, 5, 5, 5);
			gbc_lblDeparture.gridx = 0;
			gbc_lblDeparture.gridy = 0;
			contentPanel.add(lblDeparture, gbc_lblDeparture);
		}
		ActionListener getDepartureListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String searchString = txtDeparture.getSelectedItem().toString();
				txtDeparture.removeAllItems();
				txtDeparture.setEnabled(false);
				WebWorker w = new WebWorker(searchString, new AirportResult() {

					@Override
					public void addAirport(Airport result) {
						txtDeparture.addItem(result);
					}

					@Override
					public void done() {
						txtDeparture.setEnabled(true);
					}

          @Override
          public void clearLastResult() {
            //Last result isn't cached
          }
          @Override
          public MapFrame getMapFrame() {
            return terraMaster.frame;
          }
				});
				w.execute();
			}
		};
		{
			txtDeparture = new JComboBox<>();
//			txtDeparture.addActionListener(getDepartureListener);
			txtDeparture.setEditable(true);
			GridBagConstraints gbc_txtDeparture = new GridBagConstraints();
			gbc_txtDeparture.insets = new Insets(5, 5, 5, 5);
			gbc_txtDeparture.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtDeparture.gridx = 1;
			gbc_txtDeparture.gridy = 0;
			contentPanel.add(txtDeparture, gbc_txtDeparture);
		}
		{
			JButton button = new JButton(">");
			button.addActionListener(getDepartureListener);
			GridBagConstraints gbc_button = new GridBagConstraints();
			gbc_button.insets = new Insets(5, 5, 5, 5);
			gbc_button.gridx = 2;
			gbc_button.gridy = 0;
			contentPanel.add(button, gbc_button);
		}
		{
			JLabel lblArrival = new JLabel("Arrival : ");
			GridBagConstraints gbc_lblArrival = new GridBagConstraints();
			gbc_lblArrival.anchor = GridBagConstraints.EAST;
			gbc_lblArrival.insets = new Insets(5, 0, 0, 5);
			gbc_lblArrival.gridx = 0;
			gbc_lblArrival.gridy = 1;
			contentPanel.add(lblArrival, gbc_lblArrival);
		}
		ActionListener getArrivalListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String searchString = txtArrival.getSelectedItem().toString();
				txtArrival.removeAllItems();
				txtArrival.setEnabled(false);
				WebWorker w = new WebWorker(searchString, new AirportResult() {

					@Override
					public void addAirport(Airport result) {
						txtArrival.addItem(result);
					}

					@Override
					public void done() {
						txtArrival.setEnabled(true);
					}

          @Override
          public void clearLastResult() {
            //not required 
          }

          @Override
          public MapFrame getMapFrame() {
            return terraMaster.frame;
          }
				});
				w.execute();
			}
		};
		{
			txtArrival = new JComboBox();
			txtArrival.setEditable(true);
//			txtArrival.addActionListener(getArrivalListener);
			GridBagConstraints gbc_txtArrival = new GridBagConstraints();
			gbc_txtArrival.insets = new Insets(5, 5, 0, 5);
			gbc_txtArrival.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtArrival.gridx = 1;
			gbc_txtArrival.gridy = 1;
			contentPanel.add(txtArrival, gbc_txtArrival);
		}
		{
			JButton button = new JButton(">");
			button.addActionListener(getArrivalListener);
			GridBagConstraints gbc_button = new GridBagConstraints();
			gbc_button.insets = new Insets(5, 5, 5, 5);
			gbc_button.gridx = 2;
			gbc_button.gridy = 1;
			contentPanel.add(button, gbc_button);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new OkActionListner(terraMaster));
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(e->setVisible(false));
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

}
