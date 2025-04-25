package simulation.gui;


import java.awt.*;

import javax.swing.*;


import observation.Connection;
import observation.Pointable;
import observation.Pulsar;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.interference.SkyState;
import simulation.Simulation;
import astrometrics.Location;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


@SuppressWarnings("serial")
public class MainWindow extends JFrame 
{

	private static MainWindow window;
	private JMenuBar menuBar;
	private JMenuItem newSkyMI;


	final static String EMPTY = "empty";
	final static String TARGETS = "targetpanel";

	private TargetIllustrationPN targetPN;

	public MainWindow(String title)
	{
		super(title);
		window = this;

		createMenuBar();
		this.setJMenuBar(menuBar);
		this.setPreferredSize(new Dimension(750, 750));
		ToolTipManager.sharedInstance().setDismissDelay(15000);
		// set defaults to get the tabbed pane built.

	}

	public void initialiseMainWindow(List<Target> targets, Schedule[] schedules, Telescope[] telescopes, SkyState sky) {
		this.getContentPane().removeAll();
		this.getContentPane().setLayout(new CardLayout());

		// Define colors for each telescope
		Color[] colors = new Color[4]; //TODO
		colors[0] = Color.RED;
		colors[1] = Color.BLUE;
		colors[2] = Color.GREEN;
		colors[3] = Color.ORANGE;
		// Add more colors if needed

		// Main panel with BorderLayout
		JPanel mainPanel = new JPanel(new BorderLayout());

		// Create the illustration panel with all telescopes
		targetPN = new TargetIllustrationPN(targets, schedules, telescopes, sky, colors);
		mainPanel.add(targetPN, BorderLayout.CENTER);

		// Control panel with checkboxes
		JPanel controlPanel = new JPanel();
		for (int i = 0; i < telescopes.length; i++) {
			JCheckBox checkBox = new JCheckBox("Telescope " + (i + 1), true);
			checkBox.setForeground(colors[i]);
			final int idx = i;
			checkBox.addItemListener(e -> {
				targetPN.setTelescopeVisibility(idx, checkBox.isSelected());
				targetPN.repaint();
			});
			controlPanel.add(checkBox);
		}

		// Create the reset button
		JButton resetButton = new JButton("Reset View");
		resetButton.addActionListener(e -> {
			targetPN.resetView();
		});
		controlPanel.add(resetButton);

		mainPanel.add(controlPanel, BorderLayout.SOUTH);
		this.getContentPane().add(TARGETS, mainPanel);
		openTargetPanel();
	}
	
//	public void update (GregorianCalendar gc, Pointable cur, Connection trodden)
//	{
//		this.targetPN.update(gc, cur, trodden);
//	
//	}
	
	public TargetIllustrationPN getIllustrationPN()
	{
		return targetPN;
	}

	private void openTargetPanel(  )
	{
		CardLayout cl = (CardLayout)(this.getContentPane().getLayout());
		cl.show(this.getContentPane(), TARGETS);
	}

	public static void resetWindow()
	{
		CardLayout cl = (CardLayout)(window.getContentPane().getLayout());
		cl.show(window.getContentPane(), EMPTY);
	}

	public static MainWindow getMainWindow()
	{
		return window;
	}

	private void createMenuBar() {

		JMenu illustrationMenu;

		MenuListener ml = new MenuListener();
		//Create the menu bar.
		menuBar = new JMenuBar();

		illustrationMenu = new JMenu("Illustration");

		menuBar.add(illustrationMenu);

		newSkyMI = new JMenuItem("Target graph");
		newSkyMI.addActionListener(ml);


		illustrationMenu.add(newSkyMI);


	}

	class MenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e) {
			JMenuItem source = (JMenuItem)(e.getSource());
			if (source.equals(newSkyMI))
				openTargetPanel();

		}


	}









}
