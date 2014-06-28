package yass;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;

/**
 * Description of the Class
 *
 * @author Saruta
 * @created 27. August 2007
 */
public class YassAutoCorrect {
    private static int FIXED_PAGE_BREAK = 0;
    /**
     * from Ultrastar/Code/TextGL.glTextWidth() : real
     * Fonts[ActFont].Width[Ord(Letter)] * Fonts[ActFont].Tex.H / 30 *
     * Fonts[ActFont].AspectW; where initially Fonts[].Tex.H := 30; and
     * Fonts[].AspectW := 0.95; and Outline / Outline2 Fonts[2].Outline := 5;
     * and Fonts[3].Outline := 4; for i := 0 to 255 do Fonts[2].Width[i] :=
     * Fonts[2].Width[i] div 2 + 2; // ganzzahlige Division Fonts[3].Width[i] :=
     * Fonts[3].Width[i] + 1; procedure SetFontSize(Size: real);
     * Fonts[ActFont].Tex.H := 30 * (Size/10); where Size is 14 for biggest font
     * "Oline2" -----------UGraphics.pas case Ini.Resolution of 0: W := 640 *
     * Screens; H := 480; 1: W := 800 * Screens; H := 600; 2: W := 1024 *
     * Screens; H := 768;
     */

    byte[] fontWidth = null;
    int fontSize = 14, outline = 3;
    private YassProperties prop = null;
    private String[] audioExtensions, imageExtensions, videoExtensions;
    private String coverID, backgroundID, videoID, videodirID;

    /**
     * Constructor for the YassAutoCorrect object
     */
    public YassAutoCorrect() {
    }

    /**
     * Gets the autoCorrectionPageBreak attribute of the YassAutoCorrect object
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionPageBreak value
     */
    public static boolean isAutoCorrectionPageBreak(String msg) {
        return msg.equals(YassRow.PAGE_OVERLAP)
                || msg.equals(YassRow.EARLY_PAGE_BREAK)
                || msg.equals(YassRow.LATE_PAGE_BREAK);
    }

    /**
     * Gets the autoCorrectionMinorPageBreak attribute of the YassAutoCorrect
     * class
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionMinorPageBreak value
     */
    public static boolean isAutoCorrectionMinorPageBreak(String msg) {
        return msg.equals(YassRow.UNCOMMON_PAGE_BREAK);
    }

    /**
     * Gets the autoCorrectionFileNames attribute of the YassAutoCorrect object
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionFileNames value
     */
    public static boolean isAutoCorrectionFileName(String msg) {
        return msg.equals(YassRow.FILE_FOUND)
                || msg.equals(YassRow.NO_COVER_LABEL)
                || msg.equals(YassRow.NO_BACKGROUND_LABEL)
                || msg.equals(YassRow.NO_VIDEO_LABEL)
                || msg.equals(YassRow.WRONG_VIDEOGAP);
    }

    /**
     * Gets the autoCorrectionTags attribute of the YassAutoCorrect class
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionTags value
     */
    public static boolean isAutoCorrectionTags(String msg) {
        return msg.equals(YassRow.MISSING_TAG)
                || msg.equals(YassRow.UNSORTED_COMMENTS);
    }

    /**
     * Gets the autoCorrectionSpacing attribute of the YassAutoCorrect class
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionSpacing value
     */
    public static boolean isAutoCorrectionSpacing(String msg) {
        return msg.equals(YassRow.TOO_MUCH_SPACES)
                || msg.equals(YassRow.UNCOMMON_SPACING);
    }

    /**
     * Gets the pause attribute of the YassAutoCorrect class
     *
     * @param in  Description of the Parameter
     * @param out Description of the Parameter
     * @param bpm Description of the Parameter
     * @return The pause value
     */
    public static double getPause(int in, int out, double bpm) {
        return Math.abs(out - in) * 60 / (4 * bpm);
    }

    /**
     * Gets the commonPageBreak attribute of the YassAutoCorrect class
     *
     * @param inout   Description of the Parameter
     * @param bpm     Description of the Parameter
     * @param inoutms Description of the Parameter
     * @return The commonPageBreak value
     */
    public static int getCommonPageBreak(int inout[], double bpm,
                                         double inoutms[]) {
        int pause = inout[1] - inout[0];
        if (pause < 0) {
            return -1;
        }

        double psec = pause * 60 / (4 * bpm);

        // FIXED_PAGE_BREAK is assured to be <= 10
        int f = 0;
        if (FIXED_PAGE_BREAK > 0) {
            f = inout[0] + FIXED_PAGE_BREAK;
            if (f > inout[1] - FIXED_PAGE_BREAK)
                f = inout[1] - FIXED_PAGE_BREAK;
            if (f < inout[0])
                f = inout[0];
        }
        if (psec >= 4) {
            if (FIXED_PAGE_BREAK > 0) {
                inout[0] = inout[1] = f;
            } else {
                int gap = (int) (2 * 4 * bpm / 60);
                inout[0] = inout[0] + gap;
                inout[1] = inout[0];
                if (inoutms != null) {
                    inoutms[0] = 2;
                    inoutms[1] = psec - 2;
                }
            }
            return 4;
        } else if (psec >= 2) {
            if (FIXED_PAGE_BREAK > 0) {
                inout[0] = inout[1] = f;
            } else {
                int gap = (int) (4 * bpm / 60);
                inout[0] = inout[0] + gap;
                inout[1] = inout[0];
                if (inoutms != null) {
                    inoutms[0] = 1;
                    inoutms[1] = psec - 1;
                }
            }
            return 2;
        }
        if (pause == 0 || pause == 1) {
            inout[1] = inout[0];
            if (inoutms != null) {
                inoutms[1] = pause * 60 / (4 * bpm);
                inoutms[0] = 0;
            }
            return 0;
        } else if (pause >= 2 && pause <= 8) {
            if (FIXED_PAGE_BREAK > 0) {
                inout[0] = inout[1] = f;
            } else {
                inout[0] = inout[1] - 2;
                inout[1] = inout[0];

                if (inoutms != null) {
                    inoutms[1] = 2 * 60 / (4 * bpm);
                    inoutms[0] = psec - inoutms[1];
                }
            }
            return 8;
        } else if (pause >= 9 && pause <= 12) {
            if (FIXED_PAGE_BREAK > 0) {
                inout[0] = inout[1] = f;
            } else {
                inout[0] = inout[1] - 3;
                inout[1] = inout[0];

                if (inoutms != null) {
                    double psec1 = (pause - 3) * 60 / (4 * bpm);
                    double psec2 = 3 * 60 / (4 * bpm);
                    inoutms[0] = psec1;
                    inoutms[1] = psec2;
                }
            }
            return 12;
        } else if (pause >= 13 && pause <= 16) {
            if (FIXED_PAGE_BREAK > 0) {
                inout[0] = inout[1] = f;
            } else {
                inout[0] = inout[1] - 4;
                inout[1] = inout[0];

                if (inoutms != null) {
                    double psec1 = 4 * 60 / (4 * bpm);
                    double psec2 = (pause - 4) * 60 / (4 * bpm);
                    inoutms[0] = psec1;
                    inoutms[1] = psec2;
                }
            }
            return 16;
        }
        // else if (pause>17) {
        if (FIXED_PAGE_BREAK > 0) {
            inout[0] = inout[1] = f;
        } else {
            inout[0] = inout[0] + 10;
            inout[1] = inout[0];

            if (inoutms != null) {
                double psec1 = 10 * 60 / (4 * bpm);
                double psec2 = (pause - 10) * 60 / (4 * bpm);
                inoutms[0] = psec1;
                inoutms[1] = psec2;
            }
        }
        return 18;
        // }
    }

