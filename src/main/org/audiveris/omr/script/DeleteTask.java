//----------------------------------------------------------------------------//
//                                                                            //
//                            D e l e t e T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
// Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//----------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.script;

import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.facets.Glyph;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;

import org.audiveris.omr.util.PointFacade;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Class {@code DeleteTask} deletes a set of (virtual) glyphs from the
 * sheet environment
 *
 * @author Hervé Bitteur
 */
public class DeleteTask
        extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Locations */
    private List<Point> locations;

    /** Wrapping of the collections of points */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private PointFacade[] points;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // DeleteTask //
    //------------//
    /**
     * Create an glyph deletion task
     *
     * @param sheet  the sheet impacted
     * @param glyphs the (virtual) glyphs to delete
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    public DeleteTask (Sheet sheet,
                       Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);

        locations = new ArrayList<>();

        for (Glyph glyph : glyphs) {
            locations.add(glyph.getAreaCenter());
        }
    }

    //------------//
    // DeleteTask //
    //------------//
    /** No-arg constructor needed for JAXB */
    private DeleteTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().syncDelete(this);
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        super.epilog(sheet);
        logger.info("Deletion of virtual {}", Glyphs.toString(glyphs));
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" delete");

        if (!locations.isEmpty()) {
            sb.append(" locations[");

            for (Point point : locations) {
                sb.append(" ").append(point.toString());
            }

            sb.append("]");
        } else {
            sb.append(" no-locations");
        }

        return sb.toString();
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    @Override
    protected SortedSet<SystemInfo> retrieveCurrentImpact (Sheet sheet)
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<>();

        for (Glyph glyph : glyphs) {
            Point location = glyph.getAreaCenter();
            SystemInfo system = sheet.getSystemOf(location);

            if (system != null) {
                // Include this system
                impactedSystems.add(system);
            }

            if (glyph.getShape().isPersistent()) {
                // Include all following systems as well
                impactedSystems.addAll(remaining(system));
            }
        }

        return impactedSystems;
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * Here, we have to retrieve virtual glyphs, based on their locations.
     */
    @Override
    protected void retrieveGlyphs ()
    {
        glyphs = new LinkedHashSet<>();

        for (Point location : locations) {
            Glyph glyph = sheet.getNest().lookupVirtualGlyph(location);
            glyphs.add(glyph);
            logger.debug("To be deleted: {}", glyph);
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent
     * object.
     */
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        // Convert array of point facades -> locations
        if (locations == null) {
            locations = new ArrayList<>();

            for (PointFacade facade : points) {
                locations.add(new Point(facade.getX(), facade.getY()));
            }
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    private void beforeMarshal (Marshaller m)
    {
        // Convert locations -> array of point facades
        if (points == null) {
            List<PointFacade> facades = new ArrayList<>();

            for (Point point : locations) {
                facades.add(new PointFacade(point));
            }

            points = facades.toArray(new PointFacade[0]);
        }
    }
}
