//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t A c t i o n s                           //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.Main;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.audiveris.omr.glyph.GlyphRepository;

import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoresManager;
import org.audiveris.omr.score.ui.ScoreController;

import org.audiveris.omr.script.RemoveTask;

import org.audiveris.omr.sheet.ScaleBuilder;
import org.audiveris.omr.sheet.Sheet;

import org.audiveris.omr.ui.MainGui;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.UIUtil;

import org.audiveris.omr.util.BasicTask;
import org.audiveris.omr.util.NameSet;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JOptionPane;

/**
 * Class {@code SheetActions} simply gathers UI actions related to sheet
 * handling. These methods are ready to be picked up by the plugins mechanism.
 *
 * @author Hervé Bitteur
 */
public class SheetActions
        extends SheetDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SheetActions.class);

    /** Singleton */
    private static SheetActions INSTANCE;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SheetActions //
    //--------------//
    /**
     * Creates a new SheetActions object.
     */
    public SheetActions ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // closeScore //
    //------------//
    /**
     * Action that handles the closing of the currently selected score.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void closeScore (ActionEvent e)
    {
        Score score = ScoreController.getCurrentScore();

        if (score != null) {
            score.close();
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized SheetActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetActions();
        }

        return INSTANCE;
    }

    //---------------//
    // openImageFile //
    //---------------//
    /**
     * Action that let the user select an image file interactively.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public OpenTask openImageFile (ActionEvent e)
    {
        String suffixes = constants.validImageExtensions.getValue();
        String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        File file = UIUtil.fileChooser(
                false,
                Main.getGui().getFrame(),
                new File(ScoresManager.getInstance().getDefaultInputDirectory()),
                new OmrFileFilter(
                "Major image files" + " (" + suffixes + ")",
                allSuffixes.split("\\s")));

        if (file != null) {
            if (file.exists()) {
                return new OpenTask(file);
            } else {
                logger.warn("File not found {}", file);
            }
        }

        return null;
    }

    //-----------//
    // plotScale //
    //-----------//
    /**
     * Action that allows to display the plot of Scale Builder.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void plotScale (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            ScaleBuilder scaleBuilder = sheet.getScaleBuilder();

            if (scaleBuilder != null) {
                scaleBuilder.displayChart();
            } else {
                logger.warn(
                        "Cannot display scale plot, for lack of scale data");
            }
        }
    }

    //--------------//
    // recordGlyphs //
    //--------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public RecordGlyphsTask recordGlyphs ()
    {
        int answer = JOptionPane.showConfirmDialog(
                null,
                "Are you sure of all the symbols of this sheet ?");

        if (answer == JOptionPane.YES_OPTION) {
            return new RecordGlyphsTask();
        } else {
            return null;
        }
    }

    //-------------//
    // removeSheet //
    //-------------//
    /**
     * Action that handles the removal of the currently selected sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void removeSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    "Do you confirm the removal of this sheet"
                    + " from its containing score ?");

            if (answer == JOptionPane.YES_OPTION) {
                new RemoveTask(sheet).launch(sheet);
            }
        }
    }

    //------------//
    // zoomHeight //
    //------------//
    /**
     * Action that allows to adjust the display zoom, so that the full height is
     * shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void zoomHeight (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        SheetAssembly assembly = sheet.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView()
                .fitHeight();
    }

    //-----------//
    // zoomWidth //
    //-----------//
    /**
     * Action that allows to adjust the display zoom, so that the full width is
     * shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void zoomWidth (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        SheetAssembly assembly = sheet.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView()
                .fitWidth();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------//
    // HistoryMenu //
    //-------------//
    /**
     * Handles the menu of sheet history.
     */
    public static class HistoryMenu
    {
        //~ Static fields/initializers -----------------------------------------

        private static HistoryMenu INSTANCE;

        //~ Instance fields ----------------------------------------------------
        //
        /** Concrete menu. */
        private JMenu menu;

        //~ Constructors -------------------------------------------------------
        //
        private HistoryMenu ()
        {
        }

        //~ Methods ------------------------------------------------------------
        public static HistoryMenu getInstance ()
        {
            if (INSTANCE == null) {
                INSTANCE = new HistoryMenu();
            }

            return INSTANCE;
        }

        public JMenu getMenu ()
        {
            if (menu == null) {
                NameSet history = ScoresManager.getInstance()
                        .getHistory();
                menu = history.menu("Sheet History", new HistoryListener());
                menu.setEnabled(!history.isEmpty());

                menu.setName("historyMenu");

                ResourceMap resource = MainGui.getInstance()
                        .getContext()
                        .getResourceMap(
                        SheetActions.class);
                resource.injectComponents(menu);
            }

            return menu;
        }

        public void setEnabled (boolean bool)
        {
            getMenu()
                    .setEnabled(bool);
        }

        //~ Inner Classes ------------------------------------------------------
        /**
         * Class {@code HistoryListener} is used to reload an image file,
         * when selected from the history of previous image files.
         */
        private static class HistoryListener
                implements ActionListener
        {
            //~ Methods --------------------------------------------------------

            @Override
            public void actionPerformed (ActionEvent e)
            {
                final String name = e.getActionCommand()
                        .trim();

                if (!name.isEmpty()) {
                    File file = new File(name);
                    new OpenTask(file).execute();
                }
            }
        }
    }

    //----------//
    // OpenTask //
    //----------//
    public static class OpenTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;

        //~ Constructors -------------------------------------------------------
        public OpenTask (File file)
        {
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (file.exists()) {
                // Actually load the image file
                Score score = new Score(file);
                score.createPages(null);
            } else {
                logger.warn("File {} does not exist", file);
            }

            return null;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Valid extensions for image files */
        Constant.String validImageExtensions = new Constant.String(
                ".bmp .gif .jpg .png .tiff .tif .pdf",
                "Valid image file extensions, whitespace-separated");

    }

    //------------------//
    // RecordGlyphsTask //
    //------------------//
    private static class RecordGlyphsTask
            extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            Sheet sheet = SheetsController.getCurrentSheet();
            GlyphRepository.getInstance()
                    .recordSheetGlyphs(
                    sheet, /* emptyStructures => */
                    sheet.isOnPatterns());

            return null;
        }
    }
}
