/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Thumbnail;
import org.dspace.core.Context;

import java.sql.SQLException;

public class ItemService
{
    public static Thumbnail getThumbnail(Context context, int itemId, boolean requireOriginal) throws SQLException
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);

        Bitstream thumbBitstream = null;
        Bitstream primaryBitstream = dao.getPrimaryBitstream(itemId, "ORIGINAL");
        if (primaryBitstream != null)
        {
            if (primaryBitstream.getFormat().getMIMEType().equals("text/html"))
            {
                return null;
            }

            thumbBitstream = dao.getNamedBitstream(itemId, "THUMBNAIL", primaryBitstream.getName() + ".jpg");
        }
        else
        {
            if (requireOriginal)
            {
                primaryBitstream = dao.getFirstBitstream(itemId, "ORIGINAL");
            }
            
            thumbBitstream   = dao.getFirstBitstream(itemId, "THUMBNAIL");
        }

        if (thumbBitstream != null)
        {
            return new Thumbnail(thumbBitstream, primaryBitstream);
        }

        return null;
    }

    public static String getFirstMetadataValue(Item item, String metadataKey) {
        DCValue[] dcValue = item.getMetadata(metadataKey);
        if(dcValue.length > 0) {
            return dcValue[0].value;
        } else {
            return "";
        }
    }

    /**
     * Helper to get a just the contributor.author's firstname. Requires some tricks, so better to make a shared method
     * @param item
     * @return
     */
    public static String getAuthorFirstName(Item item) {
        String authorName = getFirstMetadataValue(item, "dc.contributor.author");
        //This metadata field is usually "Lastname, FirstName MiddleName, so just split on comma
        String[] authorNameTokens = authorName.split(",");
        if(authorNameTokens.length > 1) {
            return authorNameTokens[1].trim();
        } else {
            //Fallback to original metadata value
            return authorName;
        }
    }
}
