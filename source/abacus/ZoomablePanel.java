package abacus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public abstract class ZoomablePanel extends JPanel implements MouseWheelListener, 
MouseListener, MouseMotionListener
{

	public double scale = 1.0;
	private Point dragStartScreen;
	private Point dragEndScreen;
	public double translateX = 0;
	public double translateY = 0;
	public static final double MIN_ZOOM = 5.0;
	public static final double MAX_ZOOM = 0.5;

	
	private static RoundRectangle2D.Double up = new RoundRectangle2D.Double(30,5,20,20,15,15);
	private static RoundRectangle2D.Double down = new RoundRectangle2D.Double(30,55,20,20,15,15);
	private static RoundRectangle2D.Double left = new RoundRectangle2D.Double(5,30,20,20,15,15);
	private static RoundRectangle2D.Double right = new RoundRectangle2D.Double(55,30,20,20,15,15);
	private static RoundRectangle2D.Double in = new RoundRectangle2D.Double(30,30,10,20,15,15);
	private static RoundRectangle2D.Double out = new RoundRectangle2D.Double(40,30,10,20,15,15);
	
	private static RoundRectangle2D.Double rects[] = 
	{
		up, down, left, right, in, out
	};
	
	private static int[] xUp = {40, 35, 45 };
	private static int[] yUp = {10, 20, 20 };
	
	private static int[] xDown = xUp;
	private static int[] yDown = {70, 60, 60 };
	
	private static int[] xLeft = yUp;
	private static int[] yLeft = xUp;
	
	private static int[] xRight = yDown;
	private static int[] yRight = xUp;
	
	
	
	private static Polygon shapes[] = 
	{
		new Polygon( xUp , yUp, 3),
		new Polygon( xDown , yDown, 3),
		new Polygon( xLeft , yLeft, 3),
		new Polygon( xRight , yRight, 3),
		null,null
	};
	
	private static RoundRectangle2D.Double toggle = new RoundRectangle2D.Double(5,5,20,20,15,15);
	
	private boolean moveOn = false;
	private boolean mouseDown = false;
	private Point mousePoint = null;
	private static Stroke med = new BasicStroke(2);
	private static Stroke thin = new BasicStroke(1);
	public static final Color babyBlue = new Color(67,203,255);
	
	public ZoomablePanel()
	{		
		addMouseWheelListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		MoveThread mt = new MoveThread();
		mt.start();
	}
	
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		AffineTransform unscaledTransform = g2d.getTransform();
		g2d.translate(translateX, translateY);
		g2d.scale(scale, scale);


		draw_scaled(g2d);
		g2d.setTransform(unscaledTransform);
		draw_unscaled(g2d);

		setupDrawing(g);
		g2d.setStroke(med);

		drawMoveMenu(g2d);
		
		drawToggle(g2d);
	}

	private void drawMoveMenu(Graphics2D g2d) {
		if (moveOn) {
			for (int x = 0; x < rects.length; x++) {
				g2d.setColor(Color.white);
				g2d.fill(rects[x]);
				g2d.setColor(Color.black);
				g2d.draw(rects[x]);

				if (x > 3) {
					Point middle = new Point(40,40);
					final int o = 2;
					g2d.setStroke(thin);
					if (x == 4) {
						middle = new Point(middle.x-5,middle.y);
						Point top = new Point(middle.x,middle.y-o);
						Point bottom = new Point(middle.x,middle.y+o);
						Point left = new Point(middle.x-o,middle.y);
						Point right = new Point(middle.x+o,middle.y);

						g2d.drawLine(top.x,top.y,bottom.x,bottom.y);
						g2d.drawLine(left.x,left.y,right.x,right.y);
					} else if (x == 5) {
						middle = new Point(middle.x+5,middle.y);

						Point left = new Point(middle.x-o,middle.y);
						Point right = new Point(middle.x+o,middle.y);

						g2d.drawLine(left.x,left.y,right.x,right.y);
					}

					g2d.setStroke(med);
				} else if (shapes[x] != null) {
					g2d.fill(shapes[x]);
				}
			}
		}
	}

	/**
	 * @param g the grahpics object to use
	 */
	private void drawToggle(Graphics2D g)
	{
		final int o = 5;
		g.setColor(Color.white);
		g.fill(toggle);
		g.setColor(moveOn ? Color.red : Color.black);
		g.draw(toggle);
		
		g.drawLine((int)toggle.x + o,(int)toggle.y + (int)toggle.height / 2,
				(int)toggle.x + (int)toggle.width - o, (int)toggle.y + (int)toggle.height / 2);
		
		if (!moveOn)
		{ // plus
			g.drawLine((int)toggle.x + (int)toggle.width / 2,(int)toggle.y + o,
					(int)toggle.x + (int)toggle.width / 2, (int)toggle.y + (int)toggle.height - o);
		}
	}
	
	private void setupDrawing(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		// Enable Anti-Aliasing
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);   
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	protected abstract void draw_scaled(Graphics2D g);
	protected abstract void draw_unscaled(Graphics2D g);


	protected Point UntransformMousePoint(Point p) {
		return new Point((int)(p.x / scale - translateX), (int)(p.y / scale - translateY));
	}

	private void clampScale() {
		scale = Math.max(MAX_ZOOM, Math.min(MIN_ZOOM, scale));
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		double oldScale = scale;
		scale -= 0.1f * e.getPreciseWheelRotation();
		clampScale();

		translateX += (e.getX() - translateX) * (1 - scale / oldScale);
		translateY += (e.getY() - translateY) * (1 - scale / oldScale);

		repaint();
	}

	public void mouseClicked(MouseEvent e){ }
	public void mousePressed(MouseEvent e) 
	{
		Point p = e.getPoint();
		if (e.getButton() == 2) {
			dragStartScreen = p;
			dragEndScreen = null;
		}

		if (e.getButton() == MouseEvent.BUTTON1) {
			mouseDown = true;
			if (toggle.contains(p)) {
				moveOn = !moveOn;
				repaint();
			}
		}
	}
	
	/**
	 * Is pressing the mouse button here a zoom action? 
	 * @param p the point where the mouse was pressed 
	 * @return true iff it was a zoom/scroll action
	 */
	public boolean isZoomAction(Point p)
	{
		boolean rv = false;
		
		if (toggle.contains(p)) {
			rv = true;
		} else if (moveOn) {
			if (up.contains(p) || down.contains(p) || left.contains(p) || right.contains(p) || in.contains(p) || out.contains(p))
				rv = true;
		}		
		return rv;
	}

	
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1)
			mouseDown = false;
	}
	public void mouseEntered(MouseEvent e) {  }
	public void mouseExited(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) 
	{
		if (e.getButton() == 2) {
			dragEndScreen = e.getPoint();
			int dx = dragEndScreen.x - dragStartScreen.x;
			int dy = dragEndScreen.y - dragStartScreen.y;

			translateX += dx;
			translateY += dy;

			dragStartScreen = dragEndScreen;
			repaint();
		}
		
	}
	
	public void mouseMoved(MouseEvent e) {
		mousePoint = e.getPoint();
	}
	
	class MoveThread extends Thread {
		public void run() {
			while (true) {
				if (mouseDown) {
					if (out.contains(mousePoint)) {
						scale += 0.1;
						clampScale();
					}
					else if (in.contains(mousePoint)) {
						scale -= 0.1;
						clampScale();
					}
					else if (up.contains(mousePoint))
						translateY -= 5;
					else if (down.contains(mousePoint))
						translateY += 5;
					else if (left.contains(mousePoint))
						translateX += 5;
					else if (right.contains(mousePoint))
						translateX -= 5;
					repaint();
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
