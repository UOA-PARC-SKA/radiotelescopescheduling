package simulation.gui;

import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import observation.Connection;
import observation.Pointable;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.interference.BadThingThatMoves;
import observation.interference.CelestialBody;
import observation.interference.Satellite;
import observation.interference.SkyState;
import observation.live.ObservationState;
import simulation.Clock;
import simulation.Simulation;
import util.Utilities;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


@SuppressWarnings("serial")
public class TargetIllustrationPN extends JPanel implements Observer
{
	private static final int X_RANGE = 2;
	private static final int Y_RANGE = 2;

	private static final int BORDER_GAP = 30;
	private static final Stroke GRAPH_STROKE = new BasicStroke(1f);
	private static int GRAPH_POINT_WIDTH = 4;
	//private Location location;
	private GregorianCalendar gc = null;
	private List<Target> targets;
	private Point startRect, endRect;
	private double xScale , yScale;
	private boolean resized = false;
	private int clipWidth = 1, clipHeight = 1;
	private int halfScreenX, halfScreenY;
	private double aspectRatio;
	private double offsetX = 1, offsetY = 1;
	private SkyState skyState = null;
	private Telescope telescope = null;
	private Schedule schedule = null;



	private ShapeResizeHandler ada = new ShapeResizeHandler();
	

	public TargetIllustrationPN( List<Target> t,Schedule schedule, Telescope scope, SkyState sky) 
	{
		this.gc = Clock.getSimulationClock().getTime();
		addMouseListener(ada);
		addMouseMotionListener(ada);
		this.targets = t;
		this.telescope = scope;
		this.skyState = sky;
		this.schedule = schedule;
		setDefaults();		
	}


	private void setDefaults()
	{
		startRect = new Point(30, 30);
		endRect = new Point(getWidth()-30, getHeight()-30);
		GRAPH_POINT_WIDTH = 4;
		resized = false;
	}

	private void resize()
	{
		Point a = startRect;
		Point b = endRect;
		startRect.setLocation(a.x, a.y);

		//if the mouse was dragged the wrong way
		if (a.getX() > b.getX())
		{
			startRect.x = b.x;
			endRect.x = a.x;
		}

		clipWidth = endRect.x - startRect.x;
		clipHeight = endRect.y - endRect.y;
		if (clipWidth == 0)
			return;

		endRect.setLocation(b.x, b.y);
		if (a.getY() > b.getY())
		{
			startRect.y = b.y;
			endRect.y = a.y;
		}

		//make it a square
		if (clipWidth > clipHeight)
		{
			endRect.y = startRect.y + clipWidth;
			clipHeight = clipWidth;
		}
		else
		{
			endRect.x = startRect.x + clipHeight;
			clipWidth = clipHeight;
		}

		aspectRatio = super.getWidth() / clipWidth;
		offsetX = 0;
		offsetY = 0;
		//do we have to paint the lines?
		if(halfScreenX > startRect.x && halfScreenX < endRect.x)
		{
			offsetX = ((double)halfScreenX - (double) startRect.x)/ (double)clipWidth;

		}
		if(halfScreenY > startRect.y && halfScreenY < endRect.y)
		{
			offsetY = ((double)halfScreenY - (double)startRect.y)/(double)clipHeight;

		}
		GRAPH_POINT_WIDTH *=2;
	}


