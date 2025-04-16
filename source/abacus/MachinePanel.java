package abacus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.TreeSet;
import java.awt.event.*;


import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

public class MachinePanel extends ZoomablePanel
{
	ArrayList<Comment> comments = new ArrayList<>();
	ArrayList<Node> nodes = new ArrayList<>();
	public static Color darkBlue = new Color(0,0,128);
	MouseAction ma = new MouseAction();
	NodeEditor parent;
	RegisterEditor re;
	Image previewImage = null;
	Point mousePoint, lastMousePoint, startPoint;
	EditNodeDialog end;
	private boolean locked = false;
	
	
	Object selection = null;
	
	public MachinePanel(NodeEditor parent, RegisterEditor re)
	{
		this.parent = parent;
		this.re = re;
		end = new EditNodeDialog(parent);
		
		setPreferredSize(new Dimension(500,500));
		setBackground(Color.white);
		
		setBorder(BorderFactory.createLineBorder(darkBlue));
		
		addMouseListener(ma);
		addMouseMotionListener(ma);
		addMouseWheelListener(ma);
	}
	
	public void clearSelection()
	{
		for (int x = nodes.size()-1; x >= 0; --x)
		{
			Node n = nodes.get(x);
			
			n.clearSelection();
		}
		
		for (int x = 0; x < comments.size(); ++x)
		{
			Comment c = comments.get(x);
			
			c.clearSelection();
		}
		
		repaint();
	}
	
	public int getRegCount()
	{
		TreeSet t = new TreeSet();
		
		for (int x = nodes.size()-1; x >= 0; --x)
		{
			Node n = (Node)nodes.get(x);
			
			t.add(n.getRegister());
		}
		
		return t.size();
	}
	
	public void lock()
	{
		locked = true;
	}
	
	public void unlock()
	{
		locked = false;
	}

	protected void draw_scaled(Graphics2D g2d)
	{
		for (int x = 0; x < comments.size(); ++x) {
			Comment c = (Comment)comments.get(x);
			c.draw(g2d);
		}

		for (int x = 0; x < nodes.size(); ++x) {
			Node n = (Node)nodes.get(x);
			n.draw(g2d);
		}

		if (nodes.size() > 0) {
			Node n = (Node)nodes.get(0);
			n.drawInitState(g2d);
		}
	}

	protected void draw_unscaled(Graphics2D g2d){
		if (previewImage != null) {
			g2d.drawImage(previewImage,mousePoint.x - previewImage.getWidth(null) / 2,
					mousePoint.y - previewImage.getHeight(null) / 2,null);
		}
	}

	
	// internal class because ZoomablePanel already implements mousemotionlistener
	class MouseAction implements MouseListener, MouseMotionListener, MouseWheelListener
	{
		public void mouseClicked(MouseEvent e)
		{

		}

		public void mousePressed(MouseEvent e)
		{
			lastMousePoint = mousePoint;
			mousePoint = e.getPoint();
		    if (isZoomAction(mousePoint))
		        return;
			Point realPoint = UntransformMousePoint(mousePoint);

			if ((parent.getState() == NodeEditor.STATE_MOD || parent.getState() == NodeEditor.STATE_DEL) && !locked) {
				for (Node n : nodes) {
					if (n.select(realPoint)) {
						selection = n;
						break;
					}
				}
				// if we didn't select a node/transition
				if (selection == null) {
					for (Comment c : comments) {
						if (c.select(realPoint)) {
							selection = c;
							break;
						}
					}
				}
			}
			repaint();

		}
		
