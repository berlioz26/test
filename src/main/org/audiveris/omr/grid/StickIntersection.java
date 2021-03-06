//----------------------------------------------------------------------------//
//                                                                            //
//                     S t i c k I n t e r s e c t i o n                      //
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
package org.audiveris.omr.grid;

import org.audiveris.omr.glyph.facets.Glyph;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code StickIntersection} records the intersection point of a
 * stick with a crossing line.
 * (A typical example is a vertical barline stick that crosses a horizontal
 * staff line)
 *
 * @author Hervé Bitteur
 */
public class StickIntersection
        implements Comparable<StickIntersection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Comparator on increasing abscissa */
    public static Comparator<StickIntersection> byAbscissa = new Comparator<StickIntersection>()
    {
        @Override
        public int compare (StickIntersection o1,
                            StickIntersection o2)
        {
            int dx = Double.compare(o1.x, o2.x);

            if (dx != 0) {
                return dx;
            } else {
                // Just to disambiguate
                return Double.compare(o1.y, o2.y);
            }
        }
    };

    /** Comparator on increasing ordinate */
    public static Comparator<StickIntersection> byOrdinate = new Comparator<StickIntersection>()
    {
        @Override
        public int compare (StickIntersection o1,
                            StickIntersection o2)
        {
            int dy = Double.compare(o1.y, o2.y);

            if (dy != 0) {
                return dy;
            } else {
                // Just to disambiguate
                return Double.compare(o1.x, o2.x);
            }
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Abscissa where the stick intersects the line */
    public final double x;

    /** Ordinate where the stick intersects the line */
    public final double y;

    /** The stick */
    private final Glyph stick;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new StickIntersection object.
     *
     * @param loc   absolute location of the intersection
     * @param stick the related stick
     */
    public StickIntersection (Point2D loc,
                              Glyph stick)
    {
        this.x = loc.getX();
        this.y = loc.getY();
        this.stick = stick;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    /** For sorting sticks on abscissa, for a given staff */
    @Override
    public int compareTo (StickIntersection that)
    {
        int dx = Double.compare(x, that.x);

        if (dx != 0) {
            return dx;
        } else {
            // Just to disambiguate
            return Double.compare(y, that.y);
        }
    }

    //----------//
    // sticksOf //
    //----------//
    /** Conversion to a sequence of sticks */
    public static List<Glyph> sticksOf (Collection<StickIntersection> sps)
    {
        List<Glyph> sticks = new ArrayList<>();

        for (StickIntersection sp : sps) {
            sticks.add(sp.getStickAncestor());
        }

        return sticks;
    }

    //------------------//
    // getStickAncestor //
    //------------------//
    /**
     * @return the stick, in fact its ancestor
     */
    public Glyph getStickAncestor ()
    {
        return stick.getAncestor();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getStickAncestor()
                .idString() + "@x:" + (float) x + ",y:" + (float) y;
    }
}
