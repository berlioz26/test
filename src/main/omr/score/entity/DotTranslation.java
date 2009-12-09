//----------------------------------------------------------------------------//
//                                                                            //
//                        D o t T r a n s l a t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.log.Logger;

import omr.score.common.SystemPoint;

import omr.sheet.Scale;

import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>DotTranslation</code> is a set of functions forassigning a dot
 * glyph, since a dot can be an augmentation dot, part of a repeat sign, a
 * staccato sign.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class DotTranslation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(DotTranslation.class);

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // populateDot //
    //-------------//
    /**
     * Try to find the best assignment for a dot (variant) glyph.
     *
     * @param glyph the glyph of dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     */
    public static void populateDot (Glyph       glyph,
                                    Measure     measure,
                                    SystemPoint dotCenter)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Chord Populating dot " + glyph);
        }

        Shape           shape = glyph.getShape();

        /** To remember results of trials */
        SortedSet<Info> infos = new TreeSet<Info>();

        // Try augmentation
        if ((shape == DOT) || (shape == COMBINING_AUGMENTATION_DOT)) {
            Info aug = tryAugmentation(glyph, measure, dotCenter);

            if (aug != null) {
                infos.add(aug);
            }
        }

        // Try repeat
        if ((shape == DOT) || (shape == REPEAT_DOTS)) {
            Info rep = tryRepeat(glyph, measure, dotCenter);

            if (rep != null) {
                infos.add(rep);
            }
        }

        // Try staccato
        if ((shape == DOT) || (shape == STACCATO)) {
            Info sta = tryStaccato(glyph, measure, dotCenter);

            if (sta != null) {
                infos.add(sta);
            }
        }

        // Results
        if (logger.isFineEnabled()) {
            logger.fine(glyph.toString());

            for (Info info : infos) {
                logger.fine(info.toString());
            }
        }

        if (!infos.isEmpty()) {
            Info  info = infos.first();
            Shape targetShape = info.getTargetShape();

            // Assign proper glyph shape (and thus color)
            if (glyph.getShape() != targetShape) {
                glyph.setShape(targetShape);
            }

            // Assign proper translation
            info.commit(glyph, measure, dotCenter);
        } else {
            measure.addError(glyph, "Dot unassigned");
        }
    }

    //-----------------//
    // tryAugmentation //
    //-----------------//
    /**
     * Try to assign a dot as a chord augmentation dot
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return the possible use as an augmentation dot
     */
    private static AugmentationInfo tryAugmentation (Glyph       glyph,
                                                     Measure     measure,
                                                     SystemPoint dotCenter)
    {
        Scale                    scale = measure.getScale();
        final int                maxDx = scale.toUnits(
            constants.maxAugmentationDotDx);
        final int                maxDy = scale.toUnits(
            constants.maxAugmentationDotDy);
        SortedMap<Double, Chord> distances = new TreeMap<Double, Chord>();

        // Check for a note/rest nearby:
        // - on the left w/ same even pitch (note w/ even pitch)
        // - slighly above or below (note with odd pitch = on a staff line)
        ChordLoop: 
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                if (!note.getShape()
                         .isWholeRest()) {
                    SystemPoint noteRef = note.getCenterRight();
                    SystemPoint toDot = new SystemPoint(
                        dotCenter.x - noteRef.x,
                        dotCenter.y - noteRef.y);

                    if (logger.isFineEnabled()) {
                        logger.info(measure.getContextString() + " " + toDot);
                    }

                    if ((glyph.getShape() == COMBINING_AUGMENTATION_DOT) ||
                        ((toDot.x > 0) && (toDot.x <= maxDx) &&
                        (Math.abs(toDot.y) <= maxDy))) {
                        distances.put(toDot.distanceSq(0, 0), chord);
                    }
                }
            }
        }

        // Assign the dot to the candidate with longest rawDuration, which boils
        // down to smallest number of flags/beams, as the note head is the same
        if (logger.isFineEnabled()) {
            logger.info(distances.size() + " Candidates=" + distances);
        }

        if (!distances.isEmpty()) {
            Double firstKey = distances.firstKey();

            // TODO: we should also handle case of double dots !
            return new AugmentationInfo(distances.get(firstKey), firstKey);
        } else {
            return null;
        }
    }

    //-----------//
    // tryRepeat //
    //-----------//
    /**
     * Try to assign a dot to the relevant repeat barline if any
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return true the possible use as a repeat dot
     */
    private static RepeatInfo tryRepeat (Glyph       glyph,
                                         Measure     measure,
                                         SystemPoint dotCenter)
    {
        // Check vertical pitch position within the staff : close to +1 or -1
        double pitchDif = Math.abs(Math.abs(glyph.getPitchPosition()) - 1);

        if (pitchDif > (2 * constants.maxRepeatDotDy.getValue())) {
            return null;
        }

        // Check abscissa wrt the (ending) repeat barline on right
        Barline     barline = measure.getBarline();
        int         dx = barline.getLeftX() - dotCenter.x;
        final Scale scale = measure.getScale();
        final int   maxDx = scale.toUnits(constants.maxRepeatDotDx);

        if ((glyph.getShape() == REPEAT_DOTS) || ((dx > 0) && (dx <= maxDx))) {
            return new RepeatInfo(barline, dx * dx);
        }

        // Check abscissa wrt the ending barline of the previous measure on left
        Measure prevMeasure = (Measure) measure.getPreviousSibling();

        if (prevMeasure != null) {
            barline = prevMeasure.getBarline();
            dx = dotCenter.x - barline.getRightX();

            if ((dx > 0) && (dx <= maxDx)) {
                return new RepeatInfo(barline, dx * dx);
            }
        }

        return null;
    }

    //-------------//
    // tryStaccato //
    //-------------//
    /**
     * Try to assign a dot as a staccato
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return true if assignment is successful
     */
    private static StaccatoInfo tryStaccato (Glyph       glyph,
                                             Measure     measure,
                                             SystemPoint dotCenter)
    {
        // Make sure dy is not too high and use dx*dx as distance
        Scale                    scale = measure.getScale();
        final int                maxDx = scale.toUnits(
            constants.maxStaccatoDotDx);
        final int                maxDy = scale.toUnits(
            constants.maxStaccatoDotDy);
        SortedMap<Double, Chord> distances = new TreeMap<Double, Chord>();

        ChordLoop: 
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                if (!note.isRest()) {
                    SystemPoint noteRef = note.getCenter();
                    SystemPoint toDot = new SystemPoint(
                        dotCenter.x - noteRef.x,
                        dotCenter.y - noteRef.y);

                    if (logger.isFineEnabled()) {
                        logger.info(measure.getContextString() + " " + toDot);
                    }

                    if ((glyph.getShape() == STACCATO) ||
                        ((Math.abs(toDot.x) <= maxDx) &&
                        (Math.abs(toDot.y) <= maxDy))) {
                        distances.put(toDot.distanceSq(0, 0), chord);
                    }
                }
            }
        }

        if (!distances.isEmpty()) {
            Double firstKey = distances.firstKey();

            return new StaccatoInfo(distances.get(firstKey), firstKey);
        } else {
            return null;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------------//
    // AugmentationInfo //
    //------------------//
    private static class AugmentationInfo
        extends Info
    {
        //~ Instance fields ----------------------------------------------------

        final Chord chord;

        //~ Constructors -------------------------------------------------------

        public AugmentationInfo (Chord  chord,
                                 double dist)
        {
            super(dist);
            this.chord = chord;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit (Glyph       glyph,
                            Measure     measure,
                            SystemPoint dotCenter)
        {
            chord.setDotsNumber(1);
            glyph.setTranslation(chord);

            if (logger.isFineEnabled()) {
                logger.fine(
                    chord.getContextString() + " dot#" + glyph.getId() +
                    " Augmented " + chord);
            }
        }

        @Override
        protected Shape getTargetShape ()
        {
            return COMBINING_AUGMENTATION_DOT;
        }

        @Override
        protected String internals ()
        {
            return "chord:" + chord;
        }
    }

    //------//
    // Info //
    //------//
    /**
     * Remember information about possible assignment of a dot
     * (augmentation dot, repeat sign, staccato)
     */
    private abstract static class Info
        implements Comparable<Info>
    {
        //~ Instance fields ----------------------------------------------------

        /* The measured distance to the related entity */
        final double dist;

        //~ Constructors -------------------------------------------------------

        public Info (double dist)
        {
            this.dist = dist;
        }

        //~ Methods ------------------------------------------------------------

        public abstract void commit (Glyph       glyph,
                                     Measure     measure,
                                     SystemPoint dotCenter);

        public int compareTo (Info other)
        {
            return Double.compare(this.dist, other.dist);
        }

        @Override
        public String toString ()
        {
            return "{" + getClass()
                             .getSimpleName() + " dist:" + (float) dist + " " +
                   internals() + "}";
        }

        protected abstract Shape getTargetShape ();

        protected String internals ()
        {
            return "";
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /**
         * Maximum dx between note and augmentation dot
         */
        Scale.Fraction maxAugmentationDotDx = new Scale.Fraction(
            1.7d,
            "Maximum dx between note and augmentation dot");

        /**
         * Maximum absolute dy between note and augmentation dot
         */
        Scale.Fraction maxAugmentationDotDy = new Scale.Fraction(
            1d,
            "Maximum absolute dy between note and augmentation dot");

        /**
         * Margin for vertical position of a dot againt a repeat barline
         */
        Scale.Fraction maxRepeatDotDy = new Scale.Fraction(
            0.5d,
            "Margin for vertical position of a dot againt a repeat barline");

        /**
         * Maximum dx between dot and edge of repeat barline
         */
        Scale.Fraction maxRepeatDotDx = new Scale.Fraction(
            1.5d,
            "Maximum dx between dot and edge of repeat barline");

        /**
         * Maximum absolute dy between note and staccato dot
         */
        Scale.Fraction maxStaccatoDotDy = new Scale.Fraction(
            3d,
            "Maximum absolute dy between note and staccato dot");

        /**
         * Maximum dx between note and staccato dot
         */
        Scale.Fraction maxStaccatoDotDx = new Scale.Fraction(
            0.5d,
            "Maximum dx between note and staccato dot");
    }

    //------------//
    // RepeatInfo //
    //------------//
    private static class RepeatInfo
        extends Info
    {
        //~ Instance fields ----------------------------------------------------

        final Barline barline;

        //~ Constructors -------------------------------------------------------

        public RepeatInfo (Barline barline,
                           double  dist)
        {
            super(dist);
            this.barline = barline;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit (Glyph       glyph,
                            Measure     measure,
                            SystemPoint dotCenter)
        {
            barline.addGlyph(glyph);
            glyph.setTranslation(barline);

            if (logger.isFineEnabled()) {
                logger.fine(
                    barline.getContextString() + " dot#" + glyph.getId() +
                    " Repeat dot for " + barline);
            }
        }

        @Override
        protected Shape getTargetShape ()
        {
            return REPEAT_DOTS;
        }

        @Override
        protected String internals ()
        {
            return "barline:" + barline;
        }
    }

    //--------------//
    // StaccatoInfo //
    //--------------//
    private static class StaccatoInfo
        extends Info
    {
        //~ Instance fields ----------------------------------------------------

        final Chord chord;

        //~ Constructors -------------------------------------------------------

        public StaccatoInfo (Chord  chord,
                             double dist)
        {
            super(dist);
            this.chord = chord;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit (Glyph       glyph,
                            Measure     measure,
                            SystemPoint dotCenter)
        {
            glyph.setTranslation(
                new Articulation(measure, dotCenter, chord, glyph));

            if (logger.isFineEnabled()) {
                logger.fine(
                    chord.getContextString() + " dot#" + glyph.getId() +
                    " Staccato " + chord);
            }
        }

        @Override
        protected Shape getTargetShape ()
        {
            return STACCATO;
        }

        @Override
        protected String internals ()
        {
            return "chord:" + chord;
        }
    }
}