		public void mouseReleased(MouseEvent e)
		{
			lastMousePoint = mousePoint;
			mousePoint = e.getPoint();
			if (isZoomAction(mousePoint))
				return;
			Point realPoint = UntransformMousePoint(mousePoint);
			if (selection == null && parent.getState() == NodeEditor.STATE_MOD) {
				String s = (String)JOptionPane.showInputDialog(
						null,
						"Comment Text, or leave blank to remove:",
						"Set Comment Text",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						"");

				//If a string was returned
				if (s != null) {
					if (s.length() > 0) {
						Comment c = new Comment();
						c.setS(s);
						c.setP(mousePoint);
						comments.add(c);
						selection = c;
					}
				}
			}



			if (!locked) {
				switch (parent.getState()) {
					case NodeEditor.STATE_ADD -> {
						Node n = new Node();
						n.setPlus(true);
						n.setLocation(new Point(realPoint.x + 8, realPoint.y - 14));
						if (nodes.size() == 0)
							n.setInitialState(true);
						nodes.add(n);
					}
					case NodeEditor.STATE_SUB -> {
						Node n = new Node();
						n.setLocation(new Point(realPoint.x, realPoint.y - 13));
						if (nodes.size() == 0)
							n.setInitialState(true);
						nodes.add(n);
					}
					case NodeEditor.STATE_MOD -> {
						Object released_over = null;
						for (Node n : nodes) {
							if (n.inBounds(realPoint)) {
								released_over = n;
								break;
							}
						}
						if (released_over != null) {
							for (Comment c : comments) {
								if (c.inBounds(realPoint)) {
									released_over = c;
									break;
								}
							}
						}
						if (released_over == selection) {
							if (selection == null) {
								String s = (String)JOptionPane.showInputDialog(
										null,
										"Comment Text, or leave blank to remove:",
										"Set Comment Text",
										JOptionPane.PLAIN_MESSAGE,
										null,
										null,
										((Comment)selection).getS());

								//If a string was returned
								if (s != null) {
									if (s.length() > 0)
										((Comment)selection).setS(s);
									else
										comments.remove(selection);
								}
							}
							else if (selection instanceof Node) {
								Node selected = (Node)selection;
								Node target = (Node) released_over;
								int sel = selected.getSelected();

								if (sel == Node.SELECTED_OUT) {
									selected.setOut(target);
								} else {
									Point p = parent.getLocation();
									p.x += getX() + e.getPoint().x;
									p.y += getY() + e.getPoint().y;
									int index = nodes.indexOf(selected);
									int rv = end.modifyNode(selected,index == 0,p);
									re.refreshReg(); // refresh register count

									if (index != 0 && rv == EditNodeDialog.MOD_MAKEINITIAL) {
										Node temp = nodes.get(index);
										Node temp2 = nodes.get(0);
										temp.setInitialState(true);
										temp2.setInitialState(false);
										nodes.set(index,temp2);
										nodes.set(0,temp);
									} else if (rv == EditNodeDialog.MOD_DELETE) {
										for (int x = nodes.size()-1; x >= 0; --x) {
											Node n = nodes.get(x);

											if (n.getOut() == selected)
												n.setOut(null);
											else if (n.getOutEmpty() == selected)
												n.setOutEmpty(null);
										}

										nodes.remove(index);
										if (nodes.size() > 0)
											nodes.get(0).setInitialState(true);
									}
								}
							}
							else if (selection instanceof Comment) {
								String s = (String)JOptionPane.showInputDialog(
										null,
										"Comment Text, or leave blank to remove:",
										"Set Comment Text",
										JOptionPane.PLAIN_MESSAGE,
										null,
										null,
										((Comment)selection).getS());

								//If a string was returned
								if (s != null) {
									if (s.length() > 0)
										((Comment)selection).setS(s);
									else
										comments.remove(selection);
								}
							}
						}
						else {
							if (selection instanceof Node) {
								if (released_over instanceof Node) {
									Node selected = (Node)selection;

									int sel = selected.getSelected();

									if (sel == Node.SELECTED_OUT) {
										selected.setOut((Node) released_over);
									} else if (sel == Node.SELECTED_OUTEMPTY) {
										selected.setOutEmpty((Node) released_over);
									}
								}
							}
						}
					}
					case NodeEditor.STATE_DEL -> {
						if (selection != null) {
							if (selection instanceof Node) {
								Node confirm_selection = null;
								for (Node n : nodes) {
									if (n.select(realPoint)) {
										confirm_selection = n;
										break;
									}
								}
								if (confirm_selection != null) {
									if (confirm_selection == selection) {
										// safely delete the node
										Node selected = confirm_selection;
										int sel = selected.getSelected();
										if (sel == Node.SELECTED_OUT) {
											selected.setOut(null);
										} else if (sel == Node.SELECTED_OUTEMPTY) {
											selected.setOutEmpty(null);
										} else if (sel == Node.SELECTED_NODE) {
											Point p = parent.getLocation();
											p.x += getX() + e.getPoint().x;
											p.y += getY() + e.getPoint().y;

											for (int x = nodes.size() - 1; x >= 0; --x) {
												Node n = nodes.get(x);

												if (n.getOut() == selected)
													n.setOut(null);
												else if (n.getOutEmpty() == selected)
													n.setOutEmpty(null);
											}
											nodes.remove(selected);
											if (nodes.size() > 0)
												nodes.get(0).setInitialState(true);
										}
										((Node) selection).clearSelection();
									}
								}

							}
							else if (selection instanceof Comment) {
								Comment confirm_selection = null;
								for (Comment c : comments) {
									if (c.select(realPoint)) {
										confirm_selection = c;
										break;
									}
								}

								if (confirm_selection != null){
									if (confirm_selection == selection) {
										confirm_selection.clearSelection();
										comments.remove(confirm_selection);
									}
								}
							}
						}
					}
				}
			}
			clearSelection();
			selection = null;
			repaint();
		}

		public void mouseEntered(MouseEvent e) { }

		public void mouseExited(MouseEvent e)
		{
			previewImage = null;
			repaint();
		}

		public void mouseDragged(MouseEvent e)
		{
			lastMousePoint = mousePoint;
			mousePoint = e.getPoint();
			if (parent.getState() == NodeEditor.STATE_MOD && !locked && startPoint != null && selection != null) {
				
				int dx = mousePoint.x - startPoint.x;
				int dy = mousePoint.y - startPoint.y;
				
				Point oldLoc = (selection instanceof Node) ? ((Node)selection).getLocation() 
						: ((Comment)selection).getP();
				
				if (selection instanceof Node)
				{
					((Node)selection).setLocation(new Point(oldLoc.x + dx,oldLoc.y + dy));
				}
				else if (selection instanceof Comment)
				{
					((Comment)selection).setP(new Point(oldLoc.x + dx,oldLoc.y + dy));
				}
				startPoint = mousePoint;
			}
			repaint();

		}

		public void mouseWheelMoved(MouseWheelEvent e)
		{
			mousePoint = lastMousePoint;
			repaint();
		}
		
		public void mouseMoved(MouseEvent e) {
			lastMousePoint = mousePoint;
			mousePoint = e.getPoint();
			if (!locked) {
				int state = parent.getState();
				
				if (state == NodeEditor.STATE_ADD) {
					previewImage = NodeEditor.transAdd;
				} else if (state == NodeEditor.STATE_SUB) {
					previewImage = NodeEditor.transSub;
				} else if (state == NodeEditor.STATE_DEL) {
					previewImage = NodeEditor.transDel;
				} else {
					previewImage = null;
				}
			}

			if (selection instanceof Node) {

			} else if (selection instanceof Comment) {
				Comment c = (Comment) selection;
			}

			repaint();
		}
		
	}

}