    /**
     * Description of the Method
     *
     * @param t  Description of the Parameter
     * @param co Description of the Parameter
     */
    public static void insertCover(YassTable t, File co) {
        YassRow r2 = t.getCommentRow("COVER:");
        if (r2 != null) {
            r2.setComment(co.getName());
        } else {
            YassRow r3;
            YassTableModel tm = ((YassTableModel) t.getModel());
            Vector<YassRow> data = tm.getData();
            int j = 0;
            while ((r3 = tm.getRowAt(j)) != null && r3.isComment()) {
                j++;
                if (r3.getCommentTag().equals("MP3:")) {
                    break;
                }
            }
            data.insertElementAt(new YassRow("#", "COVER:", co.getName(), "",
                    ""), j);
        }
    }

    /**
     * Description of the Method
     *
     * @param t  Description of the Parameter
     * @param bg Description of the Parameter
     */
    public static void insertBackground(YassTable t, File bg) {
        YassRow r2 = t.getCommentRow("BACKGROUND:");
        if (r2 != null) {
            r2.setComment(bg.getName());
        } else {
            YassRow r3;
            YassTableModel tm = ((YassTableModel) t.getModel());
            Vector<YassRow> data = tm.getData();
            int j = 0;
            int k = 0;
            while ((r3 = tm.getRowAt(j)) != null && r3.isComment()) {
                j++;
                if (r3.getCommentTag().equals("MP3:")) {
                    k = j;
                }
                if (r3.getCommentTag().equals("COVER:")) {
                    k = j;
                    break;
                }
            }
            if (k == 0) {
                k = j;
            }
            data.insertElementAt(new YassRow("#", "BACKGROUND:", bg.getName(),
                    "", ""), k);
        }
    }

