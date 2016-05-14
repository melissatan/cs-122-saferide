/*************************************************************************
 *  Compilation:  javac GenDraw.java
 *  Execution:    java GenDraw
 *
 *  Generalization of the Standard graphics library.
 *
 *  Todo
 *  ----
 *    -  Add support for gradient fill, etc.
 *
 *  Remarks
 *  -------
 *    -  don't use AffineTransform for rescaling since it inverts
 *       images and strings
 *    -  careful using setFont in inner loop within an animation -
 *       it can cause flicker
 *
 *  To generate javadoc
 *  -------------------
 *  javadoc -notree -noindex -nohelp -nonavbar GenDraw.java
 *
 *************************************************************************/

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.util.LinkedList;

/**
 *  <i>Generalized draw</i>. Our class GenDraw is a generalization of S&W's
 * StdDraw that supports multiple drawings.  StdDraw provides a basic capability for 
 *  creating drawings with your programs. It uses a simple graphics model that
 *  allows you to create drawings consisting of points, lines, and curves
 *  in a window on your computer and to save the drawings to a file.
 *  <p>
 *  For documentation on StdDraw, see Section 1.5 of <i>Introduction to Programming in Java: An Interdisciplinary Approach, Spring 2007 preliminary version</i> and
 *  <a href="http://www.cs.princeton.edu/introcs/15inout">http://www.cs.princeton.edu/introcs/15inout</a>
 */
public final class GenDraw {
    // pre-defined colors
    public static final Color BLACK      = Color.BLACK;
    public static final Color BLUE       = Color.BLUE;
    public static final Color CYAN       = Color.CYAN;
    public static final Color DARK_GRAY  = Color.DARK_GRAY;
    public static final Color GRAY       = Color.GRAY;
    public static final Color GREEN      = Color.GREEN;
    public static final Color LIGHT_GRAY = Color.LIGHT_GRAY;
    public static final Color MAGENTA    = Color.MAGENTA;
    public static final Color ORANGE     = Color.ORANGE;
    public static final Color PINK       = Color.PINK;
    public static final Color RED        = Color.RED;
    public static final Color WHITE      = Color.WHITE;
    public static final Color YELLOW     = Color.YELLOW;

    // default colors
    private static final Color DEFAULT_PEN_COLOR   = BLACK;
    private static final Color DEFAULT_CLEAR_COLOR = WHITE;

    // current pen color
    private Color penColor;

    // default canvas size is SIZE-by-SIZE
    private static final int DEFAULT_SIZE = 512;
    private int width  = DEFAULT_SIZE;
    private int height = DEFAULT_SIZE;

    // default final
    private static final String DEFAULT_TITLE = "Generalized Draw";
    private String title = DEFAULT_TITLE;

    // default pen radius
    private static final double DEFAULT_PEN_RADIUS = 0.002;

    // current pen radius
    private double penRadius;

    // show we draw immediately or wait until next show?
    private boolean defer = false;

    // boundary of drawing canvas, 5% border
    private static final double BORDER = 0.05;
    private static final double DEFAULT_XMIN = 0.0;
    private static final double DEFAULT_XMAX = 1.0;
    private static final double DEFAULT_YMIN = 0.0;
    private static final double DEFAULT_YMAX = 1.0;
    private double xmin, ymin, xmax, ymax;

    // for synchronization
    // one per drawing
    private Object mouseLock = new Object();
    private Object keyLock = new Object();

    // default font
    private static final Font DEFAULT_FONT = new Font("Sans Serif", Font.PLAIN, 16);
    private static final Font SMALL_FONT = new Font("Sans Serif", Font.PLAIN, 12);

    // current font
    private Font font;

    // double buffered graphics
    private BufferedImage offscreenImage, onscreenImage;
    private Graphics2D offscreen, onscreen;

    // the frame for drawing to the screen
    private JFrame frame = null;

    // mouse state
    private boolean mousePressed = false;
    private double mouseX = 0;
    private double mouseY = 0;

    // keyboard state
    private LinkedList<Character> keysTyped = new LinkedList<Character>();

