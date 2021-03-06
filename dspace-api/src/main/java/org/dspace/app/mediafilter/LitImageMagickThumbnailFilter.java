/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.MediaFilter;
import org.dspace.app.mediafilter.SelfRegisterInputFormats;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.ProcessStarter;

import org.dspace.core.ConfigurationManager;

/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public abstract class LitImageMagickThumbnailFilter extends MediaFilter implements SelfRegisterInputFormats
{
    private static Logger log = Logger.getLogger(LitImageMagickThumbnailFilter.class);

    static int width;
    static int height;

    static {
        String pre = LitImageMagickThumbnailFilter.class.getName();
        String s = ConfigurationManager.getProperty(pre + ".ProcessStarter");
        ProcessStarter.setGlobalSearchPath(s);
        width = ConfigurationManager.getIntProperty("thumbnail.maxwidth", 180);
        height = ConfigurationManager.getIntProperty("thumbnail.maxheight", 120);
    }
	
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     *  
     */
    public String getBundleName()
    {
        return "THUMBNAIL";
    }

    /**
     * @return String bitstreamformat
     */
    public String getFormatString()
    {
        return "JPEG";
    }

    /**
     * @return String description
     */
    public String getDescription()
    {
        return "LIT Thumbnail";
    }

    public static File inputStreamToTempFile(InputStream source, String prefix, String suffix) throws IOException {
        File tempDirectory = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
        tempDirectory.mkdirs();
        File f = new File(tempDirectory.getPath() + File.separatorChar + prefix + suffix);
		f.deleteOnExit();
    	FileOutputStream fos = new FileOutputStream(f);
    	
    	byte[] buffer = new byte[1024];
    	int len = source.read(buffer);
    	while (len != -1) {
    		fos.write(buffer, 0, len);
    		len = source.read(buffer);
    	}
    	fos.close();
		return f;
    }
    
    public static  File getThumbnailFile(File f) throws IOException, InterruptedException, IM4JavaException {
    	File f2 = new File(f.getParentFile(), f.getName() + ".jpg");
    	f2.deleteOnExit();
    	ConvertCmd cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
        op.size(width*2, height*2);
		op.addImage(f.getAbsolutePath());
        op.thumbnail(width, height, "^");
        op.gravity("center");
        op.extent(width, height);
		op.addImage(f2.getAbsolutePath());
        log.debug("THUMB: " + op);
		cmd.run(op);
		return f2;
    }
    
    public static File getImageFile(File f, int page) throws IOException, InterruptedException, IM4JavaException {
    	File f2 = new File(f.getParentFile(), f.getName() + ".jpg");
    	f2.deleteOnExit();
    	ConvertCmd cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
		String s = "[" + page + "]";
		op.addImage(f.getAbsolutePath()+s);
		op.addImage(f2.getAbsolutePath());
		log.info("IMAGE: "+op);
		cmd.run(op);
		return f2;
    }
    
    public boolean preProcessBitstream(Context c, Item item, Bitstream source)
            throws Exception
    {
    	String nsrc = source.getName();
    	for(Bundle b: item.getBundles("THUMBNAIL")) {
    		for(Bitstream bit: b.getBitstreams()) {
    			String n = bit.getName();
    			if (n != null) {
    				if (nsrc != null) {
    					if (!n.startsWith(nsrc)) continue;
    				}
    			}
     	    	String description = bit.getDescription();
    	    	//If anything other than a generated thumbnail is found, halt processing
    	    	if (description != null) {
        	    	if (description.endsWith("Generated Thumbnail")) continue;
        	    	if (description.endsWith("LIT Thumbnail")) continue;    	    		
    	    	}
    			System.out.println("Custom Thumbnail exists for " + nsrc + " for item " + item.getHandle() + ".  Thumbnail will not be generated. ");
    			return false;
    		}
    	}
    	
    	
        return true; //assume that the thumbnail is a custom one
    }

     public String[] getInputMIMETypes()
    {
        return ImageIO.getReaderMIMETypes();
    }

    public String[] getInputDescriptions()
    {
        return null;
    }

    public String[] getInputExtensions()
    {
        // Temporarily disabled as JDK 1.6 only
        // return ImageIO.getReaderFileSuffixes();
        return null;
    }
}