    /**
     * Description of the Method
     *
     * @param t  Description of the Parameter
     * @param vd Description of the Parameter
     */
    public static void insertVideo(YassTable t, File vd) {
        YassRow r2 = t.getCommentRow("VIDEO:");
        if (r2 != null) {
            r2.setComment(vd.getName());
        } else {
            YassRow r3;
            YassTableModel tm = ((YassTableModel) t.getModel());
            Vector<YassRow> data = tm.getData();
            int coverIndex = -1;
            int j = 0;
            while ((r3 = tm.getRowAt(j)) != null && r3.isComment()) {
                j++;
                if (r3.getCommentTag().equals("COVER:")) {
                    coverIndex = j;
                }
                if (r3.getCommentTag().equals("BACKGROUND:")) {
                    break;
                }
            }
            if (r3 != null && !r3.isComment()) {
                j = coverIndex + 1;
            }
            data.insertElementAt(new YassRow("#", "VIDEO:", vd.getName(), "",
                    ""), j);
        }
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     */
    public static void sortComments(YassTable table) {
        int kk = 0;
        int n = table.getRowCount();
        YassRow rr = table.getRowAt(kk);
        while (kk < n && rr.isComment()) {
            kk++;
            rr = table.getRowAt(kk);
        }
        Vector<YassRow> rrv = new Vector<>(kk);
        for (int j = 0; j < kk; j++) {
            rrv.addElement((YassRow) table.getRowAt(j).clone());
        }
        Collections.sort(rrv);
        for (int j = 0; j < kk; j++) {
            table.getRowAt(j).setRow((YassRow) rrv.elementAt(j));
        }
    }

    /**
     * Description of the Method
     *
     * @param p Description of the Parameter
     * @param a Description of the Parameter
     */
    public void init(YassProperties p, YassActions a) {
        prop = p;
        YassRow.setValidTags(prop.getProperty("valid-tags"));
        YassRow.setValidLines(prop.getProperty("valid-lines"));
        loadFont(prop.getProperty("font-file"));

        String ext = prop.getProperty("audio-files");
        StringTokenizer st = new StringTokenizer(ext, "|");
        int n = st.countTokens();
        audioExtensions = new String[n];
        for (int i = 0; i < n; i++) {
            audioExtensions[i] = st.nextToken().toLowerCase();
        }

        ext = prop.getProperty("image-files");
        st = new StringTokenizer(ext, "|");
        n = st.countTokens();
        imageExtensions = new String[n];
        for (int i = 0; i < n; i++) {
            imageExtensions[i] = st.nextToken().toLowerCase();
        }

        ext = prop.getProperty("video-files");
        st = new StringTokenizer(ext, "|");
        n = st.countTokens();
        videoExtensions = new String[n];
        for (int i = 0; i < n; i++) {
            videoExtensions[i] = st.nextToken().toLowerCase();
        }

        coverID = prop.getProperty("cover-id").toLowerCase();
        backgroundID = prop.getProperty("background-id").toLowerCase();
        videoID = prop.getProperty("video-id").toLowerCase();
        videodirID = prop.getProperty("videodir-id").toLowerCase();
    }

    /**
     * Description of the Method
     *
     * @param msg Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectionSupported(String msg) {
        return msg.equals(YassRow.EMPTY_LINE)
                || msg.equals(YassRow.UNCOMMON_SPACING)
                || msg.equals(YassRow.TOO_MUCH_SPACES)
                || msg.equals(YassRow.OUT_OF_ORDER)
                || msg.equals(YassRow.PAGE_OVERLAP)
                || msg.equals(YassRow.EARLY_PAGE_BREAK)
                || msg.equals(YassRow.LATE_PAGE_BREAK)
                || msg.equals(YassRow.UNCOMMON_PAGE_BREAK)
                || msg.equals(YassRow.UNSORTED_COMMENTS)
                || msg.equals(YassRow.FILE_FOUND)
                || msg.equals(YassRow.NO_COVER_LABEL)
                || msg.equals(YassRow.NO_BACKGROUND_LABEL)
                || msg.equals(YassRow.NO_VIDEO_LABEL)
                || msg.equals(YassRow.WRONG_VIDEOGAP)
                || msg.equals(YassRow.TRANSPOSED_NOTES)
                || msg.equals(YassRow.INVALID_NOTE_LENGTH)
                || msg.equals(YassRow.MISSING_TAG)
                || msg.equals(YassRow.NOTES_TOUCHING)
                || msg.equals(YassRow.NONZERO_FIRST_BEAT);
    }

    // should return true if messages were added;
    // for now, only returns false if table is relative

    /**
     * Gets the autoCorrectionSafe attribute of the YassAutoCorrect object
     *
     * @param msg Description of the Parameter
     * @return The autoCorrectionSafe value
     */
    public boolean isAutoCorrectionSafe(String msg) {
        return msg.equals(YassRow.EMPTY_LINE)
                || msg.equals(YassRow.UNCOMMON_SPACING)
                || msg.equals(YassRow.TOO_MUCH_SPACES)
                || msg.equals(YassRow.UNSORTED_COMMENTS)
                || msg.equals(YassRow.PAGE_OVERLAP)
                || msg.equals(YassRow.EARLY_PAGE_BREAK)
                || msg.equals(YassRow.LATE_PAGE_BREAK)
                || msg.equals(YassRow.UNCOMMON_PAGE_BREAK)
                || msg.equals(YassRow.FILE_FOUND)
                || msg.equals(YassRow.NO_COVER_LABEL)
                || msg.equals(YassRow.NO_BACKGROUND_LABEL)
                || msg.equals(YassRow.NO_VIDEO_LABEL)
                || msg.equals(YassRow.WRONG_VIDEOGAP)
                || msg.equals(YassRow.TRANSPOSED_NOTES)
                || msg.equals(YassRow.INVALID_NOTE_LENGTH)
                || msg.equals(YassRow.MISSING_TAG)
                || msg.equals(YassRow.NOTES_TOUCHING)
                || msg.equals(YassRow.NONZERO_FIRST_BEAT);
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectAllSafe(YassTable table) {
        int n = 0;
        boolean changed = true;
        boolean changedAny = false;
        while (changed && n++ < 20) {
            if (!checkData(table, true, true)) {
                return changedAny;
            }

            changed = false;
            for (int i = 0; i < YassRow.ALL_MESSAGES.length; i++) {
                if (isAutoCorrectionSafe(YassRow.ALL_MESSAGES[i])) {
                    if (autoCorrect(table, true, YassRow.ALL_MESSAGES[i])) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /**
     * Description of the Method
     *
     * @param table      Description of the Parameter
     * @param withMinors Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectAllPageBreaks(YassTable table, boolean withMinors) {
        // quick hack; should loop until no change is made
        if (!checkData(table, false, true)) {
            return false;
        }

        boolean match = false;
        int n = 1;
        boolean changed = true;
        boolean changedAny = true;
        while (changed && n++ < 20) {
            for (int i = 0; i < YassRow.ALL_MESSAGES.length; i++) {

                match = isAutoCorrectionPageBreak(YassRow.ALL_MESSAGES[i]);
                if (!match) {
                    match = withMinors
                            && isAutoCorrectionMinorPageBreak(YassRow.ALL_MESSAGES[i]);
                }

                if (match) {
                    changed = false;
                    if (autoCorrect(table, true, YassRow.ALL_MESSAGES[i])) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectTransposed(YassTable table) {
        if (!checkData(table, false, true)) {
            return false;
        }

        boolean changed = autoCorrect(table, true, YassRow.TRANSPOSED_NOTES);
        if (changed) {
            table.addUndo();
            ((YassTableModel) table.getModel()).fireTableDataChanged();
            return true;
        }

        return false;
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectAllFileNames(YassTable table) {
        // quick hack; should loop until no change is made
        int n = 1;
        boolean changed = true;
        boolean changedAny = false;
        while (changed && n++ < 10) {
            changed = false;
            for (int i = 0; i < YassRow.ALL_MESSAGES.length; i++) {
                if (isAutoCorrectionFileName(YassRow.ALL_MESSAGES[i])) {
                    if (!checkData(table, true, true)) {
                        return changedAny;
                    }
                    if (autoCorrect(table, true, YassRow.ALL_MESSAGES[i])) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectSpacing(YassTable table) {
        // quick hack; should loop until no change is made
        int n = 1;
        boolean changed = true;
        boolean changedAny = false;
        while (changed && n++ < 10) {
            changed = false;
            for (int i = 0; i < YassRow.ALL_MESSAGES.length; i++) {
                if (isAutoCorrectionSpacing(YassRow.ALL_MESSAGES[i])) {
                    if (!checkData(table, false, true)) {
                        return changedAny;
                    }
                    if (autoCorrect(table, true, YassRow.ALL_MESSAGES[i])) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectTags(YassTable table) {
        // quick hack; should loop until no change is made
        int n = 1;
        boolean changed = true;
        boolean changedAny = false;
        while (changed && n++ < 10) {
            changed = false;
            for (int i = 0; i < YassRow.ALL_MESSAGES.length; i++) {
                if (isAutoCorrectionTags(YassRow.ALL_MESSAGES[i])) {
                    if (!checkData(table, true, true)) {
                        return changedAny;
                    }
                    if (autoCorrect(table, true, YassRow.ALL_MESSAGES[i])) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    // return percentage of filled screen width

    // negative for text that doesn't fit on screen

    /**
     * Description of the Method
     *
     * @param table Description of the Parameter
     * @param msg   Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrectAllSafe(YassTable table, Vector<?> msg) {
        int n = 0;
        boolean changed = true;
        boolean changedAny = false;
        while (changed && n++ < 20) {
            if (!checkData(table, true, true)) {
                return changedAny;
            }

            changed = false;
            for (Enumeration<?> e = msg.elements(); e.hasMoreElements(); ) {
                String m = (String) e.nextElement();
                if (isAutoCorrectionSafe(m)) {
                    if (autoCorrect(table, true, m)) {
                        changed = true;
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /**
     * Description of the Method
     *
     * @param table              Description of the Parameter
     * @param checkHeaderAndText Description of the Parameter
     * @param checkExtensive     Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean checkData(YassTable table, boolean checkHeaderAndText,
                             boolean checkExtensive) {
        table.resetMessages();

        String fixString = prop.getProperty("correct-uncommon-pagebreaks-fix");
        FIXED_PAGE_BREAK = fixString != null ? new Integer(fixString)
                .intValue() : 0;

        YassRow r = null;
        try {
            YassTableModel tm = (YassTableModel) table.getModel();
            Vector<?> data = tm.getData();
            int n = data.size();
            String dir = table.getDir();
            boolean inHeader = true;
            int lastTagPos = -1;
            int tagPos = -1;

            int durationNormal = 0;

            int durationGolden = 0;

            YassRow firstnormal = null;
            YassRow firstnote = null;
            YassRow firstgolden = null;
            YassRow lastnote = null;
            boolean end = false;
            boolean firstonpage = true;

            String freestyleCountsString = (String) prop
                    .get("freestyle-counts");
            boolean freestyleCounts = freestyleCountsString != null
                    && freestyleCountsString.equals("true");
            String touchingSyllablesString = (String) prop
                    .get("touching-syllables");
            boolean touchingSyllables = touchingSyllablesString != null
                    && touchingSyllablesString.equals("true");

            for (int i = 0; i < n; i++) {
                r = table.getRowAt(i);
                // @bug: shouldn't remove YassTable.addRow()-Messages (will
                // recheck them anyway)
                r.removeAllMessages();

                String type = r.getType();
                if (YassRow.getValidLines().indexOf(type) < 0) {
                    r.addMessage(YassRow.INVALID_LINE);
                    table.addMessage(YassRow.INVALID_LINE);
                }

                if (r.isEnd() && r.getComment().length() > 0) {
                    r.addMessage(YassRow.COMMENT_AFTER_END);
                    table.addMessage(YassRow.COMMENT_AFTER_END);
                }

                boolean isComment = r.isComment();
                if (inHeader && !isComment) {
                    inHeader = false;
                }

                if (isComment) {
                    if (!checkHeaderAndText) {
                        continue;
                    }

                    String tag = r.getCommentTag();
                    int tagLength = tag.length();
                    int commentLength = r.getComment().length();

                    if (tagLength < 1 && commentLength < 1) {
                        r.addMessage(YassRow.EMPTY_LINE);
                        table.addMessage(YassRow.EMPTY_LINE);
                        continue;
                    }
                    tagPos = YassRow.getValidTags().indexOf(
                            " " + tag.substring(0, tagLength - 1) + " ");
                    if (commentLength > 0 && tagPos < 0) {
                        r.addMessage(YassRow.INVALID_TAG);
                        table.addMessage(YassRow.INVALID_TAG);
                    } else if (!inHeader) {
                        r.addMessage(YassRow.OUT_OF_ORDER_COMMENT);
                        table.addMessage(YassRow.OUT_OF_ORDER_COMMENT);
                    } else if (tag.equals("TITLE:")) {
                        YassRow r2 = tm.getCommentRow("MP3:");
                        if (r2 == null) {
                            File mp3 = YassUtils.getFileWithExtension(dir,
                                    null, audioExtensions);
                            if (mp3 != null) {
                                r.addMessage(YassRow.FILE_FOUND,
                                        I18.get("correct_add_tag") + " " + mp3);
                                table.addMessage(YassRow.FILE_FOUND);
                            }
                        }
                        r2 = tm.getCommentRow("COVER:");
                        if (r2 == null) {
                            File co = YassUtils.getFileWithExtension(dir,
                                    coverID, imageExtensions);
                            if (co != null) {
                                r.addMessage(YassRow.FILE_FOUND,
                                        I18.get("correct_add_tag") + " " + co);
                                table.addMessage(YassRow.FILE_FOUND);
                            }
                        }
                        r2 = tm.getCommentRow("BACKGROUND:");
                        if (r2 == null) {
                            File bg = YassUtils.getFileWithExtension(dir,
                                    backgroundID, imageExtensions);
                            if (bg != null) {
                                r.addMessage(YassRow.FILE_FOUND,
                                        I18.get("correct_add_tag") + " " + bg);
                                table.addMessage(YassRow.FILE_FOUND);
                            }
                        }
                        r2 = tm.getCommentRow("VIDEO:");
                        if (r2 == null) {
                            File vd = YassUtils.getFileWithExtension(dir,
                                    videoID, videoExtensions);
                            if (vd != null) {
                                r.addMessage(YassRow.FILE_FOUND,
                                        I18.get("correct_add_tag") + " " + vd);
                                table.addMessage(YassRow.FILE_FOUND);
                            }
                        }
                        r2 = tm.getCommentRow("LANGUAGE:");
                        if (r2 == null) {
                            r.addMessage(YassRow.MISSING_TAG,
                                    I18.get("correct_add_language"));
                            table.addMessage(YassRow.MISSING_TAG);
                        }
                        r2 = tm.getCommentRow("GENRE:");
                        if (r2 == null) {
                            r.addMessage(YassRow.MISSING_TAG,
                                    I18.get("correct_add_genre"));
                            table.addMessage(YassRow.MISSING_TAG);
                        }
                    } else if (tag.equals("MP3:")) {
                        String filename = r.getComment();
                        if (!new File(dir + File.separator + filename).exists()) {
                            File mp3 = YassUtils.getFileWithExtension(dir,
                                    null, audioExtensions);
                            if (mp3 != null) {
                                r.addMessage(YassRow.FILE_FOUND, MessageFormat
                                        .format(I18
                                                        .get("correct_file_not_found"),
                                                filename, mp3));
                                table.addMessage(YassRow.FILE_FOUND);
                            } else {
                                r.addMessage(YassRow.FILE_NOT_FOUND, filename);
                                table.addMessage(YassRow.FILE_NOT_FOUND);
                            }
                        }
                    } else if (tag.equals("COVER:")) {
                        String filename = r.getComment();
                        if (!new File(dir + File.separator + filename).exists()) {
                            File co = YassUtils.getFileWithExtension(dir,
                                    coverID, imageExtensions);
                            if (co != null) {
                                r.addMessage(YassRow.FILE_FOUND, MessageFormat
                                        .format(I18
                                                        .get("correct_file_not_found"),
                                                filename, co));
                                table.addMessage(YassRow.FILE_FOUND);
                            } else {
                                r.addMessage(YassRow.FILE_NOT_FOUND, filename);
                                table.addMessage(YassRow.FILE_NOT_FOUND);
                            }
                        }
                    } else if (tag.equals("BACKGROUND:")) {
                        String filename = r.getComment();
                        if (!new File(dir + File.separator + filename).exists()) {
                            File bg = YassUtils.getFileWithExtension(dir,
                                    backgroundID, imageExtensions);
                            if (bg != null) {
                                r.addMessage(YassRow.FILE_FOUND, MessageFormat
                                        .format(I18
                                                        .get("correct_file_not_found"),
                                                filename, bg));
                                table.addMessage(YassRow.FILE_FOUND);
                            } else {
                                r.addMessage(YassRow.FILE_NOT_FOUND, filename);
                                table.addMessage(YassRow.FILE_NOT_FOUND);
                            }
                        }
                    } else if (tag.equals("VIDEO:")) {
                        String filename = r.getComment();
                        if (!new File(dir + File.separator + filename).exists()) {
                            File vd = YassUtils.getFileWithExtension(dir,
                                    videoID, videoExtensions);
                            if (vd != null) {
                                r.addMessage(YassRow.FILE_FOUND, MessageFormat
                                        .format(I18
                                                        .get("correct_file_not_found"),
                                                filename, vd));
                                table.addMessage(YassRow.FILE_FOUND);
                            } else {
                                r.addMessage(YassRow.FILE_NOT_FOUND, filename);
                                table.addMessage(YassRow.FILE_NOT_FOUND);
                            }
                        } else {
                            String vg = YassUtils
                                    .getWildcard(filename, videoID);
                            double oldvgap = table.getVideoGap();
                            if (vg != null) {
                                vg = vg.replace(',', '.');
                                double vgap = new Double(vg).doubleValue();

                                if (tm.getCommentRow("VIDEOGAP:") == null) {
                                    r.addMessage(
                                            YassRow.WRONG_VIDEOGAP,
                                            MessageFormat.format(
                                                    I18.get("correct_wrong_videogap_1"),
                                                    vgap + ""));
                                    table.addMessage(YassRow.WRONG_VIDEOGAP);
                                } else if (vgap != oldvgap) {
                                    r.addMessage(
                                            YassRow.WRONG_VIDEOGAP,
                                            MessageFormat.format(
                                                    I18.get("correct_wrong_videogap_2"),
                                                    vgap + "", oldvgap + ""));
                                    table.addMessage(YassRow.WRONG_VIDEOGAP);
                                }
                            }
                        }
                    }
                    if (tagPos < lastTagPos) {
                        r.addMessage(YassRow.UNSORTED_COMMENTS);
                        table.addMessage(YassRow.UNSORTED_COMMENTS);
                    }
                    lastTagPos = tagPos;
                } else if (r.isNote()) {
                    lastnote = r;

                    if (firstnote == null) {
                        firstnote = r;
                        int beat = r.getBeatInt();
                        double gap = table.getGap();
                        double bpm = table.getBPM();
                        if (beat != 0) {
                            double ms = beat * (60 * 1000) / (4 * bpm);
                            double newgap = gap + ms;
                            newgap = ((int) (newgap * 100)) / 100.0;
                            r.addMessage(YassRow.NONZERO_FIRST_BEAT,
                                    MessageFormat.format(I18
                                                    .get("correct_nonzero_first_beat"),
                                            beat + "", gap + "", newgap + ""));
                            table.addMessage(YassRow.NONZERO_FIRST_BEAT);
                        }
                    }

                    if (r.isGolden()) {
                        if (firstgolden == null) {
                            firstgolden = r;
                        }
                        durationGolden += r.getLengthInt();
                    } else {
                        if (firstnormal == null) {
                            firstnormal = r;
                        }
                        if (!r.isFreeStyle() || freestyleCounts) {
                            durationNormal += r.getLengthInt();
                        }
                    }

                    if (r.getBeat().length() < 1 || r.getLength().length() < 1
                            || r.getText().length() < 1) {
                        r.addMessage(YassRow.LINE_CUT);
                        table.addMessage(YassRow.LINE_CUT);
                        continue;
                    }
                    if (r.getLengthInt() < 1) {
                        r.addMessage(YassRow.INVALID_NOTE_LENGTH);
                        table.addMessage(YassRow.INVALID_NOTE_LENGTH);
                        continue;
                    }
                    String txt = r.getText();
                    boolean startswithspace = txt
                            .startsWith(YassRow.SPACE + "");
                    boolean endswithspace = txt.endsWith(YassRow.SPACE + "");
                    if (txt.indexOf(YassRow.SPACE + "" + YassRow.SPACE) >= 0) {
                        r.addMessage(YassRow.TOO_MUCH_SPACES);
                        table.addMessage(YassRow.TOO_MUCH_SPACES);
                    } else if (endswithspace) {
                        r.addMessage(YassRow.UNCOMMON_SPACING);
                        table.addMessage(YassRow.UNCOMMON_SPACING);
                    } else if (firstonpage || startswithspace) {
                        if (i > 0) {
                            YassRow r2 = table.getRowAt(i - 1);
                            if (r2.isPageBreak() && i > 1) {
                                r2 = table.getRowAt(i - 2);
                            }
                            if (r2.isNote()) {
                                String txt2 = r2.getText();
                                if (startswithspace
                                        && txt2.endsWith(YassRow.SPACE + "")) {
                                    r.addMessage(YassRow.TOO_MUCH_SPACES);
                                    table.addMessage(YassRow.TOO_MUCH_SPACES);
                                }

                                int beat = r.getBeatInt();
                                int beat2 = r2.getBeatInt() + r2.getLengthInt();
                                if (beat == beat2 && !touchingSyllables
                                        && r2.getLengthInt() > 1) {
                                    r2.addMessage(YassRow.NOTES_TOUCHING);
                                    table.addMessage(YassRow.NOTES_TOUCHING);
                                }
                            } else if (startswithspace) {
                                r.addMessage(YassRow.TOO_MUCH_SPACES);
                                table.addMessage(YassRow.TOO_MUCH_SPACES);
                            }
                        }
                    }
                    if (touchingSyllables) {
                        if (i > 0) {
                            YassRow r2 = table.getRowAt(i - 1);
                            if (r2.isPageBreak() && i > 1) {
                                r2 = table.getRowAt(i - 2);
                            }
                            if (r2.isNote()) {
                                int beat = r.getBeatInt();
                                int beat2 = r2.getBeatInt() + r2.getLengthInt();
                                if (beat == beat2 && r2.getLengthInt() > 1) {
                                    r2.addMessage(YassRow.NOTES_TOUCHING);
                                    table.addMessage(YassRow.NOTES_TOUCHING);
                                }
                            }
                        }
                    }

                    if (checkExtensive) {
                        YassRow r2 = null;
                        if (i > 0) {
                            r2 = table.getRowAt(i - 1);
                        }
                        if (i > 0 && r2.isComment()) {
                            int minH = 128;
                            int maxH = 0;
                            YassRow r3 = null;
                            for (int j = 0; j < n; j++) {
                                r3 = table.getRowAt(j);
                                if (r3.isNote()) {
                                    int height = r3.getHeightInt();
                                    minH = Math.min(minH, height);
                                    maxH = Math.max(maxH, height);
                                }
                            }
                            int range = maxH - minH;
                            if (minH >= 12
                                    || (range <= 48 && (minH < -12 || maxH > 36))) {
                                int minHd = (int) (minH / 12) * 12;
                                int bias = minH - minHd;
                                int newMin = bias;
                                int newMax = maxH - minH + bias;
                                r.addMessage(YassRow.TRANSPOSED_NOTES,
                                        MessageFormat.format(
                                                I18.get("correct_transposed"),
                                                minH, maxH, newMin, newMax));
                                table.addMessage(YassRow.TRANSPOSED_NOTES);
                            }
                        }
                        if (i > 0 && (!(r2.isNote()))) {
                            int ij[] = null;
                            ij = table.enlargeToPages(i, i);
                            StringBuffer sb = new StringBuffer();
                            while (ij[0] <= ij[1]) {
                                r2 = table.getRowAt(ij[0]);
                                if (r2.isNote()) {
                                    sb.append(r2.getText().replace(
                                            YassRow.SPACE, ' '));
                                }
                                ij[0]++;
                            }
                            String s = sb.toString();
                            double percentFree = getPageSpace(s);
                            if (percentFree < 0) {
                                // System.out.println(percentFree + " outside");
                                int pf = -(int) (percentFree * 100);
                                if (pf == 0) {
                                    r.addMessage(
                                            YassRow.TOO_MUCH_TEXT,
                                            MessageFormat.format(
                                                    I18.get("correct_too_much_text_1"),
                                                    prop.getProperty("font-file")));
                                } else {
                                    r.addMessage(
                                            YassRow.TOO_MUCH_TEXT,
                                            MessageFormat.format(
                                                    I18.get("correct_too_much_text_1"),
                                                    prop.getProperty("font-file"),
                                                    pf));
                                }
                                table.addMessage(YassRow.TOO_MUCH_TEXT);
                            }
                        }
                    }

                    int beat = r.getBeatInt();
                    if (i > 0) {
                        YassRow r2 = table.getRowAt(i - 1);
                        if (r2.isNote()) {
                            int beat2 = r2.getBeatInt();
                            int dur2 = r2.getLengthInt();
                            if (beat2 > beat) {
                                r.addMessage(YassRow.OUT_OF_ORDER);
                                table.addMessage(YassRow.OUT_OF_ORDER);
                            } else if (beat2 + dur2 > beat) {
                                r.addMessage(YassRow.NOTES_OVERLAP);
                                table.addMessage(YassRow.NOTES_OVERLAP);
                            }
                        }
                    }
                    firstonpage = false;
                } // check & autocorrect EARLY, LATE, UNCOMMON, OVERLAPPING page
                // breaks
                else if (r.isPageBreak()) {
                    int beat = r.getBeatInt();

                    YassRow r2 = (i > 0) ? table.getRowAt(i - 1) : null;
                    YassRow r3 = (i < n - 1) ? table.getRowAt(i + 1) : null;

                    if (r2 != null && r3 != null) {
                        int beat2 = r.getSecondBeatInt();
                        int comm[] = new int[]{0, 0};
                        if (r2.isNote()) {
                            comm[0] = r2.getBeatInt() + r2.getLengthInt();
                        }
                        if (r3.isNote()) {
                            comm[1] = r3.getBeatInt();
                            firstonpage = true;
                        }
                        if (comm[0] != 0 && comm[1] != 0) {
                            if (beat < comm[0] || beat2 > comm[1]) {
                                r.addMessage(YassRow.PAGE_OVERLAP);
                                table.addMessage(YassRow.PAGE_OVERLAP);
                            }
                            boolean early = getPause(comm[0], beat,
                                    table.getBPM()) < .05;
                            boolean late = getPause(beat2, comm[1],
                                    table.getBPM()) < .05;

                            double ms[] = new double[2];
                            int ptype = getCommonPageBreak(comm,
                                    table.getBPM(), ms);
                            boolean canchange = (ptype >= 0)
                                    && (beat != comm[0] || beat2 != comm[1]);

                            String key = FIXED_PAGE_BREAK > 0 ? "correct_pause_fix_"
                                    + ptype
                                    : "correct_pause_" + ptype;
                            String details = ptype < 0 ? "" : MessageFormat
                                    .format(I18.get(key),
                                            new Double(((int) (ms[0] * 100)) / 100.0),
                                            new Double(((int) (ms[1] * 100)) / 100.0), FIXED_PAGE_BREAK);

                            if (early && canchange) {
                                r.addMessage(YassRow.EARLY_PAGE_BREAK, details);
                                table.addMessage(YassRow.EARLY_PAGE_BREAK);
                            } else if (late && canchange) {
                                r.addMessage(YassRow.LATE_PAGE_BREAK, details);
                                table.addMessage(YassRow.LATE_PAGE_BREAK);
                            } else if (canchange) {
                                r.addMessage(YassRow.UNCOMMON_PAGE_BREAK,
                                        details);
                                table.addMessage(YassRow.UNCOMMON_PAGE_BREAK);
                            }
                        }
                    }
                } else if (r.isEnd()) {
                    end = true;
                }
            }

            if (!end) {
                table.addMessage(YassRow.MISSING_END);
                if (lastnote != null) {
                    lastnote.addMessage(YassRow.MISSING_END);
                }
            }

            int idealGoldenPoints = Integer.parseInt(prop.getProperty("max-golden"));
            int maxPoints = Integer.parseInt(prop.getProperty("max-points"));
            String goldenVarianceString = prop.getProperty("golden-allowed-variance");
            int goldenVariance = goldenVarianceString != null ? Integer.parseInt(goldenVarianceString)
                    : 0;
            maxPoints += idealGoldenPoints;

            // The maximum score is 10.000. Maximum phrase bonus is 1.000. Notes span one or more beats, points are given per beat.
            // Golden notes give double points. Normally you want the maximum golden notes to be near about 1.000.
            // That defines this formula:
            // Golden / MaxScore = 2*SumOfGoldenBeats / (SumOfNormalBeats + 2*SumOfGoldenBeats)
            // Example: Golden=1000, MaxScore=8000, TotalBeats=100 --> SumOfGoldenBeats=6

            int idealGoldenBeats = (int) Math.round(idealGoldenPoints * durationNormal / (2 * maxPoints - 2 * idealGoldenPoints));

            int goldenPoints = durationNormal + 2 * durationGolden > 0 ? (int) Math.round(maxPoints * 2 * durationGolden / (durationNormal + 2 * durationGolden)) : 0;

            String diff = idealGoldenBeats > durationGolden ? "+"
                    + (idealGoldenBeats - durationGolden) : ""
                    + (idealGoldenBeats - durationGolden);

            if (Math.abs(goldenPoints - idealGoldenPoints) > goldenVariance) {
                String key = "correct_golden";
                String details = MessageFormat.format(I18.get(key), "" + idealGoldenPoints, ""
                        + goldenPoints, "" + idealGoldenBeats, "" + durationGolden, diff);

                if (firstgolden == null) {
                    firstgolden = firstnormal;
                }
                if (firstgolden != null) {
                    firstgolden.addMessage(YassRow.UNCOMMON_GOLDEN, details);
                    table.addMessage(YassRow.UNCOMMON_GOLDEN);
                }
            }
            table.setGoldenPoints(goldenPoints, idealGoldenPoints, goldenVariance, durationGolden, idealGoldenBeats, diff);

        } catch (Throwable th) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            JOptionPane.showMessageDialog(
                    JOptionPane.getFrameForComponent(table),
                    "<html>"
                            + MessageFormat.format(
                            I18.get("correct_parse_error_msg"),
                            table.getDir(), th.getMessage(),
                            r.toString(), sw.toString()),
                    I18.get("correct_parse_error_title"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Description of the Method
     *
     * @param table          Description of the Parameter
     * @param all            Description of the Parameter
     * @param currentMessage Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean autoCorrect(YassTable table, boolean all,
                               String currentMessage) {
        // correct messages until data is inserted/removed

        String dir = table.getDir();

        int rows[];
        int n = 0;

        if (all) {
            n = table.getRowCount();
            rows = new int[n];
            for (int i = 0; i < n; i++) {
                rows[i] = i;
            }
        } else {
            rows = table.getSelectedRows();
            n = rows.length;
            Arrays.sort(rows);
        }

        boolean changed = false;
        YassTableModel tm = (YassTableModel) table.getModel();
        Vector<?> data = tm.getData();

        for (int k = 0; k < n; k++) {
            int i = rows[k];
            YassRow r = table.getRowAt(i);
            Vector<?> msg = r.getMessages();
            if (msg == null || msg.size() < 1) {
                continue;
            }
            boolean found = false;
            for (Enumeration<?> en = msg.elements(); en.hasMoreElements()
                    && !found; ) {
                String[] m = (String[]) en.nextElement();
                if (currentMessage.equals(m[0])) {
                    found = true;
                }
            }
            if (!found) {
                continue;
            }

            if (currentMessage.equals(YassRow.UNSORTED_COMMENTS)) {
                sortComments(table);
                return true;
            }
            if (currentMessage.equals(YassRow.TRANSPOSED_NOTES)) {
                int minH = 128;
                int maxH = 0;
                YassRow r3 = null;
                n = table.getRowCount();
                for (int j = 0; j < n; j++) {
                    r3 = table.getRowAt(j);
                    if (r3.isNote()) {
                        int height = r3.getHeightInt();
                        minH = Math.min(minH, height);
                        maxH = Math.max(maxH, height);
                    }
                }
                int minHd = (int) (minH / 12) * 12;
                int bias = minH - minHd;
                for (int j = 0; j < n; j++) {
                    r3 = table.getRowAt(j);
                    if (r3.isNote()) {
                        int height = r3.getHeightInt();
                        height = height - minH + bias;
                        r3.setHeight(height);
                    }
                }
                return true;
            }
            if (currentMessage.equals(YassRow.NONZERO_FIRST_BEAT)) {
                double gap = table.getGap();
                double bpm = table.getBPM();
                int beat = r.getBeatInt();

                double ms = beat * (60 * 1000) / (4 * bpm);

                gap = gap + ms;
                gap = ((int) (gap * 100)) / 100.0;
                table.setGap(gap);

                YassRow r3 = null;
                n = table.getRowCount();
                for (int j = 0; j < n; j++) {
                    r3 = table.getRowAt(j);
                    if (r3.isNote()) {
                        int beat3 = r3.getBeatInt();
                        beat3 = beat3 - beat;
                        r3.setBeat(beat3);
                    }
                }
                return true;
            }
            if (currentMessage.equals(YassRow.EMPTY_LINE)) {
                tm.getData().remove(r);
                return true;
            } else if (currentMessage.equals(YassRow.FILE_FOUND)) {
                String tag = r.getCommentTag();

                boolean isTitle = tag.equals("TITLE:");
                if (isTitle || tag.equals("MP3:")) {
                    File f = YassUtils.getFileWithExtension(dir, null,
                            audioExtensions);
                    if (f != null) {
                        table.setMP3(f.getName());
                        changed = true;
                    }
                }
                if (isTitle || tag.equals("COVER:")) {
                    File f = YassUtils.getFileWithExtension(dir, coverID,
                            imageExtensions);
                    if (f != null) {
                        table.setCover(f.getName());
                        changed = true;
                    }
                }
                if (isTitle || tag.equals("BACKGROUND:")) {
                    File f = YassUtils.getFileWithExtension(dir, backgroundID,
                            imageExtensions);
                    if (f != null) {
                        table.setBackground(f.getName());
                        changed = true;
                    }
                }
                if (isTitle || tag.equals("VIDEO:")) {
                    File f = YassUtils.getFileWithExtension(dir, videoID,
                            imageExtensions);
                    if (f != null) {
                        table.setVideo(f.getName());
                        changed = true;
                    }
                }
                if (isTitle) {
                    n = data.size();
                }
                return true;
            } else if (currentMessage.equals(YassRow.MISSING_TAG)) {
                YassRow r2 = tm.getCommentRow("LANGUAGE:");
                if (r2 == null) {
                    table.setLanguage("Other");
                    changed = true;
                }
                r2 = tm.getCommentRow("GENRE:");
                if (r2 == null) {
                    table.setGenre("Other");
                    changed = true;
                }
                if (changed) {
                    return true;
                }
            } else if (currentMessage.equals(YassRow.WRONG_VIDEOGAP)) {
                // msg set on video tag, so comment is filename
                String filename = r.getComment();
                String vg = YassUtils.getWildcard(filename, videoID);
                if (vg != null) {
                    table.setVideoGap(vg);
                }
                return true;
            } else if (currentMessage.equals(YassRow.UNCOMMON_SPACING)) {
                if (r.isNote()) {
                    String txt = r.getText();
                    if (txt.endsWith(YassRow.SPACE + "")) {
                        r.setText(txt.substring(0, txt.length() - 1));
                        if (i + 1 < n) {
                            YassRow r2 = table.getRowAt(i + 1);
                            if (r2.isNote()) {
                                r2.setText(YassRow.SPACE + r2.getText());
                            }
                        }
                        changed = true;
                    }
                }
            } else if (currentMessage.equals(YassRow.INVALID_NOTE_LENGTH)) {
                if (r.isNote()) {
                    r.setLength(1);
                    changed = true;
                }
            } else if (currentMessage.equals(YassRow.NOTES_TOUCHING)) {
                if (r.isNote()) {
                    int len = r.getLengthInt();
                    if (len > 1) {
                        r.setLength(len - 1);
                        changed = true;
                    }
                }
            } else if (currentMessage.equals(YassRow.TOO_MUCH_SPACES)) {
                if (r.isNote()) {
                    String txt = r.getText();
                    int j = 0;
                    while ((j = txt.indexOf(YassRow.SPACE + "" + YassRow.SPACE)) > 0) {
                        txt = txt.substring(0, j) + txt.substring(j + 1);
                    }
                    while (txt.startsWith(YassRow.SPACE + "" + YassRow.SPACE)) {
                        txt = txt.substring(1);
                    }
                    if (txt.startsWith(YassRow.SPACE + "") && i > 0) {
                        YassRow r2 = table.getRowAt(i - 1);
                        if (!(r2.isNote())) {
                            txt = txt.substring(1);
                        }
                    }
                    r.setText(txt);
                    changed = true;
                }
            } else if (currentMessage.equals(YassRow.OUT_OF_ORDER)) {
                int beat = r.getBeatInt();
                int j = i - 1;
                int beat2 = table.getRowAt(j).getBeatInt();
                while (beat > beat2) {
                    beat2 = table.getRowAt(--j).getBeatInt();
                }
                r.setBeat(beat2 + table.getRowAt(j).getLengthInt());
                changed = true;
            } else if (currentMessage.equals(YassRow.PAGE_OVERLAP)
                    || currentMessage.equals(YassRow.EARLY_PAGE_BREAK)
                    || currentMessage.equals(YassRow.LATE_PAGE_BREAK)
                    || currentMessage.equals(YassRow.UNCOMMON_PAGE_BREAK)) {

                int comm[] = new int[2];
                comm[0] = table.getRowAt(i - 1).getBeatInt()
                        + table.getRowAt(i - 1).getLengthInt();
                comm[1] = table.getRowAt(i + 1).getBeatInt();
                int pause = getCommonPageBreak(comm, table.getBPM(), null);
                if (pause >= 0) {
                    r.setBeat(comm[0]);
                    r.setSecondBeat(comm[1]);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Description of the Method
     *
     * @param fname Description of the Parameter
     */
    public void loadFont(String fname) {
        fontWidth = new byte[256];
        try {
            InputStream is = getClass().getResourceAsStream(
                    prop.getProperty("font-file"));
            int numRead = is.read(fontWidth, 0, fontWidth.length);
            is.close();
        } catch (Exception e) {
            System.out.println("Font file not found: "
                    + prop.getProperty("font-file"));
            e.printStackTrace();
        }
        // for (int i=0; i<bytes.length; i++)
        // System.out.print( ((char)i)+"="+bytes[i]+" ");
    }

    /**
     * Gets the pageSpace attribute of the YassAutoCorrect object
     *
     * @param s Description of the Parameter
     * @return The pageSpace value
     */
    public double getPageSpace(String s) {
        return (800 - getStringWidth(s)) / 800.0;
        // Render width is 800
        // Screen width might be 600, 800, 1024
    }

    /**
     * Gets the stringWidth attribute of the YassAutoCorrect object
     *
     * @param s Description of the Parameter
     * @return The stringWidth value
     */
    public int getStringWidth(String s) {
        double fontH = fontSize / 10.0;

        char c[] = s.toCharArray();
        int n = c.length;
        int ascii = 0;
        int fw = 0;
        double cw = 0;
        double w = 0;
        double aspectW = 0.95;
        for (int i = 0; i < n; i++) {
            ascii = (int) (c[i]);
            if (ascii > 255) {
                if (ascii >= 8216 && ascii <= 8218) {
                    ascii = (int) '\'';
                }
                // replace right/left single quotation mark
                if (ascii >= 8220 && ascii <= 8222) {
                    ascii = (int) '\'';
                } // replace right/left/low double quotation mark
                else if (ascii == 8230) {
                    ascii = (int) 'w';
                } // replace '...'
                else {
                    ascii = (int) 'W';
                }
                //
            }
            fw = (int) (fontWidth[ascii] / 2) + 2;
            // Oline
            // fw = fontWidth[(int)(c[i])]+1; //Oline2
            cw = fw * fontH * aspectW;
            w += cw;
        }

        w = w + (2 * outline);
        return (int) w;
    }

    /**
     * Sets the fontSize attribute of the YassAutoCorrect object
     *
     * @param size The new fontSize value
     */
    public void setFontSize(int size) {
        fontSize = size;
    }
}