	@Override
	protected void paintComponent(Graphics g) 
	{

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		xScale = ((double) getWidth() - 2 * BORDER_GAP)/(X_RANGE ) ;
		yScale = ((double) getHeight() - 2 * BORDER_GAP) / (Y_RANGE );
		if (resized)
		{
			int x1 = (int) (getWidth() * offsetX);
			int y1 = (int) (getHeight() * offsetY);
			g2.drawLine(BORDER_GAP, y1, getWidth() - BORDER_GAP, y1);
			g2.drawLine(x1, getHeight() - BORDER_GAP, x1, BORDER_GAP);			
		}
		else
		{
			g2.drawLine(BORDER_GAP, getHeight()/2, getWidth() - BORDER_GAP, getHeight()/2);
			g2.drawLine(getWidth()/2, getHeight() - BORDER_GAP, getWidth()/2, BORDER_GAP);
		}
		halfScreenX = getWidth()/2;
		halfScreenY = getHeight()/2;

		if (gc == null)
			return;

		for (Target t : targets) 
		{
			HorizonCoordinates hc = t.getHorizonCoordinates(telescope.getLocation(), gc);

			if(hc.getAltitude() < 0)
				continue;
			double[] xy = getXY(hc);

			int x1 = (int) (xy[0] * xScale  + (getWidth()/2));
			int y1 = (int) ((0 - xy[1]) * yScale  + (getHeight()/2));

			if (resized)
			{
				x1 = (int) ((x1-startRect.x) * aspectRatio);
				y1 = (int) ((y1-startRect.y) * aspectRatio);
			}

			int x = x1 - GRAPH_POINT_WIDTH / 2;
			int y = y1 - GRAPH_POINT_WIDTH / 2;

			g2.setColor(Color.lightGray);

			g2.fillOval(x, y, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);

			//if (p.getPulsarName().equals("J1614-38") || p.getPulsarName().equals("J1618-39") || p.getPulsarName().equals("J1638-42") || p.getPulsarName().equals("J1617-4216") )
			//g2.drawString(t.getName(),x,y); 
			g2.setColor(Color.lightGray);
			g2.setStroke(GRAPH_STROKE);

		}

		List<ObservationState> states = schedule.getScheduleStates();
		Pointable p = null;
		Pointable other = null;
		Connection c;
		if(states.size() > 0)
		{			
			synchronized(states){
				for (ObservationState state : states) 
				{
//					//only draw this state if it happened before the current simulation time
					if (state.getStartTime() < gc.getTime().getTime())
					{
						p = state.getCurrentTarget();
						HorizonCoordinates hc = p.getHorizonCoordinates(telescope.getLocation(), gc);

						if(hc.getAltitude() < 0)
							continue;
						double[] xy = getXY(hc);

						int[] coords = doPointScaling(xy[0], xy[1]);

						g2.setColor(Color.GREEN);
						g2.fillOval(coords[0], coords[1], GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
						
						//g2.setColor(Color.BLACK);
						//g2.drawString(state.getCurrentObservable().getName(), coords[0], coords[1]);
						g2.setColor(Color.GREEN);
						if (state.getLinkToHere() != null)
						{
							c = state.getLinkToHere();
							HorizonCoordinates hc1 = c.getFirst().getHorizonCoordinates(telescope.getLocation(), gc);
							HorizonCoordinates hc2 = c.getOtherTarget(c.getFirst()).getHorizonCoordinates(telescope.getLocation(), gc);
							if (hc1.getAltitude() > 0 || hc2.getAltitude() > 0)
							{
								double[] xy1 = getXY(hc1);
								double[] xy2 = getXY(hc2);

								int x1 = (int) (xy1[0] * xScale  + (getWidth()/2));
								int y1 = (int) ((0 - xy1[1]) * yScale  + (getHeight()/2));
								int x2 = (int) (xy2[0] * xScale  + (getWidth()/2));
								int y2 = (int) ((0 - xy2[1]) * yScale  + (getHeight()/2));
			
								if (resized)
								{
									x1 = (int) ((x1-startRect.x) * aspectRatio);
									y1 = (int) ((y1-startRect.y) * aspectRatio);
									x2 = (int) ((x2-startRect.x) * aspectRatio);
									y2 = (int) ((y2-startRect.y) * aspectRatio);
			
								}
								g2.drawLine(x1, y1, x2, y2);   
								    
							}
						}
					}
					else
					{
						other = state.getCurrentTarget();
						HorizonCoordinates hc = other.getHorizonCoordinates(telescope.getLocation(), gc);

						if(hc.getAltitude() < 0)
							continue;
						double[] xy = getXY(hc);

						int[] coords = doPointScaling(xy[0], xy[1]);

						g2.setColor(Color.BLUE);
						g2.fillOval(coords[0], coords[1], GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
					//	g2.setColor(Color.BLACK);
					//	g2.drawString(state.getCurrentObservable().getName(), coords[0], coords[1]);
					}
				}	
			}
		}

		if(skyState != null)
		{
			List<CelestialBody> planets = skyState.getCelestialBodies();
			for (CelestialBody celestialBody : planets) 
			{
				HorizonCoordinates hc = celestialBody.getHorizonCoordinates(gc.getTime());
				if(hc.getAltitude() < 0)
					continue;
				double[] xy = getXY(hc);
				switch (celestialBody.getNovasID()) {
				case CelestialBody.SUN:
					g2.setColor(Color.YELLOW);
					break;
				case CelestialBody.MOON:
					g2.setColor(Color.LIGHT_GRAY);
					break;
				default:
					g2.setColor(Color.DARK_GRAY);
					break;
				}
				int[] coords = doPointScaling(xy[0], xy[1]);
				int r = scaleRadius(celestialBody.getRadius());
				g2.fillOval(coords[0]-r, coords[1]-r, 2*r, 2*r);
				g2.setColor(Color.ORANGE);
				g2.drawOval(coords[0]-r, coords[1]-r, 2*r, 2*r);
			}
			List<Satellite> badThingsThatMove = skyState.getSatellites();
			for (Satellite satellite : badThingsThatMove) 
			{
				HorizonCoordinates hc = satellite.getHorizonCoordinates(gc);
				
				if(hc.getAltitude() < 0)
					continue;
				double[] xy = getXY(hc);

				int[] coords = doPointScaling(xy[0], xy[1]);

				if(satellite.getType().equals("beidou"))
					g2.setColor(Color.MAGENTA);
				else if (satellite.getType().equals("galileo"))
					g2.setColor(Color.CYAN);
				else if (satellite.getType().equals("glo-ops"))
					g2.setColor(Color.ORANGE);
				else if (satellite.getType().equals("gps-ops"))
					g2.setColor(Color.YELLOW);
				else //iridium
					g2.setColor(Color.BLACK);
				
				g2.fillRect(coords[0]-GRAPH_POINT_WIDTH/2, coords[1]-GRAPH_POINT_WIDTH/2, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
				g2.setColor(Color.BLACK);
				g2.drawRect(coords[0]-GRAPH_POINT_WIDTH/2, coords[1]-GRAPH_POINT_WIDTH/2, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);	
			}

		}
		//the last one (current position)
		if(p != null)
		{
			g2.setColor(Color.red);
			HorizonCoordinates hcCurrent = p.getHorizonCoordinates(telescope.getLocation(), gc);
			if (hcCurrent.getAltitude() > 0)
			{
				double[] xy1 = getXY(hcCurrent );
				int [] coords = doPointScaling(xy1[0], xy1[1]);

				g2.fillOval(coords[0], coords[1], GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);  
				List<Connection> connections = p.getNeighbours();
				for (Connection conn : connections) 
				{
					HorizonCoordinates hc1 = conn.getFirst().getHorizonCoordinates(telescope.getLocation(), gc);
					HorizonCoordinates hc2 = conn.getOtherTarget(conn.getFirst()).getHorizonCoordinates(telescope.getLocation(), gc);
					if (hc1.getAltitude() < 0 || hc2.getAltitude() < 0)
						continue;
					 xy1 = getXY(hc1);
					double[] xy2 = getXY(hc2);
					int x1 = (int) (xy1[0] * xScale  + (getWidth()/2));
					int y1 = (int) ((0 - xy1[1]) * yScale  + (getHeight()/2));
					int x2 = (int) (xy2[0] * xScale  + (getWidth()/2));
					int y2 = (int) ((0 - xy2[1]) * yScale  + (getHeight()/2));

					if (resized)
					{
						x1 = (int) ((x1-startRect.x) * aspectRatio);
						y1 = (int) ((y1-startRect.y) * aspectRatio);
						x2 = (int) ((x2-startRect.x) * aspectRatio);
						y2 = (int) ((y2-startRect.y) * aspectRatio);
					}
					g2.setColor(Color.lightGray);
					g2.drawLine(x1, y1, x2, y2);       
				}	
				
			}
			// this will show the pulsars which are followed by a wait. They are false positives. The observation algorithm is correct,
			// does not observe after the target has set. 
			//else
			//	System.out.println("Current target "+p+" under horizon, alt "+hcCurrent.getAltitude()+" time "+gc.getTime().getTime());
		}

		String time = Utilities.getDateAsString(gc.getTime());

		g2.setColor(Color.black);		
		g2.drawString(time,30,getHeight()-30); 
		
		g2.setColor(Color.MAGENTA);
		g2.fillRect(10, 10, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		
		g2.setColor(Color.CYAN);
		g2.fillRect(10, 30, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		
		g2.setColor(Color.ORANGE);
		g2.fillRect(10, 50, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);

		g2.setColor(Color.YELLOW);
		g2.fillRect(10, 70, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		
		g2.setColor(Color.BLACK);
		g2.fillRect(10, 90, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
			
		g2.drawRect(10, 10, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		g2.drawString("Beidou",25,15);
		g2.drawRect(10, 30, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		g2.drawString("Galileo",25,35);
		g2.drawRect(10, 50, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		g2.drawString("Glo-ops",25,55);
		g2.drawRect(10, 70, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		g2.drawString("Gps-ops",25,75);
		g2.drawRect(10, 90, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
		g2.drawString("Iridium",25,95);
	}

	private int scaleRadius(double radius) {
		
		radius *= getWidth(); // x and yscale are the same

		if (resized)
		{
			radius *= aspectRatio;
		}
		return (int) Math.ceil(radius);
		
	}


	private int[] doPointScaling(double x, double y)
	{
		int[] coords = new int[2];
		int x1 = (int) (x * xScale  + (getWidth()/2));
		int y1 = (int) ((0 - y) * yScale  + (getHeight()/2));


		if (resized)
		{
			x1 = (int) ((x1-startRect.x) * aspectRatio);
			y1 = (int) ((y1-startRect.y) * aspectRatio);

		}
		//if (y1 > 1000)

		coords[0] = x1 - GRAPH_POINT_WIDTH / 2;
		coords[1] = y1 - GRAPH_POINT_WIDTH / 2;
		return coords;
	}
	
//	@Override
//	protected void paintComponent(Graphics g) 
//	{
//
//		super.paintComponent(g);
//		Graphics2D g2 = (Graphics2D)g;
//		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		xScale = ((double) getWidth() - 2 * BORDER_GAP)/(X_RANGE ) ;
//		yScale = ((double) getHeight() - 2 * BORDER_GAP) / (Y_RANGE );
//		if (resized)
//		{
//			int x1 = (int) (getWidth() * offsetX);
//			int y1 = (int) (getHeight() * offsetY);
//			g2.drawLine(BORDER_GAP, y1, getWidth() - BORDER_GAP, y1);
//			g2.drawLine(x1, getHeight() - BORDER_GAP, x1, BORDER_GAP);			
//		}
//		else
//		{
//			g2.drawLine(BORDER_GAP, getHeight()/2, getWidth() - BORDER_GAP, getHeight()/2);
//			g2.drawLine(getWidth()/2, getHeight() - BORDER_GAP, getWidth()/2, BORDER_GAP);
//		}
//		halfScreenX = getWidth()/2;
//		halfScreenY = getHeight()/2;
//
//		if (gc == null)
//			return;
//
//		//for (Target t : targets) 
//		for (int i = 0; i < targets.size(); i++) 
//		{
//			Target t = targets.get(i);
//			HorizonCoordinates hc = t.getHorizonCoordinates(location, gc);
//			if(current == t)
//				currentHC = hc;
//			if(hc.getAltitude() < 0)
//				continue;
//			double[] xy = getXY(hc);
//
//			int x1 = (int) (xy[0] * xScale  + (getWidth()/2));
//			int y1 = (int) ((0 - xy[1]) * yScale  + (getHeight()/2));
//
//
//			if (resized)
//			{
//				x1 = (int) ((x1-startRect.x) * aspectRatio);
//				y1 = (int) ((y1-startRect.y) * aspectRatio);
//
//			}
//			//if (y1 > 1000)
//
//			int x = x1 - GRAPH_POINT_WIDTH / 2;
//			int y = y1 - GRAPH_POINT_WIDTH / 2;
//
//			if(t.needsObserving())
//			{
//				g2.setColor(Color.blue);
//				if(t.hasCompleteObservation())
//					g2.setColor(Color.green);
//			}
//			else
//				g2.setColor(Color.lightGray);
//			if(current == t)
//				g2.setColor(Color.red);
//			g2.fillOval(x, y, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
//
//			//if (p.getPulsarName().equals("J1614-38") || p.getPulsarName().equals("J1618-39") || p.getPulsarName().equals("J1638-42") || p.getPulsarName().equals("J1617-4216") )
//			//g2.drawString(t.getName(),x,y); 
//			g2.setColor(Color.lightGray);
//			g2.setStroke(GRAPH_STROKE);
//			if(connections == null)
//				continue;
//			synchronized(connections){
//				for (Connection c : connections) 
//				{
//					HorizonCoordinates hc1 = c.getFirst().getHorizonCoordinates(location, gc);
//					HorizonCoordinates hc2 = c.getOtherTarget(c.getFirst()).getHorizonCoordinates(location, gc);
//					if (hc1.getAltitude() < 0 || hc2.getAltitude() < 0)
//						continue;
//					double[] xy1 = getXY(hc1);
//					double[] xy2 = getXY(hc2);
//					x1 = (int) (xy1[0] * xScale  + (getWidth()/2));
//					y1 = (int) ((0 - xy1[1]) * yScale  + (getHeight()/2));
//					int x2 = (int) (xy2[0] * xScale  + (getWidth()/2));
//					int y2 = (int) ((0 - xy2[1]) * yScale  + (getHeight()/2));
//
//					if (resized)
//					{
//						x1 = (int) ((x1-startRect.x) * aspectRatio);
//						y1 = (int) ((y1-startRect.y) * aspectRatio);
//						x2 = (int) ((x2-startRect.x) * aspectRatio);
//						y2 = (int) ((y2-startRect.y) * aspectRatio);
//					}
//					g2.drawLine(x1, y1, x2, y2);       
//				}	
//			}
//		}
//		if(troddenPath != null )
//		{
//			g2.setColor(Color.green);
//			synchronized (troddenPath) {
//				for (Iterator<Connection> iterator = troddenPath.iterator(); iterator.hasNext();) {
//					Connection c = iterator.next();
//					Pointable t1 = c.getFirst();
//					Pointable t2 = c.getOtherTarget(t1);
//					HorizonCoordinates hc1 = c.getFirst().getHorizonCoordinates(location, gc);
//					HorizonCoordinates hc2 = c.getOtherTarget(c.getFirst()).getHorizonCoordinates(location, gc);
//					if (hc1.getAltitude() < 0 || hc2.getAltitude() < 0)
//						continue;
//					double[] xy1 = getXY(hc1);
//					double[] xy2 = getXY(hc2);
//					int x1 = (int) (xy1[0] * xScale  + (getWidth()/2));
//					int y1 = (int) ((0 - xy1[1]) * yScale  + (getHeight()/2));
//					int x2 = (int) (xy2[0] * xScale  + (getWidth()/2));
//					int y2 = (int) ((0 - xy2[1]) * yScale  + (getHeight()/2));
//
//					if (resized)
//					{
//						x1 = (int) ((x1-startRect.x) * aspectRatio);
//						y1 = (int) ((y1-startRect.y) * aspectRatio);
//						x2 = (int) ((x2-startRect.x) * aspectRatio);
//						y2 = (int) ((y2-startRect.y) * aspectRatio);
//
//					}
//					g2.drawLine(x1, y1, x2, y2);       
//				}	
//			}
//		}
//		if(skyState != null)
//		{
//			List<Satellite> badThingsThatMove = skyState.getSatellites();
//			for (Satellite satellite : badThingsThatMove) 
//			{
//				HorizonCoordinates hc = satellite.getHorizonCoordinates(gc.getTime());
//				
//				if(hc.getAltitude() < 0)
//					continue;
//				double[] xy = getXY(hc);
//
//				int x1 = (int) (xy[0] * xScale  + (getWidth()/2));
//				int y1 = (int) ((0 - xy[1]) * yScale  + (getHeight()/2));
//
//
//				if (resized)
//				{
//					x1 = (int) ((x1-startRect.x) * aspectRatio);
//					y1 = (int) ((y1-startRect.y) * aspectRatio);
//
//				}
//				//if (y1 > 1000)
//
//				int x = x1 - GRAPH_POINT_WIDTH / 2;
//				int y = y1 - GRAPH_POINT_WIDTH / 2;
//
//				g2.setColor(Color.MAGENTA);
//				g2.fillRect(x, y, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
//				g2.setColor(Color.BLACK);
//				g2.drawRect(x, y, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);
//			
//			}
//
//		}
//
////		if(current != null)
////		{
////			g2.setColor(Color.red);
////			
////			double[] xy1 = getXY(current, currentHC);
////
////			int x1 = (int) (xy1[0] * xScale  + (getWidth()/2));
////			int y1 = (int) ((0 - xy1[1]) * yScale  + (getHeight()/2));
////
////			if (resized)
////			{
////				x1 = (int) ((x1-startRect.x) * aspectRatio);
////				y1 = (int) ((y1-startRect.y) * aspectRatio);
////
////			}
////			g2.fillOval(x1, y1, GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH);       
////		}
//
//		String time = sdf.format(gc.getTime());
//
//		g2.setColor(Color.black);		
//		g2.drawString(time,30,680); 
//	}
//


	private double[] getXY(HorizonCoordinates hc)
	{
		double[] xy = new double[2];
		double radius =  1- (hc.getAltitude()*2)/Math.PI;

		xy[0] = Math.sin(hc.getAzimuth()) * radius;
		xy[1] = Math.cos(hc.getAzimuth()) * radius;

		return xy;
	}


	//	public synchronized void update(GregorianCalendar gc, Pointable cur, Connection trodden)
	//	{
	//		this.gc = gc;
	//		current = cur;
	//		if(current != null)
	//		{
	//			connections.clear();
	//			connections.addAll(current.getNeighbours());
	//		}
	//		if (trodden != null)
	//		{
	//			troddenPath.add(trodden);
	//		}
	//		this.repaint();
	//	}

	class ShapeResizeHandler extends MouseAdapter 
	{
		public void mousePressed(MouseEvent event) 
		{
			if(resized)
				return;
			startRect = event.getPoint();
		}

		public void mouseReleased(MouseEvent event) 
		{
			if(resized)
				return;
			endRect = event.getPoint();
			resize();
			resized = true;
			repaint();
		}

		//		    public void mouseDragged(MouseEvent event) 
		//		    {
		//		      if (pos == -1)
		//		        return;
		//
		//		      
		//		    }

		public void mouseClicked(MouseEvent event)
		{
			if (event.getClickCount() == 2)  // double click
			{
				if(!resized)
					return;
				resized = false;
				setDefaults();
				repaint();
			}
		}
	}

	@Override
	public synchronized void update(Observable arg0, Object arg1) 
	{
 
		this.repaint();

	}


}
