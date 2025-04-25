package simulation.gui;


import java.awt.CardLayout;
import java.awt.Dimension;

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
	private final List<TargetIllustrationPN> targetPNs = new ArrayList<>();

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
		this.getContentPane().setLayout(new CardLayout());
		JPanel emptyPN = new JPanel();

		JTabbedPane tabbedPane = new JTabbedPane();

		for (int i = 0; i < telescopes.length; i++) {
			TargetIllustrationPN panel =
					new TargetIllustrationPN(targets, schedules[i], telescopes[i], sky);
			tabbedPane.addTab("Telescope " + (i+1), panel);
			targetPNs.add(panel);
		}

		this.getContentPane().add(EMPTY, emptyPN);
		this.getContentPane().add(TARGETS, tabbedPane);
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

	public List<TargetIllustrationPN> getAllIllustrationPNs() {
		return targetPNs;
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
