package simulation.gui;


import java.awt.*;

import javax.swing.*;


import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.interference.SkyState;
import simulation.Clock;

import java.awt.event.*;
import java.util.Hashtable;
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
		this.setPreferredSize(new Dimension(1000, 1000));
		ToolTipManager.sharedInstance().setDismissDelay(15000);
		// set defaults to get the tabbed pane built.

	}

	public void initialiseMainWindow(List<Target> targets, Schedule[] schedules, Telescope[] telescopes, SkyState sky) {
		this.getContentPane().removeAll();
		this.getContentPane().setLayout(new CardLayout());

		// Define colors for each telescope
		Color[] colors = new Color[8]; //TODO
		colors[0] = Color.RED;
		colors[1] = Color.BLUE;
		colors[2] = Color.GREEN;
		colors[3] = Color.ORANGE;
		colors[4] = Color.MAGENTA;
		colors[5] = Color.YELLOW;
		colors[6] = Color.CYAN;
		colors[7] = Color.PINK;
		// Add more colors if needed

		// Main panel with BorderLayout
		JPanel mainPanel = new JPanel(new BorderLayout());

		// Create the illustration panel with all telescopes
		targetPN = new TargetIllustrationPN(targets, schedules, telescopes, sky, colors);
		mainPanel.add(targetPN, BorderLayout.CENTER);

		// Control panel with checkboxes
		JPanel telescopePanel = new JPanel();
		for (int i = 0; i < telescopes.length; i++) {
			JCheckBox checkBox = new JCheckBox("Telescope " + (i + 1), true);
			checkBox.setForeground(colors[i]);
			final int idx = i;
			checkBox.addItemListener(e -> {
				targetPN.setTelescopeVisibility(idx, checkBox.isSelected());
				targetPN.repaint();
			});
			telescopePanel.add(checkBox);
		}

		JCheckBox checkBox = new JCheckBox("Show Neighbours", false);
		checkBox.addItemListener(e -> {
			targetPN.setNeighbourVisibility(checkBox.isSelected());
			targetPN.repaint();
		});
		telescopePanel.add(checkBox);

		// Speed control with slider
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		controlPanel.add(new JLabel("Simulation Speed:"));

		JSlider speedSlider = new JSlider(0, 500, 100);
		speedSlider.setSnapToTicks(true);
		speedSlider.setMajorTickSpacing(100);
		speedSlider.setMinorTickSpacing(50);
		speedSlider.setPaintTicks(true);
		speedSlider.setPaintLabels(true);

		Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
		labelTable.put(0, new JLabel("0x"));
		labelTable.put(100, new JLabel("1x"));
		labelTable.put(200, new JLabel("2x"));
		labelTable.put(300, new JLabel("3x"));
		labelTable.put(400, new JLabel("4x"));
		labelTable.put(500, new JLabel("5x"));
		speedSlider.setLabelTable(labelTable);

		speedSlider.addChangeListener(e -> {
			double speed = speedSlider.getValue() / 100.0;
			Clock.getSimulationClock().setSpeedMultiplier(speed);
		});

		controlPanel.add(speedSlider);
		controlPanel.add(Box.createRigidArea(new Dimension(20, 0)));

		// Create the reset button
		JButton resetButton = new JButton("Reset View");
		resetButton.addActionListener(e -> {
			targetPN.resetView();
		});
		controlPanel.add(resetButton);
		controlPanel.add(Box.createRigidArea(new Dimension(20, 0)));

		// Past Observations
		JLabel historyLabel = new JLabel("Number of past observations (1-100):");
		JSpinner historySpinner = new JSpinner(new SpinnerNumberModel(100, 1, 100, 1));
		historySpinner.setPreferredSize(new Dimension(50, 25));

		historySpinner.addChangeListener(e -> {
			int history = (Integer)historySpinner.getValue();
			for (Component comp : mainPanel.getComponents()) {
				if (comp instanceof TargetIllustrationPN) {
					((TargetIllustrationPN)comp).setVisibleHistory(history);
				}
			}
		});

		controlPanel.add(historyLabel);
		controlPanel.add(historySpinner);

		JPanel southContainer = new JPanel(new BorderLayout());
		southContainer.add(telescopePanel, BorderLayout.CENTER);
		southContainer.add(controlPanel, BorderLayout.SOUTH);
		mainPanel.add(southContainer, BorderLayout.SOUTH);
		this.getContentPane().add(TARGETS, mainPanel);
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