    // where to locate the frame on the screen
    // currLoc, currLoc
    // will be incremented for every new frame
    private static final int LOC_INCR = 50;
    private static final int MAX_LOCATION = 500;
    private static final int INIT_LOCATION = 20;
    private static int currLoc = INIT_LOCATION;
    private static Object currLocLock = new Object();

    public GenDraw(String title) { 
	init(title);
    }

    public GenDraw() { 
	init();
    }

    /**
     * Set the window size to w-by-h pixels
     *
     * @param w the width as a number of pixels
     * @param h the height as a number of pixels
     * @throws a RunTimeException if the width or height is 0 or negative.
     */
    public void setCanvasSize(int w, int h) {
        if (w < 1 || h < 1) throw new RuntimeException("width and height must be positive");
        width = w;
        height = h;
        init();
    }

    private void init() {
	init(title);
    }

    // init
    private void init(String s) {
        if (frame != null) frame.setVisible(false);
        frame = new JFrame();
	frame.setVisible(false);

	synchronized (currLocLock) {
	    currLoc = currLoc + LOC_INCR;
	    if (currLoc >= MAX_LOCATION)
		currLoc = INIT_LOCATION;
	    frame.setLocation(currLoc, currLoc);
	}

        offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        onscreenImage  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        offscreen = offscreenImage.createGraphics();
        onscreen  = onscreenImage.createGraphics();
        setXscale();
        setYscale();
        offscreen.setColor(DEFAULT_CLEAR_COLOR);
        offscreen.fillRect(0, 0, width, height);
        setPenColor();
        setPenRadius();
        setFont();
        clear();

        // add antialiasing
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                                                  RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        offscreen.addRenderingHints(hints);

        // frame stuff
        ImageIcon icon = new ImageIcon(onscreenImage);
        JLabel draw = new JLabel(icon);

	draw.addMouseListener(mkMouseListener(this));
        draw.addMouseMotionListener(mkMouseMotionAdapter(this));

        frame.setContentPane(draw);
        frame.addKeyListener(mkKeyListener(this)); 	// JLabel cannot get eyboard focus

        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);            // closes all windows
        // frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);      // closes only current window
	title = s;
        frame.setTitle(title);
        frame.setJMenuBar(createMenuBar());
        frame.pack();
    }

    // create the menu bar (changed to private)
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem menuItem1 = new JMenuItem(" Save...   ");
        menuItem1.addActionListener(mkActionListener(this));
        menuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(menuItem1);
        return menuBar;
    }


   /*************************************************************************
    *  User and screen coordinate systems
    *************************************************************************/

    /**
     * Set the X scale to be the default
     */
    public void setXscale() { setXscale(DEFAULT_XMIN, DEFAULT_XMAX); }
    /**
     * Set the Y scale to be the default
     */
    public void setYscale() { setYscale(DEFAULT_YMIN, DEFAULT_YMAX); }
    /**
     * Set the X scale (a border is added to the values)
     * @param min the minimum value of the X scale
     * @param max the maximum value of the X scale
     */
    public void setXscale(double min, double max) {
        double size = max - min;
        xmin = min - BORDER * size;
        xmax = max + BORDER * size;
    }
    /**
     * Set the Y scale (a border is added to the values)
     * @param min the minimum value of the Y scale
     * @param max the maximum value of the Y scale
     */
    public void setYscale(double min, double max) {
        double size = max - min;
        ymin = min - BORDER * size;
        ymax = max + BORDER * size;
    }

    // helper functions that scale from user coordinates to screen coordinates and back
    private  double  scaleX(double x) { return width  * (x - xmin) / (xmax - xmin); }
    private  double  scaleY(double y) { return height * (ymax - y) / (ymax - ymin); }
    private  double factorX(double w) { return w * width  / Math.abs(xmax - xmin);  }
    private  double factorY(double h) { return h * height / Math.abs(ymax - ymin);  }
    private  double   userX(double x) { return xmin + x * (xmax - xmin) / width;    }
    private  double   userY(double y) { return ymax - y * (ymax - ymin) / height;   }


    /**
     * Clear the screen with the default color, white
     */
    public void clear() { clear(DEFAULT_CLEAR_COLOR); }
    /**
     * Clear the screen with the given color.
     * @param color the Color to make the background
     */
    public  void clear(Color color) {
        offscreen.setColor(color);
        offscreen.fillRect(0, 0, width, height);
        offscreen.setColor(penColor);
	//	show();
    }

    /**
     * Set the pen size to the default
     */
    public void setPenRadius() { setPenRadius(DEFAULT_PEN_RADIUS); }
    /**
     * Set the pen size to the given size 
     * @param r the radius of the pen
     * @throws RuntimeException if r is negative
     */
    public void setPenRadius(double r) {
        if (r < 0) throw new RuntimeException("pen radius must be positive");
        penRadius = r * DEFAULT_SIZE;
        // BasicStroke stroke = new BasicStroke((float) penRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke stroke = new BasicStroke((float) penRadius);
        offscreen.setStroke(stroke);
    }

    /**
     * Set the pen color to the default which is BLACK.
     */
    public void setPenColor() { setPenColor(DEFAULT_PEN_COLOR); }
    /**
     * Set the pen color to the given color. The available pen colors are 
       BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA, 
       ORANGE, PINK, RED, WHITE, and YELLOW.
     * @param color the Color to make the pen
     */
    public void setPenColor(Color color) {
        penColor = color;
        offscreen.setColor(penColor);
    }

    /* From: http://stackoverflow.com/questions/223971/how-to-generate-spectrum-color-palettes */
    /** 
     * Generate an array of n distict colors */
    public static Color[] generateColorMap(int n) {
        Color[] cols = new Color[n];
        for(int i = 0; i < n; i++)  {
	    cols[i] = Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
	}
        return cols;
    }



    /**
     * Set the font to be the default for all string writing
     */
    public void setFont() { setFont(DEFAULT_FONT); }
    /**
     * Set the font as given for all string writing
     * @param f the font to make text
     */
    public  void setFont(Font f) { font = f; }


   /*************************************************************************
    *  Drawing geometric shapes.
    *************************************************************************/

    /**
     * Draw a line from (x0, y0) to (x1, y1)
     * @param x0 the x-coordinate of the starting point
     * @param y0 the y-coordinate of the starting point
     * @param x1 the x-coordinate of the destination point
     * @param y1 the y-coordinate of the destination point
     */
    public void line(double x0, double y0, double x1, double y1) {
        offscreen.draw(new Line2D.Double(scaleX(x0), scaleY(y0), scaleX(x1), scaleY(y1)));
	//	show();
    }

    /**
     * Draw one pixel at (x, y)
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     */
    public void pixel(double x, double y) {
        offscreen.fillRect((int) Math.round(scaleX(x)), (int) Math.round(scaleY(y)), 1, 1);
    }

    /**
     * Draw a point at (x, y)
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     */
    public void point(double x, double y) {
        double xs = scaleX(x);
        double ys = scaleY(y);
        double r = penRadius;
        // double ws = factorX(2*r);
        // double hs = factorY(2*r);
        // if (ws <= 1 && hs <= 1) pixel(x, y);
        if (r <= 1) pixel(x, y);
        else offscreen.fill(new Ellipse2D.Double(xs - r/2, ys - r/2, r, r));
	//	show();
    }

    /**
     * Draw circle of radius r, centered on (x, y); degenerate to pixel if small
     * @param x the x-coordinate of the center of the circle
     * @param y the y-coordinate of the center of the circle
     * @param r the radius of the circle
     * @throws RuntimeException if the radius of the circle is negative
     */
    public void circle(double x, double y, double r) {
        if (r < 0) throw new RuntimeException("circle radius can't be negative");
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(2*r);
        double hs = factorY(2*r);
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else offscreen.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	//	show();
    }

    /**
     * Draw filled circle of radius r, centered on (x, y); degenerate to pixel if small
     * @param x the x-coordinate of the center of the circle
     * @param y the y-coordinate of the center of the circle
     * @param r the radius of the circle
     * @throws RuntimeException if the radius of the circle is negative
     */
    public void filledCircle(double x, double y, double r) {
        if (r < 0) throw new RuntimeException("circle radius can't be negative");
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(2*r);
        double hs = factorY(2*r);
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else offscreen.fill(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	//	show();
    }

    /**
     * Draw an arc of radius r, centered on (x, y), from angle1 to angle2 (in degrees). 
     * @param x the x-coordinate of the center of the circle
     * @param y the y-coordinate of the center of the circle
     * @param r the radius of the circle
     * @param angle1 the starting angle. 0 would mean an arc beginning at 3 o'clock.
     * @param angle2 the angle at the end of the arc. For example, if 
     *        you want a 90 degree arc, then angle2 should be angle1 + 90.
     * @throws RuntimeException if the radius of the circle is negative
     */
    public void arc(double x, double y, double r, double angle1, double angle2) {
        if (r < 0) throw new RuntimeException("arc radius can't be negative");
        while (angle2 < angle1) angle2 += 360;
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(2*r);
        double hs = factorY(2*r);
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else offscreen.draw(new Arc2D.Double(xs - ws/2, ys - hs/2, ws, hs, angle1, angle2 - angle1, Arc2D.OPEN));
	//	show();
    }

    /**
     * Draw squared of side length 2r, centered on (x, y); degenerate to pixel if small
     * @param x the x-coordinate of the center of the square
     * @param y the y-coordinate of the center of the square
     * @param r radius is half the length of any side of the square
     * @throws RuntimeException if r is negative
     */
    public void square(double x, double y, double r) {
        if (r < 0) throw new RuntimeException("square side length can't be negative");
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(2*r);
        double hs = factorY(2*r);
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else offscreen.draw(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	//	show();
    }

    /**
     * Draw a filled square of side length 2r, centered on (x, y); degenerate to pixel if small
     * @param x the x-coordinate of the center of the square
     * @param y the y-coordinate of the center of the square
     * @param r radius is half the length of any side of the square
     * @throws RuntimeException if r is negative
     */
    public void filledSquare(double x, double y, double r) {
        if (r < 0) throw new RuntimeException("square side length can't be negative");
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(2*r);
        double hs = factorY(2*r);
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else offscreen.fill(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	//	show();
    }
    
    /**
     * custom function:
     * MAKE MAP TILE (square), with four arrows
     * @param x the x-coordinate of the center of the tile
     * @param y the y-coordinate of the center of the tile
     * @param r radius is half the length of any side of the square
     * @param n boolean. True if north != null
     * @param s True if south != null
     * @param e True if east != null
     * @param w True if west != null
     * @throws RuntimeException if r is negative
     */
    public void mapTile(double x, double y, double r, boolean n, boolean s, boolean e, boolean w) {
    	setPenColor(GenDraw.LIGHT_GRAY);
    	filledSquare(x,y,r);
    	setPenColor(GenDraw.WHITE);
		filledSquare(x-0.6*r,y-0.6*r,0.4*r); //sw corner
		filledSquare(x+0.6*r,y-0.6*r,0.4*r); //se corner
		filledSquare(x-0.6*r,y+0.6*r,0.4*r); //nw corner
		filledSquare(x+0.6*r,y+0.6*r,0.4*r); //ne corner
		setPenColor(GenDraw.BLACK);
		filledCircle(x, y, 0.04);		
		if (n) {
			textsmall(x+0.015, y+0.23, "^");
		}
		if (s) {
			textsmall(x-0.01, y-0.31, "v");
		}
		if (e) {
			textsmall(x+0.27, y-0.02, ">");
		}
		if (w) {
			textsmall(x-0.27, y-0.02, "<");
		}
		
    }
          
    
    
    
    

    /**
     * Draw a polygon with the given (x[i], y[i]) coordinates
     * @param x an array of all the x-coordindates of the polygon
     * @param y an array of all the y-coordindates of the polygon
     */
    public void polygon(double[] x, double[] y) {
        int N = x.length;
        GeneralPath path = new GeneralPath();
        path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
        for (int i = 0; i < N; i++)
            path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
        path.closePath();
        offscreen.draw(path);
	//	show();
    }

    /**
     * Draw a filled polygon with the given (x[i], y[i]) coordinates
     * @param x an array of all the x-coordindates of the polygon
     * @param y an array of all the y-coordindates of the polygon
     */
    public void filledPolygon(double[] x, double[] y) {
        int N = x.length;
        GeneralPath path = new GeneralPath();
        path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
        for (int i = 0; i < N; i++)
            path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
        path.closePath();
        offscreen.fill(path);
	//	show();
    }



   /*************************************************************************
    *  Drawing images.
    *************************************************************************/

    // get an image from the given filename
    public Image getImage(String filename) {

        // to read from file
        ImageIcon icon = new ImageIcon(filename);

        // try to read from URL
        if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
            try {
                URL url = new URL(filename);
                icon = new ImageIcon(url);
            } catch (Exception e) { /* not a url */ }
        }

        // in case file is inside a .jar
        if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
            URL url = GenDraw.class.getResource(filename);
            if (url == null) throw new RuntimeException("image " + filename + " not found");
            icon = new ImageIcon(url);
        }

        return icon.getImage();
    }

    /**
     * Draw picture (gif, jpg, or png) centered on (x, y).
     * @param x the center x-coordinate of the image
     * @param y the center y-coordinate of the image
     * @param s the name of the image/picture, e.g., "ball.gif"
     * @throws RuntimeException if the image's width or height are negative
     */
    public void picture(double x, double y, String s) {
        Image image = getImage(s);
        double xs = scaleX(x);
        double ys = scaleY(y);
        int ws = image.getWidth(null);
        int hs = image.getHeight(null);
        if (ws < 0 || hs < 0) throw new RuntimeException("image " + s + " is corrupt");

        offscreen.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
	//	show();
    }

    /**
     * Draw picture (gif, jpg, or png) centered on (x, y),
     * rotated given number of degrees
     * @param x the center x-coordinate of the image
     * @param y the center y-coordinate of the image
     * @param s the name of the image/picture, e.g., "ball.gif"
     * @param degrees is the number of degrees to rotate counterclockwise
     * @throws RuntimeException if the image's width or height are negative
     */
    public void picture(double x, double y, String s, double degrees) {
        Image image = getImage(s);
        double xs = scaleX(x);
        double ys = scaleY(y);
        int ws = image.getWidth(null);
        int hs = image.getHeight(null);
        if (ws < 0 || hs < 0) throw new RuntimeException("image " + s + " is corrupt");

        offscreen.rotate(Math.toRadians(-degrees), xs, ys);
        offscreen.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
        offscreen.rotate(Math.toRadians(+degrees), xs, ys);

	//	show();
    }

    /**
     * Draw picture (gif, jpg, or png) centered on (x, y). 
     * Rescaled to w-by-h.
     * @param x the center x coordinate of the image
     * @param y the center y coordinate of the image
     * @param s the name of the image/picture, e.g., "ball.gif"
     * @param w the width of the image
     * @param h the height of the image
     */
    public void picture(double x, double y, String s, double w, double h) {
        Image image = getImage(s);
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(w);
        double hs = factorY(h);
        if (ws < 0 || hs < 0) throw new RuntimeException("image " + s + " is corrupt");
        if (ws <= 1 && hs <= 1) pixel(x, y);
        else {
            offscreen.drawImage(image, (int) Math.round(xs - ws/2.0),
                                       (int) Math.round(ys - hs/2.0),
                                       (int) Math.round(ws),
                                       (int) Math.round(hs), null);
        }
	//	show();
    }


    /**
     * Draw picture (gif, jpg, or png) centered on (x, y),
     * rotated given number of degrees, rescaled to w-by-h.
     * @param x the center x-coordinate of the image
     * @param y the center y-coordinate of the image
     * @param s the name of the image/picture, e.g., "ball.gif"
     * @param w the width of the image
     * @param h the height of the image
     * @param degrees is the number of degrees to rotate counterclockwise
     * @throws RuntimeException if the image's width or height are negative
     */
    public void picture(double x, double y, String s, double w, double h, double degrees) {
        Image image = getImage(s);
        double xs = scaleX(x);
        double ys = scaleY(y);
        double ws = factorX(w);
        double hs = factorY(h);
        if (ws < 0 || hs < 0) throw new RuntimeException("image " + s + " is corrupt");
        if (ws <= 1 && hs <= 1) pixel(x, y);

        offscreen.rotate(Math.toRadians(-degrees), xs, ys);
        offscreen.drawImage(image, (int) Math.round(xs - ws/2.0),
                                   (int) Math.round(ys - hs/2.0),
                                   (int) Math.round(ws),
                                   (int) Math.round(hs), null);
        offscreen.rotate(Math.toRadians(+degrees), xs, ys);

	//	show();
    }


   /*************************************************************************
    *  Drawing text.
    *************************************************************************/

    /**
     * Write the given text string in the current font, center on (x, y).
     * @param x the center x coordinate of the text
     * @param y the center y coordinate of the text
     * @param s the text
     */
    public void text(double x, double y, String s, double targetLen) {
        offscreen.setFont(font);
        FontMetrics metrics = offscreen.getFontMetrics();
        double xs = scaleX(x);
        double ys = scaleY(y);
	String cs = clipString(s, targetLen);
        int ws = metrics.stringWidth(cs);
        int hs = metrics.getDescent();
        offscreen.drawString(cs, (float) (xs - ws/2.0), (float) (ys + hs));
       	//	show();
    }

    public void text(double x, double y, String s) {
	text(x, y, s, width);
    }
    
    /**
     * Write given text string in smaller font size (custom function)
     * @param x the center x coordinate of the text
     * @param y the center y coordinate of the text
     * @param s the text
     */
    public void textsmall(double x, double y, String s) {
    	 offscreen.setFont(SMALL_FONT);
         FontMetrics metrics = offscreen.getFontMetrics();
         double xs = scaleX(x);
         double ys = scaleY(y); 	
         int ws = metrics.stringWidth(s);
         int hs = metrics.getDescent();
         offscreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
    }

    private double strWidth(String s) {
	FontMetrics metrics = offscreen.getFontMetrics();
        return metrics.stringWidth(s);
    }

    private String clipString(String s, double targetLen) {

	if (s.length() <= 1) {
	    return s;
	}
	    
        double ws = strWidth(s);
	if (ws <= targetLen) {
	    return s;
	}

	int halfLen = s.length()/2;
	String left = s.substring(0, halfLen);
	double wsLeft = strWidth(left);
	if (wsLeft == targetLen) {
	    return left;
	} else if (wsLeft < targetLen) {
	    String rs = s.substring(halfLen+1, s.length());
	    return left +  clipString(rs,  targetLen-wsLeft);
	} else if (wsLeft > targetLen) {
	    return clipString(left, targetLen);
	}

	// should not get here.
	return null;
    }

    /**
     * Write the given text string in the current font, center on (x, y).
     * @param x the center x coordinate of the text
     * @param y the center y coordinate of the text
     * @param s the text
     */
    public void rotText(double x, double y, double degrees, String s, 
			double targetLen) {
        offscreen.setFont(font);
        FontMetrics metrics = offscreen.getFontMetrics();
        double xs = scaleX(x);
        double ys = scaleY(y);
	String cs = clipString(s, targetLen);
        int ws = metrics.stringWidth(cs);
        int hs = metrics.getDescent();
        offscreen.rotate(Math.toRadians(-degrees), xs, ys);
        offscreen.drawString(cs, (float) (xs - ws/2.0), (float) (ys + hs));
        offscreen.rotate(Math.toRadians(+degrees), xs, ys);
	//	show();
    }

    public void rotText(double x, double y, double degrees, String s) {
	double targetLen = Math.sqrt((double) (width*width + height*height));
	rotText(x, y, degrees, s, (int) targetLen);
    }

    // return the width of the string s in pixels.
    public int textWidth(String s) {
        FontMetrics metrics = offscreen.getFontMetrics();
        return  metrics.stringWidth(s);
    }

    // return the width of the string s in pixels.
    public int textDescent() {
        FontMetrics metrics = offscreen.getFontMetrics();
        return  metrics.getDescent();
    }

    /**
     * Display on screen and pause for t milliseconds.
     * Calling this method means that the screen will NOT be redrawn
     * after each line(), circle(), or square(). This is useful when there
     * are many methods to call to draw a complete picture.
     * @param t number of milliseconds
     */
    public void show(int t) {
        defer = true;
        onscreen.drawImage(offscreenImage, 0, 0, null);
        frame.repaint();
        try { Thread.currentThread().sleep(t); }
        catch (InterruptedException e) { System.out.println("Error sleeping"); }
    }


    /**
     * Display on-screen;
     * calling this method means that the screen WILL be redrawn
     * after each line(), circle(), or square(). This is the default.
     */
    public void show() {
        frame.requestFocusInWindow();
        frame.setVisible(true);
        if (!defer) onscreen.drawImage(offscreenImage, 0, 0, null);
        if (!defer) frame.repaint();
    }


   /*************************************************************************
    *  Save drawing to a file.
    *************************************************************************/

    /**
     * Save to file - suffix must be png, jpg, or gif.
     * @param filename the name of the file with one of the required suffixes
     */
    public void save(String filename) {
        File file = new File(filename);
        String suffix = filename.substring(filename.lastIndexOf('.') + 1);

        // png files
        if (suffix.toLowerCase().equals("png")) {
            try { ImageIO.write(offscreenImage, suffix, file); }
            catch (IOException e) { e.printStackTrace(); }
        }

        // need to change from ARGB to RGB for jpeg
        // reference: http://archives.java.sun.com/cgi-bin/wa?A2=ind0404&L=java2d-interest&D=0&P=2727
        else if (suffix.toLowerCase().equals("jpg")) {
            WritableRaster raster = offscreenImage.getRaster();
            WritableRaster newRaster;
            newRaster = raster.createWritableChild(0, 0, width, height, 0, 0, new int[] {0, 1, 2});
            DirectColorModel cm = (DirectColorModel) offscreenImage.getColorModel();
            DirectColorModel newCM = new DirectColorModel(cm.getPixelSize(),
                                                          cm.getRedMask(),
                                                          cm.getGreenMask(),
                                                          cm.getBlueMask());
            BufferedImage rgbBuffer = new BufferedImage(newCM, newRaster, false,  null);
            try { ImageIO.write(rgbBuffer, suffix, file); }
            catch (IOException e) { e.printStackTrace(); }
        }

        else {
            System.out.println("Invalid image file type: " + suffix);
        }
    }


    /**
     * Open a save dialog when the user selects "Save As" from the menu
     */

    private ActionListener mkActionListener(GenDraw arg) {
	final GenDraw gd = arg;
	ActionListener al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    FileDialog chooser = new FileDialog(gd.frame, "Use a .png or .jpg extension", FileDialog.SAVE);
		    chooser.setVisible(true);
		    String filename = chooser.getFile();
		    if (filename != null) {
			gd.save(chooser.getDirectory() + File.separator + chooser.getFile());
		    }
		}
	    };
	return al;
    }

   /*************************************************************************
    *  Mouse interactions.
    *************************************************************************/

    /**
     * Where is the mouse?
     * @return the value of the x-coordinate of the mouse
     */
    public  double mouseX() {
	synchronized (mouseLock) {
	    return mouseX;
	}
    }
    
    /**
     * Where is the mouse?
     * @return the value of the y-coordinate of the mouse
     */
    public  double mouseY() {
	synchronized (mouseLock) {
	    return mouseY;
	}
    }
	
    private MouseListener mkMouseListener(GenDraw arg) {
	final GenDraw gd = arg;
	MouseListener ml = new MouseListener() {
		/**
		 * Is the mouse being pressed?
		 * @return true or false
		 */
		public boolean mousePressed() {
		    synchronized (gd.mouseLock) {
			return gd.mousePressed;
		    }
		}
	
		public void mouseClicked(MouseEvent e) { }
		
		public void mouseEntered(MouseEvent e) { }
		
		public void mouseExited(MouseEvent e) { }

		public void mousePressed(MouseEvent e) {
		    synchronized (gd.mouseLock) {
			gd.mouseX = gd.userX(e.getX());
			gd.mouseY = gd.userY(e.getY());
			gd.mousePressed = true;
		    }
		}

		public void mouseReleased(MouseEvent e) {
		    synchronized (gd.mouseLock) {
			gd.mousePressed = false;
		    }
		}
	    };
	return ml;
    }

    private MouseMotionAdapter mkMouseMotionAdapter(GenDraw arg) {
	final GenDraw gd = arg;
	MouseMotionAdapter mma = new MouseMotionAdapter() {
		public void mouseDragged(MouseEvent e)  {
		    synchronized (gd.mouseLock) {
			gd.mouseX = gd.userX(e.getX());
			gd.mouseY = gd.userY(e.getY());
		    }
		}

		public void mouseMoved(MouseEvent e) {
		    synchronized (gd.mouseLock) {
			gd.mouseX = gd.userX(e.getX());
			gd.mouseY = gd.userY(e.getY());
		    }
		}    
		
	    };
	return mma;
    }
	    
   /*************************************************************************
    *  Keyboard interactions.
    *************************************************************************/

    /**
     * Has the user typed a key?
     * @return true if the user has typed a key, false otherwise
     */
    public  boolean hasNextKeyTyped() {
        synchronized (keyLock) {
            return !keysTyped.isEmpty();
        }
    }

    /**
     * What is the next key that was typed by the user?
     * @return the next key typed
     */
    public char nextKeyTyped() {
	synchronized (keyLock) {
	    return keysTyped.removeLast();
	}
    }

    private KeyListener mkKeyListener(GenDraw arg) {
	final GenDraw gd = arg;
	KeyListener kl = new KeyListener() { 

		public void keyTyped(KeyEvent e) {
		    synchronized (gd.keyLock) {
			gd.keysTyped.addFirst(e.getKeyChar());
		    }
		}

		public void keyPressed(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }
	    };

	return kl;
    };




    // test client
    private static GenDraw testGD(String s) {
	GenDraw gd = new GenDraw(s);

        gd.square(.2, .8, .1);
        gd.filledSquare(.8, .8, .2);
        gd.circle(.8, .2, .2);

        gd.setPenColor(GenDraw.MAGENTA);
        gd.setPenRadius(.02);
        gd.arc(.8, .2, .1, 200, 45);

        // draw a blue diamond
        gd.setPenRadius();
        gd.setPenColor(GenDraw.BLUE);
        double[] x = { .1, .2, .3, .2 };
        double[] y = { .2, .3, .2, .1 };
        gd.filledPolygon(x, y);

        // text
        gd.setPenColor(GenDraw.BLACK);
        gd.text(0.2, 0.5, "black text");
        gd.setPenColor(GenDraw.WHITE);
        gd.text(0.8, 0.8, "white text");

	return gd;
    }

    public static void main(String[] args) {
	/*
	GenDraw foo = new GenDraw("hello");
	int size = (int) 512;
	foo.setCanvasSize(size, size);
	foo.setXscale(0.0, size);
	foo.setYscale(0.0, size);

	int useXmin = 0.0;
	int userXmax = 2000;

	int useYmin = 0.0;
	int userYmax = 2000;

	int hs = foo.textDescent();
	int border = 10;
	int hash = 10;
	int hashLabelWidth = 
	    foo.textWidth(String.format("%.0f", 1999.0));
	int gap = 20;
	
	int x0 = border+hash+hashLabelWidth+gap;
	int x1 = size - border - gap;
	int xw = (x1-x0);
	double tw = (userXmax-userXmin)/xw;

	int y0 = border+hash+hashLabelWidth + gap;
	int y1 = size - border - gap;
	int yh = (y1-y0);
	double th = (userYmax-userYmin)/yh;

	foo.line(x0, y0, x1, y0);
	foo.line(x0, y0, x0, y1);

	int inc = 100;
	int i;
	int label = inc;


	for (i=userXmin+inc; i < (userXmax; i+=inc) {
cd
	    int xt = (i)*tw + x0;
	    foo.line(i,  y0+hash/2, i, y0-hash/2);
	    String s = String.format("%.0f", (double) label);
	    foo.rotText(i, y0-hash/2-hashLabelWidth/2 - gap/2, 90, s);
	    label += inc;
	}

	label = inc;
	for (i=y0+inc; i < y1; i+=inc) {
	    foo.line(x0+hash/2, i, x0-hash/2, i);
	    String s = String.format("%.0f", (double) label);
	    foo.text(x0-hash/2-hashLabelWidth/2-gap, i, s);
	    label += inc;
	}

	foo.text(x0+(x1-x0)/2, y0-hash/2-hashLabelWidth - gap*2, "Quantity");
	foo.rotText(x0-hash/2-hashLabelWidth-gap*2, y0+(y1-y0)/2,
		    90, "Price");
	*/
    }

}
