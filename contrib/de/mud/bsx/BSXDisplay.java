/**
 * Implementation of a BSX Display capable of handling basic BSX-Calls.
 * Handles: @SCE, @DFS, @VIO, @RMO, @DFO, @RFS
 * Uses:    BSXCache, BSXObject, BSXScene
 */
package de.mud.bsx;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Canvas;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Frame;

import java.util.Enumeration;

public final class BSXDisplay extends Canvas
{
    /** The offscreen doublebuffer */
    protected Image picture;
    /** the scene-cache */
    protected BSXCache scenes = new BSXCache();
    /** the object-cache */
    protected BSXCache objects = new BSXCache();
    /** current scene name */
    protected String scene = "<undefined>";
    /** drawable scene data */
    protected BSXScene bsxscene;
    /** current scene data */
    protected BSXScene working;
    /** redraw info */
    protected boolean sceneChanged=true;
    /** my minimum Dimension */
    protected Dimension dim=new Dimension(512, 256);
    /** an empty scene (black rectangle) */
    protected static final int[][] EMPTY_SCENE = 
	new int[][] { { 4, 0, 0, 255, 255, 255, 255, 0, 0, 0 } }; 
    /**
     * @SCEscene.
     @return query for scene data if not in cache
     */
    public String showScene(String scene)
	{
	    String res=null;
	    working=(BSXScene)scenes.getEntry(scene);
	    if (working==null)
		{
		    working=new BSXScene(scene,newImage(),EMPTY_SCENE);
		    scenes.addEntry(scene,working);
		    res = "#RQS "+scene+"\n";
	            this.scene=scene;
		}
	    else
		{
		    if (!this.scene.equals(scene))
			{
			    this.scene=scene;
			    sceneChanged=true;
			    if (bsxscene!=null)
				bsxscene.clean();
			}
		    bsxscene=working;
		}
	    return res;
	}
    /**
     * @DFSscene.data
     */
    public void defineScene(String scene, int[][] data)
	{
	    BSXScene _bsxscene;
	    if (scenes.containsEntry(scene))
		{
		    _bsxscene = (BSXScene)scenes.getEntry(scene);
		    _bsxscene.setData(data);
		}
	    else
		{
		    _bsxscene=new BSXScene(scene,newImage(),data);
		    scenes.addEntry(scene,_bsxscene);
		}
	    if (this.scene.equals(scene))
		{
		    bsxscene=_bsxscene;
		    sceneChanged=true;
		}
	}
    /**
     * @VIOid.xy
     @return query for object data if not in cache
     */
    public String showObject(String obj, int x, int y)
	{
	    BSXObject bsxobject;
	    if (working!=null&&!working.containsObject(obj))
		{
		    working.addObject(obj,x,y);
		    sceneChanged=(working==bsxscene);
		}
	    if (!objects.containsEntry(obj))
		{
		    return "#RQO "+obj+"\n";
		}
	    return null;
	}
    /**
     * @RMOid.
     */
    public void removeObject(String obj)
	{
	    if (working.containsObject(obj))
		{
		    bsxscene.removeObject(obj);
		    sceneChanged=(working==bsxscene);
		}
	}
    /**
     * @DFOid.
     */
    public void defineObject(String id, int[][] data)
	{
	    if (objects.containsEntry(id))
		{
		    BSXObject r = objects.getEntry(id);
		    r.setData(data);
		}
	    else
		{
		    objects.addEntry(id,new BSXObject(id,data));
		}
	    sceneChanged=((bsxscene!=null)&&(bsxscene.containsObject(id)));
	}
    /**
     * @RFS
     */
    public void refreshScene()
	{
	    // Avoid double computing if not necessary
	    if (sceneChanged)
		{
		    sceneChanged=false;
		    redraw();
		    repaint();
		}
	}
    // Die eigentliche Malroutine
    private void redraw()
    {
	if (bsxscene==null) return;
	Graphics g;
	g=picture.getGraphics();
	bsxscene.fill(g,this);
	Enumeration ob_enum;
	for(int layer=7;layer>=0;layer--)
	    {
		ob_enum = bsxscene.objects(layer);
		while(ob_enum.hasMoreElements())
		    {
			String id = (String)ob_enum.nextElement();
			Point pos = bsxscene.locateObject(id);
			if (objects.containsEntry(id))
			    {
				BSXObject obj = objects.getEntry(id);
				obj.draw( g, pos.x, pos.y, this );
			    }    
		    }
	    }
    }
    public void paint(Graphics g)
    {
	g.drawImage( picture, 0, 0, this );
    }
    public void addNotify()
    {
	super.addNotify();
	picture = newImage();
    }
    /**
     * creates a new offscreenimage to draw on
     @return an offscreenimage
    */
    private Image newImage()
    {
	return createImage(dim.width, dim.height);
    }
    public void removeNotify()
    {
	picture.flush();
    }
    public Dimension getPreferredSize()
    {
	return dim;
    }
    public Dimension getMinimumSize()
    {
	return dim;
    }
    public static void main(String args[])
    {
	Frame f;
	BSXDisplay b;

	f = new Frame("BSXDisplay Test");
	b = new BSXDisplay();
	f.add(b);
	f.pack();
	f.setResizable(false);
	f.show();

	int[][] scene = new int[3][];
	scene[0] = new int[] { 0, 0,0, 255,0, 255,255, 0,255 };
	scene[1] = new int[] { 4, 127,127, 255,255, 0,255 };
	scene[2] = new int[] { 8, 120,120, 134,120, 134, 134, 120,134 };

	b.defineScene("int/picture",scene);
	b.showScene("int/picture");
	b.refreshScene();

	int[][] bsx = new int[1][];
	bsx[0] = new int[] { 2, 40,0,  215,0, 255,127, 0,127 };

	b.defineObject("rahmen",bsx);
	b.showObject("rahmen",7,7);
	b.refreshScene();
    } 
}
