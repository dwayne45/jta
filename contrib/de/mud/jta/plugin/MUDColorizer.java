package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.PluginBus;
import de.mud.jta.PluginConfig;
import de.mud.jta.FilterPlugin;
import de.mud.jta.event.EndOfRecordRequest;
import de.mud.jta.event.EndOfRecordListener;

import java.io.IOException;

import gnu.regexp.RE;
import gnu.regexp.REException;

/** 
 * Some little hack for colors and prompts.
 * We are using GNU, so we should release this under the GPL :)
 * <ul>
 * <li>needs gnu.regexp package (approx. 22 kB)
 * <li>handles prompt with EOR (maybe buggy, but testet with mglib 3.2.6)
 * <li>colorizes single lines using regular expressions
 * </ul>
 * @author Thomas Kriegelstein
 */
public class MUDColorizer extends Plugin
    implements FilterPlugin, EndOfRecordListener {

    public static String BLACK   = "[30m";
    public static String RED     = "[31m";
    public static String BRED    = "[1;31m";
    public static String GREEN   = "[32m";
    public static String BGREEN  = "[1;32m";
    public static String YELLOW  = "[33m";
    public static String BYELLOW = "[1;33m";
    public static String BLUE    = "[34m";
    public static String BBLUE   = "[1;34m";
    public static String PINK    = "[35m";
    public static String BPINK   = "[1;35m";
    public static String CYAN    = "[36m";
    public static String BCYAN   = "[1;36m";
    public static String WHITE   = "[37m";
    public static String BWHITE  = "[1;37m";
    public static String NORMAL  = "[0m";
    public static String BOLD    = "[1m";

    /*  Prompthandling:
     *  if we do have a prompt, in every read of a new line, write a
     *  Clearline (ie. \r\e[K), then write the text and then
     *  rewrite the prompt after the last \n
     */

    private java.util.Hashtable exps = new java.util.Hashtable();

    public MUDColorizer(PluginBus bus, final String id) {
	super(bus, id);
	bus.registerPluginListener(this);
	try {
	    exps.put(new RE("\\[Allgemein:.*\\].*"),BYELLOW);
	    if (true) {
		exps.put(new RE("\\[Abenteuer:.*\\].*"),YELLOW);
		exps.put(new RE("\\[Magier:.*\\].*"),BGREEN);
		exps.put(new RE("\\[D-chat:.*\\].*"),GREEN);
		exps.put(new RE("\\[D-code:.*\\].*"),BCYAN);
		exps.put(new RE(".* sagt: .*$"),BYELLOW);
		exps.put(new RE(".* teilt Dir mit: .*"),CYAN);
		exps.put(new RE("Du sagst: .*"),BOLD);
		exps.put(new RE("Es gibt .* Ausga[e]?ng[e]?: .*\\..*"),CYAN);
		exps.put(new RE("Ein.* .*\\. \\[/.*/.*\\]*.*"),RED);
	    }
	} catch (Exception e) {
	    System.err.println("Something wrong with regexp: "+exps.size());
	    System.err.println(e);
	}
    }
    
    FilterPlugin source;
    
    public void setFilterSource(FilterPlugin source) {
	this.source = source;
    }

    public void EndOfRecord() {
	readprompt=true;
    }
    private byte[] transpose(byte[] buf) {
	byte[] nbuf;
	int nbufptr = 0;
	nbuf = new byte[8192];

	/* Prompthandling I */
	if (promptwritten && prompt != null&& prompt.length > 0) {
	    // "unwrite"
	    nbuf[nbufptr++] = (byte)'\r';
	    nbuf[nbufptr++] = 27;
	    nbuf[nbufptr++] = (byte)'[';
	    nbuf[nbufptr++] = (byte)'K';
	    promptwritten=false;
	}
        if (readprompt) {
	    int index;
	    for(index = buf.length-1; index >= 0; index--)
		if (buf[index] == '\n') break;
	    index++;
	    prompt=new byte[buf.length-index];
	    System.arraycopy(buf,index,prompt,0,buf.length-index);
	    readprompt = false; writeprompt=true;
	    promptwritten=false; promptread=true;
	    // System.out.println("Neues Prompt: $"+new String(prompt)+"$");
	}
	/* /Prompthandling I */

	/* Colorhandling should be done herein
	 * Problem:  Strings aren�t allways transposed completely
	 *           sometimes a \n is in the next transpose buffer
	 * Solution: Buffer lines outside like read does
	 */
	if (promptwritten) { lp=0; line[0]=0; }

	for (int i = 0; i < buf.length; i++, lp++) {
	    // nbuf[nbufptr++] = buf[i];
	    line[lp]=buf[i];
	    if (line[lp]=='\n') {
		String l=new String(line,0,lp+1);
		boolean colored=false;
		java.util.Enumeration keys = exps.keys();
		while(!colored && keys.hasMoreElements()) {
		    RE exp = (RE)keys.nextElement();
		    if (null!=exp.getMatch(l)) {
			byte[] color=(byte[])((String)exps.get(exp)).getBytes();
			System.arraycopy(color,0,nbuf,nbufptr,color.length);
			nbufptr+=color.length;
			System.arraycopy(line,0,nbuf,nbufptr,lp+1);
			nbufptr+=lp+1;
			byte[] normal = NORMAL.getBytes();
			System.arraycopy(normal,0,nbuf,nbufptr,normal.length);
			nbufptr+=normal.length;
			colored=true;
		    }
		}
		if (!colored) {
		    System.arraycopy(line,0,nbuf,nbufptr,lp+1);
		    nbufptr+=lp+1;
		}
		colored=false;
		lp=-1;
		line[0]=0; // gets overwritten soon;
	    }
	}
	if (promptread) { lp=0; line[0]=0; promptread=false; }
	/* /Colorhandling */

	/* Prompthandling II */
	if (buf[buf.length-1]=='\n') writeprompt=true;
	if (buf[buf.length-1]=='\r') writeprompt=true;
	if (writeprompt && prompt != null&& prompt.length > 0) {
	    // "rewrite"
	    nbuf[nbufptr++] = (byte)'\r';
	    nbuf[nbufptr++] = 27;
	    nbuf[nbufptr++] = (byte)'[';
	    nbuf[nbufptr++] = (byte)'K';
	    System.arraycopy(prompt,0,nbuf,nbufptr,prompt.length);
	    nbufptr+=prompt.length;
	    promptwritten=true;
	    writeprompt=false;
	}
	/* /Promphandling II */

	byte[] xbuf = new byte[nbufptr];
	System.arraycopy(nbuf, 0, xbuf, 0, nbufptr);
	return xbuf;
    }

    // einzufaerbende zeile
    private int     lp            = 0;
    private byte[]  line          = new byte[8192];
    // prompt handeln
    private boolean readprompt    = false;
    private boolean promptread    = false;
    private boolean writeprompt   = false;
    private boolean promptwritten = false;
    private byte[]  prompt        = null;
    // bufferoverflows handeln
    private byte[]  buffer        = null;
    private int     pos           = 0;

    public int read(byte[] b) throws IOException {
	// empty the buffer before reading more data
	if(buffer != null) {
	    int amount = (buffer.length - pos) <= b.length ? 
		buffer.length - pos : b.length;
	    System.arraycopy(buffer, pos, b, 0, amount);
	    if(pos + amount < buffer.length) {
		pos += amount;
	    } else {
		buffer = null;
		pos = 0;
	    }
	    return amount;
	}

	// now we are sure the buffer is empty and read on 
	int n = source.read(b);
	if(n > 0) {
	    byte[] tmp = new byte[n];
	    System.arraycopy(b, 0, tmp, 0, n);
	    buffer = transpose(tmp);
	    if(buffer != null && buffer.length > 0) {
		int amount = buffer.length <= b.length ? buffer.length : b.length;
		System.arraycopy(buffer, 0, b, 0, amount);
		pos = n = amount;
		if(amount == buffer.length) {
		    buffer = null;
		    pos = 0;
		}
	    } else
		return 0;
	}
	return n;
    }

    public void write(byte[] b) throws IOException {
	if (b[b.length-1]=='\n')  { writeprompt=true; promptwritten=false; }
	source.write(b);
    }
}