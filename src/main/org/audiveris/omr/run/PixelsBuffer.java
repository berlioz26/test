//----------------------------------------------------------------------------//
//                                                                            //
//                          P i x e l s B u f f e r                           //
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
package org.audiveris.omr.run;

import org.audiveris.omr.run.GlobalDescriptor;

import net.jcip.annotations.ThreadSafe;

import java.awt.Dimension;
import java.util.Arrays;

/**
 * Class {@code PixelsBuffer} handles a plain rectangular buffer of
 * chars.
 * It is an efficient {@link PixelFilter} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelsBuffer
        implements PixelFilter
{
    //~ Instance fields --------------------------------------------------------

    /** Width of the table */
    private final int width;

    /** Height of the table */
    private final int height;

    /** Underlying buffer */
    private char[] buffer;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // PixelsBuffer //
    //--------------//
    /**
     * Creates a new PixelsBuffer object.
     *
     * @param dimension the buffer dimension
     */
    public PixelsBuffer (Dimension dimension)
    {
        width = dimension.width;
        height = dimension.height;

        buffer = new char[width * height];

        // Initialize the whole buffer with background color value
        Arrays.fill(buffer, (char) BACKGROUND);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(BACKGROUND / 2);
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return height;
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public int getPixel (int x,
                         int y)
    {
        return buffer[(y * width) + x];
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
    }

    //--------//
    // isFore //
    //--------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getPixel(x, y) != BACKGROUND;
    }

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int x,
                          int y,
                          char val)
    {
        buffer[(y * width) + x] = val;
    }
}
